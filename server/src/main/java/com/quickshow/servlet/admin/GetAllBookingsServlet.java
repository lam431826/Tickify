package com.quickshow.servlet.admin;

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
 * GET /api/admin/all-bookings — admin auth required.
 * Returns all bookings across all users with user, show, and movie details.
 *
 * Response: {success:true, bookings:[{bookedSeats:["A1",...], amount,
 *            show:{showDateTime, movie:{title}}, user:{name}}]}
 */
@WebServlet("/api/admin/all-bookings")
public class GetAllBookingsServlet extends HttpServlet {

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
        if (!UserDAO.isAdmin(userId)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            writeError(resp, "Admin access required");
            return;
        }

        List<Map<String, Object>> bookings = BookingDAO.getAllBookingsAdmin();

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
