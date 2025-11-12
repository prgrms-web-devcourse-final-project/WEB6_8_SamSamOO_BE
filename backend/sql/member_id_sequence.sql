-- ============================================
-- 공유 member_id 시퀀스 테이블 생성
-- ============================================
-- 주의: 기존 배포 환경에 데이터가 있다면
-- backend/sql/migrate_member_id_sequence.sql을 먼저 실행해야 합니다!
-- ============================================

CREATE TABLE IF NOT EXISTS member_id_sequence (
    sequence_name VARCHAR(255) NOT NULL PRIMARY KEY,
    next_val BIGINT NOT NULL
);

-- 초기값 설정: 신규 설치 시 1부터 시작
-- 기존 환경은 마이그레이션 스크립트로 처리
INSERT INTO member_id_sequence (sequence_name, next_val)
VALUES ('member_id_seq', 1)
ON DUPLICATE KEY UPDATE sequence_name = sequence_name;
