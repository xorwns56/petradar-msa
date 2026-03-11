from transformers import CLIPProcessor, CLIPModel
from PIL import Image
import torch
import io

# CLIP 모델을 싱글톤으로 로딩 (앱 시작 시 한 번만 로드)
# Spring의 @Component + @Bean과 유사한 개념
MODEL_NAME = "openai/clip-vit-base-patch32"

model = CLIPModel.from_pretrained(MODEL_NAME)
processor = CLIPProcessor.from_pretrained(MODEL_NAME)
model.eval()  # 추론 모드 (학습 X)

def get_image_vector(image_bytes: bytes) -> list[float]:
    """
    이미지 바이트 → CLIP 이미지 인코더 → 512차원 벡터 반환
    report-service에서 실종 신고 등록 시 호출
    """
    image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    inputs = processor(images=image, return_tensors="pt")

    with torch.no_grad():  # 추론 시 gradient 계산 불필요 (메모리/속도 최적화)
        image_features = model.get_image_features(**inputs)
        # L2 정규화: 벡터 크기를 1로 맞춰 코사인 유사도 계산에 최적화
        image_features = image_features / image_features.norm(dim=-1, keepdim=True)

    return image_features[0].tolist()

def get_text_vector(text: str) -> list[float]:
    """
    텍스트 → CLIP 텍스트 인코더 → 512차원 벡터 반환
    이미지와 같은 벡터 공간에 있어 크로스 모달 검색 가능
    ex) "갈색 강아지" → 갈색 강아지 이미지와 유사한 벡터 생성
    """
    inputs = processor(text=[text], return_tensors="pt", padding=True, truncation=True)

    with torch.no_grad():
        text_features = model.get_text_features(**inputs)
        text_features = text_features / text_features.norm(dim=-1, keepdim=True)

    return text_features[0].tolist()
