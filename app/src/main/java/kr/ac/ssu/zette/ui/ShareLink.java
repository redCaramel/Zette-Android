package kr.ac.ssu.zette.ui;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import kr.ac.ssu.zette.dto.Place;
import kr.ac.ssu.zette.dto.Routine;
import kr.ac.ssu.zette.dto.Schedule;

// 공유 링크의 값
public final class ShareLink {

    public static final String SCHEME = "zette";
    public static final String HOST_SCHEDULE = "schedule";
    public static final String HOST_ROUTINE = "routine";

    public static final String KIND = "k";
    public static final String KIND_SCHEDULE = "s";
    public static final String KIND_ROUTINE = "r";

    static final String VERSION = "1";
    static final int MAX_NAME = 30;          // 이름 최대 길이와 같다
    static final int MAX_TEXT = 100;
    static final int MAX_BUFFER = 60;        // BufferDialogs.MAX_MINUTES

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

    private ShareLink() {
    }

    @NonNull
    public static Map<String, String> toParams(@NonNull Schedule s) {
        Map<String, String> p = head(KIND_SCHEDULE, s.name);
        p.put("d", s.date.toString());
        putCommon(p, s.arrivalTime, s.place, s.routeType, s.arrivalBuffer);
        return p;
    }

    // wd = 월~일 0/1 7자리, md = "1,15"
    @NonNull
    public static Map<String, String> toParams(@NonNull Routine r) {
        Map<String, String> p = head(KIND_ROUTINE, r.name);
        if (r.repeatType == Routine.RepeatType.MONTHLY) {
            StringBuilder md = new StringBuilder();
            for (Integer day : r.repeatDate) md.append(md.length() == 0 ? "" : ",").append(day);
            p.put("md", md.toString());
        } else {
            StringBuilder wd = new StringBuilder();
            for (boolean on : r.weekdays) wd.append(on ? '1' : '0');
            p.put("wd", wd.toString());
        }
        putCommon(p, r.arrivalTime, r.place, r.routeType, r.arrivalBuffer);
        return p;
    }

    // 검증 실패 시 null
    @Nullable
    public static Schedule scheduleFrom(@NonNull Map<String, String> p) {
        Common c = common(p, KIND_SCHEDULE);
        if (c == null) return null;
        try {
            Schedule s = new Schedule();
            s.name = c.name;
            s.date = LocalDate.parse(p.get("d"));
            s.arrivalTime = c.time;
            s.place = c.place;
            s.routeType = c.routeType;
            s.arrivalBuffer = c.buffer;
            return s;
        } catch (DateTimeParseException | NullPointerException e) {
            return null;
        }
    }

    // 검증 실패 시 null
    @Nullable
    public static Routine routineFrom(@NonNull Map<String, String> p) {
        Common c = common(p, KIND_ROUTINE);
        if (c == null) return null;
        String wd = p.get("wd");
        String md = p.get("md");
        if ((wd == null) == (md == null)) return null;      // 둘 중 하나만

        if (wd != null) {
            if (!wd.matches("[01]{7}") || !wd.contains("1")) return null;
            boolean[] weekdays = new boolean[7];
            for (int i = 0; i < 7; i++) weekdays[i] = wd.charAt(i) == '1';
            return new Routine(0, c.name, Routine.RepeatType.WEEKLY, c.time, c.place, c.routeType, c.buffer, weekdays, null);
        }
        if (!md.matches("\\d{1,2}(,\\d{1,2}){0,30}")) return null;
        List<Integer> days = new ArrayList<>();
        for (String d : md.split(",")) {
            int day = Integer.parseInt(d);
            if (day < 1 || day > 31 || days.contains(day)) return null;
            days.add(day);
        }
        return new Routine(0, c.name, Routine.RepeatType.MONTHLY, c.time, c.place, c.routeType, c.buffer, null, days);
    }

    @NonNull
    public static Uri toUri(@NonNull String host, @NonNull Map<String, String> params) {
        Uri.Builder b = new Uri.Builder().scheme(SCHEME).authority(host);
        for (Map.Entry<String, String> e : params.entrySet()) b.appendQueryParameter(e.getKey(), e.getValue());
        return b.build();
    }

    @NonNull
    public static Map<String, String> paramsOf(@NonNull Uri uri) {
        Map<String, String> p = new HashMap<>();
        if (!uri.isHierarchical()) return p;
        for (String name : uri.getQueryParameterNames()) p.put(name, uri.getQueryParameter(name));
        return p;
    }

    @NonNull
    private static Map<String, String> head(@NonNull String kind, @NonNull String name) {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("v", VERSION);
        p.put(KIND, kind);
        p.put("n", name);
        return p;
    }

    private static void putCommon(@NonNull Map<String, String> p, @NonNull LocalTime time, @Nullable Place place,
                                  @Nullable Schedule.RouteType routeType, int buffer) {
        p.put("t", time.format(TIME));
        if (place != null) {
            if (place.id != null) p.put("pid", place.id);
            p.put("pn", place.name);
            if (place.address != null) p.put("pa", place.address);
            p.put("lat", String.valueOf(place.lat));
            p.put("lng", String.valueOf(place.lng));
        }
        if (routeType != null) p.put("rt", routeType.name());
        p.put("b", String.valueOf(buffer));
    }

    // 일정·루틴 공통 값
    private static final class Common {
        String name;
        LocalTime time;
        Place place;
        Schedule.RouteType routeType;
        int buffer;
    }

    @Nullable
    private static Common common(@NonNull Map<String, String> p, @NonNull String kind) {
        if (!VERSION.equals(p.get("v")) || !kind.equals(p.get(KIND))) return null;
        String name = text(p.get("n"), MAX_NAME);
        String placeName = text(p.get("pn"), MAX_TEXT);
        if (name == null || placeName == null) return null;
        try {
            double lat = Double.parseDouble(p.get("lat"));
            double lng = Double.parseDouble(p.get("lng"));
            if (!(lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180)) return null;  // NaN 포함
            int buffer = Integer.parseInt(p.get("b"));
            if (buffer < 0 || buffer > MAX_BUFFER) return null;

            Common c = new Common();
            c.name = name;
            c.time = LocalTime.parse(p.get("t"), TIME);
            c.place = new Place(text(p.get("pid"), MAX_TEXT), placeName, text(p.get("pa"), MAX_TEXT * 2), lat, lng, null);
            c.routeType = routeType(p.get("rt"));
            c.buffer = buffer;
            return c;
        } catch (DateTimeParseException | NumberFormatException | NullPointerException e) {
            return null;
        }
    }

    @Nullable
    private static String text(@Nullable String value, int max) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() || t.length() > max ? null : t;
    }

    @NonNull
    private static Schedule.RouteType routeType(@Nullable String value) {
        for (Schedule.RouteType t : Schedule.RouteType.values()) {
            if (t.name().equals(value)) return t;
        }
        return Schedule.RouteType.MinTime;
    }
}
