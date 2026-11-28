package kr.ac.ssu.zette.ui.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.data.local.ScheduleRepository;
import kr.ac.ssu.zette.data.local.ZetteData;
import kr.ac.ssu.zette.databinding.FragmentHomeBinding;
import kr.ac.ssu.zette.dto.Schedule;
import kr.ac.ssu.zette.dto.ScheduleKey;
import kr.ac.ssu.zette.dto.ScheduleStatus;
import kr.ac.ssu.zette.ui.Loading;
import kr.ac.ssu.zette.ui.Network;
import kr.ac.ssu.zette.ui.activity.RealtimeArrivalActivity;
import kr.ac.ssu.zette.ui.activity.ScheduleDetailActivity;
import kr.ac.ssu.zette.ui.adapter.ScheduleAdapter;
import kr.ac.ssu.zette.ui.item.ScheduleCardItem;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ScheduleAdapter scheduleAdapter;
    private ScheduleRepository repository;

    @Nullable
    private LocalDate observedDate;
    @Nullable
    private LiveData<List<Schedule>> todaySchedules;
    @Nullable
    private List<Schedule> latestSchedules;
    @Nullable
    private Schedule heroSchedule;
    // 02-L1: 오늘 일정을 처음 불러오는 동안 Hero·일정 카드 스켈레톤
    private Loading.Gate firstLoad;
    // 02-L2: 출발 시각을 서버에서 받아오는 동안 Hero 남은 시간 자리에 "출발 시각 계산 중…"
    private boolean heroLoading;
    // 02-E: 출발 시각을 못 받으면 Hero 자리에 오류 카드. 연결되면 자동으로 다시 계산한다
    private boolean heroError;
    // 03: 출발할 때 Hero 의 버스 도착 정보. departingFor 일정의 값이고, departingBus 가 null 이면 불러오는 중
    @Nullable
    private ScheduleKey departingFor;
    @Nullable
    private String departingBus;
    private int departingArrivalMinutes;
    @Nullable
    private String departingStop;
    private int departingWalkMinutes;
    private final Runnable heroLoaded = () -> {
        heroLoading = false;
        // TODO - api연결 구현: 실제 응답 실패(10초 초과·5xx)도 오류로 처리
        heroError = !Network.get(requireContext()).isOnline();
        if (binding != null) renderHero(latestSchedules);
    };

    private static final DateTimeFormatter HERO_TIME_FORMAT =
            DateTimeFormatter.ofPattern("a h시 mm분", Locale.KOREAN);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = ZetteData.get(requireContext()).schedules;

        scheduleAdapter = new ScheduleAdapter(new ScheduleAdapter.OnScheduleActionListener() {
            @Override
            public void onItemClick(@NonNull ScheduleCardItem item) {
                startActivity(ScheduleDetailActivity.intent(requireContext(), item.key()));
            }

            @Override
            public void onStatusToggle(@NonNull ScheduleCardItem item) {
                repository.setScheduleActivated(item.key(), item.status != ScheduleCardItem.Status.SCHEDULED);
            }

            @Override
            public void onCompleteClick(@NonNull ScheduleCardItem item) {
                repository.markDone(item.key());
            }
        });
        binding.rvSchedule.setAdapter(scheduleAdapter);

        View.OnClickListener openRealtime = v -> {
            if (heroSchedule != null) {
                startActivity(RealtimeArrivalActivity.intent(requireContext(), heroSchedule.key()));
            }
        };
        binding.nextDepartureHero.setOnClickListener(openRealtime);
        binding.heroDeparting.getRoot().setOnClickListener(openRealtime);
        binding.heroDeparting.layoutDepartingLoading.tvLoading.setText(R.string.hero_departing_loading);
        binding.heroDeparting.layoutDepartingLoading.tvLoading.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_on_dark_soft));

        firstLoad = new Loading.Gate(binding.heroSkeleton.getRoot(), binding.layoutScheduleSkeleton);
        firstLoad.start();

        binding.layoutHeroCalculating.tvLoading.setText(R.string.home_hero_calculating);
        binding.layoutHeroCalculating.tvLoading.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_on_dark_soft));
        binding.heroError.btnRetry.setOnClickListener(v -> loadHero());
        Network.get(requireContext()).observe(getViewLifecycleOwner(), online -> {
            if (online && heroError) loadHero();
        });
        loadHero();
    }

    private void loadHero() {
        binding.getRoot().removeCallbacks(heroLoaded);
        binding.getRoot().removeCallbacks(departingLoaded);
        heroLoading = true;
        heroError = false;
        departingFor = null;
        renderHero(latestSchedules);
        // TODO - api연결 구현
        binding.getRoot().postDelayed(heroLoaded, Loading.FAKE_DELAY_MS);
    }

    // 매 분 정각(시스템 시계와 같은 순간)에 Hero 를 다시 그린다. 화면이 보일 때만 받는다
    private final BroadcastReceiver minuteTick = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshForNow();
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        refreshForNow();
        ContextCompat.registerReceiver(requireContext(), minuteTick,
                new IntentFilter(Intent.ACTION_TIME_TICK), ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireContext().unregisterReceiver(minuteTick);
    }

    // 날짜가 바뀌었으면 오늘 일정을 다시 불러오고, 아니면 남은 시간·Hero 상태만 갱신
    private void refreshForNow() {
        LocalDate today = LocalDate.now();
        if (!today.equals(observedDate)) {
            observeDay(today);
        } else {
            renderHero(latestSchedules);
        }
    }

    private void observeDay(@NonNull LocalDate date) {
        if (todaySchedules != null) {
            todaySchedules.removeObservers(getViewLifecycleOwner());
        }
        observedDate = date;
        todaySchedules = repository.observeSchedulesOn(date);
        todaySchedules.observe(getViewLifecycleOwner(), list -> firstLoad.finish(() -> render(list)));
    }

    private void render(@Nullable List<Schedule> schedules) {
        List<ScheduleCardItem> items = new ArrayList<>();
        if (schedules != null) {
            for (Schedule s : schedules) items.add(ScheduleCardItem.from(s));
        }
        scheduleAdapter.submitList(items);
        binding.tvScheduleCount.setText(getString(R.string.calendar_event_count, items.size()));
        latestSchedules = schedules;
        renderHero(schedules);
    }

    enum HeroState { UPCOMING, DEPARTING, EMPTY_NONE, EMPTY_DONE }

    @NonNull
    static HeroState heroStateOf(@NonNull List<Schedule> schedules, @Nullable Schedule nearest,
                                 @NonNull LocalDateTime now) {
        if (nearest == null) return schedules.isEmpty() ? HeroState.EMPTY_NONE : HeroState.EMPTY_DONE;
        // TODO - 출발할 때 판정 연결
        LocalDateTime departure = nearest.computed != null ? nearest.computed.expectedDeparture : null;
        return departure != null && !now.isBefore(departure) ? HeroState.DEPARTING : HeroState.UPCOMING;
    }

    private void renderHero(@Nullable List<Schedule> schedules) {
        binding.heroEmpty.getRoot().setVisibility(View.GONE);
        binding.heroDeparting.getRoot().setVisibility(View.GONE);
        binding.heroError.getRoot().setVisibility(View.GONE);
        binding.nextDepartureHero.setVisibility(View.GONE);
        if (schedules == null) return;     // 아직 첫 조회 전

        LocalDateTime now = LocalDateTime.now();
        heroSchedule = findNearest(schedules, now);
        HeroState state = heroStateOf(schedules, heroSchedule, now);
        if (state == HeroState.EMPTY_NONE || state == HeroState.EMPTY_DONE) {
            binding.heroEmpty.tvEmptyBody.setText(state == HeroState.EMPTY_DONE
                    ? R.string.hero_empty_body_done : R.string.hero_empty_body_none);
            binding.heroEmpty.getRoot().setVisibility(View.VISIBLE);
            return;
        }
        Schedule s = heroSchedule;
        if (heroError) {
            binding.heroError.tvErrorEyebrow.setText(getString(R.string.hero_error_eyebrow,
                    s.arrivalTime.format(TIME_FORMAT), s.name));
            binding.heroError.getRoot().setVisibility(View.VISIBLE);
            return;
        }
        // 계산중
        if (state == HeroState.DEPARTING && !heroLoading) {
            renderDeparting(s);
            return;
        }
        binding.nextDepartureHero.setVisibility(View.VISIBLE);

        binding.tvHeroTitle.setText(getString(R.string.home_hero_title,
                s.arrivalTime.format(HERO_TIME_FORMAT), s.name, subjectParticle(s.name)));
        binding.tvHeroPlace.setText(s.place != null ? s.place.name : null);

        binding.layoutHeroCountdown.setVisibility(heroLoading ? View.GONE : View.VISIBLE);
        binding.layoutHeroCalculating.getRoot().setVisibility(heroLoading ? View.VISIBLE : View.GONE);
        if (heroLoading) {
            binding.tvHeroDeparture.setText(R.string.home_hero_departure_calculating);
            return;
        }

        // TODO - 출발 시각은 백그라운드 경로 계산 결과
        LocalDateTime departure = s.computed != null ? s.computed.expectedDeparture : null;
        if (departure == null) {
            binding.tvHeroLeftTime.setText(R.string.home_sample_left_time);
            binding.tvHeroDeparture.setText(getString(R.string.calendar_departure,
                    getString(R.string.detail_sample_departure)));
        } else {
            binding.tvHeroLeftTime.setText(formatLeft(Duration.between(now, departure)));
            binding.tvHeroDeparture.setText(getString(R.string.calendar_departure,
                    departure.toLocalTime().format(TIME_FORMAT)));
        }
    }

    // 출발할 때 Hero. 이 일정의 버스 도착 정보가 아직 없으면 불러온다 (03-L)
    private void renderDeparting(@NonNull Schedule s) {
        if (!s.key().equals(departingFor)) {
            departingFor = s.key();
            loadDeparting();
            return;     // loadDeparting 이 다시 그린다
        }
        binding.heroDeparting.getRoot().setVisibility(View.VISIBLE);
        boolean loading = departingBus == null;
        binding.heroDeparting.layoutDepartingEyebrow.setVisibility(loading ? View.GONE : View.VISIBLE);
        binding.heroDeparting.layoutDepartingCountdown.setVisibility(loading ? View.GONE : View.VISIBLE);
        binding.heroDeparting.layoutDepartingLoading.getRoot().setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            binding.heroDeparting.tvDepartingTitle.setText(R.string.hero_departing_loading_title);
            // 정류장·도보 시간은 아직 모르므로 도착 장소와 계산 중을 대신 보여준다
            binding.heroDeparting.tvDepartingStop.setText(s.place != null ? s.place.name : null);
            binding.heroDeparting.tvDepartingWalk.setText(R.string.home_hero_departure_calculating);
            return;
        }
        binding.heroDeparting.tvDepartingTitle.setText(
                getString(R.string.hero_departing_title, departingBus, departingArrivalMinutes));
        binding.heroDeparting.tvDepartingArrival.setText(
                getString(R.string.hero_departing_arrival, departingArrivalMinutes));
        binding.heroDeparting.tvDepartingStop.setText(departingStop);
        binding.heroDeparting.tvDepartingWalk.setText(getString(R.string.hero_departing_walk, departingWalkMinutes));
    }

    // 03-L: 출발할 때 Hero 에 타야 할 버스 도착 정보를 불러온다
    private void loadDeparting() {
        binding.getRoot().removeCallbacks(departingLoaded);
        departingBus = null;
        renderHero(latestSchedules);
        // TODO - api연결 구현: 실시간 버스 도착 API 로 바꾸고 결과를 applyDeparting 으로 넘긴다. 지금은 3초 뒤 샘플
        binding.getRoot().postDelayed(departingLoaded, Loading.FAKE_DELAY_MS);
    }

    private final Runnable departingLoaded = () -> {
        if (binding == null) return;
        // TODO - api연결 구현: 실제 응답 실패(10초 초과·5xx)도 오류로 처리
        if (!Network.get(requireContext()).isOnline()) {
            heroError = true;
            departingFor = null;    // 다시 시도하면 처음부터 불러온다
            renderHero(latestSchedules);
            return;
        }
        applyDeparting(getString(R.string.detail_sample_bus_number), 8, getString(R.string.realtime_sample_stop), 4);
    };

    // 출발할 때 Hero 에 버스 도착 정보를 넣는다
    private void applyDeparting(@NonNull String busNumber, int arrivalMinutes, @NonNull String stopName, int walkMinutes) {
        departingBus = busNumber;
        departingArrivalMinutes = arrivalMinutes;
        departingStop = stopName;
        departingWalkMinutes = walkMinutes;
        if (binding != null) renderHero(latestSchedules);
    }

    @Nullable
    static Schedule findNearest(@Nullable List<Schedule> schedules, @NonNull LocalDateTime now) {
        if (schedules == null) return null;
        Schedule nearest = null;
        LocalDateTime nearestAt = null;
        for (Schedule s : schedules) {
            if (s.status == ScheduleStatus.DONE || s.arrivalTime == null) continue;
            LocalDateTime at = LocalDateTime.of(s.date, s.arrivalTime);
            if (at.isBefore(now)) continue;
            if (nearestAt == null || at.isBefore(nearestAt)) {
                nearest = s;
                nearestAt = at;
            }
        }
        return nearest;
    }

    @NonNull
    private String formatLeft(@NonNull Duration left) {
        long minutes = Math.max(0, left.toMinutes());
        return minutes >= 60
                ? getString(R.string.home_left_hours_minutes, (int) (minutes / 60), (int) (minutes % 60))
                : getString(R.string.home_left_minutes, (int) minutes);
    }

    // 받침이 있으면 "이", 없으면 "가" (한글이 아니면 "가")
    @NonNull
    static String subjectParticle(@Nullable String word) {
        if (word == null || word.isEmpty()) return "가";
        char last = word.charAt(word.length() - 1);
        if (last < 0xAC00 || last > 0xD7A3) return "가";
        return (last - 0xAC00) % 28 == 0 ? "가" : "이";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.getRoot().removeCallbacks(heroLoaded);
        binding.getRoot().removeCallbacks(departingLoaded);
        departingFor = null;
        firstLoad.cancel();
        heroLoading = false;
        binding = null;
        scheduleAdapter = null;
        observedDate = null;
        todaySchedules = null;
        latestSchedules = null;
        heroSchedule = null;
    }
}
