package it.carlotto.tiwria.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.carlotto.tiwria.beans.Article;
import it.carlotto.tiwria.beans.Auction;
import it.carlotto.tiwria.beans.AuctionWithArticles;
import it.carlotto.tiwria.beans.User;
import it.carlotto.tiwria.exceptions.InvalidCharsException;
import it.carlotto.tiwria.utils.InputSanitizer;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

public class wonAuctionsWithArticles extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // List of auctions won by the user
        final List<Auction> wonAuctionList = Auction.getAuctionsWonByUser(
                ((User) req.getSession().getAttribute("user")).getId()
        );
        // Build the list of auctions and articles
        final List<AuctionWithArticles> auctionWithArticlesList =
                AuctionWithArticles.getMultipleAuctionWithArticles(wonAuctionList);

        Gson gson = new GsonBuilder().create();
        final String wonAuctions = gson.toJson(auctionWithArticlesList);

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(wonAuctions);
    }
}
