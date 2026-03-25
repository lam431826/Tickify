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
 * PUT /api/admin/show/update — admin auth required.
 *
 * Request body: {"showId": "uuid", "showDateTime": "2026-07-10T18:30", "showPrice": 12.5}
 * Response: {success:true, message:"Show updated"}
 */
public class UpdateShowServlet extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
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

        if (!body.has("showId") || !body.has("showDateTime") || !body.has("showPrice")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(resp, "Missing required fields: showId, showDateTime, showPrice");
            return;
        }

        String showId       = body.get("showId").asText();
        String showDateTime = body.get("showDateTime").asText();
        double showPrice    = body.get("showPrice").asDouble();

        if (showPrice <= 0) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(resp, "showPrice must be greater than 0");
            return;
        }

        boolean updated = ShowDAO.updateShow(showId, showDateTime, showPrice);
        if (!updated) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            writeError(resp, "Show not found or invalid datetime");
            return;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Show updated");
        MAPPER.writeValue(resp.getWriter(), response);
    }

    private void writeError(HttpServletResponse resp, String message) throws IOException {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("success", false);
        error.put("message", message);
        MAPPER.writeValue(resp.getWriter(), error);
    }
}
