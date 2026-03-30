import "./MissingReport.css";
import Header from "@/widgets/header/ui/Header";
import Button from "@/shared/ui/button/Button";
import LocationMap from "@/shared/map/LocationMap";
import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useCreateReport } from "@/entities/report/api/reportApi";

const MissingReportPage = () => {
  const nav = useNavigate();
  const params = useParams();
  const createReport = useCreateReport();

  const [form, setForm] = useState({
    title: "",
    content: "",
    petImage: "",
    petReportPlace: "",
    petReportPoint: null,
  });
  const onSubmitButtonClick = async () => {
      try {
          const { petImage, ...requestData } = form;

          // multipart/form-data로 전송 (백엔드가 @RequestPart로 수신)
          const formData = new FormData();
          formData.append("request", new Blob([JSON.stringify(requestData)], { type: "application/json" }));

          // state에 저장된 File 객체 첨부
          if (petImage) {
            formData.append("image", petImage);
          }

          await createReport.mutateAsync({
            missingId: params.petMissingId,
            formData,
          });
          nav("/missingList");
      } catch (error) {
          alert("제보 등록에 실패했습니다. 다시 시도해주세요.");
      }
  };

  const handleChange = (e) => {
    const { name, value, files } = e.target;
    // 이미지는 File 객체를 state에 저장 (제출 시 FormData로 전송)
    if (name === "petImage" && files?.[0]) {
      setForm((prev) => ({ ...prev, petImage: files[0] }));
      return;
    }

    setForm((prev) => ({
      ...prev,
      [name]: value,
    }));
  };
  const onLocationSelect = (latlng) => {
    setForm((prev) => ({
      ...prev,

      petReportPoint: {
        lat: latlng.lat,
        lng: latlng.lng,
      },
    }));
  };

  return (
    <div className="MissingReport">
      <Header leftChild={true} />
      <div className="MissingReport-container inner">
        <div className="menu-title">
          <h3>실종 동물 제보하기</h3>
        </div>
        <div className="MissingReportForms">
          <div className="MissingReportForm">
            <h3>사진</h3>
            <input
              type="file"
              name="petImage"
              accept="image/*"
              onChange={handleChange}
            />
            {/* <input name="petImage" value={form.title} onChange={handleChange} /> */}
          </div>
          <div className="MissingReportForm">
            <h3>제목</h3>
            {/* <input type="text" /> */}

            <input name="title" value={form.title} onChange={handleChange} />
          </div>
          <div className="MissingReportForm">
            <h3>내용</h3>
            {/* <textarea type="text" placeholder="상세한 설명을 적어주세요." /> */}
            <textarea
              name="content"
              value={form.content}
              onChange={handleChange}
              placeholder="상세한 설명을 적어주세요."
            />
          </div>

          <div className="MissingReportForm">
            <h3>발견 장소</h3>
            <LocationMap onSelect={onLocationSelect} />
          </div>
          <div className="MissingReport-btn">
            <Button
              onClick={onSubmitButtonClick}
              text={"신고하기"}
              type={"Square_lg"}
            ></Button>
          </div>
        </div>
      </div>
    </div>
  );
};
export default MissingReportPage;
