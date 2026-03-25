package com.quickshow.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshow.config.DBConfig;
import com.quickshow.util.TmdbUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data access object for the Movies table.
 */
public class MovieDAO {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // -----------------------------------------------------------------------
    // Upsert (used when admin adds a show)
    // -----------------------------------------------------------------------

    /**
     * Fetches movie details from TMDB and upserts the record into the Movies table.
     *
     * @param movieId TMDB movie ID
     * @return true on success
     */
    public static boolean upsertMovieFromTmdb(int movieId) {
        JsonNode node = TmdbUtil.fetchMovieDetails(movieId);
        if (node == null) return false;

        String title       = safeText(node, "title");
        String overview    = safeText(node, "overview");
        String posterPath  = safeText(node, "poster_path");
        String backdropPath= safeText(node, "backdrop_path");
        String releaseDate = safeText(node, "release_date");
        int    runtime     = node.has("runtime") ? node.get("runtime").asInt(0) : 0;
        double voteAvg     = node.has("vote_average") ? node.get("vote_average").asDouble(0) : 0;
        int    voteCount   = node.has("vote_count") ? node.get("vote_count").asInt(0) : 0;
        String genresJson  = TmdbUtil.extractGenresJson(node);
        String castsJson   = TmdbUtil.extractCastsJson(node);

        // SQL Server MERGE (upsert)
        String sql = "MERGE Movies AS target " +
                "USING (SELECT ? AS id) AS source ON target.id = source.id " +
                "WHEN MATCHED THEN UPDATE SET " +
                "  title=?, overview=?, poster_path=?, backdrop_path=?, release_date=?, " +
                "  runtime=?, vote_average=?, vote_count=?, genres=?, casts=? " +
                "WHEN NOT MATCHED THEN INSERT " +
                "  (id,title,overview,poster_path,backdrop_path,release_date,runtime,vote_average,vote_count,genres,casts) " +
                "  VALUES (?,?,?,?,?,?,?,?,?,?,?);";

        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // USING clause
            ps.setInt(1, movieId);
            // UPDATE set
            ps.setString(2, title);
            ps.setString(3, overview);
            ps.setString(4, posterPath);
            ps.setString(5, backdropPath);
            ps.setString(6, releaseDate.isBlank() ? null : releaseDate);
            ps.setInt(7, runtime);
            ps.setDouble(8, voteAvg);
            ps.setInt(9, voteCount);
            ps.setString(10, genresJson);
            ps.setString(11, castsJson);
            // INSERT values
            ps.setInt(12, movieId);
            ps.setString(13, title);
            ps.setString(14, overview);
            ps.setString(15, posterPath);
            ps.setString(16, backdropPath);
            ps.setString(17, releaseDate.isBlank() ? null : releaseDate);
            ps.setInt(18, runtime);
            ps.setDouble(19, voteAvg);
            ps.setInt(20, voteCount);
            ps.setString(21, genresJson);
            ps.setString(22, castsJson);

            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            System.err.println("[MovieDAO] upsertMovieFromTmdb error: " + e.getMessage());
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // Read
    // -----------------------------------------------------------------------

    /**
     * Returns a full movie record as a Map.
     * genres and casts are returned as parsed JSON lists.
     */
    public static Map<String, Object> findById(int movieId) {
        String sql = "SELECT * FROM Movies WHERE id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rowToMap(rs);
                }
            }
        } catch (Exception e) {
            System.err.println("[MovieDAO] findById error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Returns a lightweight movie map with fields needed by the client.
     */
    public static Map<String, Object> findByIdLight(int movieId) {
        String sql = "SELECT id, title, poster_path, vote_average, vote_count, release_date FROM Movies WHERE id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("_id",          rs.getInt("id"));
                    m.put("title",        rs.getString("title"));
                    m.put("poster_path",  rs.getString("poster_path"));
                    m.put("vote_average", rs.getDouble("vote_average"));
                    m.put("vote_count",   rs.getInt("vote_count"));
                    Date rd = rs.getDate("release_date");
                    m.put("release_date", rd != null ? rd.toString() : null);
                    return m;
                }
            }
        } catch (Exception e) {
            System.err.println("[MovieDAO] findByIdLight error: " + e.getMessage());
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // Favorites
    // -----------------------------------------------------------------------

    /**
     * Returns the list of favorite movie IDs for a user.
     */
    public static List<Integer> getFavoriteMovieIds(String userId) {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT movie_id FROM Favorites WHERE user_id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("movie_id"));
            }
        } catch (Exception e) {
            System.err.println("[MovieDAO] getFavoriteMovieIds error: " + e.getMessage());
        }
        return ids;
    }

    /**
     * Returns the full list of favorite movies (light) for a user.
     */
    public static List<Map<String, Object>> getFavoriteMovies(String userId) {
        List<Map<String, Object>> movies = new ArrayList<>();
        String sql = "SELECT m.id, m.title, m.poster_path, m.vote_average, m.release_date " +
                     "FROM Movies m JOIN Favorites f ON m.id = f.movie_id " +
                     "WHERE f.user_id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("_id",          rs.getInt("id"));
                    m.put("title",        rs.getString("title"));
                    m.put("poster_path",  rs.getString("poster_path"));
                    m.put("vote_average", rs.getDouble("vote_average"));
                    Date rd = rs.getDate("release_date");
                    m.put("release_date", rd != null ? rd.toString() : null);
                    movies.add(m);
                }
            }
        } catch (Exception e) {
            System.err.println("[MovieDAO] getFavoriteMovies error: " + e.getMessage());
        }
        return movies;
    }

    /**
     * Toggles a favorite: adds if absent, removes if present.
     *
     * @return true if now favorited, false if now unfavorited
     */
    public static boolean toggleFavorite(String userId, int movieId) {
        // Check existence
        String checkSql = "SELECT COUNT(*) FROM Favorites WHERE user_id = ? AND movie_id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
            checkPs.setString(1, userId);
            checkPs.setInt(2, movieId);
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    // Remove
                    String delSql = "DELETE FROM Favorites WHERE user_id = ? AND movie_id = ?";
                    try (PreparedStatement delPs = conn.prepareStatement(delSql)) {
                        delPs.setString(1, userId);
                        delPs.setInt(2, movieId);
                        delPs.executeUpdate();
                    }
                    return false;
                } else {
                    // Add
                    String insSql = "INSERT INTO Favorites (user_id, movie_id) VALUES (?, ?)";
                    try (PreparedStatement insPs = conn.prepareStatement(insSql)) {
                        insPs.setString(1, userId);
                        insPs.setInt(2, movieId);
                        insPs.executeUpdate();
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("[MovieDAO] toggleFavorite error: " + e.getMessage());
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    static Map<String, Object> rowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",           rs.getInt("id"));
        m.put("title",        rs.getString("title"));
        m.put("overview",     rs.getString("overview"));
        m.put("poster_path",  rs.getString("poster_path"));
        m.put("backdrop_path",rs.getString("backdrop_path"));
        Date rd = rs.getDate("release_date");
        m.put("release_date", rd != null ? rd.toString() : null);
        m.put("runtime",      rs.getInt("runtime"));
        m.put("vote_average", rs.getDouble("vote_average"));
        m.put("vote_count",   rs.getInt("vote_count"));

        // Parse stored JSON strings back to objects for proper serialization
        m.put("genres", parseJsonArray(rs.getString("genres")));
        m.put("casts",  parseJsonArray(rs.getString("casts")));
        return m;
    }

    private static List<Object> parseJsonArray(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            JsonNode node = MAPPER.readTree(json);
            if (node.isArray()) {
                List<Object> list = new ArrayList<>();
                for (JsonNode item : node) {
                    list.add(MAPPER.convertValue(item, Map.class));
                }
                return list;
            }
        } catch (Exception ignored) {}
        return Collections.emptyList();
    }

    private static String safeText(JsonNode node, String field) {
        return (node.has(field) && !node.get(field).isNull()) ? node.get(field).asText("") : "";
    }
}
