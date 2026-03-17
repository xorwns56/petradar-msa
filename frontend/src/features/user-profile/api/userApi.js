import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import api from "@/shared/api/apiInstance";

// 내 정보 조회
export const useGetMe = () => {
  return useQuery({
    queryKey: ["user", "me"],
    queryFn: async () => {
      const response = await api.get("/api/user/me");
      return response.data;
    },
  });
};

// 내 정보 수정
export const useUpdateMe = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ pw, hp }) => {
      const response = await api.patch("/api/user/me", { pw, hp });
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["user", "me"] });
    },
  });
};

// 회원 탈퇴
export const useDeleteMe = () => {
  return useMutation({
    mutationFn: async () => {
      await api.delete("/api/user/me");
    },
  });
};
