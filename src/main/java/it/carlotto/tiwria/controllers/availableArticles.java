package it.carlotto.tiwria.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.carlotto.tiwria.beans.Article;
import it.carlotto.tiwria.beans.User;
import it.carlotto.tiwria.exceptions.InvalidCharsException;
import it.carlotto.tiwria.utils.InputSanitizer;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

public class availableArticles extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final int user_id = ((User)req.getSession().getAttribute("user")).getId();

        final List<Article> articleList = Article.getArticlesByUser(
                user_id,
                true);

        Gson gson = new GsonBuilder().create();
        final String articleListJson = gson.toJson(articleList);

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(articleListJson);
    }
}
