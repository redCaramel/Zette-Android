package kr.ac.ssu.zette.ui;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Map;

import kr.ac.ssu.zette.dto.Place;
import kr.ac.ssu.zette.dto.Routine;
import kr.ac.ssu.zette.dto.Schedule;

public class ShareLinkTest {

    private static Place place() {
        return new Place("p1", "강남역 2번 출구", "서울 강남구", 37.4979, 127.0276, null);
    }

    private static Schedule schedule() {
        Schedule s = new Schedule();
        s.name = "팀 프로젝트 회의";
        s.date = LocalDate.of(2026, 10, 8);
        s.arrivalTime = LocalTime.of(15, 0);
        s.place = place();
        s.routeType = Schedule.RouteType.MinWalk;
        s.arrivalBuffer = 5;
        return s;
    }

    private static Routine weekly() {
        boolean[] mwf = {true, false, true, false, true, false, false};
        return new Routine(7, "출근 준비", Routine.RepeatType.WEEKLY, LocalTime.of(8, 30), place(),
                Schedule.RouteType.MinTransfer, 10, mwf, null);
    }

    private static Routine monthly() {
        return new Routine(8, "월간 정산 회의", Routine.RepeatType.MONTHLY, LocalTime.of(14, 0), place(),
                Schedule.RouteType.MinTime, 0, null, Arrays.asList(1, 15));
    }

    @Test
    public void scheduleRoundTrip() {
        Schedule r = ShareLink.scheduleFrom(ShareLink.toParams(schedule()));
        assertNotNull(r);
        assertEquals("팀 프로젝트 회의", r.name);
        assertEquals(LocalDate.of(2026, 10, 8), r.date);
        assertEquals(LocalTime.of(15, 0), r.arrivalTime);
        assertEquals("강남역 2번 출구", r.place.name);
        assertEquals(37.4979, r.place.lat, 1e-9);
        assertEquals(Schedule.RouteType.MinWalk, r.routeType);
        assertEquals(5, r.arrivalBuffer);
    }

    @Test
    public void scheduleRejectsBadValues() {
        String[][] bad = {
                {"v", "2"}, {"k", "r"}, {"n", " "}, {"n", "가".repeat(31)}, {"d", "2026-13-01"},
                {"t", "25:00"}, {"lat", "91"}, {"lng", "NaN"}, {"b", "-1"}, {"b", "61"}, {"pn", ""},
        };
        for (String[] kv : bad) {
            Map<String, String> p = ShareLink.toParams(schedule());
            p.put(kv[0], kv[1]);
            assertNull(kv[0] + "=" + kv[1], ShareLink.scheduleFrom(p));
        }
        Map<String, String> missing = ShareLink.toParams(schedule());
        missing.remove("lat");
        assertNull(ShareLink.scheduleFrom(missing));
    }

    @Test
    public void unknownRouteTypeFallsBack() {
        Map<String, String> p = ShareLink.toParams(schedule());
        p.put("rt", "Teleport");
        assertEquals(Schedule.RouteType.MinTime, ShareLink.scheduleFrom(p).routeType);
    }

    @Test
    public void routineRoundTrip() {
        Routine w = ShareLink.routineFrom(ShareLink.toParams(weekly()));
        assertNotNull(w);
        assertEquals(0, w.id);      // 받은 루틴은 새로 저장된다
        assertEquals("출근 준비", w.name);
        assertEquals(Routine.RepeatType.WEEKLY, w.repeatType);
        assertArrayEquals(new boolean[]{true, false, true, false, true, false, false}, w.weekdays);
        assertEquals(LocalTime.of(8, 30), w.arrivalTime);
        assertEquals(Schedule.RouteType.MinTransfer, w.routeType);
        assertEquals(10, w.arrivalBuffer);

        Routine m = ShareLink.routineFrom(ShareLink.toParams(monthly()));
        assertNotNull(m);
        assertEquals(Routine.RepeatType.MONTHLY, m.repeatType);
        assertEquals(Arrays.asList(1, 15), m.repeatDate);
    }

    @Test
    public void routineRejectsBadRepeat() {
        String[][] badWeekly = {{"wd", "0000000"}, {"wd", "10101"}, {"wd", "1x10000"}, {"k", "s"}};
        for (String[] kv : badWeekly) {
            Map<String, String> p = ShareLink.toParams(weekly());
            p.put(kv[0], kv[1]);
            assertNull(kv[0] + "=" + kv[1], ShareLink.routineFrom(p));
        }
        String[] badMonthly = {"", "0", "32", "1,1", "1,,2", "a"};
        for (String md : badMonthly) {
            Map<String, String> p = ShareLink.toParams(monthly());
            p.put("md", md);
            assertNull("md=" + md, ShareLink.routineFrom(p));
        }
        Map<String, String> both = ShareLink.toParams(weekly());
        both.put("md", "1");
        assertNull(ShareLink.routineFrom(both));
    }
}
