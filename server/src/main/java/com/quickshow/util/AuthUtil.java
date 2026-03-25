package com.quickshow.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * Utility class for custom HS256 JWT authentication.
 *
 * <p>Flow:
 * <ol>
 *   <li>Client sends {@code Authorization: Bearer <jwt>} header.</li>
 *   <li>Server verifies the JWT using HMAC256 with the {@code JWT_SECRET} env var.</li>
 *   <li>The {@code sub} claim is returned as the userId.</li>
 * </ol>
 */
public class AuthUtil {

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Extracts and verifies the JWT from the Authorization header.
     *
     * @param request the incoming HTTP request
     * @return the user ID (sub claim), or {@code null} if auth fails
     */
    public static String getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7).trim();
        return verifyJwt(token);
    }

    /**
     * Generates a signed HS256 JWT for the given userId, expiring in 7 days.
     *
     * @param userId the user's ID to embed as the subject
     * @return signed JWT string, or null if JWT_SECRET is not configured
     */
    public static String generateToken(String userId) {
        String secret = getSecret();
        if (secret == null) {
            System.err.println("[AuthUtil] JWT_SECRET is not set — cannot generate token");
            return null;
        }
        return JWT.create()
                .withSubject(userId)
                .withExpiresAt(new Date(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000))
                .sign(Algorithm.HMAC256(secret));
    }

    /**
     * Verifies an HS256 JWT and returns the sub (userId) claim.
     *
     * @param token raw JWT string
     * @return userId or null on any verification failure
     */
    public static String verifyJwt(String token) {
        String secret = getSecret();
        if (secret == null) {
            System.err.println("[AuthUtil] JWT_SECRET is not set — cannot verify token");
            return null;
        }
        try {
            DecodedJWT decoded = JWT.require(Algorithm.HMAC256(secret))
                    .build()
                    .verify(token);
            String userId = decoded.getSubject();
            System.err.println("[AuthUtil] Token verified OK, userId=" + userId);
            return userId;
        } catch (JWTVerificationException e) {
            System.err.println("[AuthUtil] JWT verification failed: " + e.getMessage());
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static String getSecret() {
        String val = System.getenv("JWT_SECRET");
        if (val == null || val.isBlank()) val = System.getProperty("JWT_SECRET");
        return (val == null || val.isBlank()) ? null : val;
    }

    private AuthUtil() {}
}
