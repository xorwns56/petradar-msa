import { useMutation } from "@tanstack/react-query";
import api from "@/shared/api/apiInstance";

// 로그인
export const useLogin = () => {
  return useMutation({
    mutationFn: async ({ id, pw }) => {
      const response = await api.post("/api/auth/login", { id, pw });
      return response.data;
    },
  });
};

// 회원가입
export const useRegister = () => {
  return useMutation({
    mutationFn: async ({ id, pw, hp }) => {
      const response = await api.post("/api/auth/register", { id, pw, hp });
      return response.data;
    },
  });
};

// 아이디 중복 확인
export const useCheckExist = () => {
  return useMutation({
    mutationFn: async (id) => {
      const response = await api.get("/api/auth/check-exist", {
        params: { id },
      });
      return response.data;
    },
  });
};
