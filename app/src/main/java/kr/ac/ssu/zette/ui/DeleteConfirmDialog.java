package kr.ac.ssu.zette.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.data.local.ScheduleRepository;
import kr.ac.ssu.zette.data.local.ZetteData;
import kr.ac.ssu.zette.databinding.DialogDeleteConfirmBinding;
import kr.ac.ssu.zette.dto.ScheduleKey;

// 삭제 최종 확인 (Figma 24). [삭제하기]를 눌렀을 때만 onDelete 를 부른다
public final class DeleteConfirmDialog {

    private DeleteConfirmDialog() {
    }

    // 일정 삭제. 루틴 회차면 "이 일정만 삭제"(그날만) / "루틴 전체 삭제"를 고르는 창(24-1)을 띄운다
    public static void confirmSchedule(@NonNull Context context, @NonNull ScheduleKey key) {
        ScheduleRepository repository = ZetteData.get(context).schedules;
        if (key.isOccurrence()) {
            OccurrenceChoiceDialog.show(context, key, true, wholeRoutine -> {
                if (wholeRoutine) {
                    repository.deleteRoutine(key.routineId, null);
                } else {
                    repository.deleteSchedule(key, null);
                }
            });
            return;
        }
        show(context, R.string.delete_schedule_title, R.string.delete_schedule_message,
                () -> repository.deleteSchedule(key, null));
    }

    public static void show(@NonNull Context context, @StringRes int title, @StringRes int message,
                            @NonNull Runnable onDelete) {
        show(context, context.getString(title), context.getString(message), onDelete);
    }

    // 문구에 이름 등을 넣어야 할 때 (예: 루틴 삭제 '출근 준비')
    public static void show(@NonNull Context context, @NonNull CharSequence title, @NonNull CharSequence message,
                            @NonNull Runnable onDelete) {
        show(context, title, message, R.string.detail_delete, onDelete);
    }

    // 빨간 버튼 문구를 바꿀 때 (예: 데이터 초기화 [초기화])
    public static void show(@NonNull Context context, @NonNull CharSequence title, @NonNull CharSequence message,
                            @StringRes int confirm, @NonNull Runnable onDelete) {
        DialogDeleteConfirmBinding binding = DialogDeleteConfirmBinding.inflate(LayoutInflater.from(context));
        binding.tvTitle.setText(title);
        binding.tvMessage.setText(message);
        binding.btnDelete.setText(confirm);

        AlertDialog dialog = cardDialog(context, binding.getRoot());

        binding.btnCancel.setOnClickListener(v -> dialog.dismiss());
        binding.btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            onDelete.run();
        });
        dialog.show();
    }

    // 카드 모양은 레이아웃이 그리므로 다이얼로그 배경은 비운다. 좌우 24dp 여백은 시안 그대로 (앱의 카드형 다이얼로그 공통)
    @NonNull
    public static AlertDialog cardDialog(@NonNull Context context, @NonNull View content) {
        int inset = context.getResources().getDimensionPixelSize(R.dimen.space_4);
        return new MaterialAlertDialogBuilder(context)
                .setView(content)
                .setBackground(new ColorDrawable(Color.TRANSPARENT))
                .setBackgroundInsetStart(inset)
                .setBackgroundInsetEnd(inset)
                .create();
    }
}
