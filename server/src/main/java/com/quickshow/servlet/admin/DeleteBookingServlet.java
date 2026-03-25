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
 * DELETE /api/admin/booking/delete — admin auth required.
 * Deletes a booking and its associated occupied seats (FK child rows first).
 * Both deletes run in a single transaction.
 *
 * Request body: {bookingId}
 * Response: {success:true, message:"Booking deleted"}
 */
@WebServlet("/api/admin/booking/delete")
public class DeleteBookingServlet extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
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

        String deleteSeats   = "DELETE FROM OccupiedSeats WHERE booking_id = ?";
        String deleteBooking = "DELETE FROM Bookings WHERE id = ?";

        Connection conn = null;
        try {
            conn = DBConfig.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(deleteSeats)) {
                ps.setString(1, bookingId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(deleteBooking)) {
                ps.setString(1, bookingId);
                ps.executeUpdate();
            }

            conn.commit();
        } catch (Exception e) {
            System.err.println("[DeleteBookingServlet] error: " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ignored) {}
            }
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeError(resp, "Failed to delete booking");
            return;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (Exception ignored) {}
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Booking deleted");
        MAPPER.writeValue(resp.getWriter(), response);
    }

    private void writeError(HttpServletResponse resp, String message) throws IOException {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("success", false);
        error.put("message", message);
        MAPPER.writeValue(resp.getWriter(), error);
    }
}
