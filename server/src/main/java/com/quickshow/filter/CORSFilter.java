package com.quickshow.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * CORS Filter for all /api/* endpoints.
 * Handles preflight OPTIONS requests and sets appropriate headers
 * for cross-origin requests from the React client.
 */
@WebFilter("/api/*")
public class CORSFilter implements Filter {

    private String clientUrl;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Read CLIENT_URL from env with fallback to context param
        clientUrl = System.getenv("CLIENT_URL");
        if (clientUrl == null || clientUrl.isBlank()) {
            clientUrl = System.getProperty("CLIENT_URL");
        }
        if (clientUrl == null || clientUrl.isBlank()) {
            clientUrl = filterConfig.getServletContext().getInitParameter("CLIENT_URL");
        }
        if (clientUrl == null || clientUrl.isBlank()) {
            clientUrl = "http://localhost:5173";
        }
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request   = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // Allow the configured client origin (or use the request Origin if it matches)
        String origin = request.getHeader("Origin");
        if (origin != null && (origin.equals(clientUrl) || clientUrl.equals("*"))) {
            response.setHeader("Access-Control-Allow-Origin", origin);
        } else {
            response.setHeader("Access-Control-Allow-Origin", clientUrl);
        }

        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
        response.setHeader("Access-Control-Allow-Headers",
                "Content-Type, Authorization, X-Requested-With, Accept, Origin");
        response.setHeader("Access-Control-Max-Age", "3600");

        // Handle preflight OPTIONS request — return immediately without further processing
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(req, res);
    }

    @Override
    public void destroy() {
        // Nothing to clean up
    }
}
