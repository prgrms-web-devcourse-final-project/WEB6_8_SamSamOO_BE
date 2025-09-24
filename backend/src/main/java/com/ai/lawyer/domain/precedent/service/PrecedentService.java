package com.ai.lawyer.domain.precedent.service;

import com.ai.lawyer.domain.precedent.dto.PrecedentSearchRequestDto;
import com.ai.lawyer.domain.precedent.dto.PrecedentSummaryListDto;
import com.ai.lawyer.domain.precedent.entity.Precedent;
import com.ai.lawyer.domain.precedent.repository.PrecedentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@AllArgsConstructor
public class PrecedentService {

    private final PrecedentRepository precedentRepository;

    private final String BASE_URL = "http://www.law.go.kr/DRF";
    private final String OC = "noheechul"; // 실제 OC로 변경 필요
    private final ObjectMapper objectMapper = new ObjectMapper();


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
     * @param requestDto 검색 조건 DTO
     * @return 사건명, 사건번호, 선고일자 리스트
     */
    public Page<PrecedentSummaryListDto> searchByKeyword(PrecedentSearchRequestDto requestDto) {
        return precedentRepository.searchPrecedentsByKeyword(requestDto);
    }

    /**
     * 1. 특정 키워드로 판례 일련번호 리스트 가져오는 메서드
     *
     * @param query 검색 키워드
     * @return 판례일련번호 리스트
     * @throws Exception JSON 파싱 오류 시 예외 발생
     */
    public List<String> getPrecedentNumbers(String query) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        List<String> precedentNumbers = new ArrayList<>();

        int page = 1;
        int display = 100;  // 한 페이지당 최대 조회 건수
        int totalCnt;

        do {
            // 페이지별로 API 호출 URL 생성
            String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/lawSearch.do")
                    .queryParam("OC", OC)
                    .queryParam("target", "prec")
                    .queryParam("type", "JSON")
                    .queryParam("display", display)
                    .queryParam("page", page)
                    .queryParam("query", query)
                    .build()
                    .toUriString();

            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(json);
            JsonNode precSearch = root.path("PrecSearch");

            // totalCnt 추출
            totalCnt = precSearch.path("totalCnt").asInt(0);
            if (totalCnt == 0) {
                return Collections.emptyList();
            }

            JsonNode precArray = precSearch.path("prec");
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

            page++;
        } while ((page - 1) * display < totalCnt);

        return precedentNumbers;
    }

    /**
     * 2. 일련번호 리스트로 상세 조회하는 메서드
     *
     * @param precedentIds 판례일련번호 리스트
     * @return 조회된 Precedent 객체 리스트
     * @throws Exception API 호출 또는 JSON 파싱 오류 시 예외 발생
     */
    public List<Precedent> getPrecedentDetails(List<String> precedentIds) throws Exception {
        List<Precedent> precedents = new ArrayList<>();

        for (String precedentId : precedentIds) {
            try {
                // 단일 판례 상세 정보 조회
                String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/lawService.do")
                        .queryParam("OC", OC)
                        .queryParam("target", "prec")
                        .queryParam("ID", precedentId)
                        .queryParam("type", "JSON")
                        .build()
                        .toUriString();

                RestTemplate restTemplate = new RestTemplate();
                String json = restTemplate.getForObject(url, String.class);

                // JSON → Precedent 엔티티 변환
                Precedent precedent = parseJsonToPrecedent(json);
                if (precedent != null) {
                    precedents.add(precedent);
                }

                // API 호출 간격 조절
                Thread.sleep(100);

            } catch (Exception e) {
                System.err.println("Error fetching precedent " + precedentId + ": " + e.getMessage());
                // 개별 오류는 무시하고 계속 진행
            }
        }

        return precedents;
    }

    /**
     * 3. 상세 조회한 판례 저장하는 메서드
     *
     * @param precedents 저장할 Precedent 객체 리스트
     * @return 저장된 Precedent 객체 리스트
     */
    public List<Precedent> savePrecedents(List<Precedent> precedents) {
        List<Precedent> savedPrecedents = new ArrayList<>();

        for (Precedent precedent : precedents) {
            try {
                // 중복 확인 (판례일련번호 기준)
                if (!precedentRepository.existsByPrecedentNumber(precedent.getPrecedentNumber())) {
                    Precedent saved = precedentRepository.save(precedent);
                    savedPrecedents.add(saved);
                    System.out.println("Saved precedent: " + precedent.getPrecedentNumber());
                } else {
                    System.out.println("Already exists: " + precedent.getPrecedentNumber());
                }
            } catch (Exception e) {
                System.err.println("Error saving precedent " + precedent.getPrecedentNumber() + ": " + e.getMessage());
            }
        }

        return savedPrecedents;
    }

    /**
     * JSON을 Precedent 엔티티로 변환하는 헬퍼 메서드
     *
     * @param json API 응답 JSON 문자열
     * @return 변환된 Precedent 객체 (실패 시 null)
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

        // 선고일자 변환 (yyyyMMdd → LocalDate)
        String dateStr = precService.path("선고일자").asText("");
        if (StringUtils.hasText(dateStr) && dateStr.length() == 8) {
            try {
                precedent.setSentencingDate(LocalDate.parse(dateStr,
                        DateTimeFormatter.ofPattern("yyyyMMdd")));
            } catch (Exception e) {
                System.err.println("Date parsing error for: " + dateStr);
            }
        }

        // 나머지 필드 매핑
        precedent.setSentence(precService.path("선고").asText(""));
        precedent.setCourtName(precService.path("법원명").asText(""));
        precedent.setCourtTypeCode(precService.path("법원종류코드").asText(""));
        precedent.setCaseTypeName(precService.path("사건종류명").asText(""));
        precedent.setCaseTypeCode(precService.path("사건종류코드").asText(""));
        precedent.setTypeOfJudgment(precService.path("판결유형").asText(""));
        precedent.setReferencePrecedent(precService.path("참조판례").asText(""));

        // 판례 내용 (원본 그대로 저장)
        precedent.setNotice(precService.path("판시사항").asText(""));
        precedent.setSummaryOfTheJudgment(precService.path("판결요지").asText(""));
        precedent.setReferenceArticle(precService.path("참조조문").asText(""));
        precedent.setPrecedentContent(precService.path("판례내용").asText(""));

        return precedent;
    }

    // ==================== 편의 메서드 ====================

    /**
     * 키워드 검색부터 저장까지 원스톱 처리
     *
     * @param query 검색 키워드
     * @return 저장된 건수
     * @throws Exception 처리 중 예외 발생 시
     */
    public int searchAndSaveAll(String query) throws Exception {
        // 1. 키워드로 판례일련번호 리스트 가져오기
        List<String> precedentIds = getPrecedentNumbers(query);
        if (precedentIds.isEmpty()) {
            return 0;
        }

        // 2. 일련번호로 상세 조회
        List<Precedent> precedents = getPrecedentDetails(precedentIds);

        // 3. 상세 조회한 판례 저장
        List<Precedent> savedPrecedents = savePrecedents(precedents);

        return savedPrecedents.size();
    }
}
