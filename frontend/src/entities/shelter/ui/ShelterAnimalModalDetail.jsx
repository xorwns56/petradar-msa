import Button from "@/shared/ui/button/Button";
import "@/style/ModalDetail.css";

const ShelterAnimalModalDetail = ({ animal, onClose }) => {
  if (!animal) return null;

  return (
    <div className="ModalDetail active" onClick={onClose}>
      <div className="Modal-container" onClick={(e) => e.stopPropagation()}>
        <div className="Modal-contents">
          <div className="img-box" style={{ height: "200px" }}>
            <img
              src={animal.IMAGE_COURS || "/image-default.png"}
              alt="동물 이미지"
              onError={(e) => {
                e.target.src = "/image-default.png";
              }}
              style={{
                objectFit: "cover",
                height: "100%",
                width: "100%",
                borderRadius: "10px",
              }}
            />
          </div>

          <div className="text-contents">
            <div className="contents-t1">
              <h3>{animal.SPECIES_NM}</h3>
            </div>
            <div>
              <h3>색상</h3>
              <p>{animal.COLOR_NM}</p>
            </div>
            <div>
              <h3>나이</h3>
              <p>{animal.AGE_INFO}</p>
            </div>
            <div>
              <h3>특이사항</h3>
              <p>{animal.SFETR_INFO}</p>
            </div>
            <div>
              <h3>보호소</h3>
              <p>{animal.SHTER_NM}</p>
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

export default ShelterAnimalModalDetail;
