package com.quickshow.servlet.booking;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshow.dao.BookingDAO;
import com.quickshow.dao.ShowDAO;
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
import java.util.stream.Collectors;

/**
 * POST /api/booking/create — user auth required.
 *
 * Request body (JSON):
 * {
 *   "showId": "uuid",
 *   "selectedSeats": ["A1", "B3", ...]
 * }
 *
 * Response: {success:true, url:"payment_placeholder_url"}
 */
@WebServlet("/api/booking/create")
public class CreateBookingServlet extends HttpServlet {

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
        // Auto-sync user to DB
        UserDAO.getOrCreateUser(userId);

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

        if (!body.has("showId") || !body.has("selectedSeats")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(resp, "Missing required fields: showId, selectedSeats");
            return;
        }

        String showId = body.get("showId").asText();
        List<String> selectedSeats;
        try {
            selectedSeats = MAPPER.convertValue(body.get("selectedSeats"),
                    new TypeReference<List<String>>() {});
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(resp, "selectedSeats must be a JSON array of strings");
            return;
        }

        if (selectedSeats == null || selectedSeats.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(resp, "No seats selected");
            return;
        }

        // Validate show exists and get price
        Map<String, Object> show = ShowDAO.findById(showId);
        if (show == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            writeError(resp, "Show not found");
            return;
        }
        double showPrice = (double) show.get("showPrice");

        // Check seats aren't already occupied
        List<String> occupied = BookingDAO.getOccupiedSeats(showId);
        for (String seat : selectedSeats) {
            if (occupied.contains(seat)) {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                writeError(resp, "Seat " + seat + " is already booked");
                return;
            }
        }

        // Create booking (inserts Bookings + OccupiedSeats in one transaction)
        String bookingId = BookingDAO.createBooking(userId, showId, selectedSeats, showPrice);
        if (bookingId == null) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeError(resp, "Failed to create booking");
            return;
        }

        // Return the payment link and bookingId
        String paymentUrl = BookingDAO.getPaymentLink(bookingId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success",   true);
        response.put("url",       paymentUrl != null ? paymentUrl : "");
        response.put("bookingId", bookingId);
        MAPPER.writeValue(resp.getWriter(), response);
    }

    private void writeError(HttpServletResponse resp, String message) throws IOException {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("success", false);
        error.put("message", message);
        MAPPER.writeValue(resp.getWriter(), error);
    }
}
