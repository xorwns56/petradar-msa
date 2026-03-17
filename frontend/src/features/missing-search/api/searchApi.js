import { useMutation } from "@tanstack/react-query";
import api from "@/shared/api/apiInstance";

// AI 텍스트 검색 (search-service)
export const useSearchByText = () => {
  return useMutation({
    mutationFn: async (query) => {
      const response = await api.post("/api/search/text", {
        query,
        top_k: 50,
      });
      return response.data.results;
    },
  });
};

// AI 이미지 검색 (search-service)
export const useSearchByImage = () => {
  return useMutation({
    mutationFn: async (imageFile) => {
      const formData = new FormData();
      formData.append("image", imageFile);
      formData.append("top_k", 50);
      const response = await api.post("/api/search/image", formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      return response.data.results;
    },
  });
};
