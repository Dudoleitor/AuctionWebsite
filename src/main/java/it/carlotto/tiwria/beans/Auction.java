package it.carlotto.tiwria.beans;

import it.carlotto.tiwria.exceptions.InsertIntoDBFailedException;
import it.carlotto.tiwria.exceptions.ObjectNotFoundException;
import it.carlotto.tiwria.utils.ConnectionsHandler;
import jakarta.servlet.UnavailableException;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Auction {
    private final int id;
    private final int creator_user_id;
    private final String creator_user_username;
    private final Timestamp created_at;
    private final Timestamp terminates_at;
    private final boolean closed_by_user;
    private final int minimum_bid_wedge;

    private Auction(int id, int creator_user_id, String creator_user_username, Timestamp created_at, Timestamp terminates_at, boolean closed_by_user, int minimum_bid_wedge) {
        this.id = id;
        this.creator_user_id = creator_user_id;
        this.creator_user_username = creator_user_username;
        this.created_at = created_at;
        this.terminates_at = terminates_at;
        this.closed_by_user = closed_by_user;
        this.minimum_bid_wedge = minimum_bid_wedge;
    }

    /**
     * Copy constructor
     * @param auction another auction to clone
     */
    public Auction(Auction auction) {
        this.id = auction.id;
        this.creator_user_id = auction.creator_user_id;
        this.creator_user_username = auction.creator_user_username;
        this.created_at = auction.created_at;
        this.terminates_at = auction.terminates_at;
        this.closed_by_user = auction.closed_by_user;
        this.minimum_bid_wedge = auction.minimum_bid_wedge;
    }

    public int getId() {
        return id;
    }

    public int getCreator_user_id() {
        return creator_user_id;
    }

    public String getCreator_user_username() {
        return creator_user_username;
    }

    public Timestamp getCreated_at() {
        return created_at;
    }

    public Timestamp getTerminates_at() {
        return terminates_at;
    }

    public LocalDateTime getTerminates_at_datetime() {
        return getTerminates_at().toLocalDateTime();
    }

    public String calculate_delta_to_termination(LocalDateTime otherDate) {
        long delta = otherDate.until(terminates_at.toLocalDateTime(), ChronoUnit.HOURS);
        return delta / 24 + "d " + delta % 24 + "h";
    }

    public boolean isClosed_by_user() {
        return closed_by_user;
    }

    public int getMinimum_bid_wedge() {
        return minimum_bid_wedge;
    }

    public boolean isOpen() {
        return terminates_at.after(Timestamp.from(Instant.now()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Auction auction = (Auction) o;
        return id == auction.id;
    }

    @Override
    public String toString() {
        return "Auction{" +
                "id=" + id +
                ", creator_user_id=" + creator_user_id +
                ", created_at=" + created_at +
                ", terminates_at=" + terminates_at +
                ", closed_by_user=" + closed_by_user +
                ", minimum_bid_wedge=" + minimum_bid_wedge +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * This function is used to load a single auction.
     * @param id ID of the auction
     * @return Auction object
     * @throws ObjectNotFoundException when no auction with the given
     *      id exists in the database
     * @throws UnavailableException when it's not possible to get a SQL connection
     *      or an SQL error happens.
     */
    public static Auction getAuctionFromId(int id) throws ObjectNotFoundException, UnavailableException {
        String query = "SELECT au.id, au.creator_user_id, u.username, au.created_at, au.terminates_at, au.closed_by_user, au.minimum_bid_wedge " +
                "FROM auctions as au JOIN users as u ON au.creator_user_id = u.id " +
                "WHERE au.id = ?";

        Connection connection = ConnectionsHandler.getConnection();

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)
        ){
            preparedStatement.setInt(1, id);
            List<Auction> auctions = loadAuctionsUsingPS(preparedStatement);
            if(auctions.isEmpty()) {  // Auction not found
                throw new ObjectNotFoundException("Auction not found");
            }
            return auctions.get(0);
        } catch (SQLException e) {
            throw new UnavailableException("SQL Exception while loading auction from DB: " + e.getMessage());
        } finally {
            ConnectionsHandler.returnConnection(connection);
        }
    }

    /**
     * This function is used to load the complete list of auctions.
     * The list is ordered using terminates_at in descending order.
     * @param onlyOpen if true, auctions with terminates_at > CURRENT_TIMESTAMP()
     *                 are omitted.
     * @return List of auctions
     * @throws UnavailableException when it's not possible to get a SQL connection
     *      or an SQL error happens.
     */
    public static List<Auction> getAuctions(boolean onlyOpen) throws UnavailableException {
        String query = "SELECT au.id, au.creator_user_id, u.username, au.created_at, au.terminates_at, au.closed_by_user, au.minimum_bid_wedge " +
                "FROM auctions as au JOIN users as u ON au.creator_user_id = u.id";

        if (onlyOpen)
            query = query + " WHERE au.terminates_at > CURRENT_TIMESTAMP()";

        query += " ORDER BY au.terminates_at DESC";
        Connection connection = ConnectionsHandler.getConnection();

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)
            ){
            return loadAuctionsUsingPS(preparedStatement);
        } catch (SQLException e) {
            throw new UnavailableException("SQL Exception while loading auctions from DB: " + e.getMessage());
        } finally {
            ConnectionsHandler.returnConnection(connection);
        }
    }

    /**
     * This function is used to load auctions given a list of ids.
     * The list is ordered using terminates_at in descending order.
     * @param ids List Integers, id of the auctions to load
     * @param onlyOpen if true, auctions with terminates_at > CURRENT_TIMESTAMP()
     *                 are omitted.
     * @return List of auctions
     * @throws UnavailableException when it's not possible to get a SQL connection
     *      or an SQL error happens.
     */
    public static List<Auction> getAuctionsFromIds(List<Integer> ids, boolean onlyOpen) throws UnavailableException {
        final List<Auction> auctionList = new ArrayList<>();
        String query = "SELECT au.id, au.creator_user_id, u.username, au.created_at, au.terminates_at, au.closed_by_user, au.minimum_bid_wedge " +
                "FROM auctions as au JOIN users as u ON au.creator_user_id = u.id " +
                "WHERE au.id = ?";

        if (onlyOpen)
            query = query + " AND au.terminates_at > CURRENT_TIMESTAMP()";

        query += " ORDER BY au.terminates_at DESC";
        Connection connection = ConnectionsHandler.getConnection();

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)
        ){
            for(int id : ids) {
                preparedStatement.setInt(1, id);
                auctionList.addAll(
                        loadAuctionsUsingPS(preparedStatement));
            }

            return auctionList;
        } catch (SQLException e) {
            throw new UnavailableException("SQL Exception while loading auctions from DB: " + e.getMessage());
        } finally {
            ConnectionsHandler.returnConnection(connection);
        }
    }

    /**
     * This function is used to load the list of auctions created by a user.
     * The list is ordered using terminates_at in ascending order.
     * @param userId Integer id of the user
     * @param openVsClosed if false auctions with closed_by_user=0 are selected,
     *                     if true auctions with closed_by_user=1 are selected
     * @return List of auctions
     * @throws UnavailableException when it's not possible to get a SQL connection
     *      or an SQL error happens.
     */
    public static List<Auction> getAuctionsPerUser(int userId, boolean openVsClosed) throws UnavailableException {
        String query = "SELECT au.id, au.creator_user_id, u.username, au.created_at, au.terminates_at, au.closed_by_user, au.minimum_bid_wedge " +
                "FROM auctions as au JOIN users as u ON au.creator_user_id = u.id " +
                "WHERE au.creator_user_id = ?";

        if (openVsClosed)  // True -> Closed | False -> open/notClosed
            query = query + " AND au.closed_by_user = 1";
        else
            query = query + " AND au.closed_by_user = 0";

        query += " ORDER BY au.terminates_at ASC";
        Connection connection = ConnectionsHandler.getConnection();

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)
        ){
            preparedStatement.setInt(1, userId);
            return loadAuctionsUsingPS(preparedStatement);
        } catch (SQLException e) {
            throw new UnavailableException("SQL Exception while loading auctions from DB: " + e.getMessage());
        } finally {
            ConnectionsHandler.returnConnection(connection);
        }
    }

    /**
     * This function is used to load the list of auctions won by
     * a specific user.
     * The list is ordered using terminates_at in descending order.
     * @param userId Int id of the winning user
     * @return List of auctions
     * @throws UnavailableException when it's not possible to get a SQL connection
     *      or an SQL error happens.
     */
    public static List<Auction> getAuctionsWonByUser(int userId) throws UnavailableException {
        String query = "SELECT au.id, au.creator_user_id, u.username, au.created_at, au.terminates_at, au.closed_by_user, au.minimum_bid_wedge " +

                "FROM auctions as au JOIN users as u ON au.creator_user_id = u.id " +  // Joining to get the AUTHOR of the auction
                "JOIN bids as b1 ON b1.auction_id = au.id " +  // Joining to get the bids for each auction
                "WHERE au.terminates_at < CURRENT_TIMESTAMP() " +  // Considering only terminated auctions
                "AND b1.bidder_user_id = ? " +  // Considering only bids by the selected user

                "AND b1.amount = (SELECT MAX(b2.amount) FROM bids as b2 WHERE b2.auction_id = au.id)";
                /* Filtering:
                keeping only bids where the user placed the highest bid (aka won)*/

        query += " ORDER BY au.terminates_at DESC";
        Connection connection = ConnectionsHandler.getConnection();

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)
        ){
            preparedStatement.setInt(1, userId);
            return loadAuctionsUsingPS(preparedStatement);
        } catch (SQLException e) {
            throw new UnavailableException("SQL Exception while loading auctions from DB: " + e.getMessage());
        } finally {
            ConnectionsHandler.returnConnection(connection);
        }
    }

    /**
     * This function is used to load the list of auctions containing a specific
     * expression in the name or in the description of an article.
     * The list is ordered using terminates_at in descending order.
     * @param expression String with the expression to search for
     * @param onlyOpen if true, auctions with terminates_at > CURRENT_TIMESTAMP()
     *                 are omitted.
     * @return List of auctions
     * @throws UnavailableException when it's not possible to get a SQL connection
     *      or an SQL error happens.
     */
    public static List<Auction> getAuctionsWithSearch(String expression, boolean onlyOpen) throws UnavailableException {
        String query = "SELECT au.id, au.creator_user_id, u.username, au.created_at, au.terminates_at, au.closed_by_user, au.minimum_bid_wedge " +
                "FROM auctions as au JOIN users as u ON au.creator_user_id = u.id " +
                "JOIN articles as ar ON ar.auction_id = au.id " +
                "WHERE (ar.name LIKE ? OR ar.description LIKE ?)";

        if (onlyOpen)
            query += " AND au.terminates_at > CURRENT_TIMESTAMP()";

        query += " ORDER BY au.terminates_at DESC";
        Connection connection = ConnectionsHandler.getConnection();

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)
        ){
            expression = "%" + expression + "%";
            preparedStatement.setString(1, expression);
            preparedStatement.setString(2, expression);
            return loadAuctionsUsingPS(preparedStatement);
        } catch (SQLException e) {
            throw new UnavailableException("SQL Exception while loading auctions from DB: " + e.getMessage());
        } finally {
            ConnectionsHandler.returnConnection(connection);
        }
    }

    /**
     * This function is used to load a list of Auctions after a PreparedStatement
     * has been prepared.
     * Note for the SQL query: auction table AS au, user table AS u.
     * @param preparedStatement PreparedStatement to execute the SQL query.
     * @return List of Auction object
     * @throws SQLException
     */
    private static List<Auction> loadAuctionsUsingPS(PreparedStatement preparedStatement) throws SQLException {
        List<Auction> auctionList = new ArrayList<>();
        try (ResultSet resultSet = preparedStatement.executeQuery()
        ){
            while(resultSet.next()) {
                auctionList.add(new Auction(
                        resultSet.getInt("au.id"),
                        resultSet.getInt("au.creator_user_id"),  // Id of the creator
                        resultSet.getString("u.username"),  // Username of the creator
                        resultSet.getTimestamp("au.created_at"),
                        resultSet.getTimestamp("au.terminates_at"),
                        resultSet.getBoolean("au.closed_by_user"),
                        resultSet.getInt("au.minimum_bid_wedge")
                ));
            }
        }
        return auctionList;
    }

    /**
     * This function loads the amount of the maximum bid for
     * the given auction (identified by the ID).
     * @param auction_id Int ID of the auction
     * @return float amount of the last bid, 0 if
     *          no bid was placed
     * @throws UnavailableException when it's not possible to get a SQL connection
     *      or an SQL error happens.
     */
    public static float getMaxBidFromAuctionId(int auction_id) throws UnavailableException {
        String query = "SELECT MAX(amount) FROM bids WHERE auction_id = ?";
        Connection connection = ConnectionsHandler.getConnection();

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)
            ){
            preparedStatement.setInt(1, auction_id);
            try(ResultSet resultSet = preparedStatement.executeQuery()) {
                if(!resultSet.isBeforeFirst()) {  // No bid found
                    return 0;
                }
                resultSet.next();

                return resultSet.getFloat(1);
            }

        } catch (SQLException e) {
            throw new UnavailableException("SQL Exception while loading max bid amount from DB: " + e.getMessage());
        } finally {
            ConnectionsHandler.returnConnection(connection);
        }
    }

    /**
     * This function is used to close an auction.
     * Note: does not check if the auction is terminated!
     * @param auctionId ID of the auction to close
     * @throws UnavailableException when it's not possible to get a SQL connection
     *      or an SQL error happens.
     */
    public static void closeAuction(int auctionId) throws UnavailableException {
        String query = "UPDATE auctions SET closed_by_user=1 WHERE id=?";
        Connection connection = ConnectionsHandler.getConnection();

        try(PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, auctionId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new UnavailableException("SQL Exception while closing auction in DB: " + e.getMessage());
        } finally {
            ConnectionsHandler.returnConnection(connection);
        }
    }

    /**
     * This function is used to create a new auction, the ID of the newly created
     * item is returned.
     * This functions receives a Connection object because supposes that
     * autocommit is disabled.
     * @param connection Connection object
     * @param creator_user_id Int ID of the creator
     * @param terminates_at Timestamp of the termination
     * @param minimum_bid_wedge Int minimum new bids delta
     * @return Int ID of the newly created item
     * @throws InsertIntoDBFailedException when the insertion into the DB fails
     */
    private static int createAuction(Connection connection, int creator_user_id, Timestamp terminates_at, int minimum_bid_wedge) throws InsertIntoDBFailedException {
        String query = "INSERT INTO auctions (creator_user_id, terminates_at, minimum_bid_wedge) VALUES (?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setInt(1, creator_user_id);
            preparedStatement.setTimestamp(2, terminates_at);
            preparedStatement.setInt(3, minimum_bid_wedge);

            try {
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                throw new InsertIntoDBFailedException("SQL Exception while inserting new auction into DB: " + e.getMessage());
            }

            try(ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
                if (resultSet.isBeforeFirst()) {  // Otherwise no row was updated
                    resultSet.next();
                    return resultSet.getInt(1);
                } else {
                    throw new InsertIntoDBFailedException("No row was added");
                }
            }

        } catch (SQLException e) {
            throw new InsertIntoDBFailedException("SQL Exception while preparing statement to insert a new auction into DB: " + e.getMessage());
        }
    }

    /**
     * This function is used to create a new auction immediately adding articles
     * to it.
     * @param user_id Int id of the owner
     * @param terminates_at Timestamp termiantion
     * @param min_bid_delta Int minimum bid delta
     * @param articles_id List of Integers, articles to add
     * @throws InsertIntoDBFailedException when the insertion into the DB fails
     * @throws UnavailableException when an SQL Exception is thrown.
     */

    public static void createAuctionWithArticles(int user_id, Timestamp terminates_at, int min_bid_delta, List<Integer> articles_id) throws InsertIntoDBFailedException, UnavailableException{
        final int auction_id;
        final Connection connection = ConnectionsHandler.getConnection();
        final String disableTrigger = "SET @AUCTION_TRIGGER_DISABLED = 1";
        final String enableTrigger = "SET @AUCTION_TRIGGER_DISABLED = 0";

        if (articles_id.isEmpty()) {
            throw new InsertIntoDBFailedException("Each auction must have at least one article, none was specified");
        }

        try {
            connection.setAutoCommit(false);
            try(Statement statement = connection.createStatement()){
                statement.executeUpdate(disableTrigger);
            }

            try {
                auction_id = Auction.createAuction(connection, user_id, terminates_at, min_bid_delta);
                Article.addArticlesToAuction(connection, auction_id, articles_id);
                try(Statement statement = connection.createStatement()){
                    statement.executeUpdate(enableTrigger);
                }
                connection.commit();
            } catch (InsertIntoDBFailedException e) {
                connection.rollback();
                try(Statement statement = connection.createStatement()){
                    statement.executeUpdate(enableTrigger);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw new UnavailableException("SQL Exception while trying to commit/rollback: " + e.getMessage());
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                throw new UnavailableException("SQL Exception while trying to re-enable autocommit: " + e.getMessage());
            } finally {
                ConnectionsHandler.returnConnection(connection);
            }
        }
    }
}
