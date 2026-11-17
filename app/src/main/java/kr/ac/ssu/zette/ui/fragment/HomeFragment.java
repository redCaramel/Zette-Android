package kr.ac.ssu.zette.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kr.ac.ssu.zette.databinding.FragmentHomeBinding;
import kr.ac.ssu.zette.dto.Place;
import kr.ac.ssu.zette.dto.Schedule;
import kr.ac.ssu.zette.ui.adapter.ScheduleAdapter;
import kr.ac.ssu.zette.ui.item.ScheduleCardItem;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ScheduleAdapter scheduleAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);


        // TODO - This is sample input. Apply Background Module.
        binding.tvHeroTitle.setText("오후 3시 00분에\n팀 프로젝트 회의가 있어요!");
        binding.tvHeroLeftTime.setText("1시간 20분");
        binding.tvHeroPlace.setText("숭실대학교 정보과학관");
        binding.tvHeroDeparture.setText("출발 오후 2:15");

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        scheduleAdapter = new ScheduleAdapter(new ScheduleAdapter.OnScheduleActionListener() {
            @Override
            public void onItemClick(@NonNull ScheduleCardItem item) {
            }

            @Override
            public void onStatusToggle(@NonNull ScheduleCardItem item) {
                ScheduleCardItem.Status next = item.status == ScheduleCardItem.Status.SCHEDULED
                        ? ScheduleCardItem.Status.UNRESERVED
                        : ScheduleCardItem.Status.SCHEDULED;
                replaceItem(item, item.withStatus(next));
            }

            @Override
            public void onCompleteClick(@NonNull ScheduleCardItem item) {
                replaceItem(item, new ScheduleCardItem(item.schedule, ScheduleCardItem.Status.DONE,
                        LocalTime.now().truncatedTo(ChronoUnit.MINUTES),
                        item.travelMinutes, item.boardingRoute));
            }
        });
        binding.rvSchedule.setAdapter(scheduleAdapter);
        scheduleAdapter.submitList(insertSampleItems());
    }

    private void replaceItem(@NonNull ScheduleCardItem oldItem, @NonNull ScheduleCardItem newItem) {
        List<ScheduleCardItem> items = new ArrayList<>(scheduleAdapter.getCurrentList());
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).schedule.id == oldItem.schedule.id) {
                items.set(i, newItem);
                break;
            }
        }
        scheduleAdapter.submitList(items);
    }

    // TODO - 로컬 + 백그라운드 모듈 연결 (당일 일정) / 아래 sample 코드는 삭제할 것
    @NonNull
    private static List<ScheduleCardItem> insertSampleItems() {
        LocalDate today = LocalDate.now();
        return Arrays.asList(
                new ScheduleCardItem(
                        sampleSchedule(1, "치과 예약", today, LocalTime.of(9, 30), "신촌역 3번 출구"),
                        ScheduleCardItem.Status.DONE, LocalTime.of(9, 26), 25, null),
                new ScheduleCardItem(
                        sampleSchedule(2, "점심 약속", today, LocalTime.of(13, 0), "합정역 4번 출구"),
                        ScheduleCardItem.Status.MOVING, LocalTime.of(12, 56), null, "472번"),
                new ScheduleCardItem(
                        sampleSchedule(3, "학과 면담", today, LocalTime.of(14, 0), "서울대입구역 3번 출구"),
                        ScheduleCardItem.Status.LATE, null, 45, null),
                new ScheduleCardItem(
                        sampleSchedule(4, "팀 프로젝트 회의", today, LocalTime.of(15, 0), "강남역 2번 출구"),
                        ScheduleCardItem.Status.SCHEDULED, LocalTime.of(14, 15), 40, null),
                new ScheduleCardItem(
                        sampleSchedule(5, "동아리 회식", today, LocalTime.of(19, 0), "건대입구역 1번 출구"),
                        ScheduleCardItem.Status.UNRESERVED, LocalTime.of(18, 25), 35, null)
        );
    }

    @NonNull
    private static Schedule sampleSchedule(long id, String name, LocalDate date, LocalTime time,
                                           String placeName) {
        Place place = new Place(String.valueOf(id), placeName, "", 0, 0, null);
        return new Schedule(id, name, date, time, place, Schedule.RouteType.MinTime, 10);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        scheduleAdapter = null;
    }
}
