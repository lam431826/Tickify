package com.quickshow.servlet.user;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshow.dao.UserDAO;
import com.quickshow.util.AuthUtil;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PUT /api/user/update-profile — user auth required.
 *
 * Request body (all fields optional except at least one of name/email/newPassword):
 * {
 *   "name": "New Name",
 *   "email": "new@email.com",
 *   "currentPassword": "oldpass",   // required when changing password
 *   "newPassword": "newpass"
 * }
 *
 * Response: {success:true, user:{id,name,email,isAdmin}}
 */
public class UpdateProfileServlet extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        String userId = AuthUtil.getUserIdFromRequest(req);
        if (userId == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            writeError(resp, "Unauthorized");
            return;
        }

        JsonNode body;
        try {
            String bodyStr = req.getReader().lines().collect(Collectors.joining());
            body = MAPPER.readTree(bodyStr);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(resp, "Invalid JSON body");
            return;
        }

        String name    = body.has("name")    ? body.get("name").asText().trim()    : null;
        String email   = body.has("email")   ? body.get("email").asText().trim()   : null;
        String newPass = body.has("newPassword") ? body.get("newPassword").asText() : null;
        String curPass = body.has("currentPassword") ? body.get("currentPassword").asText() : null;

        // Validate
        if ((name == null || name.isEmpty()) && (email == null || email.isEmpty()) && (newPass == null || newPass.isEmpty())) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(resp, "Nothing to update");
            return;
        }

        // Get current user to preserve values
        Map<String, Object> currentUser = UserDAO.findById(userId);
        if (currentUser == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            writeError(resp, "User not found");
            return;
        }

        // Update name/email
        String newName  = (name  != null && !name.isEmpty())  ? name  : (String) currentUser.get("name");
        String newEmail = (email != null && !email.isEmpty()) ? email : (String) currentUser.get("email");

        boolean profileUpdated = UserDAO.updateProfile(userId, newName, newEmail);
        if (!profileUpdated) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeError(resp, "Failed to update profile");
            return;
        }

        // Update password if requested
        if (newPass != null && !newPass.isEmpty()) {
            if (curPass == null || curPass.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                writeError(resp, "Current password is required to set a new password");
                return;
            }
            String hash = UserDAO.getPasswordHash(userId);
            if (hash == null || BCrypt.verifyer().verify(curPass.toCharArray(), hash).verified == false) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                writeError(resp, "Current password is incorrect");
                return;
            }
            if (newPass.length() < 6) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                writeError(resp, "New password must be at least 6 characters");
                return;
            }
            String newHash = BCrypt.withDefaults().hashToString(12, newPass.toCharArray());
            UserDAO.updatePassword(userId, newHash);
        }

        // Return updated user
        Map<String, Object> updatedUser = UserDAO.findById(userId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("user", updatedUser);
        MAPPER.writeValue(resp.getWriter(), response);
    }

    private void writeError(HttpServletResponse resp, String message) throws IOException {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("success", false);
        error.put("message", message);
        MAPPER.writeValue(resp.getWriter(), error);
    }
}
