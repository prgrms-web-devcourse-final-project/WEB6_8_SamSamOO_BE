package com.ai.lawyer.domain.poll.dto;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PollGenderStaticsDto {
    private Long pollItemsId;
    private Integer pollOptionIndex;
    private java.util.List<GenderCountDto> genderCounts;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class GenderCountDto {
        private String option;
        private String gender;
        private Long voteCount;
    }
}
