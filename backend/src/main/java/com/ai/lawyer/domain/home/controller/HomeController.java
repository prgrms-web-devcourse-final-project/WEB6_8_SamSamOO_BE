package com.ai.lawyer.domain.home.controller;

import com.ai.lawyer.domain.home.dto.FullData;
import com.ai.lawyer.domain.home.service.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
@Tag(name = "홈", description = "홈 화면 API")
public class HomeController {

    private final HomeService homeService;

    @Operation(summary = "데이터 수", description = "판례 법령 채팅 투표 수 조회")
    @PostMapping("/data-count")
    public ResponseEntity<FullData> getDataCount() {
        return ResponseEntity.ok(homeService.getDataCount());
    }

}
