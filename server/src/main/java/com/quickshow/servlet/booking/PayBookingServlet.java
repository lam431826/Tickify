package com.quickshow.servlet.booking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshow.config.DBConfig;
import com.quickshow.util.AuthUtil;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * POST /api/booking/pay — user auth required.
 *
 * Request body: {"bookingId": "uuid"}
 *
 * Verifies the booking belongs to the user, then marks it as paid.
 * Response: {success:true, message:"Payment successful"}
 */
public class PayBookingServlet extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        // Auth: require logged-in user
        String userId = AuthUtil.getUserIdFromRequest(req);
        if (userId == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            writeError(resp, "Unauthorized");
            return;
        }

        // Parse body
        JsonNode body;
        try {
            String bodyStr = req.getReader().lines().collect(Collectors.joining());
            body = MAPPER.readTree(bodyStr);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(resp, "Invalid JSON body");
            return;
        }

        if (!body.has("bookingId")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(resp, "Missing required field: bookingId");
            return;
        }

        String bookingId = body.get("bookingId").asText();

        // Verify the booking belongs to this user and check paid status
        String checkSql = "SELECT id, is_paid FROM Bookings WHERE id = ? AND user_id = ?";
        String updateSql = "UPDATE Bookings SET is_paid = 1 WHERE id = ?";

        try (Connection conn = DBConfig.getConnection()) {
            boolean found = false;
            boolean alreadyPaid = false;

            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, bookingId);
                ps.setString(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        found = true;
                        alreadyPaid = rs.getBoolean("is_paid");
                    }
                }
            }

            if (!found) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                writeError(resp, "Booking not found");
                return;
            }

            if (alreadyPaid) {
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("success", true);
                response.put("message", "Already paid");
                MAPPER.writeValue(resp.getWriter(), response);
                return;
            }

            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setString(1, bookingId);
                ps.executeUpdate();
            }

        } catch (Exception e) {
            System.err.println("[PayBookingServlet] error: " + e.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeError(resp, "Payment processing failed");
            return;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Payment successful");
        MAPPER.writeValue(resp.getWriter(), response);
    }

    private void writeError(HttpServletResponse resp, String message) throws IOException {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("success", false);
        error.put("message", message);
        MAPPER.writeValue(resp.getWriter(), error);
    }
}
