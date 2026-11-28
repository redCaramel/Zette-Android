package kr.ac.ssu.zette.ui.activity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.LocalDateTime;

public class RealtimeArrivalModeTest {

    private static final LocalDateTime DEPARTURE = LocalDateTime.of(2026, 11, 28, 14, 15);

    @Test
    public void farUntilOneHourBeforeDeparture() {
        assertEquals(RealtimeArrivalActivity.Mode.FAR,
                RealtimeArrivalActivity.modeAt(LocalDateTime.of(2026, 11, 28, 9, 41), DEPARTURE));
        assertEquals(RealtimeArrivalActivity.Mode.FAR,
                RealtimeArrivalActivity.modeAt(LocalDateTime.of(2026, 11, 28, 13, 14), DEPARTURE));
    }

    @Test
    public void realtimeFromOneHourBeforeDeparture() {
        assertEquals(RealtimeArrivalActivity.Mode.REALTIME,
                RealtimeArrivalActivity.modeAt(LocalDateTime.of(2026, 11, 28, 13, 15), DEPARTURE));
        assertEquals(RealtimeArrivalActivity.Mode.REALTIME,
                RealtimeArrivalActivity.modeAt(LocalDateTime.of(2026, 11, 28, 14, 30), DEPARTURE));
    }
}
