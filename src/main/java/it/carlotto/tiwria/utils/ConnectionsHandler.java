package it.carlotto.tiwria.utils;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.UnavailableException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class will handle connections to the database.
 */
public class ConnectionsHandler implements ServletContextListener {

    // This list will store available connections
    private static final List<ConnectionWithExpiration> connections = new LinkedList<>();

    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private static final long cleanerPeriodSeconds = 5;
    private static final long connectionExpirationSeconds = 30;

    private static final int maxOpenConnections = 10;

    /* This object is used to keep track of connection given to servlets
    and open connections in the list.
    This int is only accessed inside a synchronized(connections) block. */
    private static int openConnections = 0;

    /*
    * Threads in wait state:
    *   - executor calling cleaner, when the list is empty
    *   - servlet calling getConnection() when max amount is reached
    *          (too many connections used by servlets, list is empty)
    *
    * Using notifyAll() when:
    *   - a connection is closed (openConnections is updated), by getConnection() and cleaner()
    *   - a connection is returned (cleaner will have something to do), by returnConnection
    */


    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContextListener.super.contextInitialized(sce);
        executor.scheduleWithFixedDelay(cleaner, cleanerPeriodSeconds, cleanerPeriodSeconds, TimeUnit.SECONDS);
        // WithFixedDelay waits for runnable termination before starting timer
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e){
            executor.shutdownNow();
        }

        synchronized (connections) {
            for (ConnectionWithExpiration c : connections) {
                try {
                    c.getConnection().close();
                } catch (SQLException e) {
                    System.err.println("SQL exception while closing connection: " + e.getMessage());
                }
            }
        }
        ServletContextListener.super.contextDestroyed(sce);
    }

    /**
     * Used by a servlet to request a connection.
     * This function tries to use the first connection in the list (as it's the most recent),
     * if no valid connection is available the function creates a new one.
     * When creating a new connection, the function makes sure
     * the limit of open connections is respected.
     * @return Connection object
     * @throws UnavailableException when the db driver can't be found or a sql error happens
     */
    public static Connection getConnection() throws UnavailableException {
        ConnectionWithExpiration c;

        /* This list is used to store invalid connections found while
        iterating over connections. */
        final List<Connection> toCloseAsync = new LinkedList<>();

        synchronized (connections) {

            if(connections.isEmpty() && // No not-expired connection is available
                openConnections>=maxOpenConnections) {  /* and we cannot create a new connection => we need to wait.
                Here we strongly rely on the fact that after a connection is used it is returned using returnConnection.
                openConnections = connections.size() + [connections used by servlets] => openConnections = [used by servlets] here

                If the list is empty, the only notifyAll() that can be executed is the one in returnConnection,
                so an available connection will be added and the list won't be empty. */

                while (connections.isEmpty()) {  // Exiting the jail as soon as one connection is available
                    try {
                        connections.wait();
                    } catch (InterruptedException e) {
                        throw new UnavailableException("Interrupted exception while waiting to get a connection: " + e.getMessage());
                    }
                }
            }

            while (!connections.isEmpty()) {  // Using not-expired connections if possible
                c = connections.get(0);  // Always using the first element, the most recent one
                connections.remove(c);

                try {
                    if(c.getConnection().isValid(1)) {

                       return c.getConnection();

                    } else {
                        openConnections -= 1;

                        if(toCloseAsync.isEmpty()) {  // Calling notifyAll() only once
                            connections.notifyAll();
                        }

                        toCloseAsync.add(c.getConnection());
                    }  // For loop continues, checking another connection
                } catch (SQLException e) {
                    System.err.println("SQL exception while closing connection: " + e.getMessage());
                }
            }

            /* If execution arrives here, no valid connection is available,
            and:
                - we closed at least one connection OR
                - at least one connection can be created without exceeding maximum allowed.
            So we need a new connection.*/
            openConnections += 1;

        }  // Releasing lock

            // Scheduling asyncCloser if needed
            if (!toCloseAsync.isEmpty()) {
                executor.submit(new asyncCloser(toCloseAsync));
            }

            try {
                return newConnection();
            } catch (SQLException e) {
                synchronized (connections) {openConnections -=1;}
                throw new UnavailableException(e.getMessage());
            }
    }

    /**
     * Used by a servlet when a connection is no longer needed.
     * The function creates a new ConnectionWithExpiration and
     * adds the connection to the list of available ones.
     * @param connection connection no longer needed
     */
    public static void returnConnection(Connection connection) {
        final ConnectionWithExpiration c =
                new ConnectionWithExpiration(connection,
                        Timestamp.from(Instant.now().plusSeconds(connectionExpirationSeconds)));
        synchronized (connections) {
            connections.add(0, c);  // Always adding as first element
            connections.notifyAll();
        }
    }

    /**
     * This function is used to create a new connection to the DB
     * @return Connection object
     * @throws SQLException when the db driver can't be found or a sql error happens
     */
    private static Connection newConnection() throws SQLException {
        final Connection connection;
        try {
            // Testing if the db driver is available
            String driver = "com.mysql.cj.jdbc.Driver";
            Class.forName(driver);

            String url = System.getenv("dbURL");
            String user = System.getenv("dbUser");
            String pass = System.getenv("dbPassword");
            connection = DriverManager.getConnection(url, user, pass);

            if (connection==null) {
                throw new SQLException("Newly obtained connection is null");
            }
            return connection;
        } catch (ClassNotFoundException e) {
            throw new SQLException("db driver not found");
        }
    }

    /**
     * This function performs clean-ups on the list of connections,
     * it iterates starting from the bottom of the list and closes
     * expired connections.
     * When the list is empty, the execution is suspended.
     */
    private static final Runnable cleaner = () -> {
        final List<Connection> expiredConnections = new LinkedList<>();
        boolean foundNotExpired=false;  // True when the first not expired connection is found
        ConnectionWithExpiration c;

        synchronized (connections) {

            while(connections.isEmpty()) {  // Pausing the execution if there is nothing to clean
                try {
                    connections.wait();
                } catch (InterruptedException e) {
                    System.err.println("Interrupted exception while pausing the cleaner: " + e.getMessage());
                    return;
                }
            }

            while(!connections.isEmpty() && !foundNotExpired) {
                c = connections.get(connections.size()-1);  // Obtaining the oldest connection
                if(c.isExpired()) {
                    connections.remove(c);

                    openConnections -= 1;
                    if (expiredConnections.isEmpty()) {  // Calling notifyAll() only once
                        connections.notifyAll();
                    }

                    expiredConnections.add(c.getConnection());  // Copying in a local list
                } else {
                    foundNotExpired=true;
                }
            }
        }

        // Closing connections
        if(!expiredConnections.isEmpty())
            executor.submit(new asyncCloser(expiredConnections));
    };

}

/**
 * This class is used to wrap together a connection and
 * its expiration timestamp.
 */
class ConnectionWithExpiration {
    private final Connection connection;
    private final Timestamp expiration;

    ConnectionWithExpiration(Connection connection, Timestamp expiration) {
        this.connection = connection;
        this.expiration = expiration;
    }

    public Connection getConnection() {
        return connection;
    }

    public boolean isExpired() {
        return expiration.before(Timestamp.from(Instant.now()));
    }
}

/**
 * This runnable is used to close invalid connections.
 * When calling getConnection(), it is used to not make
 * the servlets wait.
 * The Runnable cleaner uses this function too.
 */
class asyncCloser implements Runnable {
    private final List<Connection> toCloseAsync;

    public asyncCloser(List<Connection> toCloseAsync) {
        this.toCloseAsync = toCloseAsync;
    }
    public void run() {
        for (Connection c : toCloseAsync) {
            try {
                if (c.isValid(1)) {
                    c.close();
                }
            } catch (SQLException e) {
                System.err.println("SQL exception while closing connection: " + e.getMessage());
            }
        }
    }
}