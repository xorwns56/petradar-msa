import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "@/features/auth/model/authStore";

/**
 * 인증이 필요한 라우트를 감싸는 가드 컴포넌트
 * 비로그인 상태면 로그인 페이지로 리다이렉트
 */
const ProtectedRoute = ({ children }) => {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const isManualLogout = useAuthStore((s) => s.isManualLogout);
  const nav = useNavigate();

  useEffect(() => {
    if (!isAuthenticated) {
      // 자발적 로그아웃이면 alert 없이 이동
      if (!isManualLogout) {
        alert("로그인이 필요합니다.");
      }
      nav("/login", { replace: true });
    }
  }, [isAuthenticated, nav]);

  // 인증되지 않은 상태에서는 아무것도 렌더링하지 않음
  if (!isAuthenticated) return null;

  return children;
};

export default ProtectedRoute;
