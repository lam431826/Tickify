package com.quickshow.servlet.show;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshow.dao.MovieDAO;
import com.quickshow.dao.ShowDAO;
import com.quickshow.dao.UserDAO;
import com.quickshow.util.AuthUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * POST /api/show/add — admin auth required.
 *
 * Request body (JSON):
 * {
 *   "movieId": 12345,
 *   "showsInput": [{"date": "2024-07-10", "time": ["18:30:00", "21:00:00"]}, ...],
 *   "showPrice": 12.50
 * }
 *
 * Note: the client sends showsInput as [{date, time}] where time is an array
 * of strings (one entry per datetime-local input the admin added).
 *
 * Response: {success:true, message:"Shows added successfully"}
 */
@WebServlet("/api/show/add")
public class AddShowServlet extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        // Auth: require admin
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

        // Parse request body
        JsonNode body;
        try {
            String bodyStr = req.getReader().lines().collect(Collectors.joining());
            body = MAPPER.readTree(bodyStr);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(resp, "Invalid JSON body");
            return;
        }

        // Validate required fields
        if (!body.has("movieId") || !body.has("showsInput") || !body.has("showPrice")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(resp, "Missing required fields: movieId, showsInput, showPrice");
            return;
        }

        int movieId     = body.get("movieId").asInt();
        double showPrice = body.get("showPrice").asDouble();
        JsonNode showsInput = body.get("showsInput");

        if (!showsInput.isArray() || showsInput.size() == 0) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(resp, "showsInput must be a non-empty array");
            return;
        }

        // Step 1: Fetch movie from TMDB and upsert into DB
        boolean movieSaved = MovieDAO.upsertMovieFromTmdb(movieId);
        if (!movieSaved) {
            resp.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            writeError(resp, "Failed to fetch movie details from TMDB for movieId: " + movieId);
            return;
        }

        // Step 2: Insert each show
        // showsInput format from client: [{date: "2024-07-10", time: ["18:30", "21:00"]}]
        // Each item has a date and an array of times.
        List<String> createdIds = new ArrayList<>();
        List<String> failures   = new ArrayList<>();

        for (JsonNode showItem : showsInput) {
            String date = showItem.has("date") ? showItem.get("date").asText("") : "";

            // "time" can be a single string or an array of strings
            JsonNode timeNode = showItem.get("time");
            List<String> times = new ArrayList<>();
            if (timeNode != null) {
                if (timeNode.isArray()) {
                    for (JsonNode t : timeNode) times.add(t.asText());
                } else {
                    times.add(timeNode.asText());
                }
            }

            for (String time : times) {
                // Combine date and time into ISO datetime
                String datetime = date + "T" + time;
                String newShowId = ShowDAO.insertShow(movieId, datetime, showPrice);
                if (newShowId != null) {
                    createdIds.add(newShowId);
                } else {
                    failures.add(datetime);
                }
            }
        }

        if (createdIds.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeError(resp, "Failed to create any shows. Check datetime format.");
            return;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Shows added successfully. Created: " + createdIds.size()
                + (failures.isEmpty() ? "" : ", Failed: " + failures.size()));
        response.put("createdShowIds", createdIds);
        MAPPER.writeValue(resp.getWriter(), response);
    }

    private void writeError(HttpServletResponse resp, String message) throws IOException {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("success", false);
        error.put("message", message);
        MAPPER.writeValue(resp.getWriter(), error);
    }
}
