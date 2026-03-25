package com.quickshow.servlet.user;

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
import java.util.List;
import java.util.Map;

/**
 * GET /api/user/favorites — user auth required.
 * Returns the current user's list of favorite movies.
 *
 * Response: {success:true, movies:[{_id, title, poster_path, vote_average, release_date}]}
 */
@WebServlet("/api/user/favorites")
public class GetFavoritesServlet extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        String userId = AuthUtil.getUserIdFromRequest(req);
        if (userId == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            writeError(resp, "Unauthorized");
            return;
        }
        UserDAO.getOrCreateUser(userId);

        List<Map<String, Object>> movies = MovieDAO.getFavoriteMovies(userId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("movies",  movies);
        MAPPER.writeValue(resp.getWriter(), response);
    }

    private void writeError(HttpServletResponse resp, String message) throws IOException {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("success", false);
        error.put("message", message);
        MAPPER.writeValue(resp.getWriter(), error);
    }
}
