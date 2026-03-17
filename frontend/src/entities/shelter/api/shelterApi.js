import { useQuery } from "@tanstack/react-query";

// 보호소 유기동물 목록 조회 (기존 ShelterData.js 대체)
export const useGetShelterAnimals = () => {
  return useQuery({
    queryKey: ["shelter", "animals"],
    queryFn: async () => {
      const res = await fetch(
        "https://openapi.gg.go.kr/AbdmAnimalProtect?Key=f2f06fff472f435591f277efff82fd36&Type=json&pIndex=1&pSize=100"
      );
      const json = await res.json();
      const items = json.AbdmAnimalProtect?.[1]?.row || [];
      return items;
    },
    // 보호소 데이터는 자주 바뀌지 않으므로 10분 캐시
    staleTime: 10 * 60 * 1000,
  });
};
