import { useEffect, useRef } from "react";
import loadKakaoMap from "../shared/map/KakaoMapLoader";
import "../style/Map.css";

const Map = ({ shelters, onSelect, setCenterRef }) => {
  const mapRef = useRef(null);
  const mapInstance = useRef(null);
  const geocoderInstance = useRef(null);

  useEffect(() => {
    // 카카오맵 스크립트 로드 (중앙화된 로더 사용)
    loadKakaoMap().then((kakao) => {
      const map = new kakao.maps.Map(mapRef.current, {
        center: new kakao.maps.LatLng(37.423233, 127.105261),
        level: 12,
      });
      mapInstance.current = map;

      const geocoder = new kakao.maps.services.Geocoder();
      geocoderInstance.current = geocoder;

      if (!Array.isArray(shelters)) return;

      shelters.forEach((shelter) => {
        const address =
          shelter.REFINE_LOTNO_ADDR || shelter.REFINE_ROADNM_ADDR;
        if (!address) return;

        geocoder.addressSearch(address, (result, status) => {
          if (status === kakao.maps.services.Status.OK) {
            const coords = new kakao.maps.LatLng(
              result[0].y,
              result[0].x
            );

            // <div> + <img> 형태로 마커 생성
            const markerWrapper = document.createElement("div");
            markerWrapper.className = "custom-marker-wrapper";

            const img = document.createElement("img");
            img.src = "/orangeMarker.png";
            img.alt = "marker";
            img.className = "custom-marker-image";

            markerWrapper.appendChild(img);

            markerWrapper.addEventListener("click", () => {
              map.panTo(coords);
              map.setLevel(5);
              onSelect?.(shelter);
            });

            const overlay = new kakao.maps.CustomOverlay({
              position: coords,
              content: markerWrapper,
              yAnchor: 1,
            });

            overlay.setMap(map);
          }
        });
      });

      // setCenterRef: 보호소 목록에서 클릭 시 지도 중심 이동
      if (setCenterRef) {
        setCenterRef.current = (shelter) => {
          const addr = shelter.REFINE_ROADNM_ADDR || shelter.REFINE_LOTNO_ADDR;
          if (!addr || !geocoderInstance.current || !mapInstance.current) return;

          geocoderInstance.current.addressSearch(addr, (result, status) => {
            if (status === kakao.maps.services.Status.OK) {
              const coords = new kakao.maps.LatLng(
                result[0].y,
                result[0].x
              );
              const current = mapInstance.current.getCenter();

              if (
                current.getLat() !== coords.getLat() ||
                current.getLng() !== coords.getLng()
              ) {
                mapInstance.current.panTo(coords);
                mapInstance.current.setLevel(9);
              }
            }
          });
        };
      }
    }).catch((err) => console.error(err));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="Map">
      <div ref={mapRef} className="Map-box" />
    </div>
  );
};

export default Map;
