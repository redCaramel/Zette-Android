package kr.ac.ssu.zette.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.data.local.ZetteData;
import kr.ac.ssu.zette.databinding.DialogDeleteConfirmBinding;
import kr.ac.ssu.zette.dto.ScheduleKey;

// 삭제 최종 확인 (Figma 24). [삭제하기]를 눌렀을 때만 onDelete 를 부른다
public final class DeleteConfirmDialog {

    private DeleteConfirmDialog() {
    }

    // 일정 삭제. 루틴 회차면 그날만 지워지고 루틴은 남는다 (repository.deleteSchedule)
    public static void confirmSchedule(@NonNull Context context, @NonNull ScheduleKey key) {
        show(context, R.string.delete_schedule_title,
                key.isOccurrence() ? R.string.delete_occurrence_message : R.string.delete_schedule_message,
                () -> ZetteData.get(context).schedules.deleteSchedule(key, null));
    }

    public static void show(@NonNull Context context, @StringRes int title, @StringRes int message,
                            @NonNull Runnable onDelete) {
        show(context, context.getString(title), context.getString(message), onDelete);
    }

    // 문구에 이름 등을 넣어야 할 때 (예: 루틴 삭제 '출근 준비')
    public static void show(@NonNull Context context, @NonNull CharSequence title, @NonNull CharSequence message,
                            @NonNull Runnable onDelete) {
        DialogDeleteConfirmBinding binding = DialogDeleteConfirmBinding.inflate(LayoutInflater.from(context));
        binding.tvTitle.setText(title);
        binding.tvMessage.setText(message);

        // 카드 모양은 레이아웃이 그리므로 다이얼로그 배경은 비운다. 좌우 24dp 여백은 시안 그대로
        int inset = context.getResources().getDimensionPixelSize(R.dimen.space_4);
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(binding.getRoot())
                .setBackground(new ColorDrawable(Color.TRANSPARENT))
                .setBackgroundInsetStart(inset)
                .setBackgroundInsetEnd(inset)
                .create();

        binding.btnCancel.setOnClickListener(v -> dialog.dismiss());
        binding.btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            onDelete.run();
        });
        dialog.show();
    }
}
