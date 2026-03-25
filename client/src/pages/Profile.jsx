import { useEffect, useState } from "react";
import { useAppContext } from "../context/AppContext";
import toast from "react-hot-toast";
import BlurCircle from "../components/BlurCircle";

const Profile = () => {
  const { user, axios, getToken, login, t } = useAppContext();

  const [name, setName] = useState(user?.name || "");
  const [email, setEmail] = useState(user?.email || "");
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (user) {
      setName(user.name || "");
      setEmail(user.email || "");
    }
  }, [user]);

  const handleSave = async (e) => {
    e.preventDefault();

    if (newPassword && newPassword !== confirmPassword) {
      return toast.error("New passwords do not match");
    }
    if (newPassword && newPassword.length < 6) {
      return toast.error("New password must be at least 6 characters");
    }

    const body = { name, email };
    if (newPassword) {
      body.currentPassword = currentPassword;
      body.newPassword = newPassword;
    }

    setSaving(true);
    try {
      const { data } = await axios.put("/api/user/update-profile", body, {
        headers: { Authorization: `Bearer ${await getToken()}` },
      });
      if (data.success) {
        // Update context user (keep existing token)
        login(getToken(), { ...data.user });
        toast.success("Profile updated");
        setCurrentPassword("");
        setNewPassword("");
        setConfirmPassword("");
      } else {
        toast.error(data.message);
      }
    } catch (error) {
      toast.error("Failed to update profile");
    }
    setSaving(false);
  };

  return (
    <div className="relative flex flex-col items-center px-6 pt-30 md:pt-40 pb-20 min-h-screen">
      <BlurCircle top="100px" left="0" />
      <BlurCircle bottom="100px" right="0" />

      <h1 className="text-2xl font-semibold mb-8 self-start md:self-auto">{t("my_profile")}</h1>

      <form
        onSubmit={handleSave}
        className="w-full max-w-lg bg-primary/10 border border-primary/20 rounded-xl p-8 space-y-5"
      >
        {/* Avatar placeholder */}
        <div className="flex items-center gap-4 mb-2">
          <div className="w-16 h-16 rounded-full bg-primary/30 flex items-center justify-center text-2xl font-bold text-white">
            {user?.name?.[0]?.toUpperCase() || "?"}
          </div>
          <div>
            <p className="font-semibold text-lg">{user?.name}</p>
            <p className="text-gray-400 text-sm">{user?.email}</p>
          </div>
        </div>

        <hr className="border-primary/20" />

        {/* Name */}
        <div>
          <label className="block text-sm text-gray-400 mb-1">{t("display_name")}</label>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            className="w-full bg-transparent border border-gray-600 rounded-md px-3 py-2 outline-none focus:border-primary text-sm transition"
          />
        </div>

        {/* Email */}
        <div>
          <label className="block text-sm text-gray-400 mb-1">{t("email")}</label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            className="w-full bg-transparent border border-gray-600 rounded-md px-3 py-2 outline-none focus:border-primary text-sm transition"
          />
        </div>

        <hr className="border-primary/20" />
        <p className="text-sm text-gray-400">{t("change_password")} <span className="text-xs">({t("leave_blank")})</span></p>

        {/* Current password */}
        <div>
          <label className="block text-sm text-gray-400 mb-1">{t("current_password")}</label>
          <input
            type="password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            placeholder={t("current_password_placeholder")}
            className="w-full bg-transparent border border-gray-600 rounded-md px-3 py-2 outline-none focus:border-primary text-sm transition"
          />
        </div>

        {/* New password */}
        <div>
          <label className="block text-sm text-gray-400 mb-1">{t("new_password")}</label>
          <input
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            placeholder={t("new_password_placeholder")}
            className="w-full bg-transparent border border-gray-600 rounded-md px-3 py-2 outline-none focus:border-primary text-sm transition"
          />
        </div>

        {/* Confirm new password */}
        <div>
          <label className="block text-sm text-gray-400 mb-1">{t("confirm_password")}</label>
          <input
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            placeholder={t("confirm_password_placeholder")}
            className="w-full bg-transparent border border-gray-600 rounded-md px-3 py-2 outline-none focus:border-primary text-sm transition"
          />
        </div>

        <button
          type="submit"
          disabled={saving}
          className="w-full bg-primary hover:bg-primary/90 transition text-white py-2.5 rounded-lg font-semibold disabled:opacity-60 cursor-pointer"
        >
          {saving ? t("saving") : t("save_changes")}
        </button>
      </form>
    </div>
  );
};

export default Profile;
