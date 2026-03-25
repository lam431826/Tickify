import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft, ArrowRight, CalendarIcon, ClockIcon, StarIcon } from "lucide-react";
import { useAppContext } from "../context/AppContext";
import timeFormat from "../lib/timeFormat";

const HeroSection = () => {
  const navigate = useNavigate();
  const { shows, image_base_url } = useAppContext();
  const [current, setCurrent] = useState(0);
  const [animating, setAnimating] = useState(false);

  const slides = shows.slice(0, 3);

  const goTo = (index) => {
    if (animating) return;
    setAnimating(true);
    setTimeout(() => {
      setCurrent(index);
      setAnimating(false);
    }, 400);
  };

  const prev = () => goTo((current - 1 + slides.length) % slides.length);
  const next = () => goTo((current + 1) % slides.length);

  useEffect(() => {
    if (slides.length === 0) return;
    const timer = setInterval(next, 6000);
    return () => clearInterval(timer);
  }, [current, slides.length]);

  if (slides.length === 0) return null;

  const slide = slides[current];

  const genreNames = (slide.genres || [])
    .map((g) => (typeof g === "string" ? g : g.name))
    .filter(Boolean)
    .slice(0, 4);

  // Use original size for crisp full-screen display instead of the default w500
  const originalBase = image_base_url.replace(/\/w\d+$/, "/original");
  const imageUrl = originalBase + (slide.backdrop_path || slide.poster_path);

  return (
    <div className="relative h-screen flex overflow-hidden">

      {/* ── LEFT: Content panel ── */}
      <div className="relative z-10 flex flex-col justify-center w-full md:w-1/2 px-6 md:px-16 lg:px-20 bg-gradient-to-r from-black via-black/95 to-black/60 md:to-transparent">

        {/* Transition wrapper */}
        <div className={`transition-all duration-400 ${animating ? "opacity-0 translate-y-4" : "opacity-100 translate-y-0"}`}>

          {/* Genres */}
          {genreNames.length > 0 && (
            <div className="flex flex-wrap gap-2 mb-4 mt-24 md:mt-0">
              {genreNames.map((g) => (
                <span
                  key={g}
                  className="px-3 py-0.5 rounded-full text-xs font-medium border border-primary/50 text-primary bg-primary/10"
                >
                  {g}
                </span>
              ))}
            </div>
          )}

          {/* Title */}
          <h1 className="text-4xl md:text-5xl lg:text-6xl font-bold leading-tight max-w-lg">
            {slide.title}
          </h1>

          {/* Description directly under title */}
          {slide.overview && (
            <p className="mt-3 text-gray-300 text-sm leading-relaxed line-clamp-3 max-w-md">
              {slide.overview}
            </p>
          )}

          {/* Meta row */}
          <div className="flex flex-wrap items-center gap-4 mt-4 text-gray-400 text-sm">
            {slide.release_date && (
              <div className="flex items-center gap-1">
                <CalendarIcon className="w-4 h-4" />
                {slide.release_date.slice(0, 4)}
              </div>
            )}
            {slide.runtime > 0 && (
              <div className="flex items-center gap-1">
                <ClockIcon className="w-4 h-4" />
                {timeFormat(slide.runtime)}
              </div>
            )}
            {slide.vote_average > 0 && (
              <div className="flex items-center gap-1 text-yellow-400">
                <StarIcon className="w-4 h-4 fill-yellow-400" />
                <span className="font-semibold">{slide.vote_average.toFixed(1)}</span>
                <span className="text-gray-500 text-xs">/ 10</span>
              </div>
            )}
          </div>

          {/* Buttons */}
          <div className="flex items-center gap-3 mt-6">
            <button
              onClick={() => navigate(`/movies/${slide._id}`)}
              className="flex items-center gap-2 px-6 py-3 text-sm bg-primary hover:bg-primary/90 transition rounded-full font-semibold cursor-pointer"
            >
              Buy Ticket
            </button>
            <button
              onClick={() => navigate("/movies")}
              className="flex items-center gap-2 px-6 py-3 text-sm border border-white/30 hover:border-white/60 hover:bg-white/10 transition rounded-full font-medium cursor-pointer"
            >
              Explore Movies
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Dot indicators */}
        <div className="flex items-center gap-2 mt-10">
          {slides.map((_, i) => (
            <button
              key={i}
              onClick={() => goTo(i)}
              className={`rounded-full transition-all duration-300 cursor-pointer ${
                i === current
                  ? "w-6 h-2 bg-primary"
                  : "w-2 h-2 bg-white/30 hover:bg-white/60"
              }`}
            />
          ))}
        </div>
      </div>

      {/* ── RIGHT: Full image panel ── */}
      <div className="hidden md:block absolute inset-0 md:left-1/3 z-0">
        <div
          className={`w-full h-full bg-cover bg-center transition-opacity duration-500 ${
            animating ? "opacity-0" : "opacity-100"
          }`}
          style={{ backgroundImage: `url(${imageUrl})` }}
        />
        {/* Fade edge toward content */}
        <div className="absolute inset-0 bg-gradient-to-r from-black via-black/30 to-transparent" />
        <div className="absolute inset-0 bg-gradient-to-t from-black/50 via-transparent to-black/20" />
      </div>

      {/* Mobile: full background */}
      <div
        className={`md:hidden absolute inset-0 z-0 bg-cover bg-center transition-opacity duration-500 ${
          animating ? "opacity-0" : "opacity-100"
        }`}
        style={{ backgroundImage: `url(${imageUrl})` }}
      >
        <div className="absolute inset-0 bg-black/60" />
      </div>

      {/* Arrow controls */}
      <button
        onClick={prev}
        className="absolute left-3 top-1/2 -translate-y-1/2 z-20 w-9 h-9 rounded-full bg-black/50 hover:bg-black/80 border border-white/20 flex items-center justify-center transition cursor-pointer"
      >
        <ArrowLeft className="w-4 h-4" />
      </button>
      <button
        onClick={next}
        className="absolute right-3 top-1/2 -translate-y-1/2 z-20 w-9 h-9 rounded-full bg-black/50 hover:bg-black/80 border border-white/20 flex items-center justify-center transition cursor-pointer"
      >
        <ArrowRight className="w-4 h-4" />
      </button>
    </div>
  );
};

export default HeroSection;
