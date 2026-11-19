package kr.ac.ssu.zette.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.data.local.ScheduleRepository;
import kr.ac.ssu.zette.data.local.ZetteData;
import kr.ac.ssu.zette.databinding.FragmentHomeBinding;
import kr.ac.ssu.zette.dto.Schedule;
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
        repository = ZetteData.get(requireContext()).schedules;

        scheduleAdapter = new ScheduleAdapter(new ScheduleAdapter.OnScheduleActionListener() {
            @Override
            public void onItemClick(@NonNull ScheduleCardItem item) {
                // TODO - 일정 상세 화면 이동 (Intent extra: item.key().encode())
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
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalDate today = LocalDate.now();
        if (!today.equals(observedDate)) {
            observeDay(today);
        }
    }

    private void observeDay(@NonNull LocalDate date) {
        if (todaySchedules != null) {
            todaySchedules.removeObservers(getViewLifecycleOwner());
        }
        observedDate = date;
        todaySchedules = repository.observeSchedulesOn(date);
        todaySchedules.observe(getViewLifecycleOwner(), this::render);
    }

    private void render(@Nullable List<Schedule> schedules) {
        List<ScheduleCardItem> items = new ArrayList<>();
        if (schedules != null) {
            for (Schedule s : schedules) items.add(ScheduleCardItem.from(s));
        }
        scheduleAdapter.submitList(items);
        binding.tvScheduleCount.setText(getString(R.string.calendar_event_count, items.size()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        scheduleAdapter = null;
        observedDate = null;
        todaySchedules = null;
    }
}
