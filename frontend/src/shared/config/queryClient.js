import { QueryClient } from "@tanstack/react-query";

// React Query 클라이언트 설정
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // 5분간 캐시 유지
      staleTime: 5 * 60 * 1000,
      // 브라우저 포커스 시 자동 재요청 비활성화
      refetchOnWindowFocus: false,
      // 실패 시 1회 재시도
      retry: 1,
    },
  },
});

export default queryClient;
