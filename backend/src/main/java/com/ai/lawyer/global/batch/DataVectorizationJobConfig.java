package com.ai.lawyer.global.batch;

/*@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataVectorizationJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final VectorStore vectorStore;

    private final JangRepository jangRepository;
    private final JoRepository joRepository;
    private final HangRepository hangRepository;
    private final HoRepository hoRepository;

    private final TokenTextSplitter tokenSplitter = TokenTextSplitter.builder()
            .withChunkSize(800)
            .withMinChunkSizeChars(0)
            .withMinChunkLengthToEmbed(5)
            .withMaxNumChunks(10000)
            .withKeepSeparator(true)
            .build();

    private static final int CHUNK_SIZE = 10; // 배치 처리 시 한 번에 읽어올 데이터 수

    @Value("${batch.page.size.precedent}")
    private int precedentPageSize; // 하루에 처리할 판례 수

    @Value("${batch.page.size.law}")
    private int lawPageSize; // 하루에 처리할 법령 수

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-thread-");
        executor.initialize();
        return executor;
    }

    // -------------- 전체 데이터 벡터화 정의 --------------
    @Bean
    public Job dataVectorizationJob() {
        return new JobBuilder("dataVectorizationJob", jobRepository)
                .start(precedentVectorizationStep()) // 판례 벡터화 Step 실행
                .next(lawVectorizationStep())        // 법령 벡터화 Step 실행
                .build();
    }

    // -------------- 판례 벡터화 ---------------
    @Bean
    public Step precedentVectorizationStep() {
        log.info(">>>>>> 판례 벡터화 시작");
        return new StepBuilder("precedentVectorizationStep", jobRepository)
                .<Precedent, List<Document>>chunk(CHUNK_SIZE, transactionManager)
                .reader(precedentItemReader())
                .processor(precedentItemProcessor())
                .writer(documentItemWriter())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public JpaPagingItemReader<Precedent> precedentItemReader() {
        return new JpaPagingItemReaderBuilder<Precedent>()
                .name("precedentItemReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .maxItemCount(precedentPageSize)
                .queryString("SELECT p FROM Precedent p ORDER BY p.id ASC")
                .build();
    }

    @Bean
    public ItemProcessor<Precedent, List<Document>> precedentItemProcessor() {

        return precedent -> {
            String content = precedent.getPrecedentContent();
            if (content == null || content.isBlank()) return null;

            Document originalDoc = new Document(content, Map.of(
                    "type", "판례",
                    "caseNumber", precedent.getCaseNumber(),
                    "court", precedent.getCourtName(),
                    "caseName", precedent.getCaseName()
            ));

            List<Document> chunkDocs = tokenSplitter.split(originalDoc);
            List<Document> finalChunks = new ArrayList<>();

            // 청크별로 메타데이터에 인덱스 추가 -> 구분 용도
            for (int i = 0; i < chunkDocs.size(); i++) {
                Document chunk = chunkDocs.get(i);
                Map<String, Object> newMetadata = new HashMap<>(chunk.getMetadata());
                newMetadata.put("chunkIndex", i);
                finalChunks.add(new Document(chunk.getText(), newMetadata));
            }
            return finalChunks;
        };
    }

    // -------------- 법령 백터화 ---------------
    @Bean
    public Step lawVectorizationStep() {
        log.info(">>>>>> 법령 벡터화 시작");
        return new StepBuilder("lawVectorizationStep", jobRepository)
                .<Law, List<Document>>chunk(CHUNK_SIZE, transactionManager) // 법령은 한 번에 10개씩 처리
                .reader(lawItemReader())
                .processor(lawItemProcessor())
                .writer(documentItemWriter())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public JpaPagingItemReader<Law> lawItemReader() {
        return new JpaPagingItemReaderBuilder<Law>()
                .name("lawItemReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .maxItemCount(lawPageSize)
                .queryString("SELECT l FROM Law l ORDER BY l.id ASC")
                .build();
    }

    @Bean
    public ItemProcessor<Law, List<Document>> lawItemProcessor() {
        return law -> {
            List<Document> finalChunks = new ArrayList<>();

            List<Jang> jangs = jangRepository.findByLaw(law);

            for (Jang jang : jangs) {

                StringBuilder contentBuilder = new StringBuilder();

                contentBuilder.append(law.getLawName()).append("\n");

                if (jang.getContent() != null && !jang.getContent().isBlank()) {
                    contentBuilder.append(jang.getContent()).append("\n");
                }

                List<Jo> jos = joRepository.findByJang(jang);
                for (Jo jo : jos) {

                    if (jo.getContent() != null && !jo.getContent().isBlank()) {
                        contentBuilder.append(jo.getContent()).append("\n");
                    }

                    List<Hang> hangs = hangRepository.findByJo(jo);
                    for (Hang hang : hangs) {
                        if (hang.getContent() != null && !hang.getContent().isBlank()) {
                            contentBuilder.append(hang.getContent()).append("\n");
                        }

                        List<Ho> hos = hoRepository.findByHang(hang);
                        for (Ho ho : hos) {
                            if (ho.getContent() != null && !ho.getContent().isBlank()) {
                                contentBuilder.append(ho.getContent()).append("\n");
                            }
                        }
                    }
                }

                // === Jang 단위로 문서화 ===
                String finalContent = contentBuilder.toString();

                if (!finalContent.isBlank()) {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("type", "법령");
                    metadata.put("lawName", law.getLawName());
                    metadata.put("jangId", jang.getId());

                    Document originalDoc = new Document(finalContent, metadata);

                    List<Document> chunkDocs = tokenSplitter.split(originalDoc);

                    for (int i = 0; i < chunkDocs.size(); i++) {
                        Document chunk = chunkDocs.get(i);
                        Map<String, Object> newMetadata = new HashMap<>(chunk.getMetadata());
                        newMetadata.put("chunkIndex", i);
                        finalChunks.add(new Document(chunk.getText(), newMetadata));
                    }
                }
            }

            return finalChunks.isEmpty() ? null : finalChunks;
        };
    }

    @Bean
    public ItemWriter<List<Document>> documentItemWriter() {
        return chunk -> {
            List<Document> totalDocuments = chunk.getItems().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            if (!totalDocuments.isEmpty()) {
                vectorStore.add(totalDocuments);
                log.info(">>>>>> {}개의 Document 청크를 벡터 저장소에 저장했습니다.", totalDocuments.size());
            }
        };
    }
}*/

