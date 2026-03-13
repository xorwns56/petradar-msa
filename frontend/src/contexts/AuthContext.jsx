import React, { createContext, useState, useEffect, useContext, useMemo, useCallback, useRef } from "react";
import axios from "axios";
import { jwtDecode } from "jwt-decode";
import { useNavigate } from "react-router-dom";
import SockJS from "sockjs-client/dist/sockjs";
import { Client } from "@stomp/stompjs";

// AuthContext 생성
const AuthContext = createContext();

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) throw new Error("useAuth must be used within an AuthProvider");
    return context;
};

export const AuthProvider = ({ children }) => {
    const navigate = useNavigate();
    const [isAuthenticated, setIsAuthenticated] = useState(!!sessionStorage.getItem("accessToken"));
    const [socket, setSocket] = useState(null);

    // 토큰 재발급 중복 요청 방지용
    const isReissuing = useRef(false);
    const reissueQueue = useRef([]);

    // 로그인 함수
    const login = (token) => {
        sessionStorage.setItem("accessToken", token);
        setIsAuthenticated(true);
    };

    // 로그아웃 함수
    const logout = useCallback(() => {
        sessionStorage.removeItem("accessToken");
        setIsAuthenticated(false);
        navigate("/login");
    }, [navigate]);

    // userId 추출
    const userId = useMemo(() => {
        const token = sessionStorage.getItem("accessToken");
        if (!token) return -1;
        try {
            const decoded = jwtDecode(token);
            return Number(decoded.sub) || -1;
        } catch (e) {
            console.error("JWT decode 실패:", e);
            return -1;
        }
    }, [isAuthenticated]);

    // axios API 인스턴스
    const api = useMemo(() => {
        const instance = axios.create({ withCredentials: true });

        instance.interceptors.request.use((config) => {
            const token = sessionStorage.getItem("accessToken");
            if (token) config.headers.Authorization = `Bearer ${token}`;
            return config;
        });

        instance.interceptors.response.use(
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
                        logout();
                        return Promise.reject(error);
                    }

                    originalRequest._retry = true;

                    // 이미 재발급 중이면 큐에 대기
                    if (isReissuing.current) {
                        return new Promise((resolve, reject) => {
                            reissueQueue.current.push({ resolve, reject });
                        }).then((newToken) => {
                            originalRequest.headers.Authorization = `Bearer ${newToken}`;
                            return instance(originalRequest);
                        });
                    }

                    // 토큰 재발급 시도
                    isReissuing.current = true;
                    try {
                        const response = await axios.post("/api/auth/reissue", null, {
                            withCredentials: true, // refreshToken 쿠키 전송
                        });
                        const newToken = response.data.accessToken;
                        sessionStorage.setItem("accessToken", newToken);

                        // 대기 중이던 요청들 재실행
                        reissueQueue.current.forEach((req) => req.resolve(newToken));
                        reissueQueue.current = [];

                        // 원래 요청 재시도
                        originalRequest.headers.Authorization = `Bearer ${newToken}`;
                        return instance(originalRequest);
                    } catch (reissueError) {
                        // refresh token도 만료 → 로그아웃
                        reissueQueue.current.forEach((req) => req.reject(reissueError));
                        reissueQueue.current = [];
                        logout();
                        return Promise.reject(reissueError);
                    } finally {
                        isReissuing.current = false;
                    }
                }

                return Promise.reject(error);
            }
        );

        return instance;
    }, [logout]);

    // 마운트 시 자동 소켓 연결 (새로고침 포함)
    useEffect(() => {
        const token = sessionStorage.getItem("accessToken");
        if(!token){
            if(socket){
                socket.deactivate();
                setSocket(null);
            }
            return;
        }
        if(socket) return;
        const newSocket = new SockJS(`/api/ws?token=Bearer ${token}`);
        const stompClient = new Client({
            webSocketFactory: () => newSocket,
            heartbeatIncoming: 10000,
            heartbeatOutgoing: 10000,
            reconnectDelay: 5000, // 끊겼을 때 자동 재연결
        });

        stompClient.onConnect = () => {
            console.log("STOMP 소켓 연결됨");
            setSocket(stompClient);
        };

        stompClient.onDisconnect = () => {
            console.log("STOMP 소켓 연결 해제됨");
            setSocket(null);
        };

        stompClient.onStompError = (frame) => console.error("STOMP 오류:", frame.body);

        stompClient.activate();
    }, [isAuthenticated]);

    return (
        <AuthContext.Provider value={{ isAuthenticated, userId, login, logout, api, socket }}>
            {children}
        </AuthContext.Provider>
    );
};
