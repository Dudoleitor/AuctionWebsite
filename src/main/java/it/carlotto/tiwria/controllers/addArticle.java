package it.carlotto.tiwria.controllers;

import it.carlotto.tiwria.beans.Article;
import it.carlotto.tiwria.beans.User;
import it.carlotto.tiwria.exceptions.InsertIntoDBFailedException;
import it.carlotto.tiwria.exceptions.InvalidCharsException;
import it.carlotto.tiwria.utils.InputSanitizer;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;

/**
 * This servlet adds an article to the DB.
 * It needs the name and the base price, the description is optional.
 * The article is created without an auction, the ID of the user
 * sending the request is automatically used for the owner field.
 * The GET param articleStatus code is used to give a feedback back
 * to the sell page.
 */
public class addArticle extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final int owner_id = ((User)req.getSession().getAttribute("user")).getId();
        final float base_price;
        final String name;
        final String description;

        if (req.getPart("basePrice") == null
                || req.getPart("name") == null ) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "not enough info provided");
            return;
        }

        try {
            base_price = InputSanitizer.sanitizeFloat(new String(req.getPart("basePrice").getInputStream().readAllBytes()));
            if (base_price<=0) throw new InvalidCharsException("");
            name = InputSanitizer.sanitizeGeneric(new String(req.getPart("name").getInputStream().readAllBytes()));
            if (req.getPart("desc")!=null)
                description = InputSanitizer.sanitizeGeneric(new String(req.getPart("desc").getInputStream().readAllBytes()));
            else description=null;
        } catch (InvalidCharsException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "provided parameters contained unexpected values");
            return;
        }

        Part imagePart = req.getPart("image");
        if (imagePart != null && imagePart.getSize() > 0) {
            String imageContentType = imagePart.getContentType();
            if (!Article.allowedContentType(imageContentType)) {  // Type not allowed
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "image file invalid");
                return;
            }
        } else {
           imagePart = null;
        }

        try {
            Article.createArticle(owner_id, base_price, name, description, imagePart);
        } catch (InsertIntoDBFailedException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "insertion into DB failed");
            return;
        }
    }
}
