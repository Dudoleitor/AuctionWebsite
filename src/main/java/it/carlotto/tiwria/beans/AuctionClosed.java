package it.carlotto.tiwria.beans;

import it.carlotto.tiwria.exceptions.ObjectNotFoundException;
import it.carlotto.tiwria.utils.ConnectionsHandler;
import jakarta.servlet.UnavailableException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This is an Auction object with the info of the winning
 * user and the winning bid.
 */
public class AuctionClosed extends Auction {
    private final int winner_user_id;
    private final String winner_user_username;
    private final String winner_user_address;
    private final float final_bid_amount;

    private AuctionClosed(Auction auction, int winner_user_id, String winner_user_username, String winner_user_address, float final_bid_amount) {
        super(auction);
        this.winner_user_id = winner_user_id;
        this.winner_user_username = winner_user_username;
        if (winner_user_address!=null)
            this.winner_user_address = winner_user_address;
        else this.winner_user_address = "";
        this.final_bid_amount = final_bid_amount;
    }

    public AuctionClosed(AuctionClosed auction) {
        super(auction);
        this.winner_user_id = auction.winner_user_id;
        this.winner_user_username = auction.winner_user_username;
        this.winner_user_address = auction.winner_user_address;
        this.final_bid_amount = auction.final_bid_amount;
    }

    public int getWinner_user_id() {
        return winner_user_id;
    }

    public String getWinner_user_username() {
        return winner_user_username;
    }

    public String getWinner_user_address() {
        return winner_user_address;
    }

    public float getFinal_bid_amount() {
        return final_bid_amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Auction.class != o.getClass()) return false;
        Auction auction = (Auction) o;
        return this.getId() == auction.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getId());
    }

    /**
     * This function is used to load a multiple auctions with the info
     * of the winning user and bid
     * @param auctionList List of Auction object
     * @return List of AuctionClosed objects
     * @throws ObjectNotFoundException when no auction with a given
     *      id exists in the database, an auction is not terminated
     * @throws UnavailableException when it's not possible to get a SQL connection
     *      or an SQL error happens.
     */
    public static List<AuctionClosed> getAuctionsTerminatedFromAuctions(List<Auction> auctionList) throws ObjectNotFoundException, UnavailableException {
        List<AuctionClosed> auctionClosedList = new ArrayList<>();
        String query = "SELECT u.id, u.username, u.address, b1.amount FROM bids as b1 " +
                "JOIN users as u ON b1.bidder_user_id = u.id " +  // Joining to get the bidder's info
                "WHERE b1.auction_id = ? AND " +  // Considering only the specified auction
                "b1.amount = " +  // Using a nested query to find the bids with the max amount
                "(SELECT MAX(amount) FROM bids as b2 WHERE b2.auction_id = b1.auction_id)";
        Connection connection = ConnectionsHandler.getConnection();

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)
        ) {
            for (Auction auction : auctionList) {
                if (auction.isOpen())
                    throw new ObjectNotFoundException("While trying to build ad AuctionTerminated the provided id corresponds to an open auction");
                preparedStatement.setInt(1, auction.getId());
                try (ResultSet resultSet = preparedStatement.executeQuery()
                ) {
                    if (!resultSet.isBeforeFirst()) {  // Winning bid not found
                        auctionClosedList.add(new AuctionClosed(auction, 0, "", "", 0));
                    } else {
                        resultSet.next();

                        auctionClosedList.add(new AuctionClosed(
                                auction,
                                resultSet.getInt("u.id"),
                                resultSet.getString("u.username"),
                                resultSet.getString("u.address"),
                                resultSet.getFloat("b1.amount")
                        ));
                    }
                }
            }
            return auctionClosedList;
        } catch (SQLException e) {
            throw new UnavailableException("SQL Exception while loading winning bid from DB: " + e.getMessage());
        } finally {
            ConnectionsHandler.returnConnection(connection);
        }
    }
}
