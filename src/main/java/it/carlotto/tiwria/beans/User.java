package it.carlotto.tiwria.beans;

import it.carlotto.tiwria.exceptions.ObjectNotFoundException;
import it.carlotto.tiwria.utils.ConnectionsHandler;
import jakarta.servlet.UnavailableException;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class User implements Serializable {
    private final int id;
    private final String username;
    private final String name;
    private final String surname;
    private final String address;

    /**
     * This constructor is used by the static DAO functions
     */
    private User(int id, String username, String name, String surname, String address) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.surname = surname;
        if (address!=null)
            this.address = address;
        else this.address = "";
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public String getAddress() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                '}';
    }

    /**
     * This function is used to authenticate a user using username and password.
     * @param username username
     * @param password clear text password
     * @return User bean object
     * @throws ObjectNotFoundException when the user is not found
     * @throws UnavailableException when it's not possible to get a sql connection or
     *          an SQL error happens.
     */
    public static User loadWithCreds(String username, String password) throws ObjectNotFoundException, UnavailableException {
        String query = "SELECT id, username, name, surname, address FROM users WHERE username = ? AND password = ?";

        try {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
        String passwordHash = bytesToHex(digest);

            Connection connection = ConnectionsHandler.getConnection();
            try (
                PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, username);
                preparedStatement.setString(2, passwordHash);
                return loadUsingPS(preparedStatement);
            } catch (SQLException e) {
                throw new UnavailableException("SQL Exception while loading user from DB: " + e.getMessage());
            } finally {
                ConnectionsHandler.returnConnection(connection);
            }

        } catch (NoSuchAlgorithmException e) {
            throw new ObjectNotFoundException("No such algorithm exception: " + e.getMessage());
        }
    }

    /**
     * This function is used to load a user with its id
     * @param id id
     * @return User bean object
     * @throws ObjectNotFoundException when the user is not found
     * @throws UnavailableException when it's not possible to get a sql connection
     *         an SQL error happens
     */
    public static User loadWithId(int id) throws ObjectNotFoundException, UnavailableException {
        String query = "SELECT id, username, name, surname, address FROM users WHERE id = ?";
        Connection connection = ConnectionsHandler.getConnection();

            try (PreparedStatement preparedStatement = connection.prepareStatement(query)
                ){
                preparedStatement.setInt(1, id);
                return loadUsingPS(preparedStatement);
            } catch (SQLException e) {
                throw new UnavailableException("SQL Exception while loading user from DB: " + e.getMessage());
            } finally {
                ConnectionsHandler.returnConnection(connection);
            }
    }

    /**
     * This function is used to load a user with its username
     * @param username username
     * @return User bean object
     * @throws ObjectNotFoundException when the user is not found
     * @throws UnavailableException when it's not possible to get a sql connection or
     *         an SQL error happens.
     */
    public static User loadWithUsername(String username) throws ObjectNotFoundException, UnavailableException {
        String query = "SELECT id, username, name, surname, address FROM users WHERE username = ?";
        Connection connection = ConnectionsHandler.getConnection();

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)
            ){
            preparedStatement.setString(1, username);
            return loadUsingPS(preparedStatement);
        } catch (SQLException e) {
            throw new UnavailableException("SQL Exception while loading user from DB: " + e.getMessage());
        } finally {
            ConnectionsHandler.returnConnection(connection);
        }
    }

    /**
     * This function is used to load a user from the DB using a Prepared statement
     * @param preparedStatement Prepared statement
     * @return User object
     * @throws ObjectNotFoundException when the user is not found
     * @throws SQLException
     */
    private static User loadUsingPS(PreparedStatement preparedStatement) throws SQLException, ObjectNotFoundException {
        try (ResultSet resultSet = preparedStatement.executeQuery()) {
            if (!resultSet.isBeforeFirst()) {  // User not found
                throw new ObjectNotFoundException("User not found");
            }
            resultSet.next();

            return new User(
                    resultSet.getInt("id"),
                    resultSet.getString("username"),
                    resultSet.getString("name"),
                    resultSet.getString("surname"),
                    resultSet.getString("address")
            );
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
