package kr.ac.ssu.zette.data.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.util.ArrayList;

import kr.ac.ssu.zette.data.local.entity.RoutineEntity;
import kr.ac.ssu.zette.data.local.entity.ScheduleEntity;
import kr.ac.ssu.zette.dto.ComputedRoute;
import kr.ac.ssu.zette.dto.Place;
import kr.ac.ssu.zette.dto.Routine;
import kr.ac.ssu.zette.dto.Schedule;

// 엔티티 ↔ dto 변환 (순수 Java)
public final class EntityMapper {

    private EntityMapper() {
    }

    // ===== Schedule =====

    @NonNull
    public static Schedule toSchedule(@NonNull ScheduleEntity e) {
        Schedule s = new Schedule();
        s.id = e.id;
        s.name = e.name;
        s.date = e.date;
        s.arrivalTime = e.arrivalTime;
        s.place = toPlace(e.placeId, e.placeName, e.placeAddress, e.placeLat, e.placeLng);
        s.routeType = e.routeType;
        s.arrivalBuffer = e.arrivalBuffer;
        s.activated = e.activated;
        s.routineId = e.routineId;
        s.occurrenceDate = e.occurrenceDate;
        copyState(e, s);
        return s;
    }

    @NonNull
    public static ScheduleEntity toEntity(@NonNull Schedule s) {
        ScheduleEntity e = new ScheduleEntity();
        e.id = s.id;
        e.name = s.name;
        e.date = s.date;
        e.arrivalTime = s.arrivalTime;
        if (s.place != null) {
            e.placeId = s.place.id;
            e.placeName = s.place.name;
            e.placeAddress = s.place.address;
            e.placeLat = s.place.lat;
            e.placeLng = s.place.lng;
        }
        e.routeType = s.routeType;
        e.arrivalBuffer = s.arrivalBuffer;
        e.activated = s.activated;
        e.status = s.status;
        e.completedAt = s.completedAt;
        e.expectedArrival = s.expectedArrival;
        if (s.computed != null) {
            e.expectedDeparture = s.computed.expectedDeparture;
            e.travelMinutes = s.computed.travelMinutes;
            e.routeSummary = s.computed.routeSummary;
            e.computedAt = s.computed.computedAt;
        }
        e.routineId = s.routineId;
        e.occurrenceDate = s.occurrenceDate;
        return e;
    }

    // 상태·계산 결과만 행 → dto로 복사 (내용은 건드리지 않음)
    public static void copyState(@NonNull ScheduleEntity from, @NonNull Schedule to) {
        to.status = from.status;
        to.completedAt = from.completedAt;
        to.expectedArrival = from.expectedArrival;
        to.computed = hasComputed(from)
                ? new ComputedRoute(from.expectedDeparture, from.travelMinutes,
                from.routeSummary, from.computedAt)
                : null;
    }

    // 루틴 내용으로 채운 기본 회차 (id = 0, 상태는 호출한 쪽에서 정한다)
    @NonNull
    public static Schedule occurrenceOf(@NonNull RoutineEntity r, @NonNull LocalDate occurrenceDate) {
        Schedule s = new Schedule();
        s.id = 0;
        s.name = r.name;
        s.date = occurrenceDate;
        s.arrivalTime = r.arrivalTime;
        s.place = toPlace(r.placeId, r.placeName, r.placeAddress, r.placeLat, r.placeLng);
        s.routeType = r.routeType;
        s.arrivalBuffer = r.arrivalBuffer;
        s.activated = r.activated;
        s.routineId = r.id;
        s.occurrenceDate = occurrenceDate;
        return s;
    }

    // 루틴 내용으로 채운 회차 상태 행 (content_overridden = false, 상태는 기본값)
    @NonNull
    public static ScheduleEntity occurrenceRowOf(@NonNull RoutineEntity r, @NonNull LocalDate occurrenceDate) {
        ScheduleEntity e = new ScheduleEntity();
        copyRoutineContent(r, e);
        e.date = occurrenceDate;
        e.activated = true;
        e.routineId = r.id;
        e.occurrenceDate = occurrenceDate;
        e.contentOverridden = false;
        e.skipped = false;
        return e;
    }

    // 회차 행의 내용(이름·시각·장소·경로 설정)을 루틴 내용으로 채운다. 날짜·상태는 그대로 둔다.
    public static void copyRoutineContent(@NonNull RoutineEntity r, @NonNull ScheduleEntity e) {
        e.name = r.name;
        e.arrivalTime = r.arrivalTime;
        e.placeId = r.placeId;
        e.placeName = r.placeName;
        e.placeAddress = r.placeAddress;
        e.placeLat = r.placeLat;
        e.placeLng = r.placeLng;
        e.routeType = r.routeType;
        e.arrivalBuffer = r.arrivalBuffer;
    }

    // ===== Routine =====

    @NonNull
    public static Routine toRoutine(@NonNull RoutineEntity e) {
        Routine r = new Routine();
        r.id = e.id;
        r.name = e.name;
        r.repeatType = e.repeatType;
        r.arrivalTime = e.arrivalTime;
        r.place = toPlace(e.placeId, e.placeName, e.placeAddress, e.placeLat, e.placeLng);
        r.routeType = e.routeType;
        r.arrivalBuffer = e.arrivalBuffer;
        r.activated = e.activated;
        r.createdAt = e.createdAt;
        r.weekdays = e.weekdays != null ? e.weekdays.clone() : new boolean[7];
        r.repeatDate = e.repeatDate != null ? new ArrayList<>(e.repeatDate) : null;
        return r;
    }

    // deleted_at은 dto에 없으므로 호출한 쪽에서 유지한다.
    @NonNull
    public static RoutineEntity toEntity(@NonNull Routine r) {
        RoutineEntity e = new RoutineEntity();
        e.id = r.id;
        e.name = r.name;
        e.repeatType = r.repeatType;
        e.arrivalTime = r.arrivalTime;
        if (r.place != null) {
            e.placeId = r.place.id;
            e.placeName = r.place.name;
            e.placeAddress = r.place.address;
            e.placeLat = r.place.lat;
            e.placeLng = r.place.lng;
        }
        e.routeType = r.routeType;
        e.arrivalBuffer = r.arrivalBuffer;
        e.activated = r.activated;
        e.createdAt = r.createdAt;
        e.weekdays = r.weekdays != null ? r.weekdays.clone() : null;
        e.repeatDate = r.repeatDate != null ? new ArrayList<>(r.repeatDate) : null;
        return e;
    }

    // ===== 내부 =====

    @Nullable
    private static Place toPlace(@Nullable String id, @Nullable String name, @Nullable String address,
                                 @Nullable Double lat, @Nullable Double lng) {
        if (id == null && name == null && address == null && lat == null && lng == null) {
            return null;
        }
        return new Place(id, name, address, lat != null ? lat : 0, lng != null ? lng : 0, null);
    }

    private static boolean hasComputed(@NonNull ScheduleEntity e) {
        return e.expectedDeparture != null || e.travelMinutes != null
                || e.routeSummary != null || e.computedAt != null;
    }
}
