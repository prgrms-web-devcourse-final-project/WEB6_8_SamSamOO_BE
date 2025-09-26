package com.ai.lawyer.domain.lawWord.repository;

import com.ai.lawyer.domain.lawWord.entity.LawWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LawWordRepository extends JpaRepository<LawWord, Long> {
    Optional<LawWord> findByWord(String word);
}
