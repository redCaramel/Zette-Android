package kr.ac.ssu.zette.ui.fragment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;

import kr.ac.ssu.zette.dto.Schedule;
import kr.ac.ssu.zette.dto.ScheduleStatus;

public class HomeHeroTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 20);
    private static final LocalDateTime NOW = LocalDateTime.of(TODAY, LocalTime.of(13, 0));

    private static Schedule at(long id, int hour) {
        return new Schedule(id, "일정" + id, TODAY, LocalTime.of(hour, 0), null, Schedule.RouteType.MinTime, 0);
    }

    @Test
    public void picksEarliestUpcoming_skippingPastAndDone() {
        Schedule past = at(1, 12);
        Schedule done = at(2, 14);
        done.status = ScheduleStatus.DONE;
        Schedule later = at(3, 17);
        Schedule next = at(4, 15);

        assertEquals(4, HomeFragment.findNearest(Arrays.asList(past, done, later, next), NOW).id);
    }

    @Test
    public void skipsUnreserved() {
        Schedule unreserved = at(1, 14);
        unreserved.activated = false;
        Schedule reserved = at(2, 16);

        assertEquals(2, HomeFragment.findNearest(Arrays.asList(unreserved, reserved), NOW).id);
        assertNull(HomeFragment.findNearest(Arrays.asList(unreserved), NOW));
    }

    @Test
    public void noneLeft_returnsNull() {
        assertNull(HomeFragment.findNearest(Arrays.asList(at(1, 9), at(2, 12)), NOW));
        assertNull(HomeFragment.findNearest(null, NOW));
    }

    @Test
    public void subjectParticle_followsFinalConsonant() {
        assertEquals("가", HomeFragment.subjectParticle("팀 프로젝트 회의"));
        assertEquals("이", HomeFragment.subjectParticle("수업"));
        assertEquals("가", HomeFragment.subjectParticle("Meeting"));
    }
}
