package it.carlotto.tiwria.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.carlotto.tiwria.beans.Auction;
import it.carlotto.tiwria.exceptions.InvalidCharsException;
import it.carlotto.tiwria.utils.InputSanitizer;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

public class openAuctionsWithSearch extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final List<Auction> auctionList;  // List of all/selected auctions

        if (req.getParameter("q")!=null && !req.getParameter("q").isEmpty()) {  // A valid expression is provided
            String searchQuery = "";
            try {
                searchQuery = InputSanitizer.sanitizeGeneric(req.getParameter("q"));
            } catch (InvalidCharsException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "provided search query contained unexpected characters");
                return;
            }
            auctionList = Auction.getAuctionsWithSearch(searchQuery, true);
        } else {  // Showing the full list
            auctionList = Auction.getAuctions(true);
        }

        Gson gson = new GsonBuilder().create();
        final String auctionListJson = gson.toJson(auctionList);

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(auctionListJson);
    }
}
