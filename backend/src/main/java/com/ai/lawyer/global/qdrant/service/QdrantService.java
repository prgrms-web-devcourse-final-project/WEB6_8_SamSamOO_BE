package com.ai.lawyer.global.qdrant.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QdrantService {

    private final VectorStore vectorStore;

    public List<Document> searchDocument(String query, String key, String value) {

        SearchRequest findCaseNumberRequest = SearchRequest.builder()
                .query(query).topK(1)
                .filterExpression(new Filter.Expression(Filter.ExpressionType.EQ, new Filter.Key(key), new Filter.Value(value)))
                .build();
        List<Document> mostSimilarDocuments = vectorStore.similaritySearch(findCaseNumberRequest);


        if (mostSimilarDocuments.isEmpty()) {
            return Collections.emptyList();
        }
        String targetCaseNumber = (String) mostSimilarDocuments.get(0).getMetadata().get("caseNumber");
        if (targetCaseNumber == null) {
            return mostSimilarDocuments;
        }

        SearchRequest fetchAllChunksRequest = SearchRequest.builder()
                .query(query).topK(100)
                .filterExpression(new Filter.Expression(Filter.ExpressionType.EQ, new Filter.Key("caseNumber"), new Filter.Value(targetCaseNumber)))
                .build();
        List<Document> allChunksOfCase = new ArrayList<>(vectorStore.similaritySearch(fetchAllChunksRequest));

        if (allChunksOfCase.isEmpty()) {
            return Collections.emptyList();
        }

        allChunksOfCase.sort(Comparator.comparingInt(doc ->
                ((Number) doc.getMetadata().get("chunkIndex")).intValue()
        ));

        String mergedContent = allChunksOfCase.stream()
                .map(Document::getText)
                .collect(Collectors.joining(""));

        Document bestScoringDoc = allChunksOfCase.stream()
                .max(Comparator.comparing(Document::getScore))
                .orElse(allChunksOfCase.get(0));

        Document finalDocument = Document.builder()
                .text(mergedContent)
                .metadata(bestScoringDoc.getMetadata())
                .score(bestScoringDoc.getScore())
                .build();

        return Collections.singletonList(finalDocument);
    }

}