import "./MissingList.css";
import { useState, useEffect } from "react";
import Button from "@/shared/ui/button/Button";
import Header from "@/widgets/header/ui/Header";
import MissingItem from "@/entities/missing/ui/MissingItem";
import PetModalDetail from "@/entities/missing/ui/PetModalDetail";
import Pagination from "@/shared/ui/pagination/Pagination";
import { useNavigate } from "react-router-dom";
import { useAuthStore, getUserId } from "@/features/auth/model/authStore";
import api from "@/shared/api/apiInstance";

const MissingListPage = () => {
  const [selectedItem, setSelectedItem] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const nav = useNavigate();
  const userId = getUserId();

  const [sortType, setSortType] = useState("latest");
  const [searchInput, setSearchInput] = useState("");

  // 표시할 목록
  const [missingList, setMissingList] = useState([]);

  // 페이지네이션 상태
  const [page, setPage] = useState(1);
  const [totalItems, setTotalItems] = useState(0);
  const itemSize = 10;

  // 목록/검색 조회
  // - 검색어 있음 → search-service(Elasticsearch) 전문 검색 (/api/search): 오타·표기 흔들림 보정 + 관련도 정렬
  // - 검색어 없음(브라우징) → report-service 목록 (/api/missing): 정렬/서버 페이지네이션
  const fetchByTitle = async (targetPage = page) => {
    try {
      if (searchInput.trim() !== "") {
        // Elasticsearch 전문 검색
        const response = await api.get("/api/search", {
          params: {
            q: searchInput,
            page: targetPage - 1, // 0-indexed
            size: itemSize,
          },
        });
        // ES 검색 결과(SearchDTO)를 목록 카드가 쓰는 형태로 매핑 (imageUrl→petImage, missingId→id)
        const items = response.data.results.map((r) => ({
          id: r.missingId,
          userId: r.userId,
          petImage: r.imageUrl,
          petName: r.petName,
          petType: r.petType,
          petGender: r.petGender,
          petBreed: r.petBreed,
          petAge: r.petAge,
          petMissingDate: r.petMissingDate,
          title: r.title,
          content: r.content,
        }));
        setMissingList(items);
        setTotalItems(response.data.totalHits);
      } else {
        // 브라우징: report-service 목록 (정렬 지원)
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
      }
    } catch (error) {
      console.error("Failed to fetch missing list:", error);
    }
  };

  // 검색 실행 (새 검색 시 1페이지로 리셋)
  const onSearch = () => {
    setPage(1);
    fetchByTitle(1);
  };

  // 페이지 변경
  const onPageClick = (newPage) => {
    setPage(newPage);
    fetchByTitle(newPage);
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

  // 엔터키로 검색
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
          <select value={sortType} onChange={onChangeSortType}>
            <option value={"latest"}>최신순</option>
            <option value={"oldest"}>오래된 순</option>
          </select>
          <input
            value={searchInput}
            onChange={onChangeInput}
            onKeyDown={onKeyDown}
            placeholder="검색할 제목을 입력하세요."
          />
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
