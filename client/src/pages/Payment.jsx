import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useAppContext } from "../context/AppContext";
import toast from "react-hot-toast";
import Loading from "../components/Loading";
import BlurCircle from "../components/BlurCircle";
import { dateFormat } from "../lib/dateFormat";

const Payment = () => {
  const { bookingId } = useParams();
  const { axios, getToken, user, sessionReady, image_base_url, t } = useAppContext();
  const navigate = useNavigate();
  const currency = import.meta.env.VITE_CURRENCY;

  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [paying, setPaying] = useState(false);

  // Fake card form state
  const [cardNumber, setCardNumber] = useState("");
  const [expiry, setExpiry] = useState("");
  const [cvv, setCvv] = useState("");
  const [cardName, setCardName] = useState("");

  const fetchBooking = async () => {
    try {
      const { data } = await axios.get(`/api/booking/id/${bookingId}`, {
        headers: { Authorization: `Bearer ${await getToken()}` },
      });
      if (data.success) {
        setBooking(data.booking);
        if (data.booking.isPaid) {
          toast("This booking is already paid.");
          navigate("/my-bookings");
          return;
        }
      } else {
        toast.error("Booking not found");
        navigate("/my-bookings");
      }
    } catch { navigate("/my-bookings"); }
    setLoading(false);
  };

  const handlePay = async (e) => {
    e.preventDefault();
    setPaying(true);
    try {
      const { data } = await axios.post("/api/booking/pay",
        { bookingId },
        { headers: { Authorization: `Bearer ${await getToken()}` } }
      );
      if (data.success) {
        toast.success("Payment successful! Enjoy your movie 🎬");
        navigate("/my-bookings");
      } else {
        toast.error(data.message);
      }
    } catch { toast.error("Payment failed"); }
    setPaying(false);
  };

  useEffect(() => {
    if (!sessionReady) return;
    if (user) fetchBooking();
    else navigate("/login");
  }, [user, sessionReady]);

  if (loading) return <Loading />;

  return (
    <div className="relative px-6 md:px-16 lg:px-40 pt-30 md:pt-40 pb-20 min-h-screen flex flex-col items-center">
      <BlurCircle top="100px" left="0" />
      <BlurCircle bottom="100px" right="0" />
      <h1 className="text-2xl font-semibold mb-8 w-full max-w-4xl">{t("complete_payment")}</h1>

      <div className="flex flex-col lg:flex-row gap-8 w-full max-w-4xl">
        {/* Booking Summary */}
        <div className="bg-primary/10 border border-primary/20 rounded-xl p-6 lg:w-80 h-max">
          <h2 className="font-semibold text-lg mb-4">{t("order_summary")}</h2>
          {booking && (
            <>
              <img
                src={image_base_url + booking.show.movie.poster_path}
                alt="poster"
                className="w-full h-44 object-cover rounded-lg mb-4"
              />
              <p className="font-semibold text-lg">{booking.show.movie.title}</p>
              <p className="text-gray-400 text-sm mt-1">{dateFormat(booking.show.showDateTime)}</p>
              <div className="mt-4 space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-gray-400">{t("seats")}</span>
                  <span>{booking.bookedSeats.join(", ")}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-400">{t("tickets")}</span>
                  <span>{booking.bookedSeats.length}</span>
                </div>
                <div className="border-t border-primary/20 pt-2 flex justify-between font-semibold text-base">
                  <span>{t("total")}</span>
                  <span className="text-primary">{currency}{booking.amount}</span>
                </div>
              </div>
            </>
          )}
        </div>

        {/* Payment Form */}
        <form onSubmit={handlePay} className="flex-1 bg-primary/10 border border-primary/20 rounded-xl p-6">
          <h2 className="font-semibold text-lg mb-6">{t("card_details")}</h2>

          {/* Mock Visa Card Visual */}
          <div className="bg-gradient-to-br from-primary/40 to-primary/10 border border-primary/30 rounded-2xl p-5 mb-6 h-44 flex flex-col justify-between">
            <div className="flex justify-between items-start">
              <span className="text-xs text-gray-300 uppercase tracking-widest">Tickify Card</span>
              <span className="text-lg font-bold italic text-white">VISA</span>
            </div>
            <p className="text-xl tracking-widest font-mono text-white">
              {cardNumber ? cardNumber.replace(/(.{4})/g, "$1 ").trim() : "•••• •••• •••• ••••"}
            </p>
            <div className="flex justify-between text-sm text-gray-300">
              <span>{cardName || "CARD HOLDER"}</span>
              <span>{expiry || "MM/YY"}</span>
            </div>
          </div>

          <div className="space-y-4">
            <div>
              <label className="block text-sm text-gray-400 mb-1">{t("card_holder")}</label>
              <input
                type="text" value={cardName}
                onChange={e => setCardName(e.target.value.toUpperCase())}
                placeholder="JOHN DOE" maxLength={26}
                className="w-full bg-transparent border border-gray-600 rounded-md px-3 py-2 outline-none focus:border-primary text-sm"
              />
            </div>
            <div>
              <label className="block text-sm text-gray-400 mb-1">{t("card_number")}</label>
              <input
                type="text" value={cardNumber}
                onChange={e => setCardNumber(e.target.value.replace(/\D/g, "").slice(0, 16))}
                placeholder="1234567890123456" maxLength={16}
                className="w-full bg-transparent border border-gray-600 rounded-md px-3 py-2 outline-none focus:border-primary text-sm font-mono"
              />
            </div>
            <div className="flex gap-4">
              <div className="flex-1">
                <label className="block text-sm text-gray-400 mb-1">{t("expiry_date")}</label>
                <input
                  type="text" value={expiry}
                  onChange={e => {
                    let v = e.target.value.replace(/\D/g, "").slice(0, 4);
                    if (v.length >= 3) v = v.slice(0, 2) + "/" + v.slice(2);
                    setExpiry(v);
                  }}
                  placeholder="MM/YY" maxLength={5}
                  className="w-full bg-transparent border border-gray-600 rounded-md px-3 py-2 outline-none focus:border-primary text-sm"
                />
              </div>
              <div className="flex-1">
                <label className="block text-sm text-gray-400 mb-1">{t("cvv")}</label>
                <input
                  type="password" value={cvv}
                  onChange={e => setCvv(e.target.value.replace(/\D/g, "").slice(0, 3))}
                  placeholder="•••" maxLength={3}
                  className="w-full bg-transparent border border-gray-600 rounded-md px-3 py-2 outline-none focus:border-primary text-sm"
                />
              </div>
            </div>
          </div>

          <button
            type="submit" disabled={paying}
            className="w-full mt-6 bg-primary hover:bg-primary/90 text-white py-3 rounded-lg font-semibold transition cursor-pointer disabled:opacity-60"
          >
            {paying ? t("processing") : `Pay ${currency}${booking?.amount}`}
          </button>
          <button
            type="button"
            onClick={() => navigate("/my-bookings")}
            disabled={paying}
            className="w-full mt-3 border border-gray-600 hover:border-gray-400 text-gray-400 hover:text-white py-3 rounded-lg font-semibold transition cursor-pointer disabled:opacity-60"
          >
            {t("pay_later")}
          </button>
          <p className="text-center text-xs text-gray-500 mt-3">🔒 {t("mock_payment")}</p>
        </form>
      </div>
    </div>
  );
};

export default Payment;
