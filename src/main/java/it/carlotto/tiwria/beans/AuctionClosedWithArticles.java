package it.carlotto.tiwria.beans;

import it.carlotto.tiwria.utils.ConnectionsHandler;
import jakarta.servlet.UnavailableException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AuctionClosedWithArticles extends AuctionClosed {
    List<Article> articleList;

    private AuctionClosedWithArticles(AuctionClosed auction, List<Article> articleList) {
        super(auction);
        this.articleList = articleList;
    }

    public List<Article> getArticleList() {
        return new ArrayList<>(articleList);
    }

    /**
     * This function is used to load multiple AuctionClosedWithArticles from the DB.
     * For each AuctionClosed object passed, a query is executed to retriever it's articles.
     * @param auctionList List of AuctionClosed objects, used to get the ID and build
     *                    the AuctionClosedWithArticles
     * @return List of AuctionClosedWithArticles
     * @throws UnavailableException when it's not possible to get a SQL connection
     *      or an SQL error happens.
     */
    public static List<AuctionClosedWithArticles> getMultipleAuctionClosedWithArticles(List<AuctionClosed> auctionList) throws UnavailableException {
        List<AuctionClosedWithArticles> auctionClosedWithArticlesList = new ArrayList<>();
        String articlesQuery = "SELECT id, owner_user_id, base_price, auction_id, name, description, img_file_name FROM articles WHERE auction_id = ?";
        Connection connection = ConnectionsHandler.getConnection();

        try (PreparedStatement preparedStatement = connection.prepareStatement(articlesQuery)
        ) {

            for(AuctionClosed auction : auctionList) {  // Iterating over the auctions
                preparedStatement.setInt(1, auction.getId());

                // Building a new AuctionWithArticles object
                auctionClosedWithArticlesList.add(new AuctionClosedWithArticles(
                        auction,
                        Article.loadArticlesUsingPS(preparedStatement)
                ));
            }

            return auctionClosedWithArticlesList;

        } catch (SQLException e) {
            throw new UnavailableException("SQL Exception while loading auction with articles from DB: " + e.getMessage());
        } finally {
            ConnectionsHandler.returnConnection(connection);
        }
    }
}
