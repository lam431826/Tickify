package com.quickshow.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for TMDB (The Movie Database) API calls.
 *
 * <p>Uses {@code TMDB_API_KEY} environment variable.
 * Base URL: https://api.themoviedb.org/3
 */
public class TmdbUtil {

    private static final String BASE_URL = "https://api.themoviedb.org/3";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Fetches the now-playing movies list from TMDB.
     *
     * @return JsonNode representing the TMDB response (contains "results" array),
     *         or null on error
     */
    public static JsonNode fetchNowPlaying() {
        String apiKey = getApiKey();
        if (apiKey == null) return null;
        String url = BASE_URL + "/movie/now_playing?api_key=" + apiKey + "&language=en-US&page=1";
        return fetchJson(url);
    }

    /**
     * Fetches full movie details including genres and cast from TMDB.
     *
     * @param movieId TMDB movie ID
     * @return JsonNode with full movie data (includes credits.cast, genres), or null
     */
    public static JsonNode fetchMovieDetails(int movieId) {
        String apiKey = getApiKey();
        if (apiKey == null) return null;
        String url = BASE_URL + "/movie/" + movieId + "?api_key=" + apiKey
                + "&append_to_response=credits&language=en-US";
        return fetchJson(url);
    }

    /**
     * Extracts genres JSON string from TMDB movie details node.
     * Format: [{"name":"Action"},{"name":"Drama"}]
     */
    public static String extractGenresJson(JsonNode movieNode) {
        try {
            JsonNode genresNode = movieNode.get("genres");
            if (genresNode == null || !genresNode.isArray()) return "[]";
            List<String> names = new ArrayList<>();
            for (JsonNode g : genresNode) {
                JsonNode nameNode = g.get("name");
                if (nameNode != null) {
                    names.add("{\"name\":\"" + escapeJson(nameNode.asText()) + "\"}");
                }
            }
            return "[" + String.join(",", names) + "]";
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * Extracts top-10 cast members JSON string from TMDB credits.
     * Format: [{"name":"...","profile_path":"..."}]
     */
    public static String extractCastsJson(JsonNode movieNode) {
        try {
            JsonNode credits = movieNode.get("credits");
            if (credits == null) return "[]";
            JsonNode castNode = credits.get("cast");
            if (castNode == null || !castNode.isArray()) return "[]";

            List<String> casts = new ArrayList<>();
            int count = 0;
            for (JsonNode c : castNode) {
                if (count >= 10) break;
                String name = c.has("name") ? escapeJson(c.get("name").asText()) : "";
                String profilePath = c.has("profile_path") && !c.get("profile_path").isNull()
                        ? escapeJson(c.get("profile_path").asText()) : "";
                casts.add("{\"name\":\"" + name + "\",\"profile_path\":\"" + profilePath + "\"}");
                count++;
            }
            return "[" + String.join(",", casts) + "]";
        } catch (Exception e) {
            return "[]";
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private static JsonNode fetchJson(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            int status = conn.getResponseCode();
            if (status != 200) {
                System.err.println("[TmdbUtil] HTTP " + status + " for URL: " + urlString);
                return null;
            }
            try (InputStream is = conn.getInputStream()) {
                return MAPPER.readTree(is);
            }
        } catch (Exception e) {
            System.err.println("[TmdbUtil] Error fetching " + urlString + ": " + e.getMessage());
            return null;
        }
    }

    private static String getApiKey() {
        String key = System.getenv("TMDB_API_KEY");
        if (key == null || key.isBlank()) key = System.getProperty("TMDB_API_KEY");
        if (key == null || key.isBlank()) {
            System.err.println("[TmdbUtil] TMDB_API_KEY environment variable is not set.");
            return null;
        }
        return key;
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    private TmdbUtil() {}
}
