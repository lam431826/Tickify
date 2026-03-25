package com.quickshow.servlet.show;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshow.dao.MovieDAO;
import com.quickshow.dao.ShowDAO;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GET /api/show/:id — public endpoint.
 * Returns full movie details plus a dateTime map for the given movie ID.
 *
 * Response: {success:true,
 *            movie:{title, overview, poster_path, backdrop_path, release_date,
 *                   runtime, vote_average, vote_count, genres:[{name}], casts:[{name,profile_path}]},
 *            dateTime:{"2024-07-10":[{time:"18:30:00", showId:"uuid"}, ...]}}
 *
 * URL pattern: /api/show/* — the servlet extracts the movie ID from the path.
 * Note: /api/show/all, /api/show/now-playing, and /api/show/add are mapped
 * to more-specific servlets that take precedence over this wildcard mapping.
 */
@WebServlet("/api/show/*")
public class GetShowByIdServlet extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        // Extract movie ID from path: /api/show/{id}
        String pathInfo = req.getPathInfo(); // e.g. "/123"
        if (pathInfo == null || pathInfo.equals("/")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(resp, "Movie ID is required");
            return;
        }

        String idStr = pathInfo.substring(1); // remove leading "/"
        int movieId;
        try {
            movieId = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(resp, "Invalid movie ID: " + idStr);
            return;
        }

        // Fetch movie details from DB
        Map<String, Object> movieRow = MovieDAO.findById(movieId);
        if (movieRow == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            writeError(resp, "Movie not found");
            return;
        }

        // Build the movie object expected by client
        Map<String, Object> movie = new LinkedHashMap<>();
        movie.put("title",        movieRow.get("title"));
        movie.put("overview",     movieRow.get("overview"));
        movie.put("poster_path",  movieRow.get("poster_path"));
        movie.put("backdrop_path",movieRow.get("backdrop_path"));
        movie.put("release_date", movieRow.get("release_date"));
        movie.put("runtime",      movieRow.get("runtime"));
        movie.put("vote_average", movieRow.get("vote_average"));
        movie.put("vote_count",   movieRow.get("vote_count"));
        movie.put("genres",       movieRow.get("genres"));
        movie.put("casts",        movieRow.get("casts"));

        // Fetch dateTime map
        Map<String, List<Map<String, Object>>> dateTime =
                ShowDAO.getDateTimeMapForMovie(movieId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success",  true);
        response.put("movie",    movie);
        response.put("dateTime", dateTime);

        MAPPER.writeValue(resp.getWriter(), response);
    }

    private void writeError(HttpServletResponse resp, String message) throws IOException {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("success", false);
        error.put("message", message);
        MAPPER.writeValue(resp.getWriter(), error);
    }
}
