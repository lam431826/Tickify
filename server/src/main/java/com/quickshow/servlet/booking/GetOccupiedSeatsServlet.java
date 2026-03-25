package com.quickshow.servlet.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshow.dao.BookingDAO;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GET /api/booking/seats/:showId — public endpoint.
 * Returns the list of already-occupied seat IDs for a given show.
 *
 * Response: {success:true, occupiedSeats:["A1","B2",...]}
 */
@WebServlet("/api/booking/seats/*")
public class GetOccupiedSeatsServlet extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        // Extract showId from path: /api/booking/seats/{showId}
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(resp, "Show ID is required");
            return;
        }
        String showId = pathInfo.substring(1); // remove leading "/"

        List<String> occupiedSeats = BookingDAO.getOccupiedSeats(showId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success",       true);
        response.put("occupiedSeats", occupiedSeats);
        MAPPER.writeValue(resp.getWriter(), response);
    }

    private void writeError(HttpServletResponse resp, String message) throws IOException {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("success", false);
        error.put("message", message);
        MAPPER.writeValue(resp.getWriter(), error);
    }
}
