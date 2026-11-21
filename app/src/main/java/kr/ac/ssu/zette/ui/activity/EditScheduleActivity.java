package kr.ac.ssu.zette.ui.activity;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;


import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.data.local.ScheduleRepository;
import kr.ac.ssu.zette.data.local.ZetteData;
import kr.ac.ssu.zette.dto.Place;
import kr.ac.ssu.zette.dto.Schedule;
import kr.ac.ssu.zette.dto.ScheduleKey;
import kr.ac.ssu.zette.ui.OccurrenceChoiceDialog;

// 새 일정 폼을 기존 일정 값으로 채워 수정한다. 루틴 회차면 그날만 바뀐다 (editOccurrence)
public class EditScheduleActivity extends NewScheduleActivity {

    private static final String EXTRA_KEY = "schedule_key";

    private ScheduleKey key;
    @Nullable
    private Schedule original;

    @NonNull
    public static Intent intent(@NonNull Context context, @NonNull ScheduleKey key) {
        return new Intent(context, EditScheduleActivity.class).putExtra(EXTRA_KEY, key.encode());
    }

    // 수정 화면을 연다. 루틴 회차면 "이 일정만 수정"(그날만) / "루틴 전체 수정"을 먼저 고른다
    public static void open(@NonNull Context context, @NonNull ScheduleKey key) {
        if (!key.isOccurrence()) {
            context.startActivity(intent(context, key));
            return;
        }
        OccurrenceChoiceDialog.show(context, key, false, wholeRoutine -> context.startActivity(wholeRoutine
                ? EditRoutineActivity.intent(context, key.routineId)
                : intent(context, key)));
    }

    @Override
    protected void onSetupMode() {
        binding.tvToolbarTitle.setText(R.string.edit_schedule_title);

        String encodedKey = getIntent().getStringExtra(EXTRA_KEY);
        if (encodedKey == null) {
            finish();
            return;
        }
        key = ScheduleKey.decode(encodedKey);

        // 처음 한 번만 폼을 채운다 (편집 중에 저장소가 바뀌어도 입력을 덮어쓰지 않는다)
        binding.btnSave.setEnabled(false);
        LiveData<Schedule> source = ZetteData.get(this).schedules.observeSchedule(key);
        source.observe(this, new Observer<Schedule>() {
            @Override
            public void onChanged(Schedule s) {
                source.removeObserver(this);
                if (s == null) {
                    finish();
                    return;
                }
                original = s;
                fillForm(s.name, s.date, s.arrivalTime, s.place, s.routeType, s.arrivalBuffer);
                binding.btnSave.setEnabled(true);
            }
        });
    }

    @Override
    protected void save() {
        if (original == null) return;
        String name = requireName(R.string.new_schedule_name_required);
        if (name == null) return;
        Place place = requirePlace();
        if (place == null) return;

        // 상태·알림·계산 결과 같은 나머지 값은 기존 일정 것을 그대로 둔다
        Schedule edited = original;
        edited.name = name;
        edited.date = date;
        edited.arrivalTime = arrivalTime;
        edited.place = place;
        edited.routeType = getSelectedRouteType();
        edited.arrivalBuffer = getSelectedArrivalBuffer();

        binding.btnSave.setEnabled(false);
        ScheduleRepository repository = ZetteData.get(this).schedules;
        ScheduleRepository.Callback<Void> done = ignored -> {
            if (isFinishing() || isDestroyed()) return;
            finish();
        };
        if (key.isOccurrence()) {
            repository.editOccurrence(key, edited, done);
        } else {
            repository.updateSchedule(edited, done);
        }
    }
}
