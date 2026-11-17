package kr.ac.ssu.zette.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.databinding.FragmentRoutineBinding;
import kr.ac.ssu.zette.ui.adapter.RoutineAdapter;
import kr.ac.ssu.zette.ui.item.RoutineItem;

public class RoutineFragment extends Fragment {

    private FragmentRoutineBinding binding;
    private RoutineAdapter routineAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRoutineBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        routineAdapter = new RoutineAdapter(new RoutineAdapter.OnRoutineActionListener() {
            @Override
            public void onRoutineClick(@NonNull RoutineItem item) {
            }

            @Override
            public void onRoutineToggle(@NonNull RoutineItem item, boolean enabled) {
                List<RoutineItem> items = new ArrayList<>(routineAdapter.getCurrentList());
                for (int i = 0; i < items.size(); i++) {
                    if (items.get(i).id == item.id) {
                        items.set(i, item.withEnabled(enabled));
                        break;
                    }
                }
                routineAdapter.submitList(items);
            }
        });
        binding.rvRoutines.setAdapter(routineAdapter);

        List<RoutineItem> samples = createSampleRoutines();
        routineAdapter.submitList(samples);
        binding.tvRoutineCount.setText(getString(R.string.routine_count, samples.size()));
    }

    @NonNull
    private static List<RoutineItem> createSampleRoutines() {
        return Arrays.asList(
                RoutineItem.weekly(1, "출근 준비",
                        EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                        LocalTime.of(8, 30), "서울대입구역 3번 출구", LocalTime.of(7, 45), 35, true),
                RoutineItem.weekly(2, "주말 운동",
                        EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                        LocalTime.of(10, 0), "올림픽공원 1번 출구", LocalTime.of(9, 10), 45, true),
                RoutineItem.monthly(3, "월간 정산 회의", 15,
                        LocalTime.of(14, 0), "여의도역 5번 출구", LocalTime.of(13, 10), 45, false)
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        routineAdapter = null;
    }
}
