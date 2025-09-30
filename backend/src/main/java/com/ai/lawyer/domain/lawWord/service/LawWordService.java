package com.ai.lawyer.domain.lawWord.service;

import com.ai.lawyer.domain.lawWord.entity.LawWord;
import com.ai.lawyer.domain.lawWord.repository.LawWordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@AllArgsConstructor
public class LawWordService {

    private final LawWordRepository lawWordRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String API_BASE_URL = "https://www.law.go.kr/DRF/lawService.do";
    private static final String API_OC = "noheechul";

    public String findDefinition(String word) {
        // 1) DB에서 먼저 조회
        return lawWordRepository.findByWord(word)
                .map(LawWord::getDefinition)
                .orElseGet(() -> fetchAndSaveDefinition(word));
    }

    private String fetchAndSaveDefinition(String word) {
        try {
            String url = buildApiUrl(word);
            String json = restTemplate.getForObject(url, String.class);

            String definition = extractDefinitionFromJson(json);
            saveDefinition(word, definition);

            return definition;

        } catch (HttpClientErrorException e) {
            log.error("API 호출 중 클라이언트 오류 발생: {}", e.getMessage());
            throw new RuntimeException("법령 API 호출 중 오류가 발생했습니다.");
        } catch (JsonProcessingException e) {
            log.error("JSON 파싱 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("법령 용어 API 응답 처리 중 파싱 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("정의 조회 실패: ", e);
            throw new RuntimeException("법령 정의 조회 중 알 수 없는 오류가 발생했습니다.");
        }
    }

    private String buildApiUrl(String word) {
        return API_BASE_URL + "?OC=" + API_OC + "&target=lstrm&type=JSON&query=" + word;
    }

    private String extractDefinitionFromJson(String json) throws JsonProcessingException {
        JsonNode rootNode = objectMapper.readTree(json);
        if (rootNode.has("Law")) {
            return rootNode.get("Law").asText();
        }
        JsonNode defNode = rootNode.path("LsTrmService").path("법령용어정의");
        if (defNode.isArray() && defNode.size() > 0) {
            return defNode.get(0).asText().trim();
//            return  defNode.get(0).asText().split("\.",2)[0].trim();
        } else {
            return defNode.asText().trim();
//            return defNode.asText().split("\.",2)[0].trim();
        }
    }

    private void saveDefinition(String word, String definition) {
        LawWord entity = LawWord.builder()
                .word(word)
                .definition(definition)
                .build();
        lawWordRepository.save(entity);
    }
}
