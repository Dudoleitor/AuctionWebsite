package it.carlotto.tiwria.controllers;

import it.carlotto.tiwria.beans.User;
import it.carlotto.tiwria.exceptions.InvalidCharsException;
import it.carlotto.tiwria.exceptions.ObjectNotFoundException;
import it.carlotto.tiwria.utils.InputSanitizer;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * This servlet handles the landing page (root)
 * with the login.
 */
public class doLogin extends HttpServlet {

    /**
     * Authenticating user using DB and saving the user
     * object in the session.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String username;
        final String password;
        User user = null;

        // If the user is already logged in, perform logout
        if (req.getSession().getAttribute("user")!=null)
            req.getSession().setAttribute("user", null);

        try {
            final String usern = req.getParameter("user");
            final String pass = req.getParameter("pass");
            if (usern == null || pass == null || usern.isEmpty() || pass.isEmpty()) {
                throw new Exception();
            }

            username = InputSanitizer.sanitizeAlphanumeric(usern);
            password = InputSanitizer.sanitizePassword(pass);

        } catch (InvalidCharsException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "provided credentials contains unexpected characters");
            return;
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "no credentials provided");
            return;
        }

        try {
            user = User.loadWithCreds(username, password);
        } catch (ObjectNotFoundException ignored) {
            // User not found, local user variable remains null
        }

        if (user != null) {
            final HttpSession s = req.getSession();
            s.setAttribute("user", user);
            getServletContext().log("Successful login, user: " + username);
        } else { // Was not set

            resp.setContentType("text/html");
            getServletContext().log("Failed login attempt, user: " + username);
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "provided credentials do not correspond to a user");
        }

    }
}
