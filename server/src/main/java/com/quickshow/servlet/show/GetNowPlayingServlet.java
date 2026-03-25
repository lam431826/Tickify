package com.quickshow.servlet.show;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshow.dao.UserDAO;
import com.quickshow.util.AuthUtil;
import com.quickshow.util.TmdbUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GET /api/show/now-playing — admin auth required.
 * Proxies the TMDB now-playing endpoint and returns the movies list.
 *
 * Response: {success:true, movies:[{id, title, poster_path, vote_average,
 *            vote_count, release_date, ...}]}
 */
@WebServlet("/api/show/now-playing")
public class GetNowPlayingServlet extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        // Auth: require admin
        String userId = AuthUtil.getUserIdFromRequest(req);
        if (userId == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            writeError(resp, "Unauthorized");
            return;
        }

        // Auto-sync user to DB
        UserDAO.getOrCreateUser(userId);

        if (!UserDAO.isAdmin(userId)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            writeError(resp, "Admin access required");
            return;
        }

        // Fetch from TMDB
        JsonNode tmdbResponse = TmdbUtil.fetchNowPlaying();
        if (tmdbResponse == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            writeError(resp, "Failed to fetch now-playing movies from TMDB");
            return;
        }

        JsonNode results = tmdbResponse.get("results");
        List<Map<String, Object>> movies = new ArrayList<>();
        if (results != null && results.isArray()) {
            for (JsonNode m : results) {
                Map<String, Object> movie = new LinkedHashMap<>();
                movie.put("id",           m.has("id") ? m.get("id").asInt() : null);
                movie.put("title",        m.has("title") ? m.get("title").asText("") : "");
                movie.put("poster_path",  m.has("poster_path") && !m.get("poster_path").isNull()
                        ? m.get("poster_path").asText() : null);
                movie.put("backdrop_path",m.has("backdrop_path") && !m.get("backdrop_path").isNull()
                        ? m.get("backdrop_path").asText() : null);
                movie.put("vote_average", m.has("vote_average") ? m.get("vote_average").asDouble() : 0);
                movie.put("vote_count",   m.has("vote_count") ? m.get("vote_count").asInt() : 0);
                movie.put("release_date", m.has("release_date") ? m.get("release_date").asText("") : "");
                movie.put("overview",     m.has("overview") ? m.get("overview").asText("") : "");
                movies.add(movie);
            }
        }

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
