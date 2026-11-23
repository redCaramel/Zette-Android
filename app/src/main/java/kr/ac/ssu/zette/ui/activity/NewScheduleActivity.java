package kr.ac.ssu.zette.ui.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.TextAppearanceSpan;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.data.local.Settings;
import kr.ac.ssu.zette.data.local.SettingsStore;
import kr.ac.ssu.zette.data.local.ZetteData;
import kr.ac.ssu.zette.databinding.ActivityNewScheduleBinding;
import kr.ac.ssu.zette.dto.Place;
import kr.ac.ssu.zette.dto.Schedule;
import kr.ac.ssu.zette.ui.BufferDialogs;
import kr.ac.ssu.zette.ui.Loading;
import kr.ac.ssu.zette.ui.adapter.PlaceSearchAdapter;

public class NewScheduleActivity extends AppCompatActivity {

    private static final boolean DEBUG_IS_NONSUPPORT = false;

    // TODO - api연결 구현: 장소 검색 응답 대신 3초 대기
    private static final long SEARCH_DELAY_MS = Loading.FAKE_DELAY_MS;
    private static final int MAX_SEARCH_RESULTS = 3;
    private static final int SAMPLE_TRAVEL_MINUTES = 40;

    private static final String TAG_DATE_PICKER = "date_picker";
    private static final String TAG_TIME_PICKER = "time_picker";

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN);

    // TODO - 샘플 데이터 삭제, 경로검색 로직 연결
    private static final List<Place> SAMPLE_PLACES = Arrays.asList(
            new Place("sample-1", "강남역 2번 출구", "서울 강남구 강남대로 지하 396", 37.4979, 127.0276, 50),
            new Place("sample-2", "강남역 11번 출구", "서울 서초구 서초동 1317-23", 37.4972, 127.0266, 120),
            new Place("sample-3", "강남역 지하쇼핑센터", "서울 강남구 역삼동 858", 37.4985, 127.0281, 210),
            new Place("sample-4", "강남역 10번 출구", "서울 강남구 역삼동 821", 37.4990, 127.0289, 350)
    );

    protected ActivityNewScheduleBinding binding;
    private PlaceSearchAdapter searchAdapter;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    @Nullable
    private Runnable pendingSearch;
    private final Runnable estimatedRouteLoaded = () -> setEstimatedRoute(SAMPLE_TRAVEL_MINUTES);

    protected LocalDate date;
    protected LocalTime arrivalTime;
    @Nullable
    protected Place selectedPlace;

    private int appliedBuffer;           // 저장할 도착 여유 (분)
    @Nullable
    private Integer customBuffer;        // [직접 정하기]로 정한 값. 칩에 "N분"으로 남는다

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityNewScheduleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right,
                    Math.max(systemBars.bottom, ime.bottom));
            return insets;
        });

        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        setupDateTime();
        setupPlaceSearch();
        setupRouteSettings();
        setupNonSupportBand();

        onSetupMode();
        binding.btnSave.setOnClickListener(v -> save());
    }

    // 하위 화면(새 루틴)이 같은 폼을 자기 모드로 바꾸는 자리
    protected void onSetupMode() {
    }

    protected void save() {
        saveSchedule();
    }

    // 수정 화면이 기존 값으로 폼을 채운다. date 가 null 이면 날짜는 그대로 둔다 (루틴)
    protected void fillForm(@Nullable String name, @Nullable LocalDate date, @NonNull LocalTime time,
                            @Nullable Place place, @Nullable Schedule.RouteType routeType, int arrivalBuffer) {
        binding.etName.setText(name);
        if (date != null) this.date = date;
        arrivalTime = time;
        renderDateTime();

        if (place != null) {
            selectedPlace = place;
            searchAdapter.setSelectedPlaceId(place.id);
            renderSelectedPlace();
            loadEstimatedRoute();
        }
        if (routeType != null) binding.chipGroupRouteType.check(routeChipId(routeType));
        renderBuffer(arrivalBuffer);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelPendingSearch();
        searchHandler.removeCallbacks(estimatedRouteLoaded);
    }

    private void setupDateTime() {
        LocalDateTime nextHour = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).plusHours(1);
        date = nextHour.toLocalDate();
        arrivalTime = nextHour.toLocalTime();
        renderDateTime();

        View.OnClickListener openDatePicker = v -> showDatePicker();
        View.OnClickListener openTimePicker = v -> showTimePicker();

        binding.etDate.setOnClickListener(openDatePicker);
        binding.tilDate.setEndIconOnClickListener(openDatePicker);
        binding.etTime.setOnClickListener(openTimePicker);
        binding.tilTime.setEndIconOnClickListener(openTimePicker);
    }

    private void showDatePicker() {
        showDatePicker(date, true, picked -> {
            date = picked;
            renderDateTime();
        });
    }

    // futureOnly: 오늘 이전 날짜를 막는다 (일정 날짜). 루틴의 반복 일은 날짜만 고르므로 false.
    // 입력 모드 토글 id는 Material의 private 리소스라 R로 참조할 수 없어 이름으로 찾는다. 버전업 때 사라지면 0이라 토글만 다시 보인다.
    @SuppressLint("DiscouragedApi")
    protected void showDatePicker(@NonNull LocalDate initial, boolean futureOnly,
                                  @NonNull Consumer<LocalDate> onPicked) {
        if (getSupportFragmentManager().findFragmentByTag(TAG_DATE_PICKER) != null) return;

        long todayUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        long selectionUtc = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder().setOpenAt(selectionUtc);
        if (futureOnly) {
            constraintsBuilder.setStart(todayUtc).setValidator(DateValidatorPointForward.from(todayUtc));
        }
        CalendarConstraints constraints = constraintsBuilder.build();

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTheme(R.style.ThemeOverlay_Zette_DatePicker)
                .setTitleText(R.string.picker_date_title)
                .setSelection(selectionUtc)
                .setCalendarConstraints(constraints)
                .setPositiveButtonText(R.string.picker_ok)
                .setNegativeButtonText(R.string.picker_cancel)
                .build();
        picker.addOnPositiveButtonClickListener(selection ->
                onPicked.accept(Instant.ofEpochMilli(selection).atZone(ZoneOffset.UTC).toLocalDate()));
        picker.getViewLifecycleOwnerLiveData().observe(this, owner -> {
            if (owner == null) return;
            int toggleId = getResources().getIdentifier("mtrl_picker_header_toggle", "id", getPackageName());
            View toggle = toggleId == 0 ? null : picker.requireView().findViewById(toggleId);
            if (toggle != null) toggle.setVisibility(View.GONE);
        });
        picker.show(getSupportFragmentManager(), TAG_DATE_PICKER);
    }

    private void showTimePicker() {
        if (getSupportFragmentManager().findFragmentByTag(TAG_TIME_PICKER) != null) return;

        SpannableStringBuilder title = new SpannableStringBuilder(getString(R.string.picker_time_title));
        int helperStart = title.length() + 1;

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTheme(R.style.ThemeOverlay_Zette_TimePicker)
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .setHour(arrivalTime.getHour())
                .setMinute(arrivalTime.getMinute())
                .setTitleText(title)
                .setPositiveButtonText(R.string.picker_ok)
                .setNegativeButtonText(R.string.picker_cancel)
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            arrivalTime = LocalTime.of(picker.getHour(), picker.getMinute());
            renderDateTime();
        });
        picker.show(getSupportFragmentManager(), TAG_TIME_PICKER);
    }

    private void renderDateTime() {
        binding.etDate.setText(date.format(DATE_FORMAT));
        binding.etTime.setText(arrivalTime.format(TIME_FORMAT));
    }

    private void setupPlaceSearch() {
        searchAdapter = new PlaceSearchAdapter(this::selectPlace);
        binding.rvSearchResults.setAdapter(searchAdapter);

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                onSearchQueryChanged(s.toString().trim());
            }
        });
        binding.btnSearchClear.setOnClickListener(v -> binding.etSearch.setText(null));

        renderSelectedPlace();
    }

    private void onSearchQueryChanged(@NonNull String query) {
        cancelPendingSearch();
        binding.btnSearchClear.setVisibility(binding.etSearch.length() > 0 ? View.VISIBLE : View.GONE);

        if (query.isEmpty()) {
            binding.cardSearchResults.setVisibility(View.GONE);
            searchAdapter.submitList(Collections.emptyList());
            return;
        }

        binding.cardSearchResults.setVisibility(View.VISIBLE);
        binding.layoutSearchLoading.setVisibility(View.VISIBLE);
        Loading.pulse(binding.layoutSearchLoading);
        binding.rvSearchResults.setVisibility(View.GONE);

        pendingSearch = () -> {
            pendingSearch = null;
            List<Place> results = searchPlaces(query);
            searchAdapter.submitList(results);
            binding.layoutSearchLoading.setVisibility(View.GONE);
            Loading.stopPulse(binding.layoutSearchLoading);
            binding.rvSearchResults.setVisibility(View.VISIBLE);
        };
        searchHandler.postDelayed(pendingSearch, SEARCH_DELAY_MS);
    }

    @NonNull
    private List<Place> searchPlaces(@NonNull String query) {
        // TODO - 경로 검색 로직 추가
        return SAMPLE_PLACES.subList(0, Math.min(MAX_SEARCH_RESULTS, SAMPLE_PLACES.size()));
    }

    private void cancelPendingSearch() {
        if (pendingSearch != null) {
            searchHandler.removeCallbacks(pendingSearch);
            pendingSearch = null;
        }
        Loading.stopPulse(binding.layoutSearchLoading);
    }

    private void selectPlace(@NonNull Place place) {
        selectedPlace = place;
        searchAdapter.setSelectedPlaceId(place.id);
        renderSelectedPlace();
        hideKeyboard();
        loadEstimatedRoute();
    }

    private void renderSelectedPlace() {
        boolean hasPlace = selectedPlace != null;
        binding.layoutSelectedPlace.setSelected(hasPlace);
        binding.ivSelectedPlace.setImageResource(hasPlace ? R.drawable.ic_place : R.drawable.ic_add_location_alt);
        binding.ivSelectedPlaceCheck.setVisibility(hasPlace ? View.VISIBLE : View.GONE);
        binding.tvSelectedPlaceName.setVisibility(hasPlace ? View.VISIBLE : View.GONE);

        if (hasPlace) {
            binding.tvSelectedPlaceName.setText(selectedPlace.name);
            binding.tvSelectedPlaceAddress.setText(selectedPlace.address);
            binding.tvSelectedPlaceAddress.setTextAppearance(R.style.TextAppearance_Zette_Caption);
        } else {
            binding.tvSelectedPlaceAddress.setText(R.string.new_schedule_selected_place_empty);
            binding.tvSelectedPlaceAddress.setTextAppearance(R.style.TextAppearance_Zette_Body);
        }
        binding.tvSelectedPlaceAddress.setTextColor(getColor(R.color.text_secondary));
    }

    @Nullable
    public Place getSelectedPlace() {
        return selectedPlace;
    }

    private void setupRouteSettings() {
        binding.chipBuffer5.setText(getString(R.string.new_schedule_buffer_minutes, 5));
        binding.chipBuffer10.setText(getString(R.string.new_schedule_buffer_minutes, 10));

        // 없음/5/10 칩을 고르면 그 값이 적용값. [직접 정하기]는 다이얼로그에서 적용해야 바뀐다 (Figma 15-2·15-3)
        binding.chipGroupArrivalBuffer.setOnCheckedStateChangeListener((group, ids) -> {
            int checked = group.getCheckedChipId();
            if (checked == R.id.chip_buffer_none) appliedBuffer = 0;
            else if (checked == R.id.chip_buffer_5) appliedBuffer = 5;
            else if (checked == R.id.chip_buffer_10) appliedBuffer = 10;
        });
        binding.chipBufferCustom.setCloseIconContentDescription(getString(R.string.buffer_edit));
        binding.chipBufferCustom.setOnClickListener(v -> openCustomBuffer());
        binding.chipBufferCustom.setOnCloseIconClickListener(v -> openCustomBuffer());

        Settings settings = SettingsStore.getInstance(this).get();
        binding.chipGroupRouteType.check(routeChipId(settings.defaultRouteType));
        renderBuffer(settings.defaultArrivalBufferMinutes);
    }

    // 직접 정하기 다이얼로그 안내 문구 ("이 일정에만" / 루틴 화면은 "이 루틴에만")
    @StringRes
    protected int customBufferMessage() {
        return R.string.buffer_custom_message_schedule;
    }

    private void openCustomBuffer() {
        BufferDialogs.showCustom(this, customBuffer != null ? customBuffer : BufferDialogs.CUSTOM_START,
                customBufferMessage(), this::renderBuffer, () -> renderBuffer(appliedBuffer));
    }

    // 없음/5/10분은 그 칩을, 나머지 값은 [N분 ✎] 칩을 고른다
    private void renderBuffer(int minutes) {
        appliedBuffer = minutes;
        if (minutes != 0 && minutes != 5 && minutes != 10) {
            customBuffer = minutes;
            binding.chipBufferCustom.setText(getString(R.string.new_schedule_buffer_minutes, minutes));
            binding.chipBufferCustom.setCloseIconVisible(true);
        }
        binding.chipGroupArrivalBuffer.check(bufferChipId(minutes));
    }

    @NonNull
    public Schedule.RouteType getSelectedRouteType() {
        int checkedId = binding.chipGroupRouteType.getCheckedChipId();
        if (checkedId == R.id.chip_route_min_walk) return Schedule.RouteType.MinWalk;
        if (checkedId == R.id.chip_route_min_transfer) return Schedule.RouteType.MinTransfer;
        return Schedule.RouteType.MinTime;
    }

    private static int routeChipId(@NonNull Schedule.RouteType routeType) {
        switch (routeType) {
            case MinWalk:
                return R.id.chip_route_min_walk;
            case MinTransfer:
                return R.id.chip_route_min_transfer;
            case MinTime:
            default:
                return R.id.chip_route_min_time;
        }
    }

    private static int bufferChipId(int minutes) {
        switch (minutes) {
            case 0:
                return R.id.chip_buffer_none;
            case 5:
                return R.id.chip_buffer_5;
            case 10:
                return R.id.chip_buffer_10;
            default:
                return R.id.chip_buffer_custom;
        }
    }

    // 비어 있으면 입력칸에 오류를 띄우고 null
    @Nullable
    protected String requireName(int errorRes) {
        CharSequence nameText = binding.etName.getText();
        String name = nameText == null ? "" : nameText.toString().trim();
        if (name.isEmpty()) {
            binding.tilName.setError(getString(errorRes));
            return null;
        }
        binding.tilName.setError(null);
        return name;
    }

    // 선택 안 했으면 안내하고 null. 검색 화면용 거리 값은 저장하지 않는다.
    @Nullable
    protected Place requirePlace() {
        if (selectedPlace == null) {
            Toast.makeText(this, R.string.new_schedule_place_required, Toast.LENGTH_SHORT).show();
            return null;
        }
        return new Place(selectedPlace.id, selectedPlace.name, selectedPlace.address,
                selectedPlace.lat, selectedPlace.lng, null);
    }

    private void saveSchedule() {
        String name = requireName(R.string.new_schedule_name_required);
        if (name == null) return;
        Place place = requirePlace();
        if (place == null) return;

        Schedule schedule = new Schedule(0, name, date, arrivalTime, place,
                getSelectedRouteType(), getSelectedArrivalBuffer());

        binding.btnSave.setEnabled(false);
        ZetteData.get(this).schedules.addSchedule(schedule, key -> {
            if (isFinishing() || isDestroyed()) return;
            if (key == null) {
                binding.btnSave.setEnabled(true);
                Toast.makeText(this, R.string.new_schedule_save_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            finish();
        });
    }

    public int getSelectedArrivalBuffer() {
        return appliedBuffer;
    }

    private void setupNonSupportBand() {
        binding.layoutNonsupportBand.setVisibility(DEBUG_IS_NONSUPPORT ? View.VISIBLE : View.GONE);
        binding.tvEstimatedLabel.setText(DEBUG_IS_NONSUPPORT
                ? R.string.new_schedule_estimate_label_reference
                : R.string.new_schedule_estimate_label);
    }

    // 예상 경로 카드에 "불러오는 중…"을 띄우고 소요 시간을 받아온다
    protected void loadEstimatedRoute() {
        searchHandler.removeCallbacks(estimatedRouteLoaded);
        binding.tvEstimatedValue.setVisibility(View.GONE);
        binding.layoutEstimatedLoading.getRoot().setVisibility(View.VISIBLE);
        binding.cardEstimatedRoute.setVisibility(View.VISIBLE);
        // TODO - api연결 구현: 경로 계산 응답 대신 3초 뒤 샘플 소요 시간
        searchHandler.postDelayed(estimatedRouteLoaded, Loading.FAKE_DELAY_MS);
    }

    public void setEstimatedRoute(@Nullable Integer travelMinutes) {
        searchHandler.removeCallbacks(estimatedRouteLoaded);
        binding.layoutEstimatedLoading.getRoot().setVisibility(View.GONE);
        binding.tvEstimatedValue.setVisibility(View.VISIBLE);
        if (travelMinutes == null) {
            binding.cardEstimatedRoute.setVisibility(View.GONE);
            return;
        }
        binding.tvEstimatedValue.setText(getString(R.string.new_schedule_estimate_value, travelMinutes));
        binding.cardEstimatedRoute.setVisibility(View.VISIBLE);
    }

    public void setEstimatedRouteLabel(@NonNull CharSequence label) {
        binding.tvEstimatedLabel.setText(label);
    }

    private void hideKeyboard() {
        InputMethodManager imm = getSystemService(InputMethodManager.class);
        if (imm != null) {
            imm.hideSoftInputFromWindow(binding.etSearch.getWindowToken(), 0);
        }
        binding.etSearch.clearFocus();
    }
}
