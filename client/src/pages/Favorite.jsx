import BlurCircle from "../components/BlurCircle";
import MovieCard from "../components/MovieCard";
import { useAppContext } from "../context/AppContext";
import { HeartIcon } from "lucide-react";
import { useNavigate } from "react-router-dom";

const Favorite = () => {
  const { favoriteMovies, user, sessionReady, t } = useAppContext();
  const navigate = useNavigate();

  if (!sessionReady) return null;

  if (!user) {
    return (
      <div className="flex flex-col items-center justify-center h-screen gap-4">
        <HeartIcon className="w-16 h-16 text-primary/40" />
        <p className="text-gray-400 text-lg">Please sign in to see your favorites</p>
        <button
          onClick={() => navigate("/login")}
          className="px-6 py-2 bg-primary hover:bg-primary/90 rounded-full text-sm font-medium transition cursor-pointer"
        >
          {t("nav_signin")}
        </button>
      </div>
    );
  }

  return (
    <div className="relative pt-32 md:pt-40 pb-60 px-6 md:px-16 lg:px-40 xl:px-44 overflow-hidden min-h-[80vh]">
      <BlurCircle top="150px" left="0" />
      <BlurCircle bottom="50px" right="50px" />

      <div className="flex items-center gap-3 mb-8">
        <HeartIcon className="w-6 h-6 fill-primary text-primary" />
        <h1 className="text-xl font-semibold">
          {t("nav_favorites")}
          <span className="ml-2 text-sm text-gray-400 font-normal">
            ({favoriteMovies.length})
          </span>
        </h1>
      </div>

      {favoriteMovies.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-32 gap-4 text-center">
          <HeartIcon className="w-14 h-14 text-gray-600" />
          <p className="text-gray-400 text-lg">You haven't added any favorites yet.</p>
          <button
            onClick={() => navigate("/movies")}
            className="px-6 py-2 bg-primary hover:bg-primary/90 rounded-full text-sm font-medium transition cursor-pointer"
          >
            {t("nav_movies")}
          </button>
        </div>
      ) : (
        <div className="flex flex-wrap max-sm:justify-center gap-8">
          {favoriteMovies.map((movie) => (
            <MovieCard movie={movie} key={movie._id} />
          ))}
        </div>
      )}
    </div>
  );
};

export default Favorite;
