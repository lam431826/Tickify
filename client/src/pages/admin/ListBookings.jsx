import { useEffect, useState } from "react";
import toast from "react-hot-toast";
import Loading from "../../components/Loading";
import Title from "../../components/admin/Title";
import { dateFormat } from "../../lib/dateFormat";
import { useAppContext } from "../../context/AppContext";

const ListBookings = () => {
  const currency = import.meta.env.VITE_CURRENCY;

  const { axios, getToken, user } = useAppContext();

  const [bookings, setBookings] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  const getAllBookings = async () => {
    try {
      const { data } = await axios.get("/api/admin/all-bookings", {
        headers: { Authorization: `Bearer ${await getToken()}` },
      });
      setBookings(data.bookings);
    } catch (error) {
      console.error("Error fetching bookings:", error);
    }
    setIsLoading(false);
  };

  const markAsPaid = async (bookingId) => {
    const { data } = await axios.patch("/api/admin/booking/mark-paid",
      { bookingId },
      { headers: { Authorization: `Bearer ${await getToken()}` } }
    );
    if (data.success) {
      toast.success("Marked as paid");
      getAllBookings();
    } else toast.error(data.message);
  };

  const deleteBooking = async (bookingId) => {
    if (!confirm("Delete this booking?")) return;
    const { data } = await axios.delete("/api/admin/booking/delete",
      { data: { bookingId }, headers: { Authorization: `Bearer ${await getToken()}` } }
    );
    if (data.success) {
      toast.success("Booking deleted");
      getAllBookings();
    } else toast.error(data.message);
  };

  useEffect(() => {
    if (user) {
      getAllBookings();
    }
  }, [user]);

  return !isLoading ? (
    <>
      <Title text1="List" text2="Bookings" />
      <div className="max-w-4xl mt-6 overflow-x-auto">
        <table className="w-full border-collapse rounded-md overflow-hidden text-nowrap">
          <thead>
            <tr className="bg-primary/20 text-left text-white">
              <th className="p-2 font-medium pl-5">User Name</th>
              <th className="p-2 font-medium">Movie Name</th>
              <th className="p-2 font-medium">Show Time</th>
              <th className="p-2 font-medium">Seats</th>
              <th className="p-2 font-medium">Amount</th>
              <th className="p-2 font-medium">Status</th>
              <th className="p-2 font-medium">Actions</th>
            </tr>
          </thead>
          <tbody className="text-sm font-light">
            {bookings.map((item, index) => (
              <tr
                key={index}
                className="border-b border-primary/20 bg-primary/5 even:bg-primary/10"
              >
                <td className="p-2 min-w-45 pl-5">{item.user.name}</td>
                <td className="p-2">{item.show.movie.title}</td>
                <td className="p-2">{dateFormat(item.show.showDateTime)}</td>
                <td className="p-2">
                  {Object.keys(item.bookedSeats)
                    .map((seat) => item.bookedSeats[seat])
                    .join(", ")}
                </td>
                <td className="p-2">
                  {currency} {item.amount}
                </td>
                <td className="p-2">
                  {item.isPaid ? (
                    <span className="px-2 py-1 rounded text-xs font-semibold bg-green-600/20 text-green-400">
                      Paid
                    </span>
                  ) : (
                    <span className="px-2 py-1 rounded text-xs font-semibold bg-yellow-500/20 text-yellow-400">
                      Unpaid
                    </span>
                  )}
                </td>
                <td className="p-2 flex gap-2">
                  {!item.isPaid && (
                    <button
                      onClick={() => markAsPaid(item.id)}
                      className="px-2 py-1 rounded text-xs font-semibold bg-green-600/20 text-green-400 hover:bg-green-600/40 transition"
                    >
                      Mark Paid
                    </button>
                  )}
                  <button
                    onClick={() => deleteBooking(item.id)}
                    className="px-2 py-1 rounded text-xs font-semibold bg-red-600/20 text-red-400 hover:bg-red-600/40 transition"
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  ) : (
    <Loading />
  );
};

export default ListBookings;
