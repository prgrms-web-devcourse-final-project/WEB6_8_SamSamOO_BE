package com.ai.lawyer.global.qdrant.loader;

import com.ai.lawyer.domain.law.entity.*;
import com.ai.lawyer.domain.law.repository.*;
import com.ai.lawyer.domain.precedent.repository.PrecedentRepository;
import com.ai.lawyer.global.qdrant.entity.Qdrent;
import com.ai.lawyer.global.qdrant.repository.QdrantRepository;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
@RequiredArgsConstructor
public class LawLoader {

    private final PrecedentRepository precedentRepository;
    private final VectorStore vectorStore;
    private final QdrantClient qdrantClient;
    private final TextSplitter textSplitter;

    private final LawRepository lawRepository;
    private final HangRepository hangRepository;
    private final JoRepository joRepository;
    private final JangRepository jangRepository;
    private final HoRepository hoRepository;
    private final QdrantRepository qdrantRepository;


    @Value("${spring.ai.vectorstore.qdrant.collection-name}")
    private String collectionName;

    @Value("${spring.ai.vectorstore.qdrant.vector-size}")
    private Long vectorSize;

    // 순서:
    @PostConstruct
    public void init() throws ExecutionException, InterruptedException {

        existQdrantCollection();

        Qdrent qdrent = qdrantRepository.findById(1L).orElse(
                Qdrent.builder().PointsCount(0L).build()
        );

        if (!verification(qdrent.getPointsCount())) {
            return;
        }

        //loadCasesIntoVectorStore();                   주석 풀기 금지  -> 과금
        //loadLawsIntoVectorStore();                    주석 풀기 금지  -> 과금

        qdrent.setPointsCount(qdrantClient.getCollectionInfoAsync(collectionName).get().getPointsCount());

        qdrantRepository.save(qdrent);
    }

    public void loadCasesIntoVectorStore() {
        log.info("판례 데이터 벡터화를 시작합니다...");

        List<Document> documents = precedentRepository.findAll().stream()
                .limit(5)
                .flatMap(lawEntity -> {
                    Document originalDoc = new Document(
                            lawEntity.getPrecedentContent(),
                            Map.of("type", "판례", "caseNumber", lawEntity.getCaseNumber(), "court", lawEntity.getCourtName())
                    );
                    return textSplitter.apply(List.of(originalDoc)).stream();
                }).toList();

        vectorStore.add(documents);
        log.info("판례 데이터 {}건을 벡터 저장소에 성공적으로 저장했습니다.", documents.size());
    }

    public void loadLawsIntoVectorStore() {
        log.info("법령 데이터 벡터화를 시작합니다...");
        List<Document> allChunks = new ArrayList<>();

        List<Law> laws = lawRepository.findAll();
        int lawCount = 0;
        for (Law law : laws) {
            if (lawCount++ >= 10) break;

            List<Jang> jangs = jangRepository.findByLaw(law);
            int jangCount = 0;
            for (Jang jang : jangs) {
                if (jangCount++ >= 10) break;

                List<Jo> jos = joRepository.findByJang(jang);
                int joCount = 0;
                for (Jo jo : jos) {
                    if (joCount++ >= 10) break;

                    StringBuilder contentBuilder = new StringBuilder();

                    if (jo.getContent() != null && !jo.getContent().isBlank()) {
                        contentBuilder.append(jo.getContent()).append("\n");
                    }

                    List<Hang> hangs = hangRepository.findByJo(jo);
                    int hangCount = 0;
                    for (Hang hang : hangs) {
                        if (hangCount++ >= 10) break;

                        if (hang.getContent() != null && !hang.getContent().isBlank()) {
                            contentBuilder.append(hang.getContent()).append("\n");
                        }

                        List<Ho> hos = hoRepository.findByHang(hang);
                        int hoCount = 0;
                        for (Ho ho : hos) {
                            if (hoCount++ >= 10) break;

                            if (ho.getContent() != null && !ho.getContent().isBlank()) {
                                contentBuilder.append(ho.getContent()).append("\n");
                            }
                        }
                    }

                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("type", "법령");
                    metadata.put("lawName", law.getLawName());

                    Document originalDoc = new Document(contentBuilder.toString(), metadata);
                    List<Document> chunks = textSplitter.apply(List.of(originalDoc));
                    allChunks.addAll(chunks);
                }
            }
        }

        vectorStore.add(allChunks);
        log.info("법령 데이터 {}건을 벡터 저장소에 성공적으로 저장했습니다.", allChunks.size());
    }

    private void existQdrantCollection() throws InterruptedException, ExecutionException {
        // 현재 Qdrant에 있는 모든 컬렉션 목록을 가져옴
        var collections = qdrantClient.listCollectionsAsync().get();
        boolean collectionExists = collections.stream()
                .anyMatch(collection -> collection.equals(collectionName));

        // 만약 컬렉션이 없다면, 새로 생성
        if (!collectionExists) {
            log.info("'{}' 컬렉션이 존재하지 않아 새로 생성중", collectionName);
            qdrantClient.createCollectionAsync(
                    collectionName,
                    Collections.VectorParams.newBuilder()
                            .setSize(vectorSize) // yml에 설정된 벡터 크기
                            .setDistance(Collections.Distance.Cosine) // 가장 일반적인 거리 측정 방식
                            .build()
            ).get();
            log.info("'{}' 컬렉션 생성을 완료했습니다.", collectionName);
        } else {
            log.info("'{}' 컬렉션이 이미 존재합니다.", collectionName);
        }
    }

    private boolean verification(Long count) throws ExecutionException, InterruptedException {

        if (lawRepository.count() == 0) {
            log.warn("데이터베이스에 법령 데이터가 없습니다. data.sql을 확인하세요.");
            return false;
        }

        if (precedentRepository.count() == 0) {
            log.warn("데이터베이스에 판례 데이터가 없습니다. data.sql을 확인하세요.");
            return false;
        }

        if (count == 0) {
            return true;
        }

        if (qdrantClient.getCollectionInfoAsync(collectionName).get().getPointsCount() == count) {
            log.info("Qdrant 벡터 저장소에 이미 모든 데이터가 존재합니다.");
            return false;
        }

        return true;
    }
}