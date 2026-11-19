package kr.ac.ssu.zette.dto;

import androidx.annotation.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;

// 백그라운드가 계산해 저장하는 경로 결과
public class ComputedRoute {

    @Nullable
    public LocalDateTime expectedDeparture; // 예상 출발 시각
    @Nullable
    public Integer travelMinutes;           // 예상 이동 시간 (분)
    @Nullable
    public String routeSummary;             // 경로 요약
    @Nullable
    public LocalDateTime computedAt;        // 조회 시각

    public ComputedRoute() {}

    public ComputedRoute(@Nullable LocalDateTime expectedDeparture, @Nullable Integer travelMinutes,
                         @Nullable String routeSummary, @Nullable LocalDateTime computedAt) {
        this.expectedDeparture = expectedDeparture;
        this.travelMinutes = travelMinutes;
        this.routeSummary = routeSummary;
        this.computedAt = computedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComputedRoute)) return false;
        ComputedRoute that = (ComputedRoute) o;
        return Objects.equals(expectedDeparture, that.expectedDeparture)
                && Objects.equals(travelMinutes, that.travelMinutes)
                && Objects.equals(routeSummary, that.routeSummary)
                && Objects.equals(computedAt, that.computedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expectedDeparture, travelMinutes, routeSummary, computedAt);
    }
}
