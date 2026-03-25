import { useMemo, useState } from "react";
import BlurCircle from "../components/BlurCircle";
import MovieCard from "../components/MovieCard";
import { useAppContext } from "../context/AppContext";

// Normalize a genre entry — may be a string or {id, name} object
const genreName = (g) => (typeof g === "string" ? g : g?.name ?? "");

const Movies = () => {
  const { shows, t } = useAppContext();
  const [activeGenre, setActiveGenre] = useState("All");

  // Collect unique genre names across all shows
  const genres = useMemo(() => {
    const set = new Set();
    shows.forEach((s) =>
      (s.genres || []).forEach((g) => {
        const name = genreName(g);
        if (name) set.add(name);
      })
    );
    return ["All", ...Array.from(set).sort()];
  }, [shows]);

  const filtered = useMemo(() =>
    activeGenre === "All"
      ? shows
      : shows.filter((s) =>
          (s.genres || []).some((g) => genreName(g) === activeGenre)
        ),
    [shows, activeGenre]
  );

  if (shows.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center h-screen">
        <h1 className="text-3xl font-bold text-center">{t("no_movies_available")}</h1>
      </div>
    );
  }

  return (
    <div className="relative my-40 mb-60 px-6 md:px-16 lg:px-40 xl:px-44 overflow-hidden min-h-[80vh]">
      <BlurCircle top="150px" left="0" />
      <BlurCircle bottom="50px" right="50px" />

      <h1 className="text-lg font-medium mb-5">{t("now_showing")}</h1>

      {/* Genre filter bar */}
      <div className="flex flex-wrap gap-2 mb-8">
        {genres.map((g) => (
          <button
            key={g}
            onClick={() => setActiveGenre(g)}
            className={`px-4 py-1.5 rounded-full text-sm font-medium transition cursor-pointer border ${
              activeGenre === g
                ? "bg-primary border-primary text-white"
                : "border-gray-600 text-gray-400 hover:border-primary/60 hover:text-white"
            }`}
          >
            {g}
          </button>
        ))}
      </div>

      {/* Results count */}
      <p className="text-xs text-gray-500 mb-4">
        {filtered.length} {filtered.length === 1 ? t("film_singular") : t("film_plural")}
        {activeGenre !== "All" && ` ${t("genre_in")} "${activeGenre}"`}
      </p>

      {filtered.length > 0 ? (
        <div className="flex flex-wrap max-sm:justify-center gap-8">
          {filtered.map((movie) => (
            <MovieCard movie={movie} key={movie._id} />
          ))}
        </div>
      ) : (
        <p className="text-gray-400 mt-10">{t("no_movies_genre")}</p>
      )}
    </div>
  );
};

export default Movies;
