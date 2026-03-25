package com.quickshow.servlet.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshow.dao.ShowDAO;
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
 * DELETE /api/admin/show/delete — admin auth required.
 *
 * Request body: {"showId": "uuid"}
 * Response: {success:true, message:"Show deleted"}
 */
public class DeleteShowServlet extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        String userId = AuthUtil.getUserIdFromRequest(req);
        if (userId == null || !UserDAO.isAdmin(userId)) {
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

        if (!body.has("showId")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(resp, "Missing required field: showId");
            return;
        }

        String showId = body.get("showId").asText();
        boolean deleted = ShowDAO.deleteShow(showId);

        if (!deleted) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            writeError(resp, "Show not found or could not be deleted");
            return;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Show deleted");
        MAPPER.writeValue(resp.getWriter(), response);
    }

    private void writeError(HttpServletResponse resp, String message) throws IOException {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("success", false);
        error.put("message", message);
        MAPPER.writeValue(resp.getWriter(), error);
    }
}
