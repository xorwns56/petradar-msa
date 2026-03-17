import "./MyPost.css";
import { useEffect, useState } from "react";
import Pagination from "@/shared/ui/pagination/Pagination";
import MypageModalDetail from "@/entities/report/ui/MypageModalDetail";
import MyPostReportItem from "@/widgets/my-post/ui/MyPostReportItem";
import MyPostMissingItem from "@/widgets/my-post/ui/MyPostMissingItem";
import { useNavigate } from "react-router-dom";
import { useGetMyMissing, useDeleteMissing } from "@/entities/missing/api/missingApi";
import { useGetReports } from "@/entities/report/api/reportApi";

const MyPost = () => {
  const nav = useNavigate();
  const [ petMissingItem, setPetMissingItem ] = useState(null);
  const [page, setPage] = useState(1);
  const itemSize = 2;
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalData, setModalData] = useState(null);

  // 내 실종 목록 조회 (React Query)
  const { data: myMissing = [], refetch: refetchMyMissing } = useGetMyMissing();

  // 제보 목록 조회 (React Query)
  const { data: reportData } = useGetReports(
    petMissingItem?.id,
    {
      page: page - 1,
      size: itemSize,
      sort: "createdAt,desc",
    }
  );
  const reports = reportData?.content || [];
  const totalReports = reportData?.totalElements || 0;

  // 실종 삭제 mutation
  const deleteMissing = useDeleteMissing();

  const onPageClick = (page) => setPage(page);

  // 초기 선택: 첫 번째 실종 항목
  useEffect(() => {
    if (myMissing.length > 0 && !petMissingItem) {
      setPetMissingItem(myMissing[0]);
    }
  }, [myMissing]);

  // 실종 항목 변경 시 페이지 리셋
  useEffect(() => {
    setPage(1);
  }, [petMissingItem]);

  // 초기 마운트 시 모달 닫기
  useEffect(() => {
    setIsModalOpen(false);
  }, []);

  return (
    <div className="MyPost">
      <div className="post-title">
        <h3>나의 실종신고</h3>
      </div>
      <div className="post-content">
        {/* 실종신고가 없을 때만 안내 텍스트 표시 */}
        {myMissing.length === 0 ? (
          <div className="none-text">
            <p>현재 실종신고가 존재하지 않습니다.</p>
          </div>
        ) : (
          <>
            <div className="missing">
              {myMissing.map((item) => {
                return (
                  <MyPostMissingItem
                    key={`petMissing${item.id}`}
                    petName={item.petName}
                    isActive={petMissingItem && item.id === petMissingItem.id}
                    onClick={() => setPetMissingItem(item)}
                  />
                );
              })}
            </div>
            {petMissingItem && (
            <div className="MyPostMissingList">
              <div className="MyPostMissingList-container">
                <div className="title">
                  <p>{petMissingItem.title}</p>
                </div>
              </div>
              <div className="btn">
                <p
                  onClick={() => {
                    nav(`/missingRevise/${petMissingItem.id}`);
                  }}
                >
                  수정
                </p>
                <span>|</span>
                <p
                  onClick={async () => {
                    if (
                      confirm(
                        `${petMissingItem.petName}의 실종신고를 정말 삭제하시겠습니까?`
                      )
                    ) {
                      deleteMissing.mutate(petMissingItem.id, {
                        onSuccess: () => {
                          // 삭제 후 목록 갱신
                          refetchMyMissing().then(({ data }) => {
                            setPetMissingItem(data?.length > 0 ? data[0] : null);
                          });
                        },
                        onError: () => {
                          alert("삭제에 실패했습니다.");
                        },
                      });
                    }
                  }}
                >
                  삭제
                </p>
              </div>
            </div>
            )}
            <div className="report">
              {reports.map((item) => {
                return (
                  <MyPostReportItem
                    key={`petReport${item.id}`}
                    title={item.title}
                    content={item.content}
                    petImage={item.petImage}
                    onClick={() => {
                      setModalData(item);
                      setIsModalOpen(true);
                    }}
                  />
                );
              })}
            </div>
            <Pagination
              totalItems={totalReports}
              page={page}
              onClick={onPageClick}
              itemSize={itemSize}
            />
          </>
        )}
      </div>
      <MypageModalDetail
        {...modalData}
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
      />
    </div>
  );
};
export default MyPost;
