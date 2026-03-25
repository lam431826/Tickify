# QuickShow — Java Servlet Backend

A Java 11 / Servlet 4.0 REST API backend for the QuickShow movie booking application, deployed on Apache Tomcat 9 and backed by Microsoft SQL Server.

---

## Tech Stack

| Component        | Version         |
|------------------|-----------------|
| Java             | 11              |
| Servlet API      | javax.servlet 4.0 |
| Tomcat           | 9.x             |
| SQL Server JDBC  | mssql-jdbc 12.4.2.jre11 |
| Jackson          | 2.15.2          |
| nimbus-jose-jwt  | 9.31 (Clerk JWT RS256) |
| Build Tool       | Maven (WAR packaging) |

---

## Project Structure

```
server/
├── pom.xml
├── .env.example
├── src/main/
│   ├── resources/schema.sql
│   ├── webapp/WEB-INF/web.xml
│   └── java/com/quickshow/
│       ├── config/
│       │   ├── DBConfig.java          # JDBC connection factory
│       │   └── AppInitializer.java    # ServletContextListener bootstrap
│       ├── filter/
│       │   └── CORSFilter.java        # CORS preflight + headers
│       ├── util/
│       │   ├── AuthUtil.java          # Clerk JWT verification (JWKS cache)
│       │   └── TmdbUtil.java          # TMDB API helper
│       ├── dao/
│       │   ├── UserDAO.java           # Users table + Clerk auto-sync
│       │   ├── MovieDAO.java          # Movies + Favorites tables
│       │   ├── ShowDAO.java           # Shows table
│       │   └── BookingDAO.java        # Bookings + OccupiedSeats tables
│       └── servlet/
│           ├── show/
│           │   ├── GetAllShowsServlet.java
│           │   ├── GetShowByIdServlet.java
│           │   ├── GetNowPlayingServlet.java
│           │   └── AddShowServlet.java
│           ├── booking/
│           │   ├── GetOccupiedSeatsServlet.java
│           │   └── CreateBookingServlet.java
│           ├── user/
│           │   ├── GetFavoritesServlet.java
│           │   ├── UpdateFavoriteServlet.java
│           │   └── GetUserBookingsServlet.java
│           └── admin/
│               ├── IsAdminServlet.java
│               ├── DashboardServlet.java
│               ├── GetAllShowsAdminServlet.java
│               └── GetAllBookingsServlet.java
```

---

## Prerequisites

- **JDK 11+** — [Download](https://adoptium.net/)
- **Apache Maven 3.8+** — [Download](https://maven.apache.org/download.cgi)
- **Apache Tomcat 9** — [Download](https://tomcat.apache.org/download-90.cgi)
- **Microsoft SQL Server** (local or Azure) — [Developer Edition](https://www.microsoft.com/en-us/sql-server/sql-server-downloads)
- **Clerk account** — [clerk.com](https://clerk.com) (free tier works)
- **TMDB API key** — [themoviedb.org](https://www.themoviedb.org/settings/api)

---

## Setup Steps

### 1. Create the Database

Open SQL Server Management Studio (SSMS) or `sqlcmd` and run:

```sql
CREATE DATABASE QuickShow;
GO
USE QuickShow;
GO
```

Then run `src/main/resources/schema.sql` to create all tables.

To grant yourself admin access (replace with your actual Clerk user ID):
```sql
UPDATE Users SET is_admin = 1 WHERE id = 'user_xxxxxxxxxx';
-- Or insert directly after first login:
INSERT INTO Users (id, name, email, is_admin) VALUES ('user_xxx', 'Admin Name', 'admin@example.com', 1);
```

### 2. Configure Environment Variables

Copy `.env.example` and set the variables before starting Tomcat.

**Option A — System environment variables (recommended for production):**
```bash
export DB_URL="jdbc:sqlserver://localhost:1433;databaseName=QuickShow;encrypt=true;trustServerCertificate=true"
export DB_USER="sa"
export DB_PASSWORD="YourPassword"
export CLERK_JWKS_URL="https://your-domain.clerk.accounts.dev/.well-known/jwks.json"
export CLERK_SECRET_KEY="sk_test_..."
export TMDB_API_KEY="your_tmdb_key"
export CLIENT_URL="http://localhost:5173"
```

**Option B — Tomcat `setenv.sh` / `setenv.bat`:**

Create `$CATALINA_HOME/bin/setenv.sh`:
```bash
#!/bin/bash
export DB_URL="jdbc:sqlserver://localhost:1433;databaseName=QuickShow;encrypt=true;trustServerCertificate=true"
export DB_USER="sa"
export DB_PASSWORD="YourPassword"
export CLERK_JWKS_URL="https://your-domain.clerk.accounts.dev/.well-known/jwks.json"
export CLERK_SECRET_KEY="sk_test_..."
export TMDB_API_KEY="your_tmdb_key"
export CLIENT_URL="http://localhost:5173"
```

On Windows, create `%CATALINA_HOME%\bin\setenv.bat`:
```bat
set DB_URL=jdbc:sqlserver://localhost:1433;databaseName=QuickShow;encrypt=true;trustServerCertificate=true
set DB_USER=sa
set DB_PASSWORD=YourPassword
set CLERK_JWKS_URL=https://your-domain.clerk.accounts.dev/.well-known/jwks.json
set CLERK_SECRET_KEY=sk_test_...
set TMDB_API_KEY=your_tmdb_key
set CLIENT_URL=http://localhost:5173
```

### 3. Build the WAR

```bash
cd server
mvn clean package
```

This produces `target/quickshow.war`.

### 4. Deploy to Tomcat

Copy the WAR to Tomcat's webapps directory:
```bash
cp target/quickshow.war $CATALINA_HOME/webapps/
```

Or for development, configure a `<Context>` in `server.xml` / `conf/Catalina/localhost/quickshow.xml`:
```xml
<Context path="/quickshow" docBase="/absolute/path/to/server/target/quickshow" reloadable="true"/>
```

### 5. Start Tomcat

```bash
$CATALINA_HOME/bin/startup.sh   # Linux/macOS
%CATALINA_HOME%\bin\startup.bat # Windows
```

The API will be available at: `http://localhost:8080/quickshow/api/...`

### 6. Configure the React Client

In the client directory, update your `.env` (or `.env.local`):
```env
VITE_BASE_URL=http://localhost:8080/quickshow
```

---

## API Endpoints

| Method | Path                        | Auth         | Description                          |
|--------|-----------------------------|--------------|--------------------------------------|
| GET    | `/api/show/all`             | Public       | All movies with shows                |
| GET    | `/api/show/now-playing`     | Admin        | Now-playing movies from TMDB         |
| POST   | `/api/show/add`             | Admin        | Add shows for a movie                |
| GET    | `/api/show/:id`             | Public       | Movie details + dateTime map         |
| GET    | `/api/booking/seats/:showId`| Public       | Occupied seats for a show            |
| POST   | `/api/booking/create`       | User         | Create a booking                     |
| GET    | `/api/user/favorites`       | User         | User's favorite movies               |
| POST   | `/api/user/update-favorite` | User         | Toggle favorite                      |
| GET    | `/api/user/bookings`        | User         | User's booking history               |
| GET    | `/api/admin/is-admin`       | User         | Check admin status                   |
| GET    | `/api/admin/dashboard`      | Admin        | Dashboard stats                      |
| GET    | `/api/admin/all-shows`      | Admin        | All shows (admin view)               |
| GET    | `/api/admin/all-bookings`   | Admin        | All bookings (admin view)            |

---

## Authentication Flow

1. The React client uses **Clerk** for authentication and obtains a short-lived JWT.
2. Every authenticated request sends: `Authorization: Bearer <jwt>`
3. The server (`AuthUtil.java`) fetches the **JWKS** from `CLERK_JWKS_URL` (cached for 1 hour) and verifies the JWT signature using RS256.
4. The `sub` claim is extracted as the Clerk user ID.
5. On first seen, the server auto-syncs the user by calling `GET https://api.clerk.com/v1/users/{userId}` using `CLERK_SECRET_KEY`, and stores `name` + `email` in the `Users` table.

---

## Making a User an Admin

After the user logs in at least once (which triggers auto-sync), run this SQL:

```sql
UPDATE Users SET is_admin = 1 WHERE id = 'user_your_clerk_id_here';
```

The Clerk user ID can be found in the Clerk Dashboard under Users, or in the browser's network tab (look for the `sub` claim in the JWT payload).

---

## Notes

- **Payment**: `CreateBookingServlet` currently returns a placeholder payment URL (`https://payment.placeholder.com/pay/{bookingId}`). To integrate a real payment gateway (e.g. Stripe, PayOS), replace the `paymentLink` logic in `BookingDAO.createBooking()`.
- **TMDB Images**: The client uses `VITE_TMDB_IMAGE_BASE_URL` (e.g. `https://image.tmdb.org/t/p/w500`) to construct full image URLs from `poster_path` values.
- **SQL Server Trust**: For local development, `trustServerCertificate=true` is used. Remove this for production and use a proper TLS certificate.
