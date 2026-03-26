import { useEffect, useMemo, useState } from "react";
import toast from "react-hot-toast";
import Loading from "../../components/Loading";
import Title from "../../components/admin/Title";
import { dateFormat } from "../../lib/dateFormat";
import { useAppContext } from "../../context/AppContext";
import { SearchIcon, XIcon } from "lucide-react";

// Convert a show's stored ISO datetime string "2026-03-20T16:00:00.0"
// into the value format required by <input type="datetime-local">: "2026-03-20T16:00"
const toInputDatetime = (raw) => {
  if (!raw) return "";
  const normalized = raw.replace(" ", "T").substring(0, 16);
  return normalized;
};

const ROWS = ["A", "B", "C", "D", "E", "F", "G", "H", "I", "J"];
const COLS = 9;

const SeatsModal = ({ show, onClose }) => {
  const occupied = Object.keys(show.occupiedSeats || {});
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 px-4">
      <div className="bg-[#111] border border-primary/20 rounded-2xl p-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="font-semibold text-lg">{show.movie.title}</h2>
            <p className="text-gray-400 text-xs mt-0.5">{dateFormat(show.showDateTime)}</p>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-white transition">
            <XIcon className="w-5 h-5" />
          </button>
        </div>

        {/* Screen indicator */}
        <div className="w-full h-1.5 bg-primary/40 rounded-full mb-1" />
        <p className="text-center text-xs text-gray-500 mb-6">SCREEN</p>

        {/* Seat grid */}
        <div className="flex flex-col items-center gap-1.5 text-xs">
          {ROWS.map((row) => (
            <div key={row} className="flex gap-1.5">
              {Array.from({ length: COLS }, (_, i) => {
                const seatId = `${row}${i + 1}`;
                const isOccupied = occupied.includes(seatId);
                return (
                  <div
                    key={seatId}
                    title={seatId}
                    className={`w-8 h-8 flex items-center justify-center rounded border font-medium ${
                      isOccupied
                        ? "bg-red-900/50 border-red-600/60 text-red-400"
                        : "bg-primary/10 border-primary/20 text-gray-400"
                    }`}
                  >
                    {seatId}
                  </div>
                );
              })}
            </div>
          ))}
        </div>

        {/* Legend + summary */}
        <div className="flex items-center justify-between mt-6 text-xs text-gray-400">
          <div className="flex gap-4">
            <span className="flex items-center gap-1.5">
              <span className="w-4 h-4 rounded border border-primary/20 bg-primary/10 inline-block" />
              Available
            </span>
            <span className="flex items-center gap-1.5">
              <span className="w-4 h-4 rounded border border-red-600/60 bg-red-900/50 inline-block" />
              Booked
            </span>
          </div>
          <span>
            <span className="text-red-400 font-semibold">{occupied.length}</span>
            {" / "}{ROWS.length * COLS} seats booked
          </span>
        </div>
      </div>
    </div>
  );
};

const ListShows = () => {
  const { axios, getToken, user } = useAppContext();
  const currency = import.meta.env.VITE_CURRENCY;

  const [shows, setShows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editingId, setEditingId] = useState(null);
  const [editDateTime, setEditDateTime] = useState("");
  const [editPrice, setEditPrice] = useState("");
  const [saving, setSaving] = useState(false);
  const [seatsShow, setSeatsShow] = useState(null);
  const [search, setSearch] = useState("");

  const getAllShow = async () => {
    try {
      const { data } = await axios.get("/api/admin/all-shows", {
        headers: { Authorization: `Bearer ${await getToken()}` },
      });
      setShows(data.shows);
    } catch (error) {
      console.error(error);
    }
    setLoading(false);
  };

  const openEdit = (show) => {
    setEditingId(show._id);
    setEditDateTime(toInputDatetime(show.showDateTime));
    setEditPrice(String(show.showPrice));
  };

  const cancelEdit = () => {
    setEditingId(null);
    setEditDateTime("");
    setEditPrice("");
  };

  const saveEdit = async (showId) => {
    if (!editDateTime || !editPrice) return toast.error("Fill in all fields");
    setSaving(true);
    try {
      const { data } = await axios.put(
        "/api/admin/show/update",
        { showId, showDateTime: editDateTime, showPrice: parseFloat(editPrice) },
        { headers: { Authorization: `Bearer ${await getToken()}` } }
      );
      if (data.success) {
        toast.success("Show updated");
        cancelEdit();
        getAllShow();
      } else {
        toast.error(data.message);
      }
    } catch (error) {
      toast.error("Failed to update show");
    }
    setSaving(false);
  };

  const deleteShow = async (showId) => {
    if (!confirm("Delete this show? All its bookings will also be removed.")) return;
    const { data } = await axios.delete("/api/admin/show/delete", {
      data: { showId },
      headers: { Authorization: `Bearer ${await getToken()}` },
    });
    if (data.success) {
      toast.success("Show deleted");
      getAllShow();
    } else {
      toast.error(data.message);
    }
  };

  const filteredShows = useMemo(() =>
    search.trim()
      ? shows.filter((s) => s.movie.title.toLowerCase().includes(search.toLowerCase()))
      : shows,
    [shows, search]
  );

  useEffect(() => {
    if (user) getAllShow();
  }, [user]);

  return !loading ? (
    <>
      <Title text1="List" text2="Shows" />

      {/* Search bar */}
      <div className="relative mt-4 max-w-xs">
        <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search by movie name..."
          className="w-full bg-primary/5 border border-primary/20 rounded-lg pl-9 pr-8 py-2 text-sm outline-none focus:border-primary transition"
        />
        {search && (
          <button onClick={() => setSearch("")} className="absolute right-2.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-white">
            <XIcon className="w-3.5 h-3.5" />
          </button>
        )}
      </div>

      <div className="max-w-4xl mt-4">
        <table className="w-full border-collapse rounded-md overflow-hidden table-fixed">
          <thead>
            <tr className="bg-primary/20 text-left text-white">
              <th className="p-2 font-medium pl-5 w-36">Movie Name</th>
              <th className="p-2 font-medium w-36">Show Time</th>
              <th className="p-2 font-medium w-24">Bookings</th>
              <th className="p-2 font-medium w-24">Earnings</th>
              <th className="p-2 font-medium w-20">Price</th>
              <th className="p-2 font-medium w-36">Actions</th>
            </tr>
          </thead>
          <tbody className="text-sm font-light">
            {filteredShows.length === 0 && (
              <tr>
                <td colSpan={6} className="text-center text-gray-400 py-6 text-sm">
                  No shows match "{search}"
                </td>
              </tr>
            )}
            {filteredShows.map((show, index) =>
              editingId === show._id ? (
                /* ── Inline edit row ── */
                <tr key={index} className="border-b border-primary/20 bg-primary/10">
                  <td className="p-2 pl-5 truncate max-w-0 text-gray-300" title={show.movie.title}>{show.movie.title}</td>
                  <td className="p-2">
                    <input
                      type="datetime-local"
                      value={editDateTime}
                      onChange={(e) => setEditDateTime(e.target.value)}
                      className="bg-transparent border border-primary/40 rounded px-2 py-1 text-xs outline-none focus:border-primary"
                    />
                  </td>
                  <td className="p-2">{Object.keys(show.occupiedSeats).length}</td>
                  <td className="p-2">{currency} {Object.keys(show.occupiedSeats).length * show.showPrice}</td>
                  <td className="p-2">
                    <input
                      type="number"
                      min="0"
                      step="0.01"
                      value={editPrice}
                      onChange={(e) => setEditPrice(e.target.value)}
                      className="w-24 bg-transparent border border-primary/40 rounded px-2 py-1 text-xs outline-none focus:border-primary"
                    />
                  </td>
                  <td className="p-2 flex gap-2">
                    <button
                      onClick={() => saveEdit(show._id)}
                      disabled={saving}
                      className="px-2 py-1 rounded text-xs font-semibold bg-green-600/20 text-green-400 hover:bg-green-600/40 transition disabled:opacity-50"
                    >
                      {saving ? "Saving…" : "Save"}
                    </button>
                    <button
                      onClick={cancelEdit}
                      className="px-2 py-1 rounded text-xs font-semibold bg-gray-600/20 text-gray-400 hover:bg-gray-600/40 transition"
                    >
                      Cancel
                    </button>
                  </td>
                </tr>
              ) : (
                /* ── Normal row ── */
                <tr
                  key={index}
                  className="border-b border-primary/10 bg-primary/5 even:bg-primary/10"
                >
                  <td className="p-2 pl-5 truncate max-w-0" title={show.movie.title}>{show.movie.title}</td>
                  <td className="p-2">{dateFormat(show.showDateTime)}</td>
                  <td className="p-2">{Object.keys(show.occupiedSeats).length}</td>
                  <td className="p-2">
                    {currency} {Object.keys(show.occupiedSeats).length * show.showPrice}
                  </td>
                  <td className="p-2">
                    {currency} {show.showPrice}
                  </td>
                  <td className="p-2 flex gap-2">
                    <button
                      onClick={() => setSeatsShow(show)}
                      className="px-2 py-1 rounded text-xs font-semibold bg-purple-600/20 text-purple-400 hover:bg-purple-600/40 transition"
                    >
                      Seats
                    </button>
                    <button
                      onClick={() => openEdit(show)}
                      className="px-2 py-1 rounded text-xs font-semibold bg-blue-600/20 text-blue-400 hover:bg-blue-600/40 transition"
                    >
                      Edit
                    </button>
                    <button
                      onClick={() => deleteShow(show._id)}
                      className="px-2 py-1 rounded text-xs font-semibold bg-red-600/20 text-red-400 hover:bg-red-600/40 transition"
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              )
            )}
          </tbody>
        </table>
      </div>

      {seatsShow && (
        <SeatsModal show={seatsShow} onClose={() => setSeatsShow(null)} />
      )}
    </>
  ) : (
    <Loading />
  );
};

export default ListShows;
