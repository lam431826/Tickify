package com.quickshow.servlet.show;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * GET /api/show/all — public endpoint.
 * Returns all unique movies that have at least one show, each merged with
 * the earliest upcoming show's price and dateTime.
 *
 * Response: {success:true, shows:[{_id, title, poster_path, vote_average,
 *            vote_count, release_date, showPrice, showDateTime}]}
 */
@WebServlet("/api/show/all")
public class GetAllShowsServlet extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        try {
            List<Map<String, Object>> shows = ShowDAO.getAllShowsPublic();

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("shows", shows);

            MAPPER.writeValue(resp.getWriter(), response);

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("message", "Failed to fetch shows: " + e.getMessage());
            MAPPER.writeValue(resp.getWriter(), error);
        }
    }
}
