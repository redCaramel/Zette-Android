package kr.ac.ssu.zette.ui;

import android.content.Context;
import android.view.LayoutInflater;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.databinding.DialogShareBinding;
import kr.ac.ssu.zette.dto.ScheduleKey;

// 일정·루틴 공유 (Figma 12-1, 14-2)
public final class ShareDialog {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN);

    private ShareDialog() {
    }

    public static void showSchedule(@NonNull Context context, @NonNull ScheduleKey key, @NonNull String name,
                                    @NonNull LocalDate date, @NonNull LocalTime time, @Nullable String place) {
        String when = context.getString(R.string.share_until, date.format(DATE_FORMAT), time.format(TIME_FORMAT));
        // TODO - 공유 구현: key 로 일정 링크 생성
        show(context, R.string.share_schedule_title, R.string.share_schedule_label, name,
                R.drawable.ic_schedule, when, place, R.string.share_schedule_note);
    }

    public static void showRoutine(@NonNull Context context, long routineId, @NonNull String name,
                                   @NonNull String repeat, @NonNull LocalTime time, @Nullable String place) {
        String when = context.getString(R.string.share_until, repeat, time.format(TIME_FORMAT));
        // TODO - 공유 구현: routineId 로 루틴 링크 생성
        show(context, R.string.share_routine_title, R.string.share_routine_label, name,
                R.drawable.ic_bottom_nav_routine, when, place, R.string.share_routine_note);
    }

    private static void show(@NonNull Context context, @StringRes int title, @StringRes int label,
                             @NonNull String name, @DrawableRes int whenIcon, @NonNull String when,
                             @Nullable String place, @StringRes int note) {
        DialogShareBinding binding = DialogShareBinding.inflate(LayoutInflater.from(context));
        binding.tvTitle.setText(title);
        binding.tvLabel.setText(label);
        binding.tvName.setText(name);
        binding.ivWhen.setImageResource(whenIcon);
        binding.tvWhen.setText(when);
        binding.tvPlace.setText(place);
        binding.tvNote.setText(note);

        AlertDialog dialog = DeleteConfirmDialog.cardDialog(context, binding.getRoot());
        binding.btnKakao.setOnClickListener(v -> {
            // TODO - 공유 구현: 카카오톡 공유
            dialog.dismiss();
        });
        binding.btnCopyLink.setOnClickListener(v -> {
            // TODO - 공유 구현: 링크 클립보드 복사
            dialog.dismiss();
        });
        binding.btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
