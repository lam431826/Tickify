package com.quickshow.config;

import jakarta.servlet.ServletContext;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database configuration and connection factory.
 * Reads connection parameters from system environment variables,
 * with fallback to Servlet context-params defined in web.xml.
 */
public class DBConfig {

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;
    private static boolean initialized = false;

    /**
     * Initialize from system environment variables.
     * Call this once at application startup (e.g. from a ServletContextListener).
     * Falls back to context params if env vars are absent.
     */
    public static synchronized void init(ServletContext ctx) {
        dbUrl      = getEnvOrParam(ctx, "DB_URL");
        dbUser     = getEnvOrParam(ctx, "DB_USER");
        dbPassword = getEnvOrParam(ctx, "DB_PASSWORD");
        initialized = true;

        try {
            // Explicitly load the SQL Server JDBC driver
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQL Server JDBC driver not found on classpath", e);
        }
    }

    /**
     * Returns a new JDBC connection. Caller is responsible for closing it.
     */
    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            throw new IllegalStateException("DBConfig has not been initialized. Call DBConfig.init(ctx) first.");
        }
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static String getEnvOrParam(ServletContext ctx, String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            value = System.getProperty(name);
        }
        if ((value == null || value.isBlank()) && ctx != null) {
            value = ctx.getInitParameter(name);
        }
        return value;
    }

    // Prevent instantiation
    private DBConfig() {}
}
