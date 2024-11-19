package it.carlotto.tiwria.beans;

import it.carlotto.tiwria.exceptions.InsertIntoDBFailedException;
import it.carlotto.tiwria.utils.ConnectionsHandler;
import jakarta.servlet.UnavailableException;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Bid {
    private final int id;
    private final Timestamp placed_at;
    private final int bidder_user_id;
    private final String bidder_user_username;
    private final int auction_id;
    private final float amount;

    private Bid(int id, Timestamp placed_at, int bidder_user_id, String bidder_user_username, int auction_id, float amount) {
        this.id = id;
        this.placed_at = placed_at;
        this.bidder_user_id = bidder_user_id;
        this.bidder_user_username = bidder_user_username;
        this.auction_id = auction_id;
        this.amount = amount;
    }

    public int getId() {
        return id;
    }

    public Timestamp getPlaced_at() {
        return placed_at;
    }

    public LocalDateTime getPlaced_at_datetime() {
        return placed_at.toLocalDateTime();
    }

    public int getBidder_user_id() {
        return bidder_user_id;
    }

    public String getBidder_user_username() {
        return bidder_user_username;
    }

    public int getAuction_id() {
        return auction_id;
    }

    public float getAmount() {
        return amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bid bid = (Bid) o;
        return id == bid.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Bid{" +
                "id=" + id +
                ", bidder_user_id=" + bidder_user_id +
                ", bidder_user_username='" + bidder_user_username + '\'' +
                ", auction_id=" + auction_id +
                ", amount=" + amount +
                '}';
    }

    /**
     * This function is used to load the list of bids placed in the same auction.
     * The list is ordered by placed_at in descending order.
     * @param auction_id ID of the auction
     * @return List of bids
     * @throws UnavailableException when it's not possible to get a SQL connection
     *          or an SQL error happens.
     */
    public static List<Bid> getBidsPerAuction(int auction_id) throws UnavailableException {
        List<Bid> bidsList = new ArrayList<>();
        String query = "SELECT b.id, b.placed_at, b.bidder_user_id, b.amount, u.username FROM bids as b JOIN users as u ON u.id=b.bidder_user_id " +
                "WHERE b.auction_id = ? ORDER BY placed_at DESC";
        Connection connection = ConnectionsHandler.getConnection();

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)
            ) {
            preparedStatement.setInt(1, auction_id);
            try(ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    bidsList.add(new Bid(
                            resultSet.getInt("b.id"),
                            resultSet.getTimestamp("b.placed_at"),
                            resultSet.getInt("b.bidder_user_id"),
                            resultSet.getString("u.username"),
                            auction_id,
                            resultSet.getFloat("b.amount")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new UnavailableException("SQL Exception while loading bids from DB: " + e.getMessage());
        } finally {
            ConnectionsHandler.returnConnection(connection);
        }
        return bidsList;
    }

    /**
     * This function is used to load the list of bids placed by a user.
     * The list is ordered by placed_at in descending order.
     * @param user User object
     * @return List of bids
     * @throws UnavailableException when it's not possible to get an SQL connection
     *          or an SQL error happens.
     */
    public static List<Bid> getBidsPerUser(User user) throws UnavailableException {
        List<Bid> bidsList = new ArrayList<>();
        String query = "SELECT id, placed_at, auction_id, amount FROM bids WHERE bidder_user_id = ? " +
                "ORDER BY placed_at DESC";
        Connection connection = ConnectionsHandler.getConnection();

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)
        ) {
            preparedStatement.setInt(1, user.getId());
            try(ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    bidsList.add(new Bid(
                            resultSet.getInt("id"),
                            resultSet.getTimestamp("placed_at"),
                            user.getId(),
                            user.getUsername(),
                            resultSet.getInt("auction_id"),
                            resultSet.getFloat("amount")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new UnavailableException("SQL Exception while loading bids from DB: " + e.getMessage());
        } finally {
            ConnectionsHandler.returnConnection(connection);
        }

        return bidsList;
    }

    /**
     * This function is used to insert a new bid inside the DB.
     * @param bidder_user_id Int id of the user placing the bid
     * @param auction_id Int id of the auction
     * @param amount Float amount of the bid
     * @throws InsertIntoDBFailedException when an SQL Exception is thrown
     *          while attempting to execute the query
     * @throws UnavailableException when an SQL Exception is thrown while
     *          attempting to prepare the PreparedStatement.
     */
    public static void placeBid(int bidder_user_id, int auction_id, float amount) throws InsertIntoDBFailedException, UnavailableException {
        String query = "INSERT INTO bids (bidder_user_id, auction_id, amount) VALUES (?, ?, ?)";
        Connection connection = ConnectionsHandler.getConnection();

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setInt(1, bidder_user_id);
            preparedStatement.setInt(2, auction_id);
            preparedStatement.setFloat(3, amount);

            try {
                preparedStatement.executeUpdate();
            } catch (SQLException e) {  // Error occurred while executing query
                throw new InsertIntoDBFailedException("SQL Exception while inserting bid into DB: " + e.getMessage());
            }
        } catch (SQLException e) {  // Error occurred while preparing statement
            throw new UnavailableException("SQL Exception preparing statement to inserting bid into DB: " + e.getMessage());
        } finally {
            ConnectionsHandler.returnConnection(connection);
        }
    }
}
