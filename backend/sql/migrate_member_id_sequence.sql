-- ============================================
-- member_id 중복 해결 및 공유 시퀀스 설정 마이그레이션
-- ============================================
-- 실행 전 주의사항:
-- 1. 반드시 데이터베이스 백업을 먼저 수행하세요!
-- 2. 서비스를 중단한 상태에서 실행하세요 (데이터 정합성)
-- 3. 실행 후 애플리케이션을 재시작하세요
-- ============================================

-- 1단계: 현재 상태 확인
SELECT '=== 현재 member 테이블 ===' as info;
SELECT COUNT(*) as count, MIN(member_id) as min_id, MAX(member_id) as max_id FROM member;

SELECT '=== 현재 oauth2_member 테이블 ===' as info;
SELECT COUNT(*) as count, MIN(member_id) as min_id, MAX(member_id) as max_id FROM oauth2_member;

SELECT '=== 중복 확인 ===' as info;
SELECT m.member_id, 'BOTH' as status
FROM member m
INNER JOIN oauth2_member o ON m.member_id = o.member_id;

-- 2단계: oauth2_member의 member_id를 재할당
-- member 테이블의 최대값 이후부터 시작
SET @max_member_id := (SELECT COALESCE(MAX(member_id), 0) FROM member);

SELECT CONCAT('member 테이블 최대 ID: ', @max_member_id) as info;

-- 임시 테이블로 매핑 생성
CREATE TEMPORARY TABLE IF NOT EXISTS oauth2_member_id_mapping (
    old_member_id BIGINT,
    new_member_id BIGINT,
    PRIMARY KEY (old_member_id)
);

-- 매핑 데이터 생성
INSERT INTO oauth2_member_id_mapping (old_member_id, new_member_id)
SELECT
    member_id as old_member_id,
    @max_member_id + ROW_NUMBER() OVER (ORDER BY member_id) as new_member_id
FROM oauth2_member
ORDER BY member_id;

SELECT '=== 매핑 테이블 ===' as info;
SELECT * FROM oauth2_member_id_mapping;

-- 3단계: 외래키 제약 확인 (있다면 비활성화)
SET FOREIGN_KEY_CHECKS = 0;

-- 4단계: 연관 테이블 업데이트 (member_id를 외래키로 가진 테이블만)
-- Post 테이블 (FK: member_id)
UPDATE post p
INNER JOIN oauth2_member_id_mapping m ON p.member_id = m.old_member_id
SET p.member_id = m.new_member_id;

-- PollVote 테이블 (FK: member_id)
UPDATE poll_vote pv
INNER JOIN oauth2_member_id_mapping m ON pv.member_id = m.old_member_id
SET pv.member_id = m.new_member_id;

-- History 테이블 (FK: member_id)
UPDATE history h
INNER JOIN oauth2_member_id_mapping m ON h.member_id = m.old_member_id
SET h.member_id = m.new_member_id;

-- 5단계: oauth2_member 테이블 업데이트
UPDATE oauth2_member o
INNER JOIN oauth2_member_id_mapping m ON o.member_id = m.old_member_id
SET o.member_id = m.new_member_id;

-- 6단계: 외래키 제약 다시 활성화
SET FOREIGN_KEY_CHECKS = 1;

-- 7단계: 시퀀스 테이블 생성
CREATE TABLE IF NOT EXISTS member_id_sequence (
    sequence_name VARCHAR(255) NOT NULL PRIMARY KEY,
    next_val BIGINT NOT NULL
);

-- 8단계: 초기값 설정 (재할당 후 최대값 + 1)
INSERT INTO member_id_sequence (sequence_name, next_val)
SELECT 'member_id_seq', COALESCE(MAX(max_id), 0) + 1
FROM (
    SELECT COALESCE(MAX(member_id), 0) as max_id FROM member
    UNION ALL
    SELECT COALESCE(MAX(member_id), 0) as max_id FROM oauth2_member
) as max_values
ON DUPLICATE KEY UPDATE next_val = VALUES(next_val);

-- 9단계: 최종 상태 확인
SELECT '=== 마이그레이션 후 member 테이블 ===' as info;
SELECT COUNT(*) as count, MIN(member_id) as min_id, MAX(member_id) as max_id FROM member;

SELECT '=== 마이그레이션 후 oauth2_member 테이블 ===' as info;
SELECT COUNT(*) as count, MIN(member_id) as min_id, MAX(member_id) as max_id FROM oauth2_member;

SELECT '=== 중복 확인 (0건이어야 함) ===' as info;
SELECT m.member_id, 'BOTH' as status
FROM member m
INNER JOIN oauth2_member o ON m.member_id = o.member_id;

SELECT '=== 시퀀스 초기값 ===' as info;
SELECT * FROM member_id_sequence;

-- 임시 테이블 삭제
DROP TEMPORARY TABLE IF EXISTS oauth2_member_id_mapping;

SELECT '=== 마이그레이션 완료! ===' as info;