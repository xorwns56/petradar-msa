import axios from "axios";

// 토큰 재발급 중복 요청 방지용
let isReissuing = false;
let reissueQueue = [];

// axios API 인스턴스
const api = axios.create({ withCredentials: true });

// 요청 인터셉터: JWT 토큰 첨부
api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem("accessToken");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// 응답 인터셉터: 토큰 갱신 + 401 자동 재발급
api.interceptors.response.use(
  (response) => {
    const newToken = response.headers.authorization;
    if (newToken) sessionStorage.setItem("accessToken", newToken);
    return response;
  },
  async (error) => {
    const originalRequest = error.config;

    // 401 응답 + 재시도하지 않은 요청일 때만 reissue 시도
    if (error.response?.status === 401 && !originalRequest._retry) {
      // reissue 요청 자체가 401이면 로그아웃 (refresh token도 만료)
      if (originalRequest.url === "/api/auth/reissue") {
        // Zustand는 React 외부에서도 접근 가능
        const { useAuthStore } = await import("@/features/auth/model/authStore");
        useAuthStore.getState().logout();
        return Promise.reject(error);
      }

      originalRequest._retry = true;

      // 이미 재발급 중이면 큐에 대기
      if (isReissuing) {
        return new Promise((resolve, reject) => {
          reissueQueue.push({ resolve, reject });
        }).then((newToken) => {
          originalRequest.headers.Authorization = `Bearer ${newToken}`;
          return api(originalRequest);
        });
      }

      // 토큰 재발급 시도
      isReissuing = true;
      try {
        const response = await axios.post("/api/auth/reissue", null, {
          withCredentials: true, // refreshToken 쿠키 전송
        });
        const newToken = response.data.accessToken;
        sessionStorage.setItem("accessToken", newToken);

        // 대기 중이던 요청들 재실행
        reissueQueue.forEach((req) => req.resolve(newToken));
        reissueQueue = [];

        // 원래 요청 재시도
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return api(originalRequest);
      } catch (reissueError) {
        // refresh token도 만료 → 로그아웃
        reissueQueue.forEach((req) => req.reject(reissueError));
        reissueQueue = [];
        const { useAuthStore } = await import("@/features/auth/model/authStore");
        useAuthStore.getState().logout();
        return Promise.reject(reissueError);
      } finally {
        isReissuing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default api;
