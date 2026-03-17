import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import api from "@/shared/api/apiInstance";

// 지도 마커 표시용 전체 실종 목록 조회
export const useGetAllMissing = () => {
  return useQuery({
    queryKey: ["missing", "all"],
    queryFn: async () => {
      const response = await api.get("/api/missing/all");
      return response.data;
    },
  });
};

// 내 실종 목록 조회
export const useGetMyMissing = () => {
  return useQuery({
    queryKey: ["missing", "me"],
    queryFn: async () => {
      const response = await api.get("/api/missing/me");
      return response.data;
    },
  });
};

// 실종 목록 페이징 조회 (제목 검색)
export const useGetMissingList = (params) => {
  return useQuery({
    queryKey: ["missing", "list", params],
    queryFn: async () => {
      const response = await api.get("/api/missing", { params });
      return response.data;
    },
    enabled: !!params, // params가 있을 때만 실행
  });
};

// 실종 상세 조회
export const useGetMissingDetail = (id) => {
  return useQuery({
    queryKey: ["missing", id],
    queryFn: async () => {
      const response = await api.get(`/api/missing/${id}`);
      return response.data;
    },
    enabled: !!id,
  });
};

// 실종 신고 등록
export const useCreateMissing = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (requestBody) => {
      const response = await api.post("/api/missing", requestBody);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["missing"] });
    },
  });
};

// 실종 신고 수정
export const useUpdateMissing = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, requestBody }) => {
      const response = await api.patch(`/api/missing/${id}`, requestBody);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["missing"] });
    },
  });
};

// 실종 신고 삭제
export const useDeleteMissing = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id) => {
      await api.delete(`/api/missing/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["missing"] });
    },
  });
};

// 배치 조회 (AI 검색 결과에서 실제 데이터 조회)
export const fetchMissingBatch = async (ids) => {
  const response = await api.get("/api/missing/batch", {
    params: { ids: ids.join(",") },
  });
  return response.data;
};
