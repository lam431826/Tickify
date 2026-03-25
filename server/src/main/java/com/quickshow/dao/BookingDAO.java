package com.quickshow.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshow.config.DBConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Data access object for the Bookings and OccupiedSeats tables.
 */
public class BookingDAO {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // -----------------------------------------------------------------------
    // Create
    // -----------------------------------------------------------------------

    /**
     * Creates a booking and inserts all selected seats into OccupiedSeats.
     * Runs in a single transaction.
     *
     * @param userId        Clerk user ID
     * @param showId        Show UUID
     * @param selectedSeats list of seat IDs e.g. ["A1", "B3"]
     * @param showPrice     price per seat
     * @return the new booking UUID, or null on failure
     */
    public static String createBooking(String userId, String showId,
                                       List<String> selectedSeats, double showPrice) {
        String bookingId  = UUID.randomUUID().toString();
        double amount     = selectedSeats.size() * showPrice;
        String seatsJson  = toJson(selectedSeats);
        String clientUrl = System.getenv("CLIENT_URL") != null ? System.getenv("CLIENT_URL") : "http://localhost:5173";
        String paymentLink = clientUrl + "/payment/" + bookingId;

        String bookingSql =
            "INSERT INTO Bookings (id, user_id, show_id, amount, booked_seats, is_paid, payment_link) " +
            "VALUES (?, ?, ?, ?, ?, 0, ?)";
        String seatSql =
            "INSERT INTO OccupiedSeats (show_id, seat_id, booking_id) VALUES (?, ?, ?)";

        Connection conn = null;
        try {
            conn = DBConfig.getConnection();
            conn.setAutoCommit(false);

            // Insert booking
            try (PreparedStatement ps = conn.prepareStatement(bookingSql)) {
                ps.setString(1, bookingId);
                ps.setString(2, userId);
                ps.setString(3, showId);
                ps.setDouble(4, amount);
                ps.setString(5, seatsJson);
                ps.setString(6, paymentLink);
                ps.executeUpdate();
            }

            // Insert occupied seats
            try (PreparedStatement ps = conn.prepareStatement(seatSql)) {
                for (String seatId : selectedSeats) {
                    ps.setString(1, showId);
                    ps.setString(2, seatId);
                    ps.setString(3, bookingId);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();
            return bookingId;

        } catch (Exception e) {
            System.err.println("[BookingDAO] createBooking error: " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ignored) {}
            }
            return null;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (Exception ignored) {}
            }
        }
    }

    // -----------------------------------------------------------------------
    // Read
    // -----------------------------------------------------------------------

    /**
     * Returns the list of occupied seat IDs for a given show.
     */
    public static List<String> getOccupiedSeats(String showId) {
        List<String> seats = new ArrayList<>();
        String sql = "SELECT seat_id FROM OccupiedSeats WHERE show_id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, showId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) seats.add(rs.getString("seat_id"));
            }
        } catch (Exception e) {
            System.err.println("[BookingDAO] getOccupiedSeats error: " + e.getMessage());
        }
        return seats;
    }

    /**
     * Returns all bookings for a user.
     * Shape per item: {id, amount, isPaid, paymentLink, bookedSeats:[...],
     *                  show:{showDateTime, movie:{title, poster_path, runtime}}}
     */
    public static List<Map<String, Object>> getBookingsByUser(String userId) {
        String sql =
            "SELECT b.id, b.amount, b.is_paid, b.payment_link, b.booked_seats, " +
            "       s.show_datetime, m.title, m.poster_path, m.runtime " +
            "FROM Bookings b " +
            "JOIN Shows s ON b.show_id = s.id " +
            "JOIN Movies m ON s.movie_id = m.id " +
            "WHERE b.user_id = ? " +
            "ORDER BY b.created_at DESC";

        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> booking = new LinkedHashMap<>();
                    booking.put("id",          rs.getString("id"));
                    booking.put("amount",       rs.getDouble("amount"));
                    booking.put("isPaid",       rs.getBoolean("is_paid"));
                    booking.put("paymentLink",  rs.getString("payment_link"));
                    booking.put("bookedSeats",  fromJsonArray(rs.getString("booked_seats")));

                    Map<String, Object> movie = new LinkedHashMap<>();
                    movie.put("title",       rs.getString("title"));
                    movie.put("poster_path", rs.getString("poster_path"));
                    movie.put("runtime",     rs.getInt("runtime"));

                    Map<String, Object> show = new LinkedHashMap<>();
                    Timestamp ts = rs.getTimestamp("show_datetime");
                    show.put("showDateTime", ts != null ? ts.toString() : null);
                    show.put("movie", movie);

                    booking.put("show", show);
                    result.add(booking);
                }
            }
        } catch (Exception e) {
            System.err.println("[BookingDAO] getBookingsByUser error: " + e.getMessage());
        }
        return result;
    }

    /**
     * Returns a single booking by ID, verifying it belongs to the given user.
     * Shape: {id, amount, isPaid, bookedSeats:[...],
     *         show:{showDateTime, movie:{title, poster_path}}}
     */
    public static Map<String, Object> getBookingById(String bookingId, String userId) {
        String sql =
            "SELECT b.id, b.amount, b.is_paid, b.booked_seats, " +
            "       s.show_datetime, m.title, m.poster_path " +
            "FROM Bookings b " +
            "JOIN Shows s ON b.show_id = s.id " +
            "JOIN Movies m ON s.movie_id = m.id " +
            "WHERE b.id = ? AND b.user_id = ?";

        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bookingId);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> booking = new LinkedHashMap<>();
                    booking.put("id",          rs.getString("id"));
                    booking.put("amount",       rs.getDouble("amount"));
                    booking.put("isPaid",       rs.getBoolean("is_paid"));
                    booking.put("bookedSeats",  fromJsonArray(rs.getString("booked_seats")));

                    Map<String, Object> movie = new LinkedHashMap<>();
                    movie.put("title",       rs.getString("title"));
                    movie.put("poster_path", rs.getString("poster_path"));

                    Map<String, Object> show = new LinkedHashMap<>();
                    Timestamp ts = rs.getTimestamp("show_datetime");
                    show.put("showDateTime", ts != null ? ts.toString() : null);
                    show.put("movie", movie);

                    booking.put("show", show);
                    return booking;
                }
            }
        } catch (Exception e) {
            System.err.println("[BookingDAO] getBookingById error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Returns the payment link for a booking.
     */
    public static String getPaymentLink(String bookingId) {
        String sql = "SELECT payment_link FROM Bookings WHERE id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("payment_link");
            }
        } catch (Exception e) {
            System.err.println("[BookingDAO] getPaymentLink error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Counts all bookings.
     */
    public static int countBookings() {
        String sql = "SELECT COUNT(*) FROM Bookings";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {
            System.err.println("[BookingDAO] countBookings error: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Sums total revenue from all bookings.
     */
    public static double totalRevenue() {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM Bookings";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getDouble(1);
        } catch (Exception e) {
            System.err.println("[BookingDAO] totalRevenue error: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Returns all bookings (admin view).
     * Shape per item: {bookedSeats:[...], amount,
     *                  show:{showDateTime, movie:{title}}, user:{name}}
     */
    public static List<Map<String, Object>> getAllBookingsAdmin() {
        String sql =
            "SELECT b.id, b.amount, b.is_paid, b.booked_seats, b.user_id, " +
            "       s.show_datetime, m.title " +
            "FROM Bookings b " +
            "JOIN Shows s ON b.show_id = s.id " +
            "JOIN Movies m ON s.movie_id = m.id " +
            "ORDER BY b.created_at DESC";

        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> booking = new LinkedHashMap<>();
                booking.put("id",          rs.getString("id"));
                booking.put("bookedSeats", fromJsonArray(rs.getString("booked_seats")));
                booking.put("amount",      rs.getDouble("amount"));
                booking.put("isPaid",      rs.getBoolean("is_paid"));

                Map<String, Object> movie = new LinkedHashMap<>();
                movie.put("title", rs.getString("title"));

                Map<String, Object> show = new LinkedHashMap<>();
                Timestamp ts = rs.getTimestamp("show_datetime");
                show.put("showDateTime", ts != null ? ts.toString() : null);
                show.put("movie", movie);
                booking.put("show", show);

                Map<String, Object> user = new LinkedHashMap<>();
                user.put("name", UserDAO.getUserName(rs.getString("user_id")));
                booking.put("user", user);

                result.add(booking);
            }
        } catch (Exception e) {
            System.err.println("[BookingDAO] getAllBookingsAdmin error: " + e.getMessage());
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static String toJson(List<String> list) {
        try {
            return MAPPER.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private static List<String> fromJsonArray(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
