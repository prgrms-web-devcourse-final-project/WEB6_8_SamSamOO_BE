package com.ai.lawyer.domain.member.repositories;

import com.ai.lawyer.domain.member.entity.Member;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import jakarta.persistence.EntityManager;
import java.io.Serializable;

/**
 * MemberRepository의 커스텀 팩토리 빈
 * findById 호출을 가로채서 AuthUtil을 통해 처리하도록 합니다.
 */
public class MemberRepositoryFactoryBean<R extends JpaRepository<T, I>, T, I extends Serializable>
        extends JpaRepositoryFactoryBean<R, T, I> {

    public MemberRepositoryFactoryBean(Class<? extends R> repositoryInterface) {
        super(repositoryInterface);
    }

    @NotNull
    @Override
    protected RepositoryFactorySupport createRepositoryFactory(@NotNull EntityManager entityManager) {
        return new MemberRepositoryFactory(entityManager);
    }

    private static class MemberRepositoryFactory extends JpaRepositoryFactory {

        public MemberRepositoryFactory(EntityManager entityManager) {
            super(entityManager);
        }

        @NotNull
        @Override
        protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
            // Member 엔티티인 경우 커스텀 베이스 클래스 사용
            if (Member.class.isAssignableFrom(metadata.getDomainType())) {
                return SmartMemberRepositoryImpl.class;
            }
            return super.getRepositoryBaseClass(metadata);
        }
    }
}
