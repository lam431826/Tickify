-- QuickShow Database Schema for Microsoft SQL Server
-- Run this script to create the database schema

CREATE TABLE Movies (
    id INT PRIMARY KEY,
    title NVARCHAR(500) NOT NULL,
    overview NVARCHAR(MAX),
    poster_path NVARCHAR(500),
    backdrop_path NVARCHAR(500),
    release_date DATE,
    runtime INT,
    vote_average FLOAT,
    vote_count INT,
    genres NVARCHAR(MAX),   -- stored as JSON string: [{"name":"Action"},...]
    casts NVARCHAR(MAX)     -- stored as JSON string: [{"name":"...","profile_path":"..."},...]
);

CREATE TABLE Users (
    id NVARCHAR(255) PRIMARY KEY,   -- Clerk user ID (sub claim)
    name NVARCHAR(500),
    email NVARCHAR(500),
    password_hash NVARCHAR(255) NULL,
    is_admin BIT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE()
);

CREATE TABLE Shows (
    id NVARCHAR(36) PRIMARY KEY,    -- UUID
    movie_id INT NOT NULL,
    show_datetime DATETIME NOT NULL,
    show_price FLOAT NOT NULL,
    FOREIGN KEY (movie_id) REFERENCES Movies(id)
);

CREATE TABLE Bookings (
    id NVARCHAR(36) PRIMARY KEY,    -- UUID
    user_id NVARCHAR(255) NOT NULL,
    show_id NVARCHAR(36) NOT NULL,
    amount FLOAT NOT NULL,
    booked_seats NVARCHAR(MAX) NOT NULL,  -- stored as JSON array string: ["A1","B2",...]
    is_paid BIT DEFAULT 0,
    payment_link NVARCHAR(MAX),
    created_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (show_id) REFERENCES Shows(id)
);

CREATE TABLE OccupiedSeats (
    id INT PRIMARY KEY IDENTITY(1,1),
    show_id NVARCHAR(36) NOT NULL,
    seat_id NVARCHAR(10) NOT NULL,
    booking_id NVARCHAR(36) NOT NULL,
    FOREIGN KEY (show_id) REFERENCES Shows(id),
    FOREIGN KEY (booking_id) REFERENCES Bookings(id)
);

CREATE TABLE Favorites (
    id INT PRIMARY KEY IDENTITY(1,1),
    user_id NVARCHAR(255) NOT NULL,
    movie_id INT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (movie_id) REFERENCES Movies(id),
    CONSTRAINT UQ_Favorites UNIQUE(user_id, movie_id)
);
