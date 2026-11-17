package kr.ac.ssu.zette.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.databinding.ItemRoutineCardBinding;
import kr.ac.ssu.zette.ui.item.RoutineItem;

public class RoutineAdapter extends ListAdapter<RoutineItem, RoutineAdapter.ViewHolder> {

    public interface OnRoutineActionListener {
        void onRoutineClick(@NonNull RoutineItem item);

        void onRoutineToggle(@NonNull RoutineItem item, boolean enabled);
    }

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN);
    private static final String DAY_SEPARATOR = " · ";

    @Nullable
    private final OnRoutineActionListener listener;

    public RoutineAdapter(@Nullable OnRoutineActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRoutineCardBinding binding = ItemRoutineCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemRoutineCardBinding binding;

        ViewHolder(@NonNull ItemRoutineCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull RoutineItem item, @Nullable OnRoutineActionListener listener) {
            Context context = binding.getRoot().getContext();

            binding.tvRoutineName.setText(item.name);
            binding.tvRoutineRepeat.setText(formatRepeat(context, item));
            binding.tvRoutineTime.setText(item.time.format(TIME_FORMAT));
            binding.tvRoutinePlace.setText(item.placeName);

            if (item.departureAlarmTime != null) {
                binding.tvRoutineDeparture.setText(context.getString(
                        R.string.routine_departure_alarm, item.departureAlarmTime.format(TIME_FORMAT)));
            } else {
                binding.tvRoutineDeparture.setText(null);
            }
            if (item.travelMinutes != null) {
                binding.tvRoutineTravel.setText(context.getString(R.string.routine_travel, item.travelMinutes));
                binding.tvRoutineTravel.setVisibility(View.VISIBLE);
            } else {
                binding.tvRoutineTravel.setVisibility(View.GONE);
            }

            binding.switchRoutine.setOnCheckedChangeListener(null);
            binding.switchRoutine.setChecked(item.enabled);

            if (listener == null) {
                binding.getRoot().setOnClickListener(null);
                return;
            }
            binding.getRoot().setOnClickListener(v -> listener.onRoutineClick(item));
            binding.switchRoutine.setOnCheckedChangeListener(
                    (button, checked) -> listener.onRoutineToggle(item, checked));
        }

        @NonNull
        private static String formatRepeat(@NonNull Context context, @NonNull RoutineItem item) {
            if (item.repeatType == RoutineItem.RepeatType.MONTHLY) {
                return context.getString(R.string.routine_repeat_monthly, item.monthDay);
            }
            List<String> days = new ArrayList<>();
            for (DayOfWeek day : item.weekDays) {
                days.add(day.getDisplayName(TextStyle.SHORT, Locale.KOREAN));
            }
            return context.getString(R.string.routine_repeat_weekly, String.join(DAY_SEPARATOR, days));
        }
    }

    private static final DiffUtil.ItemCallback<RoutineItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<RoutineItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull RoutineItem oldItem, @NonNull RoutineItem newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull RoutineItem oldItem, @NonNull RoutineItem newItem) {
                    return oldItem.equals(newItem);
                }
            };
}
