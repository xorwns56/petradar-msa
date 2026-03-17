import { create } from "zustand";

// 실종 동물 검색 상태 관리
export const useSearchStore = create((set) => ({
  // 검색 타입: title(제목 검색), text(AI 텍스트 검색), image(AI 이미지 검색)
  searchType: "title",
  sortType: "latest",
  searchInput: "",
  imageFile: null,
  page: 1,
  totalItems: 0,
  itemSize: 10,

  // AI 검색 클라이언트 페이징용 전체 결과 보관
  allResults: [],
  // 현재 페이지에 표시할 목록
  missingList: [],

  setSearchType: (searchType) => set({ searchType }),
  setSortType: (sortType) => set({ sortType }),
  setSearchInput: (searchInput) => set({ searchInput }),
  setImageFile: (imageFile) => set({ imageFile }),
  setPage: (page) => set({ page }),
  setTotalItems: (totalItems) => set({ totalItems }),
  setAllResults: (allResults) => set({ allResults }),
  setMissingList: (missingList) => set({ missingList }),

  // 검색 타입 변경 시 초기화
  resetSearch: () =>
    set({
      searchInput: "",
      imageFile: null,
      page: 1,
      totalItems: 0,
      missingList: [],
      allResults: [],
    }),
}));
