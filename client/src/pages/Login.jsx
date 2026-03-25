import { useState } from "react";
import { useAppContext } from "../context/AppContext";
import axios from "axios";
import toast from "react-hot-toast";
import { Link } from "react-router-dom";

const Login = () => {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const { login, navigate, t } = useAppContext();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const { data } = await axios.post("/api/auth/login", { email, password });
      if (data.success) {
        login(data.token, data.user);
        toast.success("Logged in successfully");
        navigate("/");
      } else {
        toast.error(data.message);
      }
    } catch (error) {
      toast.error("Login failed");
    }
    setLoading(false);
  };

  return (
    <div className="flex items-center justify-center min-h-screen">
      <form onSubmit={handleSubmit} className="bg-primary/10 border border-primary/20 rounded-xl p-8 w-full max-w-md space-y-4">
        <h1 className="text-2xl font-semibold text-center mb-6">{t("sign_in")}</h1>
        <div>
          <label className="block text-sm text-gray-400 mb-1">{t("email")}</label>
          <input type="email" value={email} onChange={e => setEmail(e.target.value)} required
            className="w-full bg-transparent border border-gray-600 rounded-md px-3 py-2 outline-none focus:border-primary" />
        </div>
        <div>
          <label className="block text-sm text-gray-400 mb-1">{t("password")}</label>
          <input type="password" value={password} onChange={e => setPassword(e.target.value)} required
            className="w-full bg-transparent border border-gray-600 rounded-md px-3 py-2 outline-none focus:border-primary" />
        </div>
        <button type="submit" disabled={loading}
          className="w-full bg-primary hover:bg-primary/90 text-white py-2 rounded-md font-medium transition cursor-pointer">
          {loading ? t("signing_in") : t("sign_in")}
        </button>
        <p className="text-center text-sm text-gray-400">
          {t("no_account")} <Link to="/register" className="text-primary hover:underline">{t("register_link")}</Link>
        </p>
      </form>
    </div>
  );
};

export default Login;
