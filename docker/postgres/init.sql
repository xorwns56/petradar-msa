-- ===========================================
-- PetRadar MSA - PostgreSQL 초기화 스크립트
-- 컨테이너 최초 실행 시 자동으로 실행됨
-- ===========================================

-- 서비스별 독립 데이터베이스 생성
CREATE DATABASE user_db;
CREATE DATABASE report_db;
CREATE DATABASE search_db;

-- search_db에 pgvector 확장 활성화 (이미지/텍스트 유사도 검색)
\c search_db
CREATE EXTENSION IF NOT EXISTS vector;
