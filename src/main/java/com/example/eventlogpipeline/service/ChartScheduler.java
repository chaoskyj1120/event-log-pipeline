package com.example.eventlogpipeline.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChartScheduler {

    private final ChartService chartService;

    // 앱 시작 1분 후 첫 생성, 이후 1시간마다 반복
    @Scheduled(initialDelay = 60_000, fixedRate = 3_600_000)
    public void generateCharts() {
        log.info("=== 차트 생성 시작 ===");
        chartService.generateAll();
    }
}