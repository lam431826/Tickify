import { createContext, useCallback, useContext, useEffect, useState } from "react";
import axios from "axios";
import { useLocation, useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import translations from "../lib/translations";

axios.defaults.baseURL = import.meta.env.VITE_BASE_URL;

export const AppContext = createContext();

export const AppProvider = ({ children }) => {
  const [isAdmin, setIsAdmin] = useState(false);
  const [language, setLanguage] = useState(() => localStorage.getItem("tickify_lang") || "en");

  const t = useCallback((key) => translations[language]?.[key] ?? key, [language]);

  const toggleLanguage = () => {
    setLanguage((prev) => {
      const next = prev === "en" ? "vi" : "en";
      localStorage.setItem("tickify_lang", next);
      return next;
    });
  };
  const [shows, setShows] = useState([]);
  const [favoriteMovies, setFavoriteMovies] = useState([]);
  const [user, setUser] = useState(null); // {id, name, email, isAdmin}
  const [sessionReady, setSessionReady] = useState(false); // true once restoreSession() has run

  const image_base_url = import.meta.env.VITE_TMDB_IMAGE_BASE_URL;
  const location = useLocation();
  const navigate = useNavigate();

  // Token helpers
  const getToken = () => localStorage.getItem("tickify_token");
  const setToken = (token) => localStorage.setItem("tickify_token", token);
  const clearToken = () => localStorage.removeItem("tickify_token");

  // Login: called after successful /api/auth/login or /api/auth/register
  const login = (token, userData) => {
    setToken(token);
    setUser(userData);
    setIsAdmin(userData.isAdmin);
  };

  // Logout
  const logout = () => {
    clearToken();
    setUser(null);
    setIsAdmin(false);
    setFavoriteMovies([]);
    navigate("/");
  };

  // Restore session on page load
  const restoreSession = async () => {
    const token = getToken();
    if (!token) { setSessionReady(true); return; }
    try {
      const { data } = await axios.get("/api/admin/is-admin", {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (data.success) {
        setIsAdmin(data.isAdmin);
        setUser(data.user); // server returns user info too
        if (!data.isAdmin && location.pathname.startsWith("/admin")) {
          navigate("/");
          toast.error("You are not authorized to access admin dashboard");
        }
      } else {
        clearToken();
      }
    } catch (error) {
      clearToken();
    }
    setSessionReady(true);
  };

  const fetchFavoriteMovies = async () => {
    const token = getToken();
    if (!token) return;
    try {
      const { data } = await axios.get("/api/user/favorites", {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (data.success) setFavoriteMovies(data.movies);
    } catch (error) {
      console.error(error);
    }
  };

  const fetchShows = async () => {
    try {
      const { data } = await axios.get("/api/show/all");
      if (data.success) setShows(data.shows);
    } catch (error) {
      console.error(error);
    }
  };

  const fetchIsAdmin = async () => {
    const token = getToken();
    if (!token) return;
    try {
      const { data } = await axios.get("/api/admin/is-admin", {
        headers: { Authorization: `Bearer ${token}` },
      });
      setIsAdmin(data.isAdmin || false);
      if (data.user) setUser(data.user);
      if (!data.isAdmin && location.pathname.startsWith("/admin")) {
        navigate("/");
        toast.error("You are not authorized to access admin dashboard");
      }
    } catch (error) {
      console.error(error);
    }
  };

  useEffect(() => {
    fetchShows();
    restoreSession();
  }, []);

  useEffect(() => {
    if (user) fetchFavoriteMovies();
  }, [user]);

  const value = {
    axios,
    user,
    sessionReady,
    getToken,
    login,
    logout,
    navigate,
    isAdmin,
    shows,
    favoriteMovies,
    fetchFavoriteMovies,
    fetchIsAdmin,
    image_base_url,
    language,
    toggleLanguage,
    t,
  };

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
};

export const useAppContext = () => useContext(AppContext);
