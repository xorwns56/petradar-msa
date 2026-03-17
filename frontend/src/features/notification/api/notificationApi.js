import { useMutation } from "@tanstack/react-query";
import api from "@/shared/api/apiInstance";
import { useNotificationStore } from "@/features/notification/model/notificationStore";

// 알림 삭제
export const useDeleteNotification = () => {
  const removeAlert = useNotificationStore((s) => s.removeAlert);
  return useMutation({
    mutationFn: async (id) => {
      await api.delete(`/api/notification/${id}`);
      return id;
    },
    onSuccess: (id) => {
      removeAlert(id);
    },
  });
};
