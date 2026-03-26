import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import Logo from "./Logo";
import { HeartIcon, MenuIcon, SearchIcon, XIcon } from "lucide-react";
import { useAppContext } from "../context/AppContext";

const Navbar = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const { user, logout, favoriteMovies, shows, language, toggleLanguage, t } = useAppContext();

  const navigate = useNavigate();
  const searchRef = useRef(null);

  const filteredShows = searchQuery.trim().length > 0
    ? shows.filter((s) =>
        s.title.toLowerCase().includes(searchQuery.toLowerCase())
      ).slice(0, 6)
    : [];

  const handleSearchSelect = (movieId) => {
    setSearchOpen(false);
    setSearchQuery("");
    navigate(`/movies/${movieId}`);
  };

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    if (filteredShows.length > 0) handleSearchSelect(filteredShows[0]._id);
  };

  // Close search when clicking outside
  useEffect(() => {
    const handler = (e) => {
      if (searchRef.current && !searchRef.current.contains(e.target)) {
        setSearchOpen(false);
        setSearchQuery("");
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  return (
    <div className="fixed top-0 left-0 z-50 w-full flex items-center justify-between px-6 md:px-16 lg:px-36 py-5">
      <Link to="/" className="max-md:flex-1">
        <Logo className="w-44 h-auto" />
      </Link>

      <div
        className={`max-md:absolute max-md:top-0 max-md:left-0 max-md:font-medium max-md:text-lg z-50 flex flex-col md:flex-row items-center max-md:justify-center gap-8 min-md:px-8 py-3 max-md:h-screen min-md:rounded-full backdrop-blur bg-black/70 md:bg-white/10 md:border border-gray-300/20 overflow-hidden transition-[width] duration-300 ${
          isOpen ? "max-md:w-full" : "max-md:w-0"
        }`}
      >
        <XIcon
          className="md:hidden absolute top-6 right-6 w-6 h-6 cursor-pointer"
          onClick={() => setIsOpen(!isOpen)}
        />

        <Link onClick={() => { scrollTo(0, 0); setIsOpen(false); }} to="/">{t("nav_home")}</Link>
        <Link onClick={() => { scrollTo(0, 0); setIsOpen(false); }} to="/movies">{t("nav_movies")}</Link>
        <Link onClick={() => { scrollTo(0, 0); setIsOpen(false); }} to="/">{t("nav_theaters")}</Link>
        <Link onClick={() => { scrollTo(0, 0); setIsOpen(false); }} to="/">{t("nav_releases")}</Link>
        {favoriteMovies.length > 0 && (
          <Link onClick={() => { scrollTo(0, 0); setIsOpen(false); }} to="/favorite" className="md:hidden">{t("nav_favorites")}</Link>
        )}
      </div>

      <div className="flex items-center gap-4">
        {/* Search */}
        <div ref={searchRef} className="relative max-md:hidden">
          {searchOpen ? (
            <form onSubmit={handleSearchSubmit} className="flex items-center">
              <input
                autoFocus
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder={t("nav_search_placeholder")}
                className="w-48 bg-black/60 border border-gray-600 rounded-full px-4 py-1.5 text-sm outline-none focus:border-primary transition"
              />
              <XIcon
                className="w-5 h-5 ml-2 cursor-pointer text-gray-400 hover:text-white"
                onClick={() => { setSearchOpen(false); setSearchQuery(""); }}
              />
            </form>
          ) : (
            <SearchIcon
              className="w-6 h-6 cursor-pointer hover:text-primary transition"
              onClick={() => setSearchOpen(true)}
            />
          )}
          {/* Dropdown results */}
          {searchOpen && filteredShows.length > 0 && (
            <div className="absolute top-10 left-0 w-64 bg-black/90 border border-primary/20 rounded-xl overflow-hidden shadow-xl z-50">
              {filteredShows.map((movie) => (
                <button
                  key={movie._id}
                  onClick={() => handleSearchSelect(movie._id)}
                  className="w-full flex items-center gap-3 px-4 py-2 hover:bg-primary/20 transition text-left"
                >
                  <img
                    src={`${import.meta.env.VITE_TMDB_IMAGE_BASE_URL}${movie.poster_path}`}
                    alt={movie.title}
                    className="w-8 h-12 object-cover rounded"
                  />
                  <div>
                    <p className="text-sm font-medium">{movie.title}</p>
                    <p className="text-xs text-gray-400">{movie.release_date?.slice(0, 4)}</p>
                  </div>
                </button>
              ))}
            </div>
          )}
          {searchOpen && searchQuery.trim().length > 0 && filteredShows.length === 0 && (
            <div className="absolute top-10 left-0 w-64 bg-black/90 border border-primary/20 rounded-xl px-4 py-3 text-sm text-gray-400 z-50">
              No movies found
            </div>
          )}
        </div>
        {/* Favorites icon */}
        {user && (
          <Link to="/favorite" onClick={() => scrollTo(0, 0)} className="relative max-md:hidden">
            <HeartIcon className={`w-6 h-6 transition ${favoriteMovies.length > 0 ? "fill-primary text-primary" : "text-gray-400 hover:text-white"}`} />
            {favoriteMovies.length > 0 && (
              <span className="absolute -top-1.5 -right-1.5 w-4 h-4 rounded-full bg-primary text-white text-[10px] font-bold flex items-center justify-center">
                {favoriteMovies.length}
              </span>
            )}
          </Link>
        )}

        {/* Language toggle */}
        <button
          onClick={toggleLanguage}
          className="px-3 py-1 rounded-full border border-gray-600 hover:border-primary/60 text-xs font-semibold text-gray-300 hover:text-white transition cursor-pointer"
        >
          {language === "en" ? "VI" : "EN"}
        </button>

        {!user ? (
          <Link
            to="/login"
            className="px-4 py-1 sm:px-7 sm:py-2 bg-primary hover:bg-primary-dull transition rounded-full font-medium cursor-pointer"
          >
            {t("nav_signin")}
          </Link>
        ) : (
          <div className="relative group">
            {/* Avatar pill button */}
            <button className="flex items-center gap-2 px-3 py-1.5 rounded-full border border-primary/30 hover:border-primary/70 bg-primary/10 hover:bg-primary/20 transition cursor-pointer select-none">
              <span className="w-6 h-6 rounded-full bg-primary flex items-center justify-center text-xs font-bold text-white shrink-0">
                {user.name?.[0]?.toUpperCase()}
              </span>
              <span className="text-sm font-medium max-w-24 truncate">{user.name}</span>
              <svg className="w-3 h-3 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
              </svg>
            </button>
            {/* Dropdown */}
            <div className="absolute right-0 top-11 w-44 bg-black/90 border border-primary/20 rounded-xl overflow-hidden shadow-xl opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-150 z-50">
              <button
                onClick={() => navigate("/my-bookings")}
                className="w-full text-left px-4 py-2.5 text-sm hover:bg-primary/20 transition"
              >
                {t("nav_my_bookings")}
              </button>
              <button
                onClick={() => navigate("/profile")}
                className="w-full text-left px-4 py-2.5 text-sm hover:bg-primary/20 transition"
              >
                {t("nav_edit_profile")}
              </button>
              <hr className="border-primary/20" />
              <button
                onClick={logout}
                className="w-full text-left px-4 py-2.5 text-sm text-red-400 hover:bg-red-600/10 transition"
              >
                {t("nav_logout")}
              </button>
            </div>
          </div>
        )}
      </div>

      <MenuIcon
        onClick={() => setIsOpen(!isOpen)}
        className="max-md:ml-4 md:hidden w-8 h-8 cursor-pointer"
      />
    </div>
  );
};

export default Navbar;
