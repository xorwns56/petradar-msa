import "./MypageModalDetail.css";

import { getMissingImage } from "@/shared/lib/get-missingPet-image"; // 수정필요
import { missingPet } from "@/shared/lib/missingPet"; // 수정필요

import Button from "@/shared/ui/button/Button";
import MyPageReportMap from "@/shared/map/MyPageReportMap";

const MypageModalDetail = ({
  title,
  content,
  petReportPoint,
  petReportPlace,
  isOpen,
  onClose,
}) => {
  return (
    <div className={`MypageModalDetail ${isOpen ? "active" : ""}`}>
      <div className="Modal-container">
        <div className="Modal-contents">
          <div className="Map-box">
            <MyPageReportMap petReportPoint={petReportPoint} />
          </div>
          <div className="text-contents">
            <div className="contents-t1">
              <p className="petType">{title}</p>
            </div>
            <div className="contents-t2">
              <p>{content}</p>
            </div>
          </div>
        </div>
        <div className="Modal-btn" onClick={onClose}>
          <Button text={"X"} type={"Circle"} />
        </div>
      </div>
    </div>
  );
};
export default MypageModalDetail;
