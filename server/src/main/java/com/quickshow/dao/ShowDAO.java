package com.quickshow.dao;

import com.quickshow.config.DBConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Data access object for the Shows table.
 */
public class ShowDAO {

    // -----------------------------------------------------------------------
    // Create
    // -----------------------------------------------------------------------

    /**
     * Inserts a single Show record.
     *
     * @param movieId      TMDB movie ID (must already exist in Movies table)
     * @param showDatetime ISO datetime string e.g. "2024-07-10T18:30:00"
     * @param showPrice    ticket price
     * @return the generated show UUID, or null on failure
     */
    public static String insertShow(int movieId, String showDatetime, double showPrice) {
        String id  = UUID.randomUUID().toString();
        String sql = "INSERT INTO Shows (id, movie_id, show_datetime, show_price) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            ps.setInt(2, movieId);

            // Parse datetime string to Timestamp
            Timestamp ts = parseTimestamp(showDatetime);
            if (ts == null) {
                System.err.println("[ShowDAO] Invalid datetime: " + showDatetime);
                return null;
            }
            ps.setTimestamp(3, ts);
            ps.setDouble(4, showPrice);
            ps.executeUpdate();
            return id;

        } catch (Exception e) {
            System.err.println("[ShowDAO] insertShow error: " + e.getMessage());
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // Read
    // -----------------------------------------------------------------------

    /**
     * Returns all shows joined with their movie, grouped for the public listing.
     * Each unique movie returns only its earliest upcoming (or latest) show.
     *
     * Shape per item: {_id, title, poster_path, vote_average, vote_count,
     *                  release_date, showPrice, showDateTime}
     */
    public static List<Map<String, Object>> getAllShowsPublic() {
        // Get all shows, latest one per movie (by show_datetime DESC so latest is first).
        // The client displays one card per movie with its upcoming showPrice/showDateTime.
        String sql =
            "SELECT s.id AS show_id, s.show_datetime, s.show_price, " +
            "       m.id AS movie_id, m.title, m.overview, m.poster_path, m.backdrop_path, " +
            "       m.vote_average, m.vote_count, m.release_date, m.runtime, m.genres " +
            "FROM Shows s " +
            "JOIN Movies m ON s.movie_id = m.id " +
            "ORDER BY m.id, s.show_datetime ASC";

        // Deduplicate: keep the first (earliest) show per movie
        java.util.Set<Integer> seen = new java.util.LinkedHashSet<>();
        java.util.List<Map<String, Object>> deduped = new ArrayList<>();

        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int movieId = rs.getInt("movie_id");
                if (seen.contains(movieId)) continue;
                seen.add(movieId);

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("_id",           movieId);
                row.put("title",         rs.getString("title"));
                row.put("overview",      rs.getString("overview"));
                row.put("poster_path",   rs.getString("poster_path"));
                row.put("backdrop_path", rs.getString("backdrop_path"));
                row.put("vote_average",  rs.getDouble("vote_average"));
                row.put("vote_count",    rs.getInt("vote_count"));
                row.put("runtime",       rs.getInt("runtime"));
                java.sql.Date rd = rs.getDate("release_date");
                row.put("release_date",  rd != null ? rd.toString() : null);
                row.put("showPrice",     rs.getDouble("show_price"));
                Timestamp ts = rs.getTimestamp("show_datetime");
                row.put("showDateTime",  ts != null ? ts.toString() : null);

                // Parse genres JSON string into a List
                String genresJson = rs.getString("genres");
                row.put("genres", parseJsonArray(genresJson));

                deduped.add(row);
            }
        } catch (Exception e) {
            System.err.println("[ShowDAO] getAllShowsPublic error: " + e.getMessage());
        }
        return deduped;
    }

    /**
     * Returns a show record by ID with its movie_id.
     */
    public static Map<String, Object> findById(String showId) {
        String sql = "SELECT id, movie_id, show_datetime, show_price FROM Shows WHERE id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, showId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> show = new LinkedHashMap<>();
                    show.put("id",           rs.getString("id"));
                    show.put("movieId",      rs.getInt("movie_id"));
                    Timestamp ts = rs.getTimestamp("show_datetime");
                    show.put("showDateTime", ts != null ? ts.toString() : null);
                    show.put("showPrice",    rs.getDouble("show_price"));
                    return show;
                }
            }
        } catch (Exception e) {
            System.err.println("[ShowDAO] findById error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Returns all shows for a given movie ID, as a dateTime map.
     * Map shape: { "2024-07-10": [{time:"18:30:00", showId:"uuid"}, ...], ... }
     */
    public static Map<String, List<Map<String, Object>>> getDateTimeMapForMovie(int movieId) {
        String sql = "SELECT id, show_datetime, show_price FROM Shows WHERE movie_id = ? ORDER BY show_datetime ASC";
        Map<String, List<Map<String, Object>>> dateTimeMap = new LinkedHashMap<>();

        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String showId = rs.getString("id");
                    Timestamp ts  = rs.getTimestamp("show_datetime");
                    if (ts == null) continue;

                    // Extract date and time parts
                    String datePart     = new SimpleDateFormat("yyyy-MM-dd").format(ts);
                    String timePart     = new SimpleDateFormat("HH:mm:ss").format(ts);
                    String isoDateTime  = datePart + "T" + timePart; // e.g. "2026-03-20T16:00:00"

                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("time",      isoDateTime);
                    entry.put("showId",    showId);
                    entry.put("showPrice", rs.getDouble("show_price"));

                    dateTimeMap.computeIfAbsent(datePart, k -> new ArrayList<>()).add(entry);
                }
            }
        } catch (Exception e) {
            System.err.println("[ShowDAO] getDateTimeMapForMovie error: " + e.getMessage());
        }
        return dateTimeMap;
    }

    /**
     * Returns all shows with movie title (admin list).
     * Shape: {_id, showDateTime, showPrice, occupiedSeats:{seatId:true,...}, movie:{title}}
     */
    public static List<Map<String, Object>> getAllShowsAdmin() {
        String sql =
            "SELECT s.id, s.show_datetime, s.show_price, m.title, " +
            "       os.seat_id " +
            "FROM Shows s " +
            "JOIN Movies m ON s.movie_id = m.id " +
            "LEFT JOIN OccupiedSeats os ON os.show_id = s.id " +
            "ORDER BY s.show_datetime ASC";

        // showId -> show map
        java.util.LinkedHashMap<String, Map<String, Object>> showMap = new java.util.LinkedHashMap<>();

        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String showId = rs.getString("id");

                if (!showMap.containsKey(showId)) {
                    Map<String, Object> show = new LinkedHashMap<>();
                    show.put("_id", showId);
                    Timestamp ts = rs.getTimestamp("show_datetime");
                    show.put("showDateTime", ts != null ? ts.toString() : null);
                    show.put("showPrice", rs.getDouble("show_price"));

                    Map<String, Object> movie = new LinkedHashMap<>();
                    movie.put("title", rs.getString("title"));
                    show.put("movie", movie);
                    show.put("occupiedSeats", new LinkedHashMap<String, Boolean>());
                    showMap.put(showId, show);
                }

                String seatId = rs.getString("seat_id");
                if (seatId != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Boolean> seats = (Map<String, Boolean>) showMap.get(showId).get("occupiedSeats");
                    seats.put(seatId, true);
                }
            }
        } catch (Exception e) {
            System.err.println("[ShowDAO] getAllShowsAdmin error: " + e.getMessage());
        }
        return new ArrayList<>(showMap.values());
    }

    /**
     * Counts shows that have a datetime in the future.
     */
    public static int countActiveShows() {
        String sql = "SELECT COUNT(*) FROM Shows WHERE show_datetime >= GETDATE()";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {
            System.err.println("[ShowDAO] countActiveShows error: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Returns up to {@code limit} upcoming shows for the dashboard activeShows list.
     * Shape per item: {_id, showPrice, showDateTime, movie:{title, poster_path, vote_average}}
     */
    public static List<Map<String, Object>> getActiveShowsForDashboard(int limit) {
        String sql =
            "SELECT TOP (?) s.id, s.show_datetime, s.show_price, " +
            "       m.title, m.poster_path, m.vote_average " +
            "FROM Shows s " +
            "JOIN Movies m ON s.movie_id = m.id " +
            "WHERE s.show_datetime >= GETDATE() " +
            "ORDER BY s.show_datetime ASC";

        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> show = new LinkedHashMap<>();
                    show.put("_id", rs.getString("id"));
                    Timestamp ts = rs.getTimestamp("show_datetime");
                    show.put("showDateTime", ts != null ? ts.toString() : null);
                    show.put("showPrice", rs.getDouble("show_price"));

                    Map<String, Object> movie = new LinkedHashMap<>();
                    movie.put("title",        rs.getString("title"));
                    movie.put("poster_path",  rs.getString("poster_path"));
                    movie.put("vote_average", rs.getDouble("vote_average"));
                    show.put("movie", movie);
                    result.add(show);
                }
            }
        } catch (Exception e) {
            System.err.println("[ShowDAO] getActiveShowsForDashboard error: " + e.getMessage());
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Delete
    // -----------------------------------------------------------------------

    /**
     * Updates the show_datetime and/or show_price of an existing show.
     *
     * @return true on success
     */
    public static boolean updateShow(String showId, String showDatetime, double showPrice) {
        String sql = "UPDATE Shows SET show_datetime = ?, show_price = ? WHERE id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            Timestamp ts = parseTimestamp(showDatetime);
            if (ts == null) {
                System.err.println("[ShowDAO] updateShow: invalid datetime " + showDatetime);
                return false;
            }
            ps.setTimestamp(1, ts);
            ps.setDouble(2, showPrice);
            ps.setString(3, showId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[ShowDAO] updateShow error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a show and all associated OccupiedSeats and Bookings in one transaction.
     *
     * @return true on success
     */
    public static boolean deleteShow(String showId) {
        Connection conn = null;
        try {
            conn = DBConfig.getConnection();
            conn.setAutoCommit(false);

            // 1. Delete occupied seats
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM OccupiedSeats WHERE show_id = ?")) {
                ps.setString(1, showId);
                ps.executeUpdate();
            }
            // 2. Delete bookings
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM Bookings WHERE show_id = ?")) {
                ps.setString(1, showId);
                ps.executeUpdate();
            }
            // 3. Delete the show
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM Shows WHERE id = ?")) {
                ps.setString(1, showId);
                int rows = ps.executeUpdate();
                if (rows == 0) { conn.rollback(); return false; }
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            System.err.println("[ShowDAO] deleteShow error: " + e.getMessage());
            if (conn != null) { try { conn.rollback(); } catch (Exception ignored) {} }
            return false;
        } finally {
            if (conn != null) { try { conn.setAutoCommit(true); conn.close(); } catch (Exception ignored) {} }
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Parses an ISO datetime string like "2024-07-10T18:30" or "2024-07-10T18:30:00"
     * into a {@link Timestamp}.
     */
    /**
     * Parses a JSON array string (stored in DB) into a Java List.
     * Returns an empty list if null or unparseable.
     */
    @SuppressWarnings("unchecked")
    private static List<Object> parseJsonArray(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, List.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static Timestamp parseTimestamp(String s) {
        if (s == null || s.isBlank()) return null;
        // Normalize: replace T separator with space
        String normalized = s.replace("T", " ");
        // Ensure seconds are present
        if (normalized.length() == 16) normalized += ":00";
        try {
            return Timestamp.valueOf(normalized);
        } catch (Exception e) {
            System.err.println("[ShowDAO] parseTimestamp failed for: " + s);
            return null;
        }
    }
}
