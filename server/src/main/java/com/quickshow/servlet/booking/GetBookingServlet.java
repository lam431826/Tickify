package com.quickshow.servlet.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshow.dao.BookingDAO;
import com.quickshow.util.AuthUtil;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GET /api/booking/id/{bookingId} — user auth required.
 *
 * Response: {success:true, booking:{id, amount, isPaid, bookedSeats:[],
 *                                   show:{showDateTime, movie:{title, poster_path}}}}
 */
public class GetBookingServlet extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        // Auth: require logged-in user
        String userId = AuthUtil.getUserIdFromRequest(req);
        if (userId == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            writeError(resp, "Unauthorized");
            return;
        }

        // Extract bookingId from path: /api/booking/id/{bookingId}
        String pathInfo = req.getPathInfo(); // e.g. "/some-uuid"
        if (pathInfo == null || pathInfo.length() <= 1) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(resp, "Missing booking ID");
            return;
        }
        String bookingId = pathInfo.substring(1); // strip leading '/'

        Map<String, Object> booking = BookingDAO.getBookingById(bookingId, userId);
        if (booking == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            writeError(resp, "Booking not found");
            return;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("booking", booking);
        MAPPER.writeValue(resp.getWriter(), response);
    }

    private void writeError(HttpServletResponse resp, String message) throws IOException {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("success", false);
        error.put("message", message);
        MAPPER.writeValue(resp.getWriter(), error);
    }
}
