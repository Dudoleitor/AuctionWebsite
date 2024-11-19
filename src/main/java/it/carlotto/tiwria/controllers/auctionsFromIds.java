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
import java.util.ArrayList;
import java.util.List;

public class auctionsFromIds extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final List<Integer> idList = new ArrayList<>();

        if(req.getParameter("id") == null || req.getParameter("id").isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "no id provided");
            return;
        }

        try {
            String[] ids = req.getParameterValues("id");
            for (String id : ids) {
                idList.add(InputSanitizer.sanitizeNumeric(id));
            }
        } catch (InvalidCharsException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "provided parameters contained unexpected values");
            return;
        }

        final List<Auction> auctionList = Auction.getAuctionsFromIds(idList, true);

        final Gson gson = new GsonBuilder().create();
        final String auctionListJson = gson.toJson(auctionList);

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(auctionListJson);
    }
}
