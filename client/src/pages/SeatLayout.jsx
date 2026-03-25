import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { assets } from "../assets/assets";
import Loading from "../components/Loading";
import { ArrowRightIcon, ClockIcon } from "lucide-react";
import isoTimeFormat from "../lib/isoTimeFormat";
import BlurCircle from "../components/BlurCircle";
import toast from "react-hot-toast";
import { useAppContext } from "../context/AppContext";

const SeatLayout = () => {
  const groupRows = [
    ["A", "B"],
    ["C", "D"],
    ["E", "F"],
    ["G", "H"],
    ["I", "J"],
  ];

  const { id, date } = useParams();
  const [selectedSeats, setSelectedSeats] = useState([]);
  const [selectedTime, setSelectedTime] = useState(null);
  const [show, setShow] = useState(null);
  const [occupiedSeats, setOccupiedSeats] = useState([]);

  const navigate = useNavigate();
  const currency = import.meta.env.VITE_CURRENCY;

  const { axios, getToken, user, t } = useAppContext();

  const getShow = async () => {
    try {
      const { data } = await axios.get(`/api/show/${id}`);
      if (data.success) {
        setShow(data);
      }
    } catch (error) {
      console.log(error);
    }
  };

  const handleSeatClick = (seatId) => {
    if (!selectedTime) {
      return toast(t("toast_select_time"));
    }
    if (occupiedSeats.includes(seatId)) {
      return toast(t("toast_seat_taken"));
    }
    if (!selectedSeats.includes(seatId) && selectedSeats.length >= 5) {
      return toast(t("toast_max_seats"));
    }
    setSelectedSeats((prev) =>
      prev.includes(seatId)
        ? prev.filter((seat) => seat !== seatId)
        : [...prev, seatId]
    );
  };

  const getSeatClass = (seatId) => {
    if (occupiedSeats.includes(seatId))
      return "bg-red-900/40 border-red-700/50 text-red-500/60 cursor-not-allowed";
    if (selectedSeats.includes(seatId))
      return "bg-primary border-primary text-white scale-105";
    return "border-gray-500 text-gray-400 hover:border-primary/60 hover:text-white cursor-pointer";
  };

  const renderSeats = (row, count = 9) => (
    <div key={row} className="flex gap-1.5 mt-1.5">
      <div className="flex items-center justify-center gap-1.5">
        {Array.from({ length: count }, (_, i) => {
          const seatId = `${row}${i + 1}`;
          return (
            <button
              key={seatId}
              onClick={() => handleSeatClick(seatId)}
              disabled={occupiedSeats.includes(seatId)}
              className={`h-8 w-8 rounded text-xs font-medium border transition-all duration-150 ${getSeatClass(seatId)}`}
            >
              {seatId}
            </button>
          );
        })}
      </div>
    </div>
  );

  const getOccupiedSeats = async () => {
    try {
      const { data } = await axios.get(
        `/api/booking/seats/${selectedTime.showId}`
      );
      if (data.success) {
        setOccupiedSeats(data.occupiedSeats);
      } else {
        toast.error(data.message);
      }
    } catch (error) {
      console.log(error);
    }
  };

  const bookTickets = async () => {
    try {
      if (!user) return toast.error(t("toast_login"));
      if (!selectedTime) return toast.error(t("toast_select_showtime"));
      if (!selectedSeats.length) return toast.error(t("toast_select_seat"));

      const { data } = await axios.post(
        "/api/booking/create",
        { showId: selectedTime.showId, selectedSeats },
        { headers: { Authorization: `Bearer ${await getToken()}` } }
      );

      if (data.success) {
        navigate(`/payment/${data.bookingId}`);
      } else {
        toast.error(data.message);
      }
    } catch (error) {
      toast.error(error.message);
    }
  };

  useEffect(() => {
    getShow();
  }, []);

  useEffect(() => {
    if (selectedTime) {
      setSelectedSeats([]);
      getOccupiedSeats();
    }
  }, [selectedTime]);

  const pricePerTicket = selectedTime?.showPrice ?? 0;
  const totalPrice = selectedSeats.length * pricePerTicket;

  return show ? (
    <div className="flex flex-col md:flex-row px-6 md:px-16 lg:px-40 py-30 md:pt-50 pb-40">
      {/* Available Timings */}
      <div className="w-60 bg-primary/10 border border-primary/20 rounded-lg py-10 h-max md:sticky md:top-30">
        <p className="text-lg font-semibold px-6">{t("available_timings")}</p>
        <div className="mt-5 space-y-1">
          {show.dateTime[date]?.map((item) => (
            <div
              key={item.time}
              onClick={() => setSelectedTime(item)}
              className={`flex items-center gap-2 px-6 py-2 w-max rounded-r-md cursor-pointer transition ${
                selectedTime?.time === item.time
                  ? "bg-primary text-white"
                  : "hover:bg-primary/20"
              }`}
            >
              <ClockIcon className="w-4 h-4" />
              <div>
                <p className="text-sm">{isoTimeFormat(item.time)}</p>
                {item.showPrice != null && (
                  <p className="text-xs text-gray-400">{currency}{item.showPrice} {t("per_seat")}</p>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Seats Layout */}
      <div className="relative flex-1 flex flex-col items-center max-md:mt-16">
        <BlurCircle top="-100px" left="-100px" />
        <BlurCircle bottom="0" right="0" />
        <h1 className="text-2xl font-semibold mb-4">{t("select_seat")}</h1>
        <img src={assets.screenImage} alt="screen" />
        <p className="text-gray-400 text-sm mb-6">{t("screen_side")}</p>

        {/* Seat grid */}
        <div className="flex flex-col items-center mt-10 text-xs text-gray-300">
          <div className="grid grid-cols-2 md:grid-cols-1 gap-8 md:gap-2 mb-6">
            {groupRows[0].map((row) => renderSeats(row))}
          </div>
          <div className="grid grid-cols-2 gap-11">
            {groupRows.slice(1).map((group, idx) => (
              <div key={idx}>{group.map((row) => renderSeats(row))}</div>
            ))}
          </div>
        </div>

        {/* Legend */}
        <div className="flex items-center gap-6 mt-8 text-xs text-gray-400">
          <span className="flex items-center gap-1.5">
            <span className="w-5 h-5 rounded border border-gray-500 inline-block" />
            {t("available")}
          </span>
          <span className="flex items-center gap-1.5">
            <span className="w-5 h-5 rounded border border-primary bg-primary inline-block" />
            {t("selected")}
          </span>
          <span className="flex items-center gap-1.5">
            <span className="w-5 h-5 rounded border border-red-700/50 bg-red-900/40 inline-block" />
            {t("booked")}
          </span>
        </div>
      </div>

      {/* Sticky bottom checkout bar */}
      {selectedTime && (
        <div className="fixed bottom-0 left-0 right-0 z-40 bg-black/90 border-t border-primary/20 px-6 md:px-16 lg:px-40 py-4 flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="flex items-center gap-6 text-sm">
            <div>
              <span className="text-gray-400">{t("selected_label")} </span>
              {selectedSeats.length > 0
                ? <span className="font-medium">{selectedSeats.join(", ")}</span>
                : <span className="text-gray-500">{t("none")}</span>
              }
            </div>
            <div>
              <span className="text-gray-400">{t("tickets_label")} </span>
              <span className="font-medium">{selectedSeats.length} / 5</span>
            </div>
            {selectedSeats.length > 0 && (
              <div>
                <span className="text-gray-400">{t("total_label")} </span>
                <span className="font-semibold text-primary text-base">{currency}{totalPrice}</span>
              </div>
            )}
          </div>
          <button
            onClick={bookTickets}
            disabled={!selectedSeats.length}
            className="flex items-center gap-1 px-8 py-2.5 text-sm bg-primary hover:bg-primary-dull transition rounded-full font-medium cursor-pointer active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {t("proceed_checkout")}
            <ArrowRightIcon strokeWidth={3} className="w-4 h-4" />
          </button>
        </div>
      )}
    </div>
  ) : (
    <Loading />
  );
};

export default SeatLayout;
