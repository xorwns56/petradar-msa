import "./MissingList.css";
import { useState, useEffect, useRef } from "react";
import Button from "@/shared/ui/button/Button";
import Header from "@/widgets/header/ui/Header";
import MissingItem from "@/entities/missing/ui/MissingItem";
import PetModalDetail from "@/entities/missing/ui/PetModalDetail";
import Pagination from "@/shared/ui/pagination/Pagination";
import { useNavigate } from "react-router-dom";
import { useAuthStore, getUserId } from "@/features/auth/model/authStore";
import api from "@/shared/api/apiInstance";
import { fetchMissingBatch } from "@/entities/missing/api/missingApi";

const MissingListPage = () => {
  const [selectedItem, setSelectedItem] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const nav = useNavigate();
  const userId = getUserId();

  // 검색 타입: title(제목 검색), image(AI 이미지 검색)
  const [searchType, setSearchType] = useState("title");
  const [sortType, setSortType] = useState("latest");
  const [searchInput, setSearchInput] = useState("");
  const [imageFile, setImageFile] = useState(null);
  const imageInputRef = useRef(null);

  // 표시할 목록
  const [missingList, setMissingList] = useState([]);
  // AI 검색 클라이언트 페이징용 전체 결과 보관
  const [allResults, setAllResults] = useState([]);

  // 페이지네이션 상태
  const [page, setPage] = useState(1);
  const [totalItems, setTotalItems] = useState(0);
  const itemSize = 10;

  // AI 검색: allResults가 바뀌거나 page가 바뀌면 현재 페이지 분량만 잘라서 표시
  useEffect(() => {
    if (searchType !== "title" && allResults.length > 0) {
      const start = (page - 1) * itemSize;
      const end = start + itemSize;
      setMissingList(allResults.slice(start, end));
    }
  }, [allResults, page, searchType]);

  // 제목 검색 (report-service, 서버 페이지네이션)
  const fetchByTitle = async (targetPage = page) => {
    try {
      const response = await api.get("/api/missing", {
        params: {
          search: searchInput,
          sort: sortType,
          page: targetPage - 1, // Spring Pageable은 0-indexed
          size: itemSize,
        }
      });
      setMissingList(response.data.content);
      setTotalItems(response.data.totalElements);
      setAllResults([]); // 서버 페이징이므로 클라이언트 캐시 비움
    } catch (error) {
      console.error("Failed to fetch missing list:", error);
    }
  };

  // AI 이미지 검색 (search-service → report-service batch, 클라이언트 페이징)
  const fetchByImage = async () => {
    if (!imageFile) {
      alert("검색할 이미지를 선택해주세요.");
      return;
    }
    try {
      // 1단계: search-service에 이미지 전송
      const formData = new FormData();
      formData.append("image", imageFile);
      formData.append("top_k", 50);
      const searchResponse = await api.post("/api/search/image", formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      const results = searchResponse.data.results;
      if (results.length === 0) {
        setAllResults([]);
        setMissingList([]);
        setTotalItems(0);
        return;
      }

      // 2단계: report-service에서 실제 데이터 조회 (유사도 순서 유지)
      const ids = results.map((r) => r.missing_id);
      const batchData = await fetchMissingBatch(ids);
      // 유사도 정보를 실제 데이터에 병합
      const similarityMap = Object.fromEntries(
        results.map((r) => [r.missing_id, r.similarity])
      );
      const merged = batchData.map((item) => ({
        ...item,
        similarity: similarityMap[item.id],
      }));
      // 전체 결과 저장 → useEffect에서 페이지별로 슬라이싱
      setAllResults(merged);
      setTotalItems(merged.length);
    } catch (error) {
      console.error("Failed to search by image:", error);
      alert("AI 이미지 검색에 실패했습니다.");
    }
  };

  // 검색 실행 (새 검색 시 1페이지로 리셋)
  const onSearch = () => {
    setPage(1);
    if (searchType === "title") fetchByTitle(1);
    else if (searchType === "image") fetchByImage();
  };

  // 페이지 변경
  const onPageClick = (newPage) => {
    setPage(newPage);
    // 제목 검색: 서버 페이징이므로 API 재호출
    if (searchType === "title") fetchByTitle(newPage);
    // AI 검색: 클라이언트 페이징이므로 useEffect에서 슬라이싱 처리
  };

  // 초기 로딩: 전체 목록 1페이지
  useEffect(() => {
    fetchByTitle(1);
  }, []);

  const onChangeInput = (e) => {
    setSearchInput(e.target.value);
  };

  const onChangeSortType = (e) => {
    setSortType(e.target.value);
  };

  // 검색 타입 변경 시 입력 및 페이지 초기화
  const onChangeSearchType = (e) => {
    setSearchType(e.target.value);
    setSearchInput("");
    setImageFile(null);
    setPage(1);
    setTotalItems(0);
    setMissingList([]);
    setAllResults([]);
    // 파일 input 초기화
    if (imageInputRef.current) {
      imageInputRef.current.value = "";
    }
  };

  // 이미지 파일 선택
  const onImageChange = (e) => {
    if (e.target.files?.[0]) {
      setImageFile(e.target.files[0]);
    }
  };

  // 엔터키로 검색 (텍스트 입력 시)
  const onKeyDown = (e) => {
    if (e.key === "Enter") onSearch();
  };

  return (
    <div className="MissingList">
      <Header leftChild={true} />
      <div className="MissingList-container inner">
        <div className="PageTitle">
          <h3>실종 동물 목록</h3>
        </div>
        {/* search-box */}
        <div className="search-box">
          {/* 검색 타입이 제목 검색일 때만 정렬 옵션 표시 */}
          {searchType === "title" && (
            <select value={sortType} onChange={onChangeSortType}>
              <option value={"latest"}>최신순</option>
              <option value={"oldest"}>오래된 순</option>
            </select>
          )}
          {/* 검색 타입 선택 */}
          <select value={searchType} onChange={onChangeSearchType}>
            <option value={"title"}>제목 검색</option>
            <option value={"image"}>AI 이미지 검색</option>
          </select>
          {/* 검색 타입에 따른 입력 필드 */}
          {searchType === "image" ? (
            <input
              ref={imageInputRef}
              type="file"
              accept="image/*"
              onChange={onImageChange}
            />
          ) : (
            <input
              value={searchInput}
              onChange={onChangeInput}
              onKeyDown={onKeyDown}
              placeholder="검색할 제목을 입력하세요."
            />
          )}
          <Button text={"조회"} type={"Square"} onClick={onSearch} />
        </div>
        <div className="MissingItems">
          {missingList.length === 0 ? (
            <p className="empty-text">검색 결과가 없습니다.</p>
          ) : (
            missingList.map((item) => (
              <MissingItem
                key={item.id}
                missingDTO={item}
                toggleModal={() => {
                  setSelectedItem(item);
                  setIsModalOpen(true);
                }}
                onClick={() => {
                  nav(`/missingReport/${item.id}`);
                }}
                myMissing={userId === item.userId}
              />
            ))
          )}
        </div>
        {/* 모든 검색 타입에서 페이지네이션 표시 */}
        <Pagination
          totalItems={totalItems}
          page={page}
          itemSize={itemSize}
          onClick={onPageClick}
        />
        <div className="MissingList-btn">
          <Button
            text={"실종 동물 신고"}
            type={"Square_lg"}
            onClick={() => {
              nav("/missingDeclaration");
            }}
          />
        </div>
      </div>

      {/* 모달: URL 오타 수정 (petMissingId → id, 이중 중괄호 제거) */}
      {selectedItem && (
        <PetModalDetail
          missingPet={selectedItem}
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          onClick={() => {
            nav(`/missingReport/${selectedItem.id}`);
          }}
          myMissing={userId === selectedItem.userId}
        />
      )}
    </div>
  );
};
export default MissingListPage;
