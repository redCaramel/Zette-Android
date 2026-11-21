package kr.ac.ssu.zette.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.data.local.ScheduleRepository;
import kr.ac.ssu.zette.data.local.ZetteData;
import kr.ac.ssu.zette.databinding.FragmentRoutineBinding;
import kr.ac.ssu.zette.dto.Routine;
import kr.ac.ssu.zette.ui.DeleteConfirmDialog;
import kr.ac.ssu.zette.ui.activity.EditRoutineActivity;
import kr.ac.ssu.zette.ui.adapter.RoutineAdapter;
import kr.ac.ssu.zette.ui.item.RoutineItem;

public class RoutineFragment extends Fragment {

    private FragmentRoutineBinding binding;
    private RoutineAdapter routineAdapter;
    private ScheduleRepository repository;

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
        repository = ZetteData.get(requireContext()).schedules;

        routineAdapter = new RoutineAdapter(new RoutineAdapter.OnRoutineActionListener() {
            @Override
            public void onEditClick(@NonNull RoutineItem item) {
                startActivity(EditRoutineActivity.intent(requireContext(), item.id));
            }

            @Override
            public void onDeleteClick(@NonNull RoutineItem item) {
                DeleteConfirmDialog.show(requireContext(), getString(R.string.delete_routine_title),
                        getString(R.string.delete_routine_message, item.name),
                        () -> repository.deleteRoutine(item.id, null));
            }

            @Override
            public void onRoutineToggle(@NonNull RoutineItem item, boolean enabled) {
                repository.setRoutineActivated(item.id, enabled);
            }
        });
        binding.rvRoutines.setAdapter(routineAdapter);

        repository.observeRoutines().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(@Nullable List<Routine> routines) {
        List<RoutineItem> items = new ArrayList<>();
        if (routines != null) {
            for (Routine r : routines) items.add(RoutineItem.from(r));
        }
        routineAdapter.submitList(items);
        binding.tvRoutineCount.setText(getString(R.string.routine_count, items.size()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        routineAdapter = null;
    }
}
