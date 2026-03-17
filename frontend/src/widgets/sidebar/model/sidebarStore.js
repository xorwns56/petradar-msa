import { create } from "zustand";

// 사이드바 UI 상태 관리
export const useSidebarStore = create((set) => ({
  isActive: false,

  // 사이드바 토글
  toggleSidebar: () => set((state) => ({ isActive: !state.isActive })),

  // 사이드바 닫기
  closeSidebar: () => set({ isActive: false }),
}));
