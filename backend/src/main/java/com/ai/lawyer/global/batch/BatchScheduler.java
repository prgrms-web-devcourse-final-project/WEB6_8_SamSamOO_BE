package com.ai.lawyer.global.batch;

/*@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class BatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job dataVectorizationJob;

    @Scheduled(cron = "#{${batch.scheduler.run-every-minute} ? '* * * * * *' : '* * 2 * * *'}")
    public void runVectorizationJob() {
        log.info("전체 데이터(판례, 법령) 벡터화 스케줄러 실행...");
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("requestDate", LocalDateTime.now().toString())
                    .toJobParameters();

            jobLauncher.run(dataVectorizationJob, jobParameters); // Job 실행
        } catch (Exception e) {
            log.error("전체 데이터 벡터화 배치 작업 실행 중 오류 발생", e);
        }
    }
}*/
