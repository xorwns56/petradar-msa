import axios from "axios";
import { create } from "zustand";
import { jwtDecode } from "jwt-decode";

// 인증 상태 관리 스토어
export const useAuthStore = create((set) => ({
  isAuthenticated: !!sessionStorage.getItem("accessToken"),
  isManualLogout: false, // 자발적 로그아웃 여부 (ProtectedRoute alert 제어용)

  // userId 추출 (토큰에서 디코딩)
  get userId() {
    const token = sessionStorage.getItem("accessToken");
    if (!token) return -1;
    try {
      const decoded = jwtDecode(token);
      return Number(decoded.sub) || -1;
    } catch (e) {
      console.error("JWT decode 실패:", e);
      return -1;
    }
  },

  // 로그인 함수
  login: (token) => {
    sessionStorage.setItem("accessToken", token);
    set({ isAuthenticated: true, isManualLogout: false });
  },

  // 로그아웃 함수 — 서버에 logout 요청하여 Redis Refresh Token 삭제 + 쿠키 만료
  logout: async (manual = true) => {
    try {
      await axios.post("/api/auth/logout", null, { withCredentials: true });
    } catch (e) {
      // 서버 요청 실패해도 로컬 로그아웃은 진행
    }
    sessionStorage.removeItem("accessToken");
    set({ isAuthenticated: false, isManualLogout: manual });
  },
}));

// userId를 getter 대신 selector 함수로 제공 (React 외부에서도 사용 가능)
export const getUserId = () => {
  const token = sessionStorage.getItem("accessToken");
  if (!token) return -1;
  try {
    const decoded = jwtDecode(token);
    return Number(decoded.sub) || -1;
  } catch (e) {
    console.error("JWT decode 실패:", e);
    return -1;
  }
};
