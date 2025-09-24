package com.ai.lawyer.domain.law.service;


import com.ai.lawyer.domain.law.dto.LawSearchRequestDto;
import com.ai.lawyer.domain.law.dto.LawsDto;
import com.ai.lawyer.domain.law.entity.*;
import com.ai.lawyer.domain.law.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class LawService {

    private LawRepository lawRepository;
    private JangRepository jangRepository;
    private JoRepository joRepository;
    private HangRepository hangRepository;
    private HoRepository hoRepository;

    private final String BASE_URL = "http://www.law.go.kr/DRF";
    private final String OC = "noheechul"; // 실제 OC로 변경 필요
    private final ObjectMapper objectMapper = new ObjectMapper();


    /**
     * 법령 검색
     * @param searchRequest
     * @return Page<Laws>
     */
    public Page<LawsDto> searchLaws(LawSearchRequestDto searchRequest) {
        return lawRepository.searchLaws(searchRequest);
    }

    /**
     * 법령 ID로 법령과 모든 하위 엔티티를 조회
     * @param lawId
     * @return Law (Jang, Jo, Hang, Ho 모두 포함)
     */
    public Law getLawWithAllChildren(Long lawId) {
        Law law = lawRepository.findWithJangById(lawId)
                .orElseThrow(() -> new EntityNotFoundException("법령이 없습니다: " + lawId));

        // 2) 각 JangEntity에 JoEntity 세팅
        List<Jang> jangs = jangRepository.findByLawId(lawId);
        law.setJangList(jangs);

        for (Jang jang : jangs) {
            Long jangId = jang.getId();
            List<Jo> jos = joRepository.findByJangId(jangId);
            jang.setJoList(jos);

            for (Jo jo : jos) {
                Long joId = jo.getId();
                List<Hang> hangs = hangRepository.findByJoId(joId);
                jo.setHangList(hangs);

                for (Hang hang : hangs) {
                    // 호는 기본 지연 로딩으로 필요 시 조회
                    hang.getHoList().size();
                }
            }
        }

        return law;
    }

    /**
     * open api 법령 리스트 조회 → 법령 ID 추출 → 상세 법령 정보 조회 → 파싱 → DB 저장
     */
    public void saveLaw(String query, int page) throws Exception {
        String lawList = getLawList(query, page);

        List<String> lawIds = getLawListIds(lawList);

        for (String lawId : lawIds) {
            String lawJson = getLawJson(lawId);
            try {
                saveLaw(lawJson);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * open api 법령 리스트 조회 (JSON 문자열 반환)
     */
    public String getLawList(String query, int page) throws Exception {
        String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/lawSearch.do")
                .queryParam("OC", OC)
                .queryParam("target", "law")
                .queryParam("type", "JSON")
                .queryParam("query", query)
                .queryParam("page", page)
                .queryParam("display", 100)
                .build()
                .toUriString();

        RestTemplate restTemplate = new RestTemplate();
        String json = restTemplate.getForObject(url, String.class);
        return  json;
    }

    /**
     * 법령 리스트 JSON에서 법령 ID 추출
     */
    private List<String> getLawListIds(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        List<String> idList = new ArrayList<>();

        JsonNode lawSearchNode = root.path("LawSearch");
        JsonNode lawArray = lawSearchNode.path("law");

        JsonNode lawNode = lawSearchNode.path("law");

        // law가 배열일 때
        if (lawNode.isArray()) {
            for (JsonNode item : lawNode) {
                String lawId = item.path("법령ID").asText();
                if (StringUtils.hasText(lawId)) {
                    idList.add(lawId);
                }
            }
        }
        // law가 단일 객체일 때
        else if (lawNode.isObject()) {
            String lawId = lawNode.path("법령ID").asText();
            if (StringUtils.hasText(lawId)) {
                idList.add(lawId);
            }
        }

        return idList;
    }

    /**
     * 법령 ID로 상세 법령 정보 조회 (JSON 문자열 반환)
     */
    private String getLawJson(String lawId) {
        String url = UriComponentsBuilder.fromHttpUrl(BASE_URL+ "/lawService.do")
                .queryParam("OC", OC)
                .queryParam("target", "law")
                .queryParam("ID", lawId)
                .queryParam("type", "JSON")
                .build()
                .toUriString();
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForObject(url, String.class);
    }

    /**
     * JSON 문자열을 파싱해 Law → Jang → Jo → Hang → Ho 계층으로 저장
     */
    private void saveLaw(String json) throws IOException {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");


        JsonNode root = objectMapper.readTree(json);
        JsonNode basic = root.path("법령").path("기본정보");

        // 1. Law 저장
        Law law = new Law();
        law.setLawName(basic.path("법령명_한글").asText());
        law.setLawField(basic.path("법종구분").path("content").asText());
        law.setMinistry(basic.path("소관부처").path("content").asText());
        law.setPromulgationNumber(basic.path("공포번호").asText());
        law.setPromulgationDate(LocalDate.parse(basic.path("공포일자").asText(), formatter));
        law.setEnforcementDate(LocalDate.parse(basic.path("시행일자").asText(), formatter));
        law = lawRepository.save(law);

        // 2. 조문단위 순회
        JsonNode articles = root.path("법령").path("조문").path("조문단위");

        Jang jang = new Jang();
        jang.setContent(null);
        jang.setLaw(law);

        if (articles.isArray()) {
            for (JsonNode art : articles) {
                String key = art.path("조문키").asText();
                String content = art.path("조문내용").asText();

                // JangEntity: key 끝자리 0
                if (key.endsWith("0")) {
                    jang = new Jang();
                    jang.setContent(content);
                    jang.setLaw(law);
                    jang = jangRepository.save(jang);
                }else {
                    if(jang.getContent()==null){
                        jang = jangRepository.save(jang);
                    }
                    Jo jo = new Jo();
                    jo.setContent(content);
                    jo.setJang(jang);
                    jo = joRepository.save(jo);

                    // HangEntity
                    JsonNode paragraphs = art.path("항");

                    if (!paragraphs.isMissingNode()) {
                        // "항"이 배열인 경우
                        if (paragraphs.isArray()) {
                            for (JsonNode p : paragraphs) {
                                Hang hang = new Hang();
                                // "항내용"이 없으면 null, 있으면 해당 텍스트 저장
                                String hangContent = p.path("항내용").isMissingNode()
                                        ? null
                                        : p.path("항내용").asText();
                                hang.setContent(hangContent);
                                hang.setJo(jo);
                                hang = hangRepository.save(hang);

                                // HoEntity 처리
                                processHoEntities(p.path("호"), hang);
                            }
                        }
                        // "항"이 객체인 경우 (항내용 없이 바로 호 배열)
                        else if (paragraphs.isObject()) {
                            Hang hang = new Hang();
                            hang.setContent(null);  // 항내용이 없으므로 null
                            hang.setJo(jo);
                            hang = hangRepository.save(hang);

                            // 객체 내의 "호" 배열 처리
                            processHoEntities(paragraphs.path("호"), hang);
                        }
                    }
                }

            }
        }
    }

    // HoEntity 처리 메서드
    private void processHoEntities(JsonNode itemsNode, Hang hang) {
        if (itemsNode.isArray()) {
            for (JsonNode h : itemsNode) {
                Ho ho = new Ho();
                ho.setContent(h.path("호내용").asText());
                ho.setHang(hang);
                hoRepository.save(ho);
            }
        }
    }


}

