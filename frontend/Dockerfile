# ===========================================
# Frontend 멀티 스테이지 빌드
# Stage 1: Node.js로 빌드 → Stage 2: nginx로 정적 파일 서빙
# ===========================================

# --- Build Stage ---
FROM node:18-alpine AS build

WORKDIR /app

# 의존성 먼저 설치 (캐시 활용)
COPY package.json package-lock.json ./
RUN npm ci

# 소스 복사 후 빌드
COPY . .
RUN npm run build

# --- Production Stage ---
FROM nginx:alpine

# Vite 빌드 결과물을 nginx html 디렉토리로 복사
COPY --from=build /app/dist /usr/share/nginx/html

# SPA 라우팅 지원: 모든 경로를 index.html로 리다이렉트
COPY --from=build /app/dist /usr/share/nginx/html
RUN echo 'server { \
    listen 3000; \
    root /usr/share/nginx/html; \
    index index.html; \
    location / { \
        try_files $uri $uri/ /index.html; \
    } \
}' > /etc/nginx/conf.d/default.conf

EXPOSE 3000

CMD ["nginx", "-g", "daemon off;"]
