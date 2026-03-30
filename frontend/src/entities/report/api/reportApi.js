import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import api from "@/shared/api/apiInstance";

// 특정 실종 신고에 대한 제보 목록 조회 (페이징)
export const useGetReports = (missingId, params) => {
  return useQuery({
    queryKey: ["reports", missingId, params],
    queryFn: async () => {
      const response = await api.get(`/api/report/missing/${missingId}`, {
        params,
      });
      return response.data;
    },
    enabled: !!missingId,
  });
};

// 제보 등록 (multipart/form-data: request(JSON) + image(File))
export const useCreateReport = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ missingId, formData }) => {
      // Content-Type 헤더를 지정하지 않아야 axios가 boundary 포함하여 자동 설정
      const response = await api.post(
        `/api/report/missing/${missingId}`,
        formData
      );
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["reports"] });
    },
  });
};
