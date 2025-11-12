package com.ai.lawyer.domain.law.service;


import com.ai.lawyer.domain.law.dto.LawSearchRequestDto;
import com.ai.lawyer.domain.law.dto.LawsDto;
import com.ai.lawyer.domain.law.entity.*;
import com.ai.lawyer.domain.law.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@AllArgsConstructor
@Slf4j
public class LawService {

    private final LawRepository lawRepository;
    private final JangRepository jangRepository;
    private final JoRepository joRepository;
    private final HangRepository hangRepository;
    private final HoRepository hoRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient = WebClient.builder().build();

    // 상수 정의
    private static final String BASE_URL = "http://www.law.go.kr/DRF";
    private static final String OC = "noheechul";
    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 조건에 맞는 법령 목록 검색
     *
     * @param searchRequest 법령 검색 조건 DTO
     * @return 검색된 법령 목록 (페이징 처리됨)
     */
    public Page<LawsDto> searchLaws(LawSearchRequestDto searchRequest) {
        return lawRepository.searchLaws(searchRequest);
    }

    /**
     * 법령 ID로 법령과 모든 하위 엔티티를 조회
     *
     * @param lawId 법령 ID
     * @return Law 엔티티 (Jang, Jo, Hang, Ho 모두 포함)
     * @throws EntityNotFoundException 해당 ID의 법령이 존재하지 않을 때 예외 발생
     */
    public Law getLawWithAllChildren(Long lawId) {
        Law law = lawRepository.findWithJangById(lawId)
                .orElseThrow(() -> new EntityNotFoundException("법령이 없습니다. 법령 id = " + lawId));

        List<Jang> jangs = jangRepository.findByLawId(lawId);
        law.setJangList(jangs);

        loadChildEntities(jangs);

        log.info("법령 상세 정보 조회 완료. 법령 ID: {}", lawId);
        return law;
    }

    /**
     * Open API를 통해 법령 데이터를 검색하고 데이터베이스에 저장
     *
     * @param query 검색 키워드
     * @throws RuntimeException API 호출 또는 저장 중 오류 발생 시 예외 발생
     */
    @Transactional
    public void saveLaw(String query) {
        try {
            log.info("법령 검색 및 저장 시작. 키워드: {}", query);

            String lawJson = getLawSearchResponse(query);
            List<String> lawIdList = extractLawIds(lawJson);

            if (lawIdList.isEmpty()) {
                log.info("검색 결과가 없습니다. 키워드: {}", query);
                return;
            }

            for (String lawId : lawIdList) {
                try {
                    String lawDetailJson = getLawDetailResponse(lawId);
                    saveLawToDatabase(lawDetailJson);
                } catch (Exception e) {
                    log.error("법령 저장 실패. 법령 ID: {}", lawId, e);
                    // 개별 실패는 무시하고 계속 진행
                }
            }

            log.info("법령 저장 완료. 키워드: {}, 처리된 법령 수: {}", query, lawIdList.size());

        } catch (Exception e) {
            log.error("법령 검색 및 저장 실패. 키워드: {}", query, e);
            throw new RuntimeException("법령 검색 및 저장 중 오류가 발생했습니다.", e);
        }
    }

    // ==================== Private Helper Methods ====================

    /**
     * 하위 엔티티들을 재귀적으로 로드
     *
     * @param jangs 장 엔티티 리스트
     */
    private void loadChildEntities(List<Jang> jangs) {
        for (Jang jang : jangs) {
            List<Jo> jos = joRepository.findByJangId(jang.getId());
            jang.setJoList(jos);

            for (Jo jo : jos) {
                List<Hang> hangs = hangRepository.findByJoId(jo.getId());
                jo.setHangList(hangs);
                // Ho는 lazy loading으로 처리되거나 필요시 추가 로드
            }
        }
    }

    /**
     * Open API에서 법령 검색 결과 조회
     *
     * @param query 검색 키워드
     * @return API 응답 JSON 문자열
     */
    private String getLawSearchResponse(String query) {
        String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/lawSearch.do")
                .queryParam("OC", OC)
                .queryParam("target", "law")
                .queryParam("type", "JSON")
                .queryParam("query", query)
                .queryParam("page", 1)
                .queryParam("display", DEFAULT_PAGE_SIZE)
                .build()
                .toUriString();

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    /**
     * Open API에서 법령 상세 정보 조회
     *
     * @param lawId 법령 ID
     * @return API 응답 JSON 문자열
     */
    private String getLawDetailResponse(String lawId) {
        String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/lawService.do")
                .queryParam("OC", OC)
                .queryParam("target", "law")
                .queryParam("ID", lawId)
                .queryParam("type", "JSON")
                .build()
                .toUriString();

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    /**
     * 법령 검색 결과 JSON에서 법령 ID 리스트 추출
     *
     * @param json API 응답 JSON 문자열
     * @return 법령 ID 리스트
     * @throws Exception JSON 파싱 오류 시 예외 발생
     */
    private List<String> extractLawIds(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode lawNode = root.path("LawSearch").path("law");

        if (lawNode.isArray()) {
            return StreamSupport.stream(lawNode.spliterator(), false)
                    .map(item -> item.path("법령ID").asText(null))
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());
        } else if (lawNode.isObject()) {
            String lawId = lawNode.path("법령ID").asText(null);
            return StringUtils.hasText(lawId) ? List.of(lawId) : List.of();
        }

        return new ArrayList<>();
    }

    /**
     * 법령 상세 정보 JSON을 파싱하여 데이터베이스에 저장
     *
     * @param json API 응답 JSON 문자열
     * @throws IOException JSON 파싱 오류 시 예외 발생
     */
    private void saveLawToDatabase(String json) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        JsonNode basic = root.path("법령").path("기본정보");

        // 1. Law 엔티티 저장
        Law law = createAndSaveLaw(basic);

        // 2. 조문 단위 처리
        JsonNode articles = root.path("법령").path("조문").path("조문단위");
        if (articles.isArray()) {
            processArticles(articles, law);
        }
    }

    /**
     * Law 엔티티 생성 및 저장
     *
     * @param basic 법령 기본정보 JSON 노드
     * @return 저장된 Law 엔티티
     */
    private Law createAndSaveLaw(JsonNode basic) {
        Law law = new Law();
        law.setLawName(basic.path("법령명_한글").asText());
        law.setLawField(basic.path("법종구분").path("content").asText());
        law.setMinistry(basic.path("소관부처").path("content").asText());
        law.setPromulgationNumber(basic.path("공포번호").asText());

        // 날짜 파싱
        parseAndSetDate(basic.path("공포일자").asText(), law::setPromulgationDate);
        parseAndSetDate(basic.path("시행일자").asText(), law::setEnforcementDate);

        return lawRepository.save(law);
    }

    /**
     * 날짜 문자열을 파싱하여 LocalDate로 변환
     *
     * @param dateStr 날짜 문자열 (yyyyMMdd 형식)
     * @param setter 날짜 설정 함수
     */
    private void parseAndSetDate(String dateStr, java.util.function.Consumer<LocalDate> setter) {
        if (StringUtils.hasText(dateStr)) {
            try {
                setter.accept(LocalDate.parse(dateStr, DATE_FORMATTER));
            } catch (DateTimeParseException e) {
                log.warn("날짜 파싱 실패: {}", dateStr);
            }
        }
    }

    /**
     * 조문 단위 배열 처리
     *
     * @param articles 조문 단위 JSON 배열
     * @param law 상위 Law 엔티티
     */
    private void processArticles(JsonNode articles, Law law) {
        Jang currentJang = createEmptyJang(law);

        for (JsonNode article : articles) {
            String key = article.path("조문키").asText();
            String content = article.path("조문내용").asText();

            if (key.endsWith("0")) {
                // 장(Jang) 처리
                currentJang = createAndSaveJang(content, law);
            } else {
                // 조(Jo) 처리
                if (currentJang.getContent() == null) {
                    currentJang = jangRepository.save(currentJang);
                }
                Jo jo = createAndSaveJo(content, currentJang);
                processHangEntities(article.path("항"), jo);
            }
        }
    }

    /**
     * 빈 Jang 엔티티 생성
     *
     * @param law 상위 Law 엔티티
     * @return 생성된 Jang 엔티티
     */
    private Jang createEmptyJang(Law law) {
        Jang jang = new Jang();
        jang.setContent(null);
        jang.setLaw(law);
        return jang;
    }

    /**
     * Jang 엔티티 생성 및 저장
     *
     * @param content 장 내용
     * @param law 상위 Law 엔티티
     * @return 저장된 Jang 엔티티
     */
    private Jang createAndSaveJang(String content, Law law) {
        Jang jang = new Jang();
        jang.setContent(content);
        jang.setLaw(law);
        return jangRepository.save(jang);
    }

    /**
     * Jo 엔티티 생성 및 저장
     *
     * @param content 조 내용
     * @param jang 상위 Jang 엔티티
     * @return 저장된 Jo 엔티티
     */
    private Jo createAndSaveJo(String content, Jang jang) {
        Jo jo = new Jo();
        jo.setContent(content);
        jo.setJang(jang);
        return joRepository.save(jo);
    }

    /**
     * 항(Hang) 엔티티들 처리
     *
     * @param paragraphs 항 JSON 노드
     * @param jo 상위 Jo 엔티티
     */
    private void processHangEntities(JsonNode paragraphs, Jo jo) {
        if (paragraphs.isMissingNode()) {
            return;
        }

        if (paragraphs.isArray()) {
            for (JsonNode paragraph : paragraphs) {
                Hang hang = createAndSaveHang(paragraph, jo);
                processHoEntities(paragraph.path("호"), hang);
            }
        } else if (paragraphs.isObject()) {
            Hang hang = createAndSaveHang(null, jo);
            processHoEntities(paragraphs.path("호"), hang);
        }
    }

    /**
     * Hang 엔티티 생성 및 저장
     *
     * @param paragraph 항 JSON 노드 (null 가능)
     * @param jo 상위 Jo 엔티티
     * @return 저장된 Hang 엔티티
     */
    private Hang createAndSaveHang(JsonNode paragraph, Jo jo) {
        Hang hang = new Hang();

        if (paragraph != null) {
            String hangContent = paragraph.path("항내용").isMissingNode()
                    ? null
                    : paragraph.path("항내용").asText();
            hang.setContent(hangContent);
        } else {
            hang.setContent(null);
        }

        hang.setJo(jo);
        return hangRepository.save(hang);
    }

    /**
     * 호(Ho) 엔티티들 처리
     *
     * @param itemsNode 호 JSON 노드
     * @param hang 상위 Hang 엔티티
     */
    private void processHoEntities(JsonNode itemsNode, Hang hang) {
        if (itemsNode.isArray()) {
            for (JsonNode item : itemsNode) {
                Ho ho = new Ho();
                ho.setContent(item.path("호내용").asText());
                ho.setHang(hang);
                hoRepository.save(ho);
            }
        }
    }


}

