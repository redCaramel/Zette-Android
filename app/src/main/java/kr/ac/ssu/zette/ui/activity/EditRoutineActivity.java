package kr.ac.ssu.zette.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import java.util.List;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.data.local.ZetteData;
import kr.ac.ssu.zette.dto.Routine;

// 새 루틴 폼을 기존 루틴 값으로 채워 수정
public class EditRoutineActivity extends NewRoutineActivity {

    private static final String EXTRA_ROUTINE_ID = "routine_id";

    private long routineId;
    @Nullable
    private Routine original;

    @NonNull
    public static Intent intent(@NonNull Context context, long routineId) {
        return new Intent(context, EditRoutineActivity.class).putExtra(EXTRA_ROUTINE_ID, routineId);
    }

    @Override
    protected void onSetupMode() {
        super.onSetupMode();
        binding.tvToolbarTitle.setText(R.string.edit_routine_title);
        binding.btnSave.setText(R.string.edit_routine_save);
        binding.tvEditBanner.setText(R.string.edit_routine_banner);
        binding.layoutEditBanner.setVisibility(View.VISIBLE);

        routineId = getIntent().getLongExtra(EXTRA_ROUTINE_ID, -1);
        if (routineId < 0) {
            finish();
            return;
        }

        binding.btnSave.setEnabled(false);
        LiveData<List<Routine>> source = ZetteData.get(this).schedules.observeRoutines();
        source.observe(this, new Observer<List<Routine>>() {
            @Override
            public void onChanged(List<Routine> routines) {
                source.removeObserver(this);
                original = find(routines, routineId);
                if (original == null) {
                    finish();
                    return;
                }
                fillRoutine(original);
                binding.btnSave.setEnabled(true);
            }
        });
    }

    @Override
    protected void save() {
        if (original == null) return;
        Routine edited = buildRoutine();
        if (edited == null) return;

        // 켜짐 상태와 회차 계산 기준(createdAt)은 기존 값을 그대로 둔다
        edited.id = original.id;
        edited.activated = original.activated;
        edited.createdAt = original.createdAt;

        binding.btnSave.setEnabled(false);
        ZetteData.get(this).schedules.updateRoutine(edited, ignored -> onSaved(true));
    }

    @Nullable
    private static Routine find(@Nullable List<Routine> routines, long id) {
        if (routines == null) return null;
        for (Routine r : routines) {
            if (r.id == id) return r;
        }
        return null;
    }
}
