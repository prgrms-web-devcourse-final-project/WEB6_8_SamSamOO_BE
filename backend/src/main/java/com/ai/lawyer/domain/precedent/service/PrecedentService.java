package com.ai.lawyer.domain.precedent.service;

import com.ai.lawyer.domain.precedent.dto.PrecedentSearchRequestDto;
import com.ai.lawyer.domain.precedent.dto.PrecedentSummaryListDto;
import com.ai.lawyer.domain.precedent.entity.Precedent;
import com.ai.lawyer.domain.precedent.repository.PrecedentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class PrecedentService {

    private final PrecedentRepository precedentRepository;
    private final EntityManager entityManager;
    private final WebClient webClient = WebClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 상수 정의
    private static final String BASE_URL = "http://www.law.go.kr/DRF";
    private static final String OC = "noheechul";
    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final int API_CALL_DELAY = 100; // milliseconds
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 주어진 id로 Precedent 조회
     *
     * @param id Precedent PK
     * @return Precedent 엔티티
     * @throws NoSuchElementException id에 해당하는 Precedent가 없으면 예외 발생
     */
    public Precedent getPrecedentById(Long id) {
        return precedentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Precedent not found for id: " + id));
    }

    /**
     * 키워드로 판례 검색 (판시사항, 판결요지, 판례내용에서 검색)
     *
     * @param requestDto 검색 조건 DTO (키워드, 페이징 정보 포함)
     * @return 검색된 판례 요약 정보 목록 (페이징 처리됨)
     */
    public Page<PrecedentSummaryListDto> searchByKeyword(PrecedentSearchRequestDto requestDto) {
        return precedentRepository.searchPrecedentsByKeyword(requestDto);
    }

    public Page<PrecedentSummaryListDto> searchByKeywordV2(PrecedentSearchRequestDto requestDto) {
        String keyword = null;
        if (StringUtils.hasText(requestDto.getKeyword())) {
            keyword = requestDto.getKeyword().trim() + "*";
        }

        int offset = requestDto.getPageNumber() * requestDto.getPageSize();

        List<Object[]> results = precedentRepository.searchByKeywordNative(
                keyword,
                requestDto.getSentencingDateStart(),
                requestDto.getSentencingDateEnd(),
                offset,
                requestDto.getPageSize()
        );

        Long total = precedentRepository.countByKeywordNative(
                keyword,
                requestDto.getSentencingDateStart(),
                requestDto.getSentencingDateEnd()
        );

        List<PrecedentSummaryListDto> content = results.stream()
                .map(this::mapToDto)
                .toList();

        return new PageImpl<>(content, requestDto.toPageable(), total);
    }

    private PrecedentSummaryListDto mapToDto(Object[] row) {
        Long id = ((Number) row[0]).longValue();
        String caseName = (String) row[1];
        String caseNumber = (String) row[2];
        java.sql.Date sqlDate = (java.sql.Date) row[3];
        String contents = (String) row[4];

        if (contents == null) contents = "";

        return new PrecedentSummaryListDto(
                id,
                caseName,
                caseNumber,
                sqlDate != null ? sqlDate.toLocalDate() : null,
                extractOrderPart(contents)
        );
    }

    private String extractOrderPart(String contents) {
        if (contents == null || contents.isBlank()) return "";

        int start = contents.indexOf("【주    문】");
        if (start == -1) return contents;

        int end = contents.indexOf("【이    유】", start);
        if (end == -1) end = contents.length();

        return contents.substring(start, end).trim();
    }

    /**
     * 특정 키워드로 법령 API에서 판례 일련번호 리스트 조회
     *
     * @param query 검색 키워드
     * @return 판례일련번호 리스트 (결과가 없으면 빈 리스트)
     * @throws RuntimeException API 호출 또는 JSON 파싱 실패 시 예외 발생
     */
    public List<String> getPrecedentNumbers(String query) {
        try {
            List<String> precedentNumbers = new ArrayList<>();
            int page = 1;
            int totalCnt;

            do {
                String url = buildSearchUrl(query, page);
                String json = webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
                JsonNode root = objectMapper.readTree(json);
                JsonNode precSearch = root.path("PrecSearch");

                totalCnt = precSearch.path("totalCnt").asInt(0);
                if (totalCnt == 0) {
                    log.info("검색 결과가 없습니다. 키워드: {}", query);
                    return Collections.emptyList();
                }

                extractPrecedentNumbers(precSearch.path("prec"), precedentNumbers);
                page++;

            } while ((page - 1) * DEFAULT_PAGE_SIZE < totalCnt);

            log.info("판례 일련번호 {}개 조회 완료. 키워드: {}", precedentNumbers.size(), query);
            return precedentNumbers;

        } catch (Exception e) {
            log.error("판례 일련번호 조회 실패. 키워드: {}", query, e);
            throw new RuntimeException("판례 일련번호 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 판례 일련번호 리스트로 법령 API에서 상세 판례 정보 조회
     *
     * @param precedentIds 판례일련번호 리스트
     * @return 조회된 Precedent 객체 리스트 (실패한 항목은 제외)
     */
    public List<Precedent> getPrecedentDetails(List<String> precedentIds) {
        List<Precedent> precedents = new ArrayList<>();

        for (String precedentId : precedentIds) {
            try {
                String url = buildDetailUrl(precedentId);
                String json = webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                Precedent precedent = parseJsonToPrecedent(json);
                if (precedent != null) {
                    precedents.add(precedent);
                }

                // API 호출 간격 조절
                Thread.sleep(API_CALL_DELAY);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("API 호출 중 인터럽트 발생: {}", precedentId, e);
                break;
            } catch (Exception e) {
                log.warn("판례 상세 조회 실패: {}", precedentId, e);
                // 개별 실패는 무시하고 계속 진행
            }
        }

        log.info("판례 상세 정보 {}개 조회 완료", precedents.size());
        return precedents;
    }

    /**
     * 판례 리스트를 데이터베이스에 일괄 저장
     *
     * @param precedents 저장할 Precedent 객체 리스트
     * @return 저장된 Precedent 객체 리스트
     * @throws RuntimeException 저장 중 오류 발생 시 예외 발생
     */
    @Transactional
    public List<Precedent> savePrecedents(List<Precedent> precedents) {
        try {
            precedentRepository.saveAll(precedents);
            precedentRepository.flush();
            entityManager.clear();

            log.info("판례 {}개 저장 완료", precedents.size());
            return precedents;

        } catch (Exception e) {
            log.error("판례 저장 실패", e);
            throw new RuntimeException("판례 저장 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 키워드 검색부터 판례 저장까지 원스톱 처리
     * 1. 키워드로 판례일련번호 리스트 조회
     * 2. 일련번호로 상세 정보 조회
     * 3. 상세 정보를 데이터베이스에 저장
     *
     * @param query 검색 키워드
     * @return 저장된 판례 건수
     * @throws RuntimeException 처리 중 오류 발생 시 예외 발생
     */
    public int searchAndSaveAll(String query) {
        try {
            log.info("판례 검색 및 저장 시작. 키워드: {}", query);

            List<String> precedentIds = getPrecedentNumbers(query);
            if (precedentIds.isEmpty()) {
                return 0;
            }

            List<Precedent> precedents = getPrecedentDetails(precedentIds);
            List<Precedent> savedPrecedents = savePrecedents(precedents);

            log.info("판례 검색 및 저장 완료. 키워드: {}, 저장된 건수: {}", query, savedPrecedents.size());
            return savedPrecedents.size();

        } catch (Exception e) {
            log.error("판례 검색 및 저장 실패. 키워드: {}", query, e);
            throw new RuntimeException("판례 검색 및 저장 중 오류가 발생했습니다.", e);
        }
    }

    // ==================== Private Helper Methods ====================

    /**
     * 판례 검색 API URL 생성
     *
     * @param query 검색 키워드
     * @param page 페이지 번호
     * @return 생성된 API URL
     */
    private String buildSearchUrl(String query, int page) {
        return UriComponentsBuilder.fromHttpUrl(BASE_URL + "/lawSearch.do")
                .queryParam("OC", OC)
                .queryParam("target", "prec")
                .queryParam("type", "JSON")
                .queryParam("display", DEFAULT_PAGE_SIZE)
                .queryParam("page", page)
                .queryParam("query", query)
                .build()
                .toUriString();
    }

    /**
     * 판례 상세 조회 API URL 생성
     *
     * @param precedentId 판례 일련번호
     * @return 생성된 API URL
     */
    private String buildDetailUrl(String precedentId) {
        return UriComponentsBuilder.fromHttpUrl(BASE_URL + "/lawService.do")
                .queryParam("OC", OC)
                .queryParam("target", "prec")
                .queryParam("ID", precedentId)
                .queryParam("type", "JSON")
                .build()
                .toUriString();
    }

    /**
     * JSON 노드에서 판례일련번호 추출하여 리스트에 추가
     *
     * @param precArray JSON 배열 또는 객체 노드
     * @param precedentNumbers 추출된 일련번호를 저장할 리스트
     */
    private void extractPrecedentNumbers(JsonNode precArray, List<String> precedentNumbers) {
        if (precArray.isArray()) {
            precArray.forEach(item -> {
                String number = item.path("판례일련번호").asText();
                if (StringUtils.hasText(number)) {
                    precedentNumbers.add(number);
                }
            });
        } else if (precArray.isObject()) {
            String number = precArray.path("판례일련번호").asText();
            if (StringUtils.hasText(number)) {
                precedentNumbers.add(number);
            }
        }
    }

    /**
     * JSON 문자열을 Precedent 엔티티로 변환
     *
     * @param json API 응답 JSON 문자열
     * @return 변환된 Precedent 객체 (변환 실패 시 null)
     * @throws Exception JSON 파싱 오류 시 예외 발생
     */
    private Precedent parseJsonToPrecedent(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode precService = root.path("PrecService");

        if (precService.isMissingNode()) {
            return null;
        }

        Precedent precedent = new Precedent();

        // 기본 정보 매핑
        precedent.setPrecedentNumber(precService.path("판례정보일련번호").asText(""));
        precedent.setCaseName(precService.path("사건명").asText(""));
        precedent.setCaseNumber(precService.path("사건번호").asText(""));
        precedent.setSentence(precService.path("선고").asText(""));
        precedent.setCourtName(precService.path("법원명").asText(""));
        precedent.setCourtTypeCode(precService.path("법원종류코드").asText(""));
        precedent.setCaseTypeName(precService.path("사건종류명").asText(""));
        precedent.setCaseTypeCode(precService.path("사건종류코드").asText(""));
        precedent.setTypeOfJudgment(precService.path("판결유형").asText(""));
        precedent.setReferencePrecedent(precService.path("참조판례").asText(""));
        precedent.setNotice(precService.path("판시사항").asText(""));
        precedent.setSummaryOfTheJudgment(precService.path("판결요지").asText(""));
        precedent.setReferenceArticle(precService.path("참조조문").asText(""));
        precedent.setPrecedentContent(precService.path("판례내용").asText(""));

        // 날짜 파싱
        parseSentencingDate(precService.path("선고일자").asText(""), precedent);

        return precedent;
    }

    /**
     * 문자열 형태의 날짜를 LocalDate로 변환하여 Precedent 객체에 설정
     *
     * @param dateStr 날짜 문자열 (yyyyMMdd 형식)
     * @param precedent 날짜를 설정할 Precedent 객체
     */
    private void parseSentencingDate(String dateStr, Precedent precedent) {
        if (StringUtils.hasText(dateStr) && dateStr.length() == 8) {
            try {
                precedent.setSentencingDate(LocalDate.parse(dateStr, DATE_FORMATTER));
            } catch (DateTimeParseException e) {
                log.warn("날짜 파싱 실패: {}", dateStr);
            }
        }
    }
}
