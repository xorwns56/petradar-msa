import { useEffect, useRef, useState } from "react";
import loadKakaoMap from "../shared/map/KakaoMapLoader";

const LocationMap = ({ init, onSelect }) => {
  const [location, setLocation] = useState(null);
  const markerRef = useRef(null);
  const isInit = useRef(false);

  const handleMapClick = (map, latlng) => {
    if (markerRef.current) {
      markerRef.current.setMap(null);
    }

    const markerImage = new window.kakao.maps.MarkerImage(
      "/orangeMarker.png",
      new window.kakao.maps.Size(40, 40),
      { offset: new window.kakao.maps.Point(20, 40) }
    );

    const newMarker = new window.kakao.maps.Marker({
      position: latlng,
      image: markerImage,
    });

    newMarker.setMap(map);
    markerRef.current = newMarker;

    if (onSelect) {
      onSelect({
        lat: latlng.getLat(),
        lng: latlng.getLng(),
      });
    }
  };

  useEffect(() => {
    // 카카오맵 스크립트 로드 (중앙화된 로더 사용)
    loadKakaoMap().then((kakao) => {
      const locationMapContainer = document.getElementById("locationMap");

      const options = {
        center: new kakao.maps.LatLng(
          37.374659507684,
          126.73570005568
        ),
        level: 3,
      };

      const map = new kakao.maps.Map(locationMapContainer, options);
      setLocation(map);

      kakao.maps.event.addListener(
        map,
        "click",
        function (mouseEvent) {
          const clickedLocation = mouseEvent.latLng;
          handleMapClick(map, clickedLocation);
        }
      );
    }).catch((err) => console.error(err));
  }, []);

  useEffect(() => {
    if (!isInit.current && init && location) {
      handleMapClick(
        location,
        new window.kakao.maps.LatLng(init.lat, init.lng)
      );
      isInit.current = true;
      location.setCenter(new window.kakao.maps.LatLng(init.lat, init.lng));
    }
  }, [init, location]);

  return (
    <div id="locationMap" style={{ width: "100%", height: "350px" }}></div>
  );
};

export default LocationMap;
