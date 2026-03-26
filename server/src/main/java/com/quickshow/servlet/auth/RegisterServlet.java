package com.quickshow.servlet.auth;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshow.dao.UserDAO;
import com.quickshow.util.AuthUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * POST /api/auth/register — public endpoint.
 * Body: {name, email, password}
 * Response: {success:true, token, user:{id, name, email, isAdmin}}
 *        or {success:false, message:"Email already registered"}
 */
@WebServlet("/api/auth/register")
public class RegisterServlet extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        JsonNode body;
        try {
            body = MAPPER.readTree(req.getInputStream());
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            MAPPER.writeValue(resp.getWriter(), Map.of("success", false, "message", "Invalid JSON body"));
            return;
        }

        String name     = body.has("name")     ? body.get("name").asText().trim()     : "";
        String username = body.has("username") ? body.get("username").asText().trim() : "";
        String email    = body.has("email")    ? body.get("email").asText().trim()    : "";
        String password = body.has("password") ? body.get("password").asText()        : "";

        if (name.isBlank() || username.isBlank() || email.isBlank() || password.isBlank()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            MAPPER.writeValue(resp.getWriter(), Map.of("success", false, "message", "name, username, email, and password are required"));
            return;
        }

        // Check if username already exists
        if (UserDAO.findByUsername(username) != null) {
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("message", "Username already taken");
            MAPPER.writeValue(resp.getWriter(), error);
            return;
        }

        // Check if email already exists
        if (UserDAO.findByEmail(email) != null) {
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("message", "Email already registered");
            MAPPER.writeValue(resp.getWriter(), error);
            return;
        }

        // Hash password
        String passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray());

        // Generate unique user ID
        String userId = UUID.randomUUID().toString();

        // Insert user
        boolean created = UserDAO.createUser(userId, name, username, email, passwordHash);
        if (!created) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            MAPPER.writeValue(resp.getWriter(), Map.of("success", false, "message", "Failed to create user"));
            return;
        }

        // Generate JWT
        String token = AuthUtil.generateToken(userId);
        if (token == null) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            MAPPER.writeValue(resp.getWriter(), Map.of("success", false, "message", "Failed to generate token — check JWT_SECRET"));
            return;
        }

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("id",       userId);
        userInfo.put("name",     name);
        userInfo.put("username", username);
        userInfo.put("email",    email);
        userInfo.put("isAdmin",  false);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("token",   token);
        response.put("user",    userInfo);

        resp.setStatus(HttpServletResponse.SC_CREATED);
        MAPPER.writeValue(resp.getWriter(), response);
    }
}
