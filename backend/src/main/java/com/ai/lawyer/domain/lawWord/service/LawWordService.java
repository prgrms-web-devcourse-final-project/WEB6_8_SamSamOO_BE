package com.ai.lawyer.domain.lawWord.service;

import com.ai.lawyer.domain.lawWord.entity.LawWord;
import com.ai.lawyer.domain.lawWord.repository.LawWordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@AllArgsConstructor
public class LawWordService {

    private final LawWordRepository lawWordRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String findDefinition(String word) {
        // 1) DB 확인
        return lawWordRepository.findByWord(word)
                .map(LawWord::getDefinition)
                .orElseGet(() -> {
                    try {
                        String url = "https://www.law.go.kr/DRF/lawService.do?OC=noheechul"
                                + "&target=lstrm&type=JSON&query=" + word;
                        String json = restTemplate.getForObject(url, String.class);
                        JsonNode rootNode = objectMapper.readTree(json);
                        // 오류 응답 처리
                        if (rootNode.has("Law")) {
                            return rootNode.get("Law").asText();
                        }
                        JsonNode serviceNode = rootNode.path("LsTrmService");
                        JsonNode defNode = serviceNode.path("법령용어정의");
                        String definition;
                        if (defNode.isArray() && defNode.size() > 0) {
//                            definition = defNode.get(0).asText().split("\\.",2)[0].trim();
                            definition = defNode.get(0).asText().trim();
                        } else {
//                            definition = defNode.asText().split("\\.",2)[0].trim();
                            definition = defNode.asText().trim();
                        }
                        LawWord entity = LawWord.builder()
                                .word(word)
                                .definition(definition)
                                .build();
                        lawWordRepository.save(entity);
                        return definition;
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to fetch definition", e);
                    }
                });
    }
}
