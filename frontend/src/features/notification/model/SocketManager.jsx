import { useEffect } from "react";
import { useLocation } from "react-router-dom";
import { useAuthStore } from "@/features/auth/model/authStore";
import { useNotificationStore } from "@/features/notification/model/notificationStore";
import { useSidebarStore } from "@/widgets/sidebar/model/sidebarStore";
import api from "@/shared/api/apiInstance";

/**
 * WebSocket 연결/해제 + 알림 구독 + 경로 변경 시 사이드바 닫기
 * 기존 AuthContext + SidebarContext의 비-UI 로직을 담당
 */
export const SocketManager = () => {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const { socket, connectSocket, disconnectSocket, setAlerts, addAlert } =
    useNotificationStore();
  const closeSidebar = useSidebarStore((s) => s.closeSidebar);
  const location = useLocation();

  // 마운트 시 자동 소켓 연결 (새로고침 포함)
  useEffect(() => {
    if (isAuthenticated) {
      connectSocket();
    } else {
      disconnectSocket();
      setAlerts([]);
    }
  }, [isAuthenticated]);

  // 알림 초기 로드 + WebSocket 구독
  useEffect(() => {
    if (!isAuthenticated) {
      setAlerts([]);
      return;
    }

    // 알림 목록 조회
    const fetchNotification = async () => {
      try {
        const response = await api.get("/api/notification/me");
        setAlerts(response.data);
      } catch (error) {
        console.error("Failed to fetch notification:", error);
      }
    };
    fetchNotification();

    // WebSocket 구독
    if (socket) {
      const subscription = socket.subscribe(
        "/user/queue/notification",
        (message) => {
          const data = JSON.parse(message.body);
          addAlert(data);
        }
      );
      return () => subscription.unsubscribe();
    }
  }, [isAuthenticated, socket]);

  // 경로가 바뀔 때마다 사이드바 닫힘
  useEffect(() => {
    closeSidebar();
  }, [location.pathname]);

  return null;
};
