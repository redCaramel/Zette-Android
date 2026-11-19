package kr.ac.ssu.zette.dto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.util.Objects;

// 일정 식별 키. 일반 일정은 "s:12", 루틴 회차는 "r:3:2026-11-18"
public final class ScheduleKey {

    private static final String PREFIX_SCHEDULE = "s";
    private static final String PREFIX_OCCURRENCE = "r";
    private static final String SEPARATOR = ":";

    @Nullable
    public final Long scheduleId;
    @Nullable
    public final Long routineId;
    @Nullable
    public final LocalDate occurrenceDate;

    private ScheduleKey(@Nullable Long scheduleId, @Nullable Long routineId,
                        @Nullable LocalDate occurrenceDate) {
        this.scheduleId = scheduleId;
        this.routineId = routineId;
        this.occurrenceDate = occurrenceDate;
    }

    @NonNull
    public static ScheduleKey ofSchedule(long id) {
        return new ScheduleKey(id, null, null);
    }

    @NonNull
    public static ScheduleKey ofOccurrence(long routineId, @NonNull LocalDate date) {
        return new ScheduleKey(null, routineId, date);
    }

    public boolean isOccurrence() {
        return routineId != null;
    }

    // Intent extra, 알림 requestCode 계산용 문자열
    @NonNull
    public String encode() {
        if (isOccurrence()) {
            return PREFIX_OCCURRENCE + SEPARATOR + routineId + SEPARATOR + occurrenceDate;
        }
        return PREFIX_SCHEDULE + SEPARATOR + scheduleId;
    }

    @NonNull
    public static ScheduleKey decode(@NonNull String s) {
        String[] parts = s.split(SEPARATOR);
        if (parts.length == 2 && PREFIX_SCHEDULE.equals(parts[0])) {
            return ofSchedule(Long.parseLong(parts[1]));
        }
        if (parts.length == 3 && PREFIX_OCCURRENCE.equals(parts[0])) {
            return ofOccurrence(Long.parseLong(parts[1]), LocalDate.parse(parts[2]));
        }
        throw new IllegalArgumentException("잘못된 ScheduleKey 문자열: " + s);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScheduleKey)) return false;
        ScheduleKey that = (ScheduleKey) o;
        return Objects.equals(scheduleId, that.scheduleId)
                && Objects.equals(routineId, that.routineId)
                && Objects.equals(occurrenceDate, that.occurrenceDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scheduleId, routineId, occurrenceDate);
    }

    @NonNull
    @Override
    public String toString() {
        return encode();
    }
}
