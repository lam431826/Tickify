import { useState } from "react";
import { useAppContext } from "../context/AppContext";
import axios from "axios";
import toast from "react-hot-toast";
import { Link } from "react-router-dom";

const Register = () => {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const { login, navigate } = useAppContext();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const { data } = await axios.post("/api/auth/register", { name, email, password });
      if (data.success) {
        login(data.token, data.user);
        toast.success("Account created successfully");
        navigate("/");
      } else {
        toast.error(data.message);
      }
    } catch (error) {
      toast.error("Registration failed");
    }
    setLoading(false);
  };

  return (
    <div className="flex items-center justify-center min-h-screen">
      <form onSubmit={handleSubmit} className="bg-primary/10 border border-primary/20 rounded-xl p-8 w-full max-w-md space-y-4">
        <h1 className="text-2xl font-semibold text-center mb-6">Create Account</h1>
        <div>
          <label className="block text-sm text-gray-400 mb-1">Name</label>
          <input type="text" value={name} onChange={e => setName(e.target.value)} required
            className="w-full bg-transparent border border-gray-600 rounded-md px-3 py-2 outline-none focus:border-primary" />
        </div>
        <div>
          <label className="block text-sm text-gray-400 mb-1">Email</label>
          <input type="email" value={email} onChange={e => setEmail(e.target.value)} required
            className="w-full bg-transparent border border-gray-600 rounded-md px-3 py-2 outline-none focus:border-primary" />
        </div>
        <div>
          <label className="block text-sm text-gray-400 mb-1">Password</label>
          <input type="password" value={password} onChange={e => setPassword(e.target.value)} required
            className="w-full bg-transparent border border-gray-600 rounded-md px-3 py-2 outline-none focus:border-primary" />
        </div>
        <button type="submit" disabled={loading}
          className="w-full bg-primary hover:bg-primary/90 text-white py-2 rounded-md font-medium transition cursor-pointer">
          {loading ? "Creating account..." : "Register"}
        </button>
        <p className="text-center text-sm text-gray-400">
          Already have an account? <Link to="/login" className="text-primary hover:underline">Sign In</Link>
        </p>
      </form>
    </div>
  );
};

export default Register;
