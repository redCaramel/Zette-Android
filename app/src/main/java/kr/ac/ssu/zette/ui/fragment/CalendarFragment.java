package kr.ac.ssu.zette.ui.fragment;

import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.GridLayoutManager;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.data.local.ScheduleRepository;
import kr.ac.ssu.zette.data.local.ZetteData;
import kr.ac.ssu.zette.databinding.FragmentCalendarBinding;
import kr.ac.ssu.zette.dto.Schedule;
import kr.ac.ssu.zette.ui.DeleteConfirmDialog;
import kr.ac.ssu.zette.ui.Loading;
import kr.ac.ssu.zette.ui.ShareDialog;
import kr.ac.ssu.zette.ui.activity.EditScheduleActivity;
import kr.ac.ssu.zette.ui.activity.ScheduleDetailActivity;
import kr.ac.ssu.zette.ui.adapter.CalendarEventAdapter;
import kr.ac.ssu.zette.ui.adapter.DateCellAdapter;
import kr.ac.ssu.zette.ui.item.CalendarEventItem;

public class CalendarFragment extends Fragment {

    private static final String KEY_SELECTED_DATE = "selected_date";
    private static final String KEY_DISPLAYED_MONTH = "displayed_month";
    private static final String KEY_EXPANDED = "expanded";

    private static final int DAYS_IN_WEEK = 7;
    private static final long EXPAND_DURATION_MS = 200;
    private static final DayOfWeek[] WEEK_ORDER = {
            DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
    };
    private static final DateTimeFormatter SELECTED_DATE_FORMAT =
            DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN);

    private FragmentCalendarBinding binding;
    private DateCellAdapter dateAdapter;
    private CalendarEventAdapter eventAdapter;
    private final TextView[] weekdayViews = new TextView[DAYS_IN_WEEK];

    private LocalDate selectedDate = LocalDate.now();
    private YearMonth displayedMonth = YearMonth.from(selectedDate);
    private boolean expanded = false;

    private ScheduleRepository repository;
    @Nullable
    private LocalDate observedDay;
    @Nullable
    private YearMonth observedMonth;
    @Nullable
    private LiveData<List<Schedule>> daySchedules;
    // 12-L: 첫 일정 목록을 불러오는 동안 스켈레톤 (300ms 안에 끝나면 안 보임)
    private Loading.Gate firstLoad;
    @Nullable
    private LiveData<Set<LocalDate>> monthDates;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            selectedDate = LocalDate.ofEpochDay(savedInstanceState.getLong(KEY_SELECTED_DATE));
            displayedMonth = YearMonth.parse(savedInstanceState.getString(KEY_DISPLAYED_MONTH));
            expanded = savedInstanceState.getBoolean(KEY_EXPANDED);
        }

        repository = ZetteData.get(requireContext()).schedules;

        setupWeekdayLabels();
        setupDateStrip();
        setupEventList();
        firstLoad = new Loading.Gate(binding.layoutSkeleton);
        firstLoad.start();
        renderCalendar();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(KEY_SELECTED_DATE, selectedDate.toEpochDay());
        outState.putString(KEY_DISPLAYED_MONTH, displayedMonth.toString());
        outState.putBoolean(KEY_EXPANDED, expanded);
    }

    @NonNull
    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    private void setupWeekdayLabels() {
        LinearLayout container = binding.layoutWeekdays;
        container.removeAllViews();
        for (int i = 0; i < DAYS_IN_WEEK; i++) {
            TextView label = new TextView(requireContext());
            label.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            label.setGravity(android.view.Gravity.CENTER);
            label.setTextAppearance(R.style.TextAppearance_Zette_LabelSmall);
            label.setText(WEEK_ORDER[i].getDisplayName(TextStyle.SHORT, Locale.KOREAN));
            container.addView(label);
            weekdayViews[i] = label;
        }
    }

    private void setupDateStrip() {
        dateAdapter = new DateCellAdapter(this::selectDate);
        binding.rvDates.setLayoutManager(new GridLayoutManager(requireContext(), DAYS_IN_WEEK));
        binding.rvDates.setItemAnimator(null);
        binding.rvDates.setAdapter(dateAdapter);

        binding.btnMonthToggle.setOnClickListener(v -> setExpanded(!expanded));
        binding.btnToday.setOnClickListener(v -> selectDate(LocalDate.now()));
        binding.btnPrevMonth.setOnClickListener(v -> showMonth(displayedMonth.minusMonths(1)));
        binding.btnNextMonth.setOnClickListener(v -> showMonth(displayedMonth.plusMonths(1)));
    }

    private void setupEventList() {
        eventAdapter = new CalendarEventAdapter(new CalendarEventAdapter.OnEventActionListener() {
            @Override
            public void onItemClick(@NonNull CalendarEventItem item) {
                startActivity(ScheduleDetailActivity.intent(requireContext(), item.key));
            }

            @Override
            public void onShareClick(@NonNull CalendarEventItem item) {
                List<Schedule> shown = daySchedules != null ? daySchedules.getValue() : null;
                if (shown == null) return;
                for (Schedule s : shown) {
                    if (s.key().equals(item.key)) ShareDialog.showSchedule(requireContext(), s);
                }
            }

            @Override
            public void onEditClick(@NonNull CalendarEventItem item) {
                EditScheduleActivity.open(requireContext(), item.key);
            }

            @Override
            public void onDeleteClick(@NonNull CalendarEventItem item) {
                DeleteConfirmDialog.confirmSchedule(requireContext(), item.key);
            }

            @Override
            public void onAlarmToggle(@NonNull CalendarEventItem item, boolean on) {
                repository.setScheduleActivated(item.key, on);
            }
        });
        binding.rvEvents.setAdapter(eventAdapter);
    }
    private void observeSchedules() {
        if (!selectedDate.equals(observedDay)) {
            if (daySchedules != null) daySchedules.removeObservers(getViewLifecycleOwner());
            observedDay = selectedDate;
            daySchedules = repository.observeSchedulesOn(selectedDate);
            daySchedules.observe(getViewLifecycleOwner(), list -> firstLoad.finish(() -> renderEvents(list)));
        }

        YearMonth month = expanded ? displayedMonth : YearMonth.from(selectedDate);
        if (!month.equals(observedMonth)) {
            if (monthDates != null) monthDates.removeObservers(getViewLifecycleOwner());
            observedMonth = month;
            monthDates = repository.observeDatesWithSchedules(month);
            monthDates.observe(getViewLifecycleOwner(), dates -> {
                if (dates != null) dateAdapter.setEventDates(dates);
            });
        }
    }

    private void renderEvents(@Nullable List<Schedule> schedules) {
        List<CalendarEventItem> items = new ArrayList<>();
        String routineLabel = getString(R.string.calendar_routine_added);
        if (schedules != null) {
            for (Schedule s : schedules) items.add(CalendarEventItem.from(s, routineLabel));
        }
        eventAdapter.submitList(items);
        binding.tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        binding.tvEventCount.setText(getString(R.string.calendar_event_count, items.size()));
    }

    private void selectDate(@NonNull LocalDate date) {
        selectedDate = date;
        if (expanded && !YearMonth.from(date).equals(displayedMonth)) {
            displayedMonth = YearMonth.from(date);
        }
        renderCalendar();
    }

    private void showMonth(@NonNull YearMonth month) {
        displayedMonth = month;
        renderCalendar();
    }

    private void setExpanded(boolean expand) {
        TransitionManager.beginDelayedTransition(binding.layoutCalendarContent,
                new AutoTransition().setDuration(EXPAND_DURATION_MS));
        expanded = expand;
        displayedMonth = YearMonth.from(selectedDate);
        renderCalendar();
    }

    private void renderCalendar() {
        LocalDate today = LocalDate.now();
        List<LocalDate> dates = expanded ? monthDates(displayedMonth) : weekDates(selectedDate);
        dateAdapter.submit(dates, expanded ? displayedMonth : null, selectedDate, today);

        if (expanded) {
            binding.tvMonth.setText(getString(R.string.calendar_year_month,
                    displayedMonth.getYear(), displayedMonth.getMonthValue()));
        } else {
            binding.tvMonth.setText(getString(R.string.calendar_month, selectedDate.getMonthValue()));
        }
        binding.ivMonthToggle.setImageResource(expanded ? R.drawable.ic_expand_less : R.drawable.ic_expand_more);
        binding.btnToday.setVisibility(expanded ? View.GONE : View.VISIBLE);
        binding.layoutMonthNav.setVisibility(expanded ? View.VISIBLE : View.GONE);

        int space1 = getResources().getDimensionPixelSize(R.dimen.space_1);
        int space2 = getResources().getDimensionPixelSize(R.dimen.space_2);
        int space3 = getResources().getDimensionPixelSize(R.dimen.space_3);
        binding.layoutWeekdays.setPadding(0, space1, 0, expanded ? space1 + space2 : 0);
        binding.layoutDateStrip.setPadding(space3, space2, space3, expanded ? space3 : space2);

        int selectedColumn = selectedDate.getDayOfWeek().getValue() % DAYS_IN_WEEK;
        for (int i = 0; i < DAYS_IN_WEEK; i++) {
            boolean highlight = !expanded && i == selectedColumn;
            weekdayViews[i].setTextColor(ContextCompat.getColor(requireContext(),
                    highlight ? R.color.green_brand : R.color.text_secondary));
        }

        binding.tvSelectedDate.setText(selectedDate.format(SELECTED_DATE_FORMAT));
        observeSchedules();
    }

    @NonNull
    private static List<LocalDate> weekDates(@NonNull LocalDate date) {
        LocalDate start = date.minusDays(date.getDayOfWeek().getValue() % DAYS_IN_WEEK);
        List<LocalDate> dates = new ArrayList<>(DAYS_IN_WEEK);
        for (int i = 0; i < DAYS_IN_WEEK; i++) {
            dates.add(start.plusDays(i));
        }
        return dates;
    }

    @NonNull
    private static List<LocalDate> monthDates(@NonNull YearMonth month) {
        LocalDate first = month.atDay(1);
        int offset = first.getDayOfWeek().getValue() % DAYS_IN_WEEK;
        int weeks = (offset + month.lengthOfMonth() + DAYS_IN_WEEK - 1) / DAYS_IN_WEEK;
        LocalDate start = first.minusDays(offset);
        List<LocalDate> dates = new ArrayList<>(weeks * DAYS_IN_WEEK);
        for (int i = 0; i < weeks * DAYS_IN_WEEK; i++) {
            dates.add(start.plusDays(i));
        }
        return dates;
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        firstLoad.cancel();
        binding = null;
        dateAdapter = null;
        eventAdapter = null;
        observedDay = null;
        observedMonth = null;
        daySchedules = null;
        monthDates = null;
    }
}
