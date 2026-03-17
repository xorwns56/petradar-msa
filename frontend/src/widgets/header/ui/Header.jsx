import "./Header.css";
import { useLocation, useNavigate } from "react-router-dom";
import { useSidebarStore } from "@/widgets/sidebar/model/sidebarStore";
import { useNotificationStore } from "@/features/notification/model/notificationStore";
import { useAuthStore } from "@/features/auth/model/authStore";

const Header = ({ leftChild }) => {
  const toggleSidebar = useSidebarStore((s) => s.toggleSidebar);
  const alerts = useNotificationStore((s) => s.alerts);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const location = useLocation();
  const nav = useNavigate();

  return (
    <header className="Header">
      <div className="header_left">
        {location.pathname === "/" ? (
          <></>
        ) : (
          <img
            src="/Prev-btn.png"
            alt="logo"
            onClick={() => {
              nav(-1);
            }}
          />
        )}
      </div>
      <div
        className="header_center"
        onClick={() => {
          nav("/");
        }}
      >
        <img src="/PetRadar-Logo-w.png" alt="logo" />
      </div>
      <div className="header_right">
        <p
          onClick={() => {
            nav(isAuthenticated ? "/myPage" : "/login");
          }}
        >
          {isAuthenticated ? "마이페이지" : "로그인/회원가입"}
        </p>
        {isAuthenticated && (
          <p className="Msg-bell" onClick={toggleSidebar}>
            {alerts && (
              <span className="Msg-cnt">{alerts.length}</span>
            )}
          </p>
        )}
      </div>
    </header>
  );
};
export default Header;
