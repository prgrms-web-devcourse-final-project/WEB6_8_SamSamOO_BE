package com.ai.lawyer.domain.poll.dto;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PollAgeStaticsDto {
    private Long pollItemsId;
    private Integer pollOptionIndex;
    private java.util.List<AgeGroupCountDto> ageGroupCounts;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class AgeGroupCountDto {
        private String option;
        private String ageGroup;
        private Long voteCount;
    }
}
