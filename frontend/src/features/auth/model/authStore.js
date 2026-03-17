import { create } from "zustand";
import { jwtDecode } from "jwt-decode";

// 인증 상태 관리 스토어
export const useAuthStore = create((set) => ({
  isAuthenticated: !!sessionStorage.getItem("accessToken"),

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
    set({ isAuthenticated: true });
  },

  // 로그아웃 함수
  logout: () => {
    sessionStorage.removeItem("accessToken");
    set({ isAuthenticated: false });
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
