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
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class LawWordService {

    private final LawWordRepository lawWordRepository;
    private final WebClient webClient = WebClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String API_BASE_URL = "https://www.law.go.kr/DRF/lawService.do";
    private static final String API_OC = "noheechul";

    // 우리말샘 API 설정
    private static final String KOREAN_DICT_API_BASE_URL = "https://opendict.korean.go.kr/api/search";
    private static final String API_KEY = "2A4D1A844C8BAB682B38E5F192D3D42A";

    public String findDefinition(String word) {
        // 1) DB에서 먼저 조회
        return lawWordRepository.findByWord(word)
                .map(LawWord::getDefinition)
                .orElseGet(() -> fetchAndSaveDefinition(word));
    }

    public String findDefinitionV2(String word) {
        // 1) DB에서 먼저 조회
        return lawWordRepository.findByWord(word)
                .map(LawWord::getDefinition)
                .orElseGet(() -> fetchAndSaveDefinitionV2(word));
    }

    private String fetchAndSaveDefinition(String word) {
        try {
            String url = buildApiUrl(word);
            // WebClient 호출 (동기 방식)
            String json = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

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

    private String fetchAndSaveDefinitionV2(String word) {
        try {
            String url = buildApiUrlV2(word);

            // WebClient 호출 (동기 방식)
            String json = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            String combinedDefinitions = extractTop3DefinitionsFromJson(json, word);
            saveDefinition(word, combinedDefinitions);

            return combinedDefinitions;

        } catch (HttpClientErrorException e) {
            log.error("한국어사전 API 호출 중 클라이언트 오류 발생: {}", e.getMessage());
            throw new RuntimeException("한국어사전 API 호출 중 오류가 발생했습니다.");
        } catch (JsonProcessingException e) {
            log.error("JSON 파싱 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("한국어사전 API 응답 처리 중 파싱 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("정의 조회 실패: ", e);
            throw new RuntimeException("한국어사전 정의 조회 중 알 수 없는 오류가 발생했습니다.");
        }
    }

    private String buildApiUrl(String word) {
        return API_BASE_URL + "?OC=" + API_OC + "&target=lstrm&type=JSON&query=" + word;
    }

    private String buildApiUrlV2(String word) {
        return UriComponentsBuilder.fromHttpUrl(KOREAN_DICT_API_BASE_URL)
                .queryParam("key", API_KEY)
                .queryParam("req_type", "json")
                .queryParam("part", "word")
                .queryParam("q", word)
                .queryParam("sort", "dict")
                .queryParam("start", "1")
                .queryParam("num", "10")
//                .queryParam("advanced", "y")
//                .queryParam("type4", "all")
//                .queryParam("cat", "23")
                .build()
                .toUriString();
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

    private String extractTop3DefinitionsFromJson(String json, String requestedWord) throws JsonProcessingException {
        JsonNode rootNode = objectMapper.readTree(json);
        JsonNode channelNode = rootNode.path("channel");

        // 1. total이 0이면 '찾을수 없는 단어입니다' 리턴
        int total = channelNode.path("total").asInt(0);
        if (total == 0) {
            return "찾을수 없는 단어입니다";
        }

        JsonNode itemsNode = channelNode.path("item");
        if (!itemsNode.isArray() || itemsNode.size() == 0) {
            return "찾을수 없는 단어입니다";
        }

        // 2. 클라이언트가 요청한 단어와 정확히 일치하는 item만 필터링
        List<JsonNode> matchingItems = new ArrayList<>();
        String normalizedRequestedWord = normalize(requestedWord);

        for (JsonNode item : itemsNode) {
            String itemWord = item.path("word").asText();
            String normalizedItemWord = normalize(itemWord);
            if (normalizedRequestedWord.equals(normalizedItemWord)) {
                matchingItems.add(item);
            }
        }

        if (matchingItems.isEmpty()) {
            return "찾을수 없는 단어입니다";
        }

        // 3. 법률 카테고리 우선순위 적용
        List<String> definitions = extractDefinitionsWithPriority(matchingItems);

        if (definitions.isEmpty()) {
            return "찾을수 없는 단어입니다";
        }

        // 같은 word면 개수 제한 없이 모든 definition 반환
        return String.join("\n", definitions);
    }

    private List<String> extractDefinitionsWithPriority(List<JsonNode> matchingItems) {
        List<String> legalDefinitions = new ArrayList<>();  // 법률 카테고리
        List<String> allDefinitions = new ArrayList<>();     // 모든 카테고리

        for (JsonNode item : matchingItems) {
            JsonNode senseNode = item.path("sense");

            if (senseNode.isArray()) {
                for (JsonNode sense : senseNode) {
                    String definition = sense.path("definition").asText();
                    String cat = sense.path("cat").asText("");

                    if (definition != null && !definition.trim().isEmpty()) {
                        String cleanDefinition = definition.trim();
                        allDefinitions.add(cleanDefinition);

                        // cat이 "법률"인 경우 별도로 수집
                        if ("법률".equals(cat)) {
                            legalDefinitions.add(cleanDefinition);
                        }
                    }
                }
            }
        }

        // 법률 카테고리가 있으면 법률만, 없으면 모든 카테고리 반환
        return legalDefinitions.isEmpty() ? allDefinitions : legalDefinitions;
    }

    private void saveDefinition(String word, String definition) {
        LawWord entity = LawWord.builder()
                .word(word)
                .definition(definition)
                .build();
        lawWordRepository.save(entity);
    }

    // 유틸: 단어 정규화 함수
    private String normalize(String input) {
        return input.replaceAll("[\\s\\^]", "");
    }
}
