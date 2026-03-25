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

/**
 * POST /api/auth/login — public endpoint.
 * Body: {email, password}
 * Response: {success:true, token, user:{id, name, email, isAdmin}}
 *        or {success:false, message:"Invalid email or password"}
 */
@WebServlet("/api/auth/login")
public class LoginServlet extends HttpServlet {

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

        String email    = body.has("email")    ? body.get("email").asText().trim() : "";
        String password = body.has("password") ? body.get("password").asText()     : "";

        if (email.isBlank() || password.isBlank()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            MAPPER.writeValue(resp.getWriter(), Map.of("success", false, "message", "email and password are required"));
            return;
        }

        // Find user by email
        Map<String, Object> user = UserDAO.findByEmail(email);
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("message", "Invalid email or password");
            MAPPER.writeValue(resp.getWriter(), error);
            return;
        }

        // Verify password
        String storedHash = (String) user.get("passwordHash");
        if (storedHash == null || storedHash.isBlank()) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("message", "Invalid email or password");
            MAPPER.writeValue(resp.getWriter(), error);
            return;
        }

        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), storedHash);
        if (!result.verified) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("message", "Invalid email or password");
            MAPPER.writeValue(resp.getWriter(), error);
            return;
        }

        // Generate JWT
        String userId = (String) user.get("id");
        String token  = AuthUtil.generateToken(userId);
        if (token == null) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            MAPPER.writeValue(resp.getWriter(), Map.of("success", false, "message", "Failed to generate token — check JWT_SECRET"));
            return;
        }

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("id",      userId);
        userInfo.put("name",    user.get("name"));
        userInfo.put("email",   user.get("email"));
        userInfo.put("isAdmin", user.get("isAdmin"));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("token",   token);
        response.put("user",    userInfo);

        MAPPER.writeValue(resp.getWriter(), response);
    }
}
