FROM python:3.11-slim

RUN groupadd -g 1001 appgroup && useradd -u 1001 -g appgroup --no-create-home appuser

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# CLIP 모델을 빌드 시점에 미리 다운로드
# 컨테이너 시작 시마다 HuggingFace에서 다운로드하지 않도록 캐싱
RUN python -c "from transformers import CLIPModel, CLIPProcessor; \
    CLIPModel.from_pretrained('openai/clip-vit-base-patch32'); \
    CLIPProcessor.from_pretrained('openai/clip-vit-base-patch32')"

COPY . .

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
