package kr.ac.ssu.zette.ui;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.databinding.DialogBufferCustomBinding;
import kr.ac.ssu.zette.databinding.DialogBufferDefaultBinding;
import kr.ac.ssu.zette.databinding.DialogRouteTypeBinding;
import kr.ac.ssu.zette.databinding.ViewBufferStepperBinding;
import kr.ac.ssu.zette.dto.Schedule;

// 경로 설정 다이얼로그: 도착 여유 직접 정하기 (Figma 15-2), 설정의 기본 도착 여유 (17-2)·기본 경로 기준 (17-1)
public final class BufferDialogs {

    public static final int MIN_MINUTES = 1;
    public static final int MAX_MINUTES = 60;
    public static final int CUSTOM_START = 15;   // 직접 정한 값이 없을 때 처음 보여줄 값

    private BufferDialogs() {
    }

    // 일정·루틴 폼의 [직접 정하기]. 적용하면 onApply(분), 취소·바깥 터치는 onCancel
    public static void showCustom(@NonNull Context context, int initial, @StringRes int message,
                                  @NonNull IntConsumer onApply, @NonNull Runnable onCancel) {
        DialogBufferCustomBinding b = DialogBufferCustomBinding.inflate(LayoutInflater.from(context));
        b.tvMessage.setText(message);
        int[] value = bindStepper(b.stepper, initial);

        AlertDialog dialog = DeleteConfirmDialog.cardDialog(context, b.getRoot());
        dialog.setOnCancelListener(d -> onCancel.run());
        b.btnCancel.setOnClickListener(v -> dialog.cancel());
        b.btnApply.setOnClickListener(v -> {
            dialog.dismiss();
            onApply.accept(value[0]);
        });
        dialog.show();
    }

    // 설정의 기본 도착 여유: 없음 / 5분 / 10분 / 직접 정하기(1~60분)
    public static void showDefault(@NonNull Context context, int current, @NonNull IntConsumer onConfirm) {
        DialogBufferDefaultBinding b = DialogBufferDefaultBinding.inflate(LayoutInflater.from(context));
        b.tvOption5.setText(context.getString(R.string.new_schedule_buffer_minutes, 5));
        b.tvOption10.setText(context.getString(R.string.new_schedule_buffer_minutes, 10));

        // 시안(17-2)의 옵션 안 스테퍼는 폼 다이얼로그보다 작다: 값 칸 96x48, 간격 8
        float density = context.getResources().getDisplayMetrics().density;
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) b.stepper.layoutValue.getLayoutParams();
        lp.width = Math.round(96 * density);
        lp.height = Math.round(48 * density);
        lp.setMarginStart(Math.round(8 * density));
        lp.setMarginEnd(Math.round(8 * density));
        b.stepper.layoutValue.setLayoutParams(lp);

        boolean preset = current == 0 || current == 5 || current == 10;
        int[] value = bindStepper(b.stepper, preset ? CUSTOM_START : current);
        int[] chosen = {preset ? current : -1};   // -1 = 직접 정하기

        Runnable render = () -> {
            select(b.optionNone, b.radioNone, chosen[0] == 0);
            select(b.option5, b.radio5, chosen[0] == 5);
            select(b.option10, b.radio10, chosen[0] == 10);
            select(b.optionCustom, b.radioCustom, chosen[0] < 0);
            b.layoutCustomInput.setVisibility(chosen[0] < 0 ? View.VISIBLE : View.GONE);
        };
        b.optionNone.setOnClickListener(v -> { chosen[0] = 0; render.run(); });
        b.option5.setOnClickListener(v -> { chosen[0] = 5; render.run(); });
        b.option10.setOnClickListener(v -> { chosen[0] = 10; render.run(); });
        b.optionCustom.setOnClickListener(v -> { chosen[0] = -1; render.run(); });
        render.run();

        AlertDialog dialog = DeleteConfirmDialog.cardDialog(context, b.getRoot());
        b.btnCancel.setOnClickListener(v -> dialog.dismiss());
        b.btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            onConfirm.accept(chosen[0] < 0 ? value[0] : chosen[0]);
        });
        dialog.show();
    }

    // 설정의 기본 경로 기준 (Figma 17-1): 최소 시간 / 최소 도보 / 최소 환승
    public static void showRouteType(@NonNull Context context, @NonNull Schedule.RouteType current,
                                     @NonNull Consumer<Schedule.RouteType> onConfirm) {
        DialogRouteTypeBinding b = DialogRouteTypeBinding.inflate(LayoutInflater.from(context));
        Schedule.RouteType[] chosen = {current};
        Runnable render = () -> {
            select(b.optionTime, b.radioTime, chosen[0] == Schedule.RouteType.MinTime);
            select(b.optionWalk, b.radioWalk, chosen[0] == Schedule.RouteType.MinWalk);
            select(b.optionTransfer, b.radioTransfer, chosen[0] == Schedule.RouteType.MinTransfer);
        };
        b.optionTime.setOnClickListener(v -> { chosen[0] = Schedule.RouteType.MinTime; render.run(); });
        b.optionWalk.setOnClickListener(v -> { chosen[0] = Schedule.RouteType.MinWalk; render.run(); });
        b.optionTransfer.setOnClickListener(v -> { chosen[0] = Schedule.RouteType.MinTransfer; render.run(); });
        render.run();

        AlertDialog dialog = DeleteConfirmDialog.cardDialog(context, b.getRoot());
        b.btnCancel.setOnClickListener(v -> dialog.dismiss());
        b.btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            onConfirm.accept(chosen[0]);
        });
        dialog.show();
    }

    private static void select(@NonNull ViewGroup option, @NonNull android.widget.RadioButton radio, boolean on) {
        option.setSelected(on);
        radio.setChecked(on);
    }

    // [-] 값 [+] 스테퍼. 값 칸은 키보드로도 입력한다. 1~60분 사이로만 움직이고,
    // 끝에 닿으면 그쪽 버튼을 끈다. 현재 값은 배열 [0]
    @NonNull
    private static int[] bindStepper(@NonNull ViewBufferStepperBinding s, int initial) {
        int[] value = {clamp(initial)};
        boolean[] writing = {false};   // 코드가 칸을 고칠 때 입력 감시가 다시 반응하지 않게
        Runnable renderButtons = () -> {
            s.btnMinus.setEnabled(value[0] > MIN_MINUTES);
            s.btnMinus.setAlpha(value[0] > MIN_MINUTES ? 1f : 0.38f);
            s.btnPlus.setEnabled(value[0] < MAX_MINUTES);
            s.btnPlus.setAlpha(value[0] < MAX_MINUTES ? 1f : 0.38f);
        };
        Runnable render = () -> {
            writing[0] = true;
            s.tvValue.setText(String.valueOf(value[0]));
            s.tvValue.setSelection(s.tvValue.length());
            writing[0] = false;
            renderButtons.run();
        };
        s.btnMinus.setOnClickListener(v -> { value[0] = clamp(value[0] - 1); render.run(); });
        s.btnPlus.setOnClickListener(v -> { value[0] = clamp(value[0] + 1); render.run(); });

        // 입력 중: 숫자가 있으면 바로 반영. 60 을 넘으면 60 으로 바꾼다 (지우는 중인 빈칸·0 은 그대로 둔다)
        s.tvValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence t, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence t, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable t) {
                if (writing[0] || t.length() == 0) return;
                int typed = Integer.parseInt(t.toString());   // inputType=number, 최대 2자리
                if (typed > MAX_MINUTES) {
                    value[0] = MAX_MINUTES;
                    render.run();
                    return;
                }
                if (typed >= MIN_MINUTES) value[0] = typed;
                renderButtons.run();
            }
        });
        // 입력을 마치면(완료 키·다른 곳 터치) 빈칸·0 을 마지막 올바른 값으로 되돌린다
        s.tvValue.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                render.run();
                v.clearFocus();
                v.getContext().getSystemService(InputMethodManager.class).hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
            return false;
        });
        s.tvValue.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) render.run();
        });
        render.run();
        return value;
    }

    static int clamp(int minutes) {
        return Math.max(MIN_MINUTES, Math.min(MAX_MINUTES, minutes));
    }
}
