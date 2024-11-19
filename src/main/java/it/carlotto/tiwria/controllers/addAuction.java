package it.carlotto.tiwria.controllers;

import it.carlotto.tiwria.beans.Article;
import it.carlotto.tiwria.beans.Auction;
import it.carlotto.tiwria.beans.User;
import it.carlotto.tiwria.exceptions.InsertIntoDBFailedException;
import it.carlotto.tiwria.exceptions.InvalidCharsException;
import it.carlotto.tiwria.exceptions.ObjectNotFoundException;
import it.carlotto.tiwria.utils.InputSanitizer;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * This servlet adds a new auction and updates articles related to the auction.
 * The operation of creating a new auction and adding articles to it
 * is executed as atomic.
 * It needs the termination timestamp and the minimum bid delta to create an auction,
 * it needs a list of integers corresponding to the articles the user wants to
 * add to the auction.
 * The servlet checks the articles belong to the user posting the request and uses
 * it's id as the owner of the new auction.
 * The GET param auctionStatusCode is used to send a feedback back to the sell page.
 */
public class addAuction extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final int user_id = ((User)req.getSession().getAttribute("user")).getId();
        final int min_bid_delta;
        final Timestamp terminates_at;
        final List<Integer> articles_id = new ArrayList<>();

        if (req.getParameter("articlesId") == null || req.getParameter("articlesId").isEmpty()
            || req.getParameter("terminatesAt") == null || req.getParameter("terminatesAt").isEmpty()
            || req.getParameter("minBidDelta") == null || req.getParameter("minBidDelta").isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "not enough arguments");
            return;
        }

        try {
            min_bid_delta = InputSanitizer.sanitizeNumeric(req.getParameter("minBidDelta"));
            terminates_at = InputSanitizer.sanitizeDateTime(req.getParameter("terminatesAt"));
            String[] articles = req.getParameterValues("articlesId");
            for (String id : articles) {
                articles_id.add(InputSanitizer.sanitizeNumeric(id));
            }
        } catch (InvalidCharsException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "provided parameters contained unexpected values");
            return;
        }

        if(terminates_at.before(Timestamp.from(Instant.now())) || min_bid_delta<=0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "termination must be in the future");
            return;
        }

        try {
            if (!Article.checkOwner(user_id, articles_id)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "some articles belong to another user");
                return;
            }
        } catch (ObjectNotFoundException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "some articles were not found in the DB");
            return;
        }

        try {
            Auction.createAuctionWithArticles(user_id, terminates_at, min_bid_delta, articles_id);
        } catch (InsertIntoDBFailedException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "insertion into DB failed");
            return;
        }
    }
}
