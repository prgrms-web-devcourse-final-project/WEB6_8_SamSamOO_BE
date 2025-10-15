package com.ai.lawyer.domain.member.repositories;

import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.global.util.AuthUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityManager;
import java.util.Optional;

/**
 * MemberRepository의 커스텀 베이스 구현체
 * findById 호출 시 AuthUtil을 통해 적절한 테이블에서 조회합니다.
 */
public class SmartMemberRepositoryImpl<T, ID> extends SimpleJpaRepository<T, ID> {

    private static final Logger log = LoggerFactory.getLogger(SmartMemberRepositoryImpl.class);

    private final JpaEntityInformation<T, ?> entityInformation;

    public SmartMemberRepositoryImpl(JpaEntityInformation<T, ?> entityInformation,
                                     EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityInformation = entityInformation;
    }

    @NotNull
    @Override
    public Optional<T> findById(@NotNull ID id) {
        // Member 엔티티이고 ID가 Long인 경우에만 AuthUtil 사용
        if (entityInformation.getJavaType().equals(Member.class) && id instanceof Long) {
            try {
                log.debug("SmartMemberRepositoryImpl.findById 호출: memberId={}", id);
                Member member = AuthUtil.getMemberOrThrow((Long) id);
                @SuppressWarnings("unchecked")
                T result = (T) member;
                return Optional.of(result);
            } catch (ResponseStatusException e) {
                log.debug("회원을 찾을 수 없음: memberId={}", id);
                return Optional.empty();
            }
        }

        // 다른 엔티티는 기본 동작 수행
        return super.findById(id);
    }
}
