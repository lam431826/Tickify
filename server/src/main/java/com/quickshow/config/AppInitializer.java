package com.quickshow.config;

import com.quickshow.dao.ShowDAO;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Application lifecycle listener.
 * Bootstraps database configuration and starts scheduled tasks on startup.
 */
@WebListener
public class AppInitializer implements ServletContextListener {

    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        ctx.log("[QuickShow] Initializing application...");
        try {
            DBConfig.init(ctx);
            ctx.log("[QuickShow] Database configuration initialized successfully.");
        } catch (Exception e) {
            ctx.log("[QuickShow] ERROR: Failed to initialize database configuration: " + e.getMessage());
        }

        // Schedule expired show cleanup every 30 minutes
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                ShowDAO.deleteExpiredEmptyShows();
            } catch (Exception e) {
                ctx.log("[QuickShow] Scheduler error: " + e.getMessage());
            }
        }, 5, 30, TimeUnit.MINUTES);

        ctx.log("[QuickShow] Expired show cleanup scheduler started (every 30 minutes).");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        sce.getServletContext().log("[QuickShow] Application shutting down.");
    }
}
