package it.carlotto.tiwria.controllers;

import it.carlotto.tiwria.beans.Auction;
import it.carlotto.tiwria.beans.User;
import it.carlotto.tiwria.exceptions.InvalidCharsException;
import it.carlotto.tiwria.exceptions.ObjectNotFoundException;
import it.carlotto.tiwria.utils.InputSanitizer;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class closeAuction extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final int auctionId;

        if (req.getParameter("auction") == null || req.getParameter("auction").isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "auction ID not provided");
            return;
        }
        try {
            auctionId = InputSanitizer.sanitizeNumeric(req.getParameter("auction"));
        } catch (InvalidCharsException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "provided parameter contained unexpected values");
            return;
        }

        final Auction auction;
        try {
            auction = Auction.getAuctionFromId(auctionId);
        } catch (ObjectNotFoundException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "the specified auction does not exist");
            return;
        }
        if (auction.getCreator_user_id() !=  // The user requested an auction belonging to somebody else
                ((User)req.getSession().getAttribute("user")).getId()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "the specified auction belongs to another user");
            return;
        }

        if (auction.isClosed_by_user()) {
            resp.sendRedirect(getServletContext().getContextPath()+"/auctionDetails?id=" + auctionId);
            return;
        }

        if(auction.isOpen()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "the specified auction is still open");
            return;
        }

        Auction.closeAuction(auctionId);
    }
}
