package com.quickshow.servlet.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshow.dao.BookingDAO;
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
 * GET /api/user/bookings — user auth required.
 * Returns all bookings belonging to the authenticated user.
 *
 * Response: {success:true, bookings:[{id, amount, isPaid, paymentLink,
 *            bookedSeats:["A1",...],
 *            show:{showDateTime, movie:{title, poster_path, runtime}}}]}
 */
@WebServlet("/api/user/bookings")
public class GetUserBookingsServlet extends HttpServlet {

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

        List<Map<String, Object>> bookings = BookingDAO.getBookingsByUser(userId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success",  true);
        response.put("bookings", bookings);
        MAPPER.writeValue(resp.getWriter(), response);
    }

    private void writeError(HttpServletResponse resp, String message) throws IOException {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("success", false);
        error.put("message", message);
        MAPPER.writeValue(resp.getWriter(), error);
    }
}
