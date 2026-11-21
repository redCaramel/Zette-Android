package kr.ac.ssu.zette.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.data.local.ZetteData;
import kr.ac.ssu.zette.databinding.DialogOccurrenceChoiceBinding;
import kr.ac.ssu.zette.dto.Routine;
import kr.ac.ssu.zette.dto.ScheduleKey;

// 루틴 회차를 수정·삭제할 때 "이 일정만" / "루틴 전체"를 고른다 (Figma 06-1, 24-1)
public final class OccurrenceChoiceDialog {

    public interface OnChosen {
        void onChosen(boolean wholeRoutine);
    }

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("M월 d일(E)", Locale.KOREAN);
    private static final String DAY_SEPARATOR = "·";

    private OccurrenceChoiceDialog() {
    }

    // context 는 Activity 여야 한다 (루틴 이름·반복을 읽는 동안 생명주기를 따른다)
    public static void show(@NonNull Context context, @NonNull ScheduleKey key, boolean delete,
                            @NonNull OnChosen onChosen) {
        if (!key.isOccurrence() || key.routineId == null || key.occurrenceDate == null) return;
        LifecycleOwner owner = (LifecycleOwner) context;
        LiveData<List<Routine>> source = ZetteData.get(context).schedules.observeRoutines();
        source.observe(owner, new Observer<List<Routine>>() {
            @Override
            public void onChanged(List<Routine> routines) {
                source.removeObserver(this);
                Routine routine = find(routines, key.routineId);
                if (routine != null) open(context, routine, key.occurrenceDate, delete, onChosen);
            }
        });
    }

    private static void open(@NonNull Context context, @NonNull Routine routine, @NonNull LocalDate date,
                             boolean delete, @NonNull OnChosen onChosen) {
        DialogOccurrenceChoiceBinding b = DialogOccurrenceChoiceBinding.inflate(LayoutInflater.from(context));
        String day = date.format(DATE_FORMAT);

        b.tvTitle.setText(delete ? R.string.choice_delete_title : R.string.choice_edit_title);
        b.tvMessage.setText(context.getString(R.string.choice_message,
                routine.name, topicParticle(routine.name), formatRepeat(context, routine)));
        b.tvThisLabel.setText(delete ? R.string.choice_delete_this : R.string.choice_edit_this);
        b.tvThisDesc.setText(context.getString(delete ? R.string.choice_delete_this_desc
                : R.string.choice_edit_this_desc, day));
        b.tvAllLabel.setText(delete ? R.string.choice_delete_all : R.string.choice_edit_all);
        b.tvAllDesc.setText(delete ? R.string.choice_delete_all_desc : R.string.choice_edit_all_desc);

        // 수정은 [다음](초록), 삭제는 [삭제](빨간 테두리)
        View confirm = delete ? b.btnDelete : b.btnNext;
        confirm.setVisibility(View.VISIBLE);

        boolean[] whole = {false};
        Runnable render = () -> {
            b.optionThis.setSelected(!whole[0]);
            b.radioThis.setChecked(!whole[0]);
            b.optionAll.setSelected(whole[0]);
            b.radioAll.setChecked(whole[0]);
        };
        b.optionThis.setOnClickListener(v -> {
            whole[0] = false;
            render.run();
        });
        b.optionAll.setOnClickListener(v -> {
            whole[0] = true;
            render.run();
        });
        render.run();

        int inset = context.getResources().getDimensionPixelSize(R.dimen.space_4);
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(b.getRoot())
                .setBackground(new ColorDrawable(Color.TRANSPARENT))
                .setBackgroundInsetStart(inset)
                .setBackgroundInsetEnd(inset)
                .create();
        b.btnCancel.setOnClickListener(v -> dialog.dismiss());
        confirm.setOnClickListener(v -> {
            dialog.dismiss();
            onChosen.onChosen(whole[0]);
        });
        dialog.show();
    }

    // "매주 월·수·금" / "매월 15일"
    @NonNull
    private static String formatRepeat(@NonNull Context context, @NonNull Routine r) {
        if (r.repeatType == Routine.RepeatType.MONTHLY) {
            List<String> days = new ArrayList<>();
            if (r.repeatDate != null) {
                for (Integer d : r.repeatDate) days.add(String.valueOf(d));
            }
            return context.getString(R.string.routine_repeat_monthly, String.join(DAY_SEPARATOR, days));
        }
        List<String> days = new ArrayList<>();
        for (DayOfWeek d : DayOfWeek.values()) {
            int i = d.getValue() - 1;
            if (r.weekdays != null && i < r.weekdays.length && r.weekdays[i]) {
                days.add(d.getDisplayName(TextStyle.SHORT, Locale.KOREAN));
            }
        }
        return context.getString(R.string.routine_repeat_weekly, String.join(DAY_SEPARATOR, days));
    }

    // 받침이 있으면 "은", 없으면 "는" (한글이 아니면 "는")
    @NonNull
    static String topicParticle(@Nullable String word) {
        if (word == null || word.isEmpty()) return "는";
        char last = word.charAt(word.length() - 1);
        if (last < 0xAC00 || last > 0xD7A3) return "는";
        return (last - 0xAC00) % 28 == 0 ? "는" : "은";
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
