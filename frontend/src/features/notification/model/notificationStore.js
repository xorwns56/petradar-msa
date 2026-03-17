import { create } from "zustand";
import SockJS from "sockjs-client/dist/sockjs";
import { Client } from "@stomp/stompjs";

// 알림 + WebSocket 관리 스토어
export const useNotificationStore = create((set, get) => ({
  alerts: [],
  socket: null,

  // 알림 설정
  setAlerts: (updater) => {
    if (typeof updater === "function") {
      set((state) => ({ alerts: updater(state.alerts) }));
    } else {
      set({ alerts: updater });
    }
  },

  // 알림 추가 (WebSocket 수신 시)
  addAlert: (alert) => {
    set((state) => ({ alerts: [alert, ...state.alerts] }));
  },

  // 알림 제거
  removeAlert: (id) => {
    set((state) => ({
      alerts: state.alerts.filter((alert) => alert.id !== id),
    }));
  },

  // WebSocket 연결
  connectSocket: () => {
    const token = sessionStorage.getItem("accessToken");
    if (!token) return;

    const { socket } = get();
    if (socket) return; // 이미 연결됨

    const newSocket = new SockJS(`/api/ws?token=Bearer ${token}`);
    const stompClient = new Client({
      webSocketFactory: () => newSocket,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      reconnectDelay: 5000, // 끊겼을 때 자동 재연결
    });

    stompClient.onConnect = () => {
      console.log("STOMP 소켓 연결됨");
      set({ socket: stompClient });
    };

    stompClient.onDisconnect = () => {
      console.log("STOMP 소켓 연결 해제됨");
      set({ socket: null });
    };

    stompClient.onStompError = (frame) =>
      console.error("STOMP 오류:", frame.body);

    stompClient.activate();
  },

  // WebSocket 연결 해제
  disconnectSocket: () => {
    const { socket } = get();
    if (socket) {
      socket.deactivate();
      set({ socket: null });
    }
  },
}));
