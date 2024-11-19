package it.carlotto.tiwria.controllers;

import it.carlotto.tiwria.beans.Article;
import it.carlotto.tiwria.beans.Auction;
import it.carlotto.tiwria.beans.Bid;
import it.carlotto.tiwria.beans.User;
import it.carlotto.tiwria.exceptions.InsertIntoDBFailedException;
import it.carlotto.tiwria.exceptions.InvalidCharsException;
import it.carlotto.tiwria.exceptions.ObjectNotFoundException;
import it.carlotto.tiwria.utils.InputSanitizer;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * This servlet handles the insertion of a new bid into the DB.
 * Checks are performed before executing the query,
 * the result (success/failure) is put into an HTTP GET parameter.
 * The servlet redirects to the auction page.
 */
public class placeBid extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final int auctionId;
        final float newBidAmount;

        if (req.getParameter("auction") == null || req.getParameter("auction").isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "no auction id provided");
            return;
        }

        try {
            auctionId = InputSanitizer.sanitizeNumeric(req.getParameter("auction"));
        } catch (InvalidCharsException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "provided parameters (auction) contained unexpected values");
            return;
        }

        if (req.getParameter("bid") == null || req.getParameter("bid").isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "no bid provided");
            return;
        }

        try {
            newBidAmount = InputSanitizer.sanitizeFloat(req.getParameter("bid"));
        } catch (InvalidCharsException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "provided parameters (bid) contained unexpected values");
            return;
        }

        final Auction auction;
        try {
            auction = Auction.getAuctionFromId(auctionId);
        } catch (ObjectNotFoundException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "the specified auction does not exist");
            return;
        }

        boolean bidPlacingSuccess;  // Used as flag

        if (  // Checking if requirements are met before executing the SQL query
                newBidAmount >= calculateBidMinimum(auctionId, auction.getMinimum_bid_wedge()) // Amount higher than minimum AND
                && auction.isOpen()  // Auction is not closed
        ) {  // Requirements met

            try {  // Attempting to place bid
                Bid.placeBid(((User) req.getSession().getAttribute("user")).getId(),
                        auctionId,
                        newBidAmount
                );
                bidPlacingSuccess = true;
            } catch (InsertIntoDBFailedException e) {
                bidPlacingSuccess = false;
            }

        } else {  // Requirements not met
            bidPlacingSuccess = false;
        }

        if (!bidPlacingSuccess)
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "requirements not met");
    }

    /**
     * This function is used to calculate the minimum allowed
     * value for the next bid.
     * @param auctionId Int id of the auction
     * @param auctionMinBidWedge auction.getMinimum_bid_wedge()
     * @return float with the minimum allowed value.
     * @throws UnavailableException when it's not possible to get a SQL connection
     *          or an SQL error happens.
     */
    private float calculateBidMinimum(int auctionId, float auctionMinBidWedge) throws UnavailableException {
        final List<Bid> bidList = Bid.getBidsPerAuction(auctionId);

        if(!bidList.isEmpty()) {
            return auctionMinBidWedge + bidList.get(0).getAmount();
        }
        List<Article> articleList = Article.getArticlesByAuction(auctionId);
        if (articleList.isEmpty()) {
            return 0;
        }
        return (float) articleList.stream().mapToDouble(Article::getBase_price).sum();
    }
}
