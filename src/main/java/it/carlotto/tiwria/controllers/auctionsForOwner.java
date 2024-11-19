package it.carlotto.tiwria.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.carlotto.tiwria.beans.*;
import it.carlotto.tiwria.exceptions.InvalidCharsException;
import it.carlotto.tiwria.exceptions.ObjectNotFoundException;
import it.carlotto.tiwria.utils.InputSanitizer;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class auctionsForOwner extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final boolean openVsClosed;

        if(req.getParameter("openVsClosed")==null || req.getParameter("openVsClosed").isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "no boolean flag provided");
            return;
        }

        try {
            openVsClosed = InputSanitizer.sanitizeBoolean(req.getParameter("openVsClosed"));
        } catch (InvalidCharsException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "provided parameter contained unexpected values");
            return;
        }

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        final Gson gson = new GsonBuilder().create();
        // List of auctions owned by the user

        final List<Auction> auctionList = Auction.getAuctionsPerUser(
                ((User) req.getSession().getAttribute("user")).getId(),
                openVsClosed
        );

        if(openVsClosed) {
            List<AuctionClosed> auctionClosedList;
            try {
                auctionClosedList = AuctionClosed.getAuctionsTerminatedFromAuctions(auctionList);
            } catch (ObjectNotFoundException ignored) {
                auctionClosedList = new ArrayList<>();
            }

            // Build the list of auctions and articles
            final List<AuctionClosedWithArticles> auctionWithArticlesList =
                    AuctionClosedWithArticles.getMultipleAuctionClosedWithArticles(auctionClosedList);

            final String toReturn = gson.toJson(auctionWithArticlesList);
            resp.getWriter().write(toReturn);
        } else {
            // Build the list of auctions and articles
            final List<AuctionWithArticles> auctionWithArticlesList =
                    AuctionWithArticles.getMultipleAuctionWithArticles(auctionList);

            final String toReturn = gson.toJson(auctionWithArticlesList);
            resp.getWriter().write(toReturn);
        }
    }
}
