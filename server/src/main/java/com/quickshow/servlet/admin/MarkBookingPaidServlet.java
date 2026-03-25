package com.quickshow.servlet.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshow.config.DBConfig;
import com.quickshow.dao.UserDAO;
import com.quickshow.util.AuthUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PATCH /api/admin/booking/mark-paid — admin auth required.
 * Marks a booking as paid.
 *
 * Request body: {bookingId}
 * Response: {success:true, message:"Booking marked as paid"}
 */
@WebServlet("/api/admin/booking/mark-paid")
public class MarkBookingPaidServlet extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        if (!"PATCH".equalsIgnoreCase(req.getMethod())) {
            resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        handlePatch(req, resp);
    }

    private void handlePatch(HttpServletRequest req, HttpServletResponse resp)
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

        @SuppressWarnings("unchecked")
        Map<String, Object> body = MAPPER.readValue(req.getReader(), Map.class);
        String bookingId = (String) body.get("bookingId");
        if (bookingId == null || bookingId.isBlank()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(resp, "bookingId is required");
            return;
        }

        String sql = "UPDATE Bookings SET is_paid = 1 WHERE id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bookingId);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("[MarkBookingPaidServlet] error: " + e.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeError(resp, "Failed to update booking");
            return;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Booking marked as paid");
        MAPPER.writeValue(resp.getWriter(), response);
    }

    private void writeError(HttpServletResponse resp, String message) throws IOException {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("success", false);
        error.put("message", message);
        MAPPER.writeValue(resp.getWriter(), error);
    }
}
