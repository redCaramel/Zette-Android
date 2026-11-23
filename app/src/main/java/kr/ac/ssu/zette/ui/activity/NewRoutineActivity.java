package kr.ac.ssu.zette.ui.activity;

import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.tabs.TabLayout;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Collections;
import java.util.Locale;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.data.local.Settings;
import kr.ac.ssu.zette.data.local.SettingsStore;
import kr.ac.ssu.zette.data.local.ZetteData;
import kr.ac.ssu.zette.dto.Place;
import kr.ac.ssu.zette.dto.Routine;
import kr.ac.ssu.zette.dto.Schedule;

// 새 일정 폼을 그대로 쓰고, 날짜 대신 반복(요일/매월)을 받아 루틴으로 저장한다 (Figma 15·16)
public class NewRoutineActivity extends NewScheduleActivity {

    private static final int TAB_WEEKLY = 0;
    private static final int WEEKDAY_SIZE_DP = 40;
    // 화면 표시 순서는 일요일부터, 저장(weekdays)은 [0]=월 … [6]=일
    private static final DayOfWeek[] DISPLAY_ORDER = {
            DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
    };

    private final boolean[] weekdays = new boolean[7];
    private int monthDay = LocalDate.now().getDayOfMonth();

    @Override
    protected void onSetupMode() {
        binding.tvToolbarTitle.setText(R.string.new_routine_title);
        binding.tvInfoTitle.setText(R.string.new_routine_info_section);
        binding.tilName.setHint(getString(R.string.new_routine_name_label));
        binding.btnSave.setText(R.string.new_routine_save);

        binding.layoutRepeatSection.setVisibility(View.VISIBLE);
        binding.tilDate.setVisibility(View.GONE);
        ((LinearLayout.LayoutParams) binding.tilTime.getLayoutParams()).setMarginStart(0);
        binding.tilTime.setHelperText(getString(R.string.new_routine_time_helper));
        binding.tilTime.setHelperTextColor(ColorStateList.valueOf(getColor(R.color.text_secondary)));
        binding.layoutNonsupportBand.setVisibility(View.GONE);

        setupWeekdayToggles();
        setupRepeatTabs();
        setupRepeatDay();
        renderDefaultNote();
    }

    @Override
    protected int customBufferMessage() {
        return R.string.buffer_custom_message_routine;
    }

    // 루틴 화면(15·16)에는 예상 경로 카드가 없다
    @Override
    public void setEstimatedRoute(@Nullable Integer travelMinutes) {
        binding.cardEstimatedRoute.setVisibility(View.GONE);
    }

    @Override
    protected void save() {
        Routine routine = buildRoutine();
        if (routine == null) return;

        binding.btnSave.setEnabled(false);
        ZetteData.get(this).schedules.addRoutine(routine, id -> onSaved(id != null));
    }

    // 폼 값을 검사해 루틴을 만든다. 빠진 값이 있으면 안내하고 null (id 는 0)
    @Nullable
    protected Routine buildRoutine() {
        String name = requireName(R.string.new_routine_name_required);
        if (name == null) return null;
        boolean weekly = binding.tabRepeat.getSelectedTabPosition() == TAB_WEEKLY;
        if (weekly && !anySelected(weekdays)) {
            Toast.makeText(this, R.string.new_routine_weekdays_required, Toast.LENGTH_SHORT).show();
            return null;
        }
        Place place = requirePlace();
        if (place == null) return null;

        return new Routine(0, name,
                weekly ? Routine.RepeatType.WEEKLY : Routine.RepeatType.MONTHLY,
                arrivalTime, place, getSelectedRouteType(), getSelectedArrivalBuffer(),
                weekly ? weekdays.clone() : null,
                weekly ? null : Collections.singletonList(monthDay));
    }

    // 저장 결과: 성공이면 닫고, 실패면 다시 누를 수 있게 한다
    protected void onSaved(boolean success) {
        if (isFinishing() || isDestroyed()) return;
        if (!success) {
            binding.btnSave.setEnabled(true);
            Toast.makeText(this, R.string.new_schedule_save_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        finish();
    }

    // 수정 화면이 기존 루틴으로 폼을 채운다
    protected void fillRoutine(@NonNull Routine r) {
        fillForm(r.name, null, r.arrivalTime, r.place, r.routeType, r.arrivalBuffer);

        boolean weekly = r.repeatType != Routine.RepeatType.MONTHLY;
        if (weekly) {
            for (int i = 0; i < weekdays.length; i++) {
                weekdays[i] = r.weekdays != null && i < r.weekdays.length && r.weekdays[i];
            }
        } else if (r.repeatDate != null && !r.repeatDate.isEmpty()) {
            monthDay = r.repeatDate.get(0);   // 화면은 하루만 고른다
        }
        for (int i = 0; i < DISPLAY_ORDER.length; i++) {
            View toggle = ((ViewGroup) binding.layoutWeekdayToggles.getChildAt(i)).getChildAt(0);
            toggle.setSelected(weekdays[DISPLAY_ORDER[i].getValue() - 1]);
        }
        renderRepeatDay();
        TabLayout.Tab tab = binding.tabRepeat.getTabAt(weekly ? TAB_WEEKLY : TAB_WEEKLY + 1);
        if (tab != null) tab.select();
    }

    private void setupWeekdayToggles() {
        int size = Math.round(WEEKDAY_SIZE_DP * getResources().getDisplayMetrics().density);
        ColorStateList textColor = getColorStateList(R.color.weekday_toggle_text);
        for (DayOfWeek day : DISPLAY_ORDER) {
            TextView toggle = new TextView(this);
            toggle.setLayoutParams(new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
            toggle.setGravity(Gravity.CENTER);
            toggle.setBackgroundResource(R.drawable.bg_weekday_toggle);
            toggle.setTextAppearance(R.style.TextAppearance_Zette_Label);
            toggle.setTextColor(textColor);
            toggle.setText(day.getDisplayName(TextStyle.SHORT, Locale.KOREAN));
            toggle.setContentDescription(day.getDisplayName(TextStyle.FULL, Locale.KOREAN));
            int index = day.getValue() - 1;
            toggle.setOnClickListener(v -> {
                weekdays[index] = !weekdays[index];
                v.setSelected(weekdays[index]);
            });

            FrameLayout cell = new FrameLayout(this);
            cell.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            cell.addView(toggle);
            binding.layoutWeekdayToggles.addView(cell);
        }
    }

    private void setupRepeatTabs() {
        binding.tabRepeat.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                boolean weekly = tab.getPosition() == TAB_WEEKLY;
                binding.layoutWeeklyRepeat.setVisibility(weekly ? View.VISIBLE : View.GONE);
                binding.tilRepeatDay.setVisibility(weekly ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    // 반복 일은 날짜 선택 다이얼로그에서 고른 날짜의 '일'만 쓴다
    private void setupRepeatDay() {
        View.OnClickListener open = v -> {
            LocalDate now = LocalDate.now();
            LocalDate initial = now.withDayOfMonth(Math.min(monthDay, now.lengthOfMonth()));
            showDatePicker(initial, false, picked -> {
                monthDay = picked.getDayOfMonth();
                renderRepeatDay();
            });
        };
        binding.etRepeatDay.setOnClickListener(open);
        binding.tilRepeatDay.setEndIconOnClickListener(open);
        renderRepeatDay();
    }

    private void renderRepeatDay() {
        binding.etRepeatDay.setText(getString(R.string.new_routine_repeat_day_value, monthDay));
    }

    private void renderDefaultNote() {
        Settings settings = SettingsStore.getInstance(this).get();
        int routeLabel = settings.defaultRouteType == Schedule.RouteType.MinWalk
                ? R.string.setting_route_type_min_walk
                : settings.defaultRouteType == Schedule.RouteType.MinTransfer
                ? R.string.setting_route_type_min_transfer
                : R.string.setting_route_type_min_time;
        String bufferLabel = settings.defaultArrivalBufferMinutes > 0
                ? getString(R.string.setting_arrival_buffer_value, settings.defaultArrivalBufferMinutes)
                : getString(R.string.setting_arrival_buffer_none);
        binding.tvRouteNote.setText(getString(R.string.new_routine_default_note,
                getString(routeLabel), bufferLabel));
    }

    private static boolean anySelected(boolean[] days) {
        for (boolean d : days) {
            if (d) return true;
        }
        return false;
    }
}
