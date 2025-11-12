package com.ai.lawyer.domain.member.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Member와 OAuth2Member가 공유하는 member_id 시퀀스 테이블
 * JPA의 @TableGenerator가 자동으로 관리하는 테이블을 엔티티로 명시
 */
@Entity
@Table(name = "member_id_sequence")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class MemberIdSequence {

    /**
     * 시퀀스 이름 (Primary Key)
     * Member와 OAuth2Member는 'member_id_seq' 값을 공유
     */
    @Id
    @Column(name = "sequence_name", nullable = false)
    private String sequenceName;

    /**
     * 다음에 할당될 member_id 값
     * JPA의 @TableGenerator가 자동으로 증가시킴
     */
    @Column(name = "next_val", nullable = false)
    private Long nextVal;
}