package com.quickshow.servlet.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * GET /api/admin/all-shows — admin auth required.
 * Returns all shows with movie title and occupied seat map.
 *
 * Response: {success:true, shows:[{_id, showDateTime, showPrice,
 *            occupiedSeats:{"A1":true,...}, movie:{title}}]}
 */
@WebServlet("/api/admin/all-shows")
public class GetAllShowsAdminServlet extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        String userId = AuthUtil.getUserIdFromRequest(req);
        if (userId == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            writeError(resp, "Unauthorized");
            return;
        }
        UserDAO.getOrCreateUser(userId);
        if (!UserDAO.isAdmin(userId)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            writeError(resp, "Admin access required");
            return;
        }

        List<Map<String, Object>> shows = ShowDAO.getAllShowsAdmin();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("shows",   shows);
        MAPPER.writeValue(resp.getWriter(), response);
    }

    private void writeError(HttpServletResponse resp, String message) throws IOException {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("success", false);
        error.put("message", message);
        MAPPER.writeValue(resp.getWriter(), error);
    }
}
