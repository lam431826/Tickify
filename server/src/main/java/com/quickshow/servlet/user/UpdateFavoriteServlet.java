package com.quickshow.servlet.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshow.dao.MovieDAO;
import com.quickshow.dao.UserDAO;
import com.quickshow.util.AuthUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * POST /api/user/update-favorite — user auth required.
 * Toggles a movie as a favorite for the current user:
 * adds it if not present, removes it if already present.
 *
 * Request body (JSON): {"movieId": 12345}
 *
 * Response: {success:true, message:"Added to favorites" | "Removed from favorites",
 *            isFavorite: true | false}
 */
@WebServlet("/api/user/update-favorite")
public class UpdateFavoriteServlet extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        String userId = AuthUtil.getUserIdFromRequest(req);
        if (userId == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            writeError(resp, "Unauthorized");
            return;
        }
        UserDAO.getOrCreateUser(userId);

        // Parse body
        JsonNode body;
        try {
            String bodyStr = req.getReader().lines().collect(Collectors.joining());
            body = MAPPER.readTree(bodyStr);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(resp, "Invalid JSON body");
            return;
        }

        if (!body.has("movieId")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(resp, "Missing required field: movieId");
            return;
        }

        int movieId = body.get("movieId").asInt();

        // Toggle favorite
        boolean isFavorite = MovieDAO.toggleFavorite(userId, movieId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success",    true);
        response.put("isFavorite", isFavorite);
        response.put("message",    isFavorite ? "Added to favorites" : "Removed from favorites");
        MAPPER.writeValue(resp.getWriter(), response);
    }

    private void writeError(HttpServletResponse resp, String message) throws IOException {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("success", false);
        error.put("message", message);
        MAPPER.writeValue(resp.getWriter(), error);
    }
}
