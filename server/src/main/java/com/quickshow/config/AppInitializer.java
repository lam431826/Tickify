package com.quickshow.config;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Application lifecycle listener.
 * Bootstraps database configuration when the web application starts.
 */
@WebListener
public class AppInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        ctx.log("[QuickShow] Initializing application...");
        try {
            DBConfig.init(ctx);
            ctx.log("[QuickShow] Database configuration initialized successfully.");
        } catch (Exception e) {
            ctx.log("[QuickShow] ERROR: Failed to initialize database configuration: " + e.getMessage());
            // Do not throw here — let the app still start so other servlets can respond
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        sce.getServletContext().log("[QuickShow] Application shutting down.");
    }
}
