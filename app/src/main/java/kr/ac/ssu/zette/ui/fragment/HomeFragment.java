package kr.ac.ssu.zette.ui.fragment;

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
import kr.ac.ssu.zette.dto.ScheduleStatus;
import kr.ac.ssu.zette.ui.Loading;
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
    private final Runnable heroLoaded = () -> {
        heroLoading = false;
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

        binding.nextDepartureHero.setOnClickListener(v -> {
            if (heroSchedule != null) {
                startActivity(RealtimeArrivalActivity.intent(requireContext(), heroSchedule.key()));
            }
        });

        firstLoad = new Loading.Gate(binding.heroSkeleton.getRoot(), binding.layoutScheduleSkeleton);
        firstLoad.start();

        binding.layoutHeroCalculating.tvLoading.setText(R.string.home_hero_calculating);
        binding.layoutHeroCalculating.tvLoading.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_on_dark_soft));
        // TODO - api연결 구현: 출발 시각 계산 응답 대신 3초 대기
        heroLoading = true;
        binding.getRoot().postDelayed(heroLoaded, Loading.FAKE_DELAY_MS);
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalDate today = LocalDate.now();
        if (!today.equals(observedDate)) {
            observeDay(today);
        } else {
            renderHero(latestSchedules);   // 남은 시간과 '가장 가까운 일정'은 시간이 지나면 바뀐다
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

    // Hero = 오늘 아직 도착 시각이 지나지 않은 일정 중 가장 가까운 것. 없으면 숨긴다
    private void renderHero(@Nullable List<Schedule> schedules) {
        heroSchedule = findNearest(schedules, LocalDateTime.now());
        if (heroSchedule == null) {
            binding.nextDepartureHero.setVisibility(View.GONE);
            return;
        }
        binding.nextDepartureHero.setVisibility(View.VISIBLE);

        Schedule s = heroSchedule;
        binding.tvHeroTitle.setText(getString(R.string.home_hero_title,
                s.arrivalTime.format(HERO_TIME_FORMAT), s.name, subjectParticle(s.name)));
        binding.tvHeroPlace.setText(s.place != null ? s.place.name : null);

        binding.layoutHeroCountdown.setVisibility(heroLoading ? View.GONE : View.VISIBLE);
        binding.layoutHeroCalculating.getRoot().setVisibility(heroLoading ? View.VISIBLE : View.GONE);
        if (heroLoading) {
            binding.tvHeroDeparture.setText(R.string.home_hero_departure_calculating);
            return;
        }

        // TODO - 출발 시각은 백그라운드 경로 계산 결과. 아직 없으면 샘플을 보여준다
        LocalDateTime departure = s.computed != null ? s.computed.expectedDeparture : null;
        if (departure == null) {
            binding.tvHeroLeftTime.setText(R.string.home_sample_left_time);
            binding.tvHeroDeparture.setText(getString(R.string.calendar_departure,
                    getString(R.string.detail_sample_departure)));
        } else {
            binding.tvHeroLeftTime.setText(formatLeft(Duration.between(LocalDateTime.now(), departure)));
            binding.tvHeroDeparture.setText(getString(R.string.calendar_departure,
                    departure.toLocalTime().format(TIME_FORMAT)));
        }
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
