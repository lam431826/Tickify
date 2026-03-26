package com.quickshow.dao;

import com.quickshow.config.DBConfig;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Data access object for the Users table.
 */
public class UserDAO {

    // -----------------------------------------------------------------------
    // Find / Create
    // -----------------------------------------------------------------------

    /**
     * Returns the user record for the given userId, or null if not found.
     *
     * @param userId the user's ID
     * @return a Map with keys: id, name, email, isAdmin — or null if not found
     */
    public static Map<String, Object> getOrCreateUser(String userId) {
        return findById(userId);
    }

    /**
     * Finds a user by their ID.
     *
     * @param userId the user's ID
     * @return a Map with keys: id, name, email, isAdmin — or null if not found
     */
    public static Map<String, Object> findById(String userId) {
        String sql = "SELECT id, name, email, is_admin FROM Users WHERE id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> user = new HashMap<>();
                    user.put("id",      rs.getString("id"));
                    user.put("name",    rs.getString("name"));
                    user.put("email",   rs.getString("email"));
                    user.put("isAdmin", rs.getBoolean("is_admin"));
                    return user;
                }
            }
        } catch (Exception e) {
            System.err.println("[UserDAO] findById error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Finds a user by their email address.
     *
     * @param email the user's email
     * @return a Map with keys: id, name, email, isAdmin, passwordHash — or null if not found
     */
    public static Map<String, Object> findByEmail(String email) {
        String sql = "SELECT id, name, username, email, is_admin, password_hash FROM Users WHERE email = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> user = new HashMap<>();
                    user.put("id",           rs.getString("id"));
                    user.put("name",         rs.getString("name"));
                    user.put("username",     rs.getString("username"));
                    user.put("email",        rs.getString("email"));
                    user.put("isAdmin",      rs.getBoolean("is_admin"));
                    user.put("passwordHash", rs.getString("password_hash"));
                    return user;
                }
            }
        } catch (Exception e) {
            System.err.println("[UserDAO] findByEmail error: " + e.getMessage());
        }
        return null;
    }

    public static Map<String, Object> findByUsername(String username) {
        String sql = "SELECT id, name, username, email, is_admin, password_hash FROM Users WHERE username = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> user = new HashMap<>();
                    user.put("id",           rs.getString("id"));
                    user.put("name",         rs.getString("name"));
                    user.put("username",     rs.getString("username"));
                    user.put("email",        rs.getString("email"));
                    user.put("isAdmin",      rs.getBoolean("is_admin"));
                    user.put("passwordHash", rs.getString("password_hash"));
                    return user;
                }
            }
        } catch (Exception e) {
            System.err.println("[UserDAO] findByUsername error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Inserts a new user with a hashed password.
     *
     * @param id           the user's unique ID
     * @param name         the user's display name
     * @param email        the user's email
     * @param passwordHash the BCrypt-hashed password
     * @return true if the insert succeeded, false otherwise
     */
    public static boolean createUser(String id, String name, String username, String email, String passwordHash) {
        String sql = "INSERT INTO Users (id, name, username, email, is_admin, password_hash) VALUES (?, ?, ?, ?, 0, ?)";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, name != null ? name : "");
            ps.setString(3, username != null ? username : "");
            ps.setString(4, email != null ? email : "");
            ps.setString(5, passwordHash);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            System.err.println("[UserDAO] createUser error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns true if the user is an admin.
     */
    public static boolean isAdmin(String userId) {
        String sql = "SELECT is_admin FROM Users WHERE id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("is_admin");
                }
            }
        } catch (Exception e) {
            System.err.println("[UserDAO] isAdmin error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Counts total users.
     */
    public static int countUsers() {
        String sql = "SELECT COUNT(*) FROM Users";
        try (Connection conn = DBConfig.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {
            System.err.println("[UserDAO] countUsers error: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Updates user name and/or email. Either field may be null to leave it unchanged.
     *
     * @return true on success
     */
    public static boolean updateProfile(String userId, String name, String email) {
        String sql = "UPDATE Users SET name = ?, email = ? WHERE id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[UserDAO] updateProfile error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates the BCrypt password hash for a user.
     *
     * @return true on success
     */
    public static boolean updatePassword(String userId, String newPasswordHash) {
        String sql = "UPDATE Users SET password_hash = ? WHERE id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setString(2, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[UserDAO] updatePassword error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns password_hash for a user (for verification before change).
     */
    public static String getPasswordHash(String userId) {
        String sql = "SELECT password_hash FROM Users WHERE id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("password_hash");
            }
        } catch (Exception e) {
            System.err.println("[UserDAO] getPasswordHash error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Returns user name for a given userId.
     */
    public static String getUserName(String userId) {
        String sql = "SELECT name FROM Users WHERE id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("name");
            }
        } catch (Exception e) {
            System.err.println("[UserDAO] getUserName error: " + e.getMessage());
        }
        return "Unknown";
    }
}
