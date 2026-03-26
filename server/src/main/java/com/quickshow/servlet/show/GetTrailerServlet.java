package com.quickshow.servlet.show;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshow.util.TmdbUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GET /api/show/trailer/{movieId} — public endpoint.
 * Returns the YouTube trailer key for the given TMDB movie ID.
 * Response: {success:true, trailerKey:"abc123"} or {success:false, message:"..."}
 */
@WebServlet("/api/show/trailer/*")
public class GetTrailerServlet extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        String pathInfo = req.getPathInfo(); // "/{movieId}"
        if (pathInfo == null || pathInfo.length() <= 1) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            write(resp, Map.of("success", false, "message", "Missing movieId"));
            return;
        }

        int movieId;
        try {
            movieId = Integer.parseInt(pathInfo.substring(1));
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            write(resp, Map.of("success", false, "message", "Invalid movieId"));
            return;
        }

        String key = TmdbUtil.fetchTrailerKey(movieId);

        Map<String, Object> response = new LinkedHashMap<>();
        if (key != null) {
            response.put("success", true);
            response.put("trailerKey", key);
        } else {
            response.put("success", false);
            response.put("message", "Trailer not found");
        }
        write(resp, response);
    }

    private void write(HttpServletResponse resp, Map<String, Object> data) throws IOException {
        MAPPER.writeValue(resp.getWriter(), data);
    }
}
