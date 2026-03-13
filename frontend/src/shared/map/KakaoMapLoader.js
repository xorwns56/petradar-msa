/**
 * 카카오맵 SDK 스크립트를 한 번만 로드하고 재사용하는 유틸
 * 여러 맵 컴포넌트(Map, LocationMap, MissingMap 등)에서 중복 로드 방지
 */
const KAKAO_APP_KEY = import.meta.env.VITE_KAKAO_APP_KEY || "b737c6777956f74337fc9bc5a08e3b55";

let loadPromise = null;

const loadKakaoMap = () => {
  // 이미 로드 중이거나 완료된 경우 기존 Promise 반환
  if (loadPromise) return loadPromise;

  loadPromise = new Promise((resolve, reject) => {
    // 이미 로드된 경우
    if (window.kakao && window.kakao.maps) {
      window.kakao.maps.load(() => resolve(window.kakao));
      return;
    }

    const script = document.createElement("script");
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_APP_KEY}&libraries=services&autoload=false`;
    script.async = true;

    script.onload = () => {
      window.kakao.maps.load(() => resolve(window.kakao));
    };

    script.onerror = () => {
      loadPromise = null; // 실패 시 재시도 가능하도록 초기화
      reject(new Error("카카오맵 스크립트 로드 실패"));
    };

    document.head.appendChild(script);
  });

  return loadPromise;
};

export default loadKakaoMap;
