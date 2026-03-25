const Logo = ({ className = "w-36 h-auto" }) => (
  <svg
    className={className}
    viewBox="0 0 158 32"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
  >
    {/* ── Ticket icon ── */}
    <path
      d="M2 7C2 4.79086 3.79086 3 6 3H26C28.2091 3 30 4.79086 30 7V12C28.3431 12 27 13.3431 27 15C27 16.6569 28.3431 18 30 18V23C30 25.2091 28.2091 27 26 27H6C3.79086 27 2 25.2091 2 23V18C3.65685 18 5 16.6569 5 15C5 13.3431 3.65685 12 2 12V7Z"
      fill="#F84565"
    />
    {/* dashed centre line */}
    <line x1="16" y1="6"  x2="16" y2="9"  stroke="white" strokeWidth="1.4" strokeLinecap="round" strokeDasharray="2 2"/>
    <line x1="16" y1="21" x2="16" y2="24" stroke="white" strokeWidth="1.4" strokeLinecap="round" strokeDasharray="2 2"/>
    {/* checkmark */}
    <path
      d="M9 15.5L13 19.5L22 11"
      stroke="white"
      strokeWidth="2.2"
      strokeLinecap="round"
      strokeLinejoin="round"
    />

    {/* ── Wordmark ── */}
    <text
      x="38"
      y="22"
      fontFamily="Outfit, sans-serif"
      fontWeight="600"
      fontSize="19"
      fill="white"
      letterSpacing="0.3"
    >
      Tickify
    </text>
  </svg>
);

export default Logo;
