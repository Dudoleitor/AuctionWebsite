package it.carlotto.tiwria.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.carlotto.tiwria.beans.Article;
import it.carlotto.tiwria.beans.Bid;
import it.carlotto.tiwria.exceptions.InvalidCharsException;
import it.carlotto.tiwria.utils.InputSanitizer;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

public class articlesByAuctionId extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final int auction_id;

        if(req.getParameter("auction")==null || req.getParameter("auction").isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "no auction id provided");
            return;
        }

        try {
            auction_id = InputSanitizer.sanitizeNumeric(req.getParameter("auction"));
        } catch (InvalidCharsException e) {  // Thrown by the sanitizer
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "provided id contained unexpected characters");
            return;
        }

        final List<Article> articleList = Article.getArticlesByAuction(auction_id);

        Gson gson = new GsonBuilder().create();
        final String articleListJson = gson.toJson(articleList);

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(articleListJson);
    }
}