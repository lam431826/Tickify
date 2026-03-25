package com.quickshow.servlet.admin;

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
 * GET /api/admin/is-admin — user auth required.
 * Returns whether the authenticated user has admin privileges,
 * along with basic user info for session restoration.
 *
 * Response: {success:true, isAdmin:bool, user:{id, name, isAdmin}}
 */
@WebServlet("/api/admin/is-admin")
public class IsAdminServlet extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        String userId = AuthUtil.getUserIdFromRequest(req);
        if (userId == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("message", "Unauthorized");
            MAPPER.writeValue(resp.getWriter(), error);
            return;
        }

        boolean isAdmin = UserDAO.isAdmin(userId);

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("id",      userId);
        userInfo.put("name",    UserDAO.getUserName(userId));
        userInfo.put("isAdmin", isAdmin);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("isAdmin", isAdmin);
        response.put("user",    userInfo);
        MAPPER.writeValue(resp.getWriter(), response);
    }
}
