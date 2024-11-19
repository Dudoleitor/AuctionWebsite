package it.carlotto.tiwria.beans;

import it.carlotto.tiwria.exceptions.ObjectNotFoundException;
import it.carlotto.tiwria.utils.ConnectionsHandler;
import jakarta.servlet.UnavailableException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AuctionWithArticles extends Auction {
    private final List<Article> articleList;
    private final float max_bid;

    private AuctionWithArticles(Auction auction, List<Article> articleList, float max_bid) {
        super(auction);
        this.articleList = articleList;
        this.max_bid = max_bid;
    }

    public List<Article> getArticleList() {
        return new ArrayList<>(articleList);
    }

    public float getMax_bid() {
        return max_bid;
    }

    /**
     * This function is used to load an AuctionWithArticles from the DB.
     * @param auction_id Int ID of the auction
     * @return AuctionWithArticles object
     * @throws ObjectNotFoundException when no auction with the given
     *      id exists in the database
     * @throws UnavailableException when it's not possible to get a SQL connection
     *      or an SQL error happens.
     */
    public static AuctionWithArticles getAuctionWithArticlesFromId(int auction_id) throws ObjectNotFoundException, UnavailableException {
        return new AuctionWithArticles(
                Auction.getAuctionFromId(auction_id),
                Article.getArticlesByAuction(auction_id),
                Auction.getMaxBidFromAuctionId(auction_id)
        );
    }

    /**
     * This function is used to load multiple AuctionWithArticles from the DB.
     * For each Auction object passed, a query is executed to retriever it's articles.
     * @param auctionList List of Auctions objects, used to get the ID and build
     *                    the AuctionWithArticles
     * @return List of AuctionWithArticles
     * @throws UnavailableException when it's not possible to get a SQL connection
     *      or an SQL error happens.
     */
    public static List<AuctionWithArticles> getMultipleAuctionWithArticles(List<Auction> auctionList) throws UnavailableException {
        List<AuctionWithArticles> auctionWithArticlesList = new ArrayList<>();
        String articlesQuery = "SELECT id, owner_user_id, base_price, auction_id, name, description, img_file_name FROM articles WHERE auction_id = ?";
        Connection connection = ConnectionsHandler.getConnection();

        try (PreparedStatement preparedStatement = connection.prepareStatement(articlesQuery)
        ) {

            for(Auction auction : auctionList) {  // Iterating over the auctions
                preparedStatement.setInt(1, auction.getId());

                // Building a new AuctionWithArticles object
                auctionWithArticlesList.add(new AuctionWithArticles(
                        auction,
                        Article.loadArticlesUsingPS(preparedStatement),
                        Auction.getMaxBidFromAuctionId(auction.getId())
                ));
            }

            return auctionWithArticlesList;

        } catch (SQLException e) {
            throw new UnavailableException("SQL Exception while loading auction with articles from DB: " + e.getMessage());
        } finally {
            ConnectionsHandler.returnConnection(connection);
        }
    }
}
