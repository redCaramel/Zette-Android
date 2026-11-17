package kr.ac.ssu.zette.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.databinding.ItemCalendarEventBinding;

public class CalendarEventAdapter extends ListAdapter<CalendarEventItem, CalendarEventAdapter.ViewHolder> {

    public interface OnEventActionListener {
        void onEditClick(@NonNull CalendarEventItem item);

        void onDeleteClick(@NonNull CalendarEventItem item);

        void onAlarmToggle(@NonNull CalendarEventItem item, boolean on);
    }

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN);
    private static final DateTimeFormatter DEPARTURE_FORMAT =
            DateTimeFormatter.ofPattern("h:mm", Locale.KOREAN);
    private static final float DEPARTURE_ALPHA_ALARM_OFF = 0.5f;

    @Nullable
    private final OnEventActionListener listener;

    public CalendarEventAdapter(@Nullable OnEventActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCalendarEventBinding binding = ItemCalendarEventBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemCalendarEventBinding binding;

        ViewHolder(@NonNull ItemCalendarEventBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull CalendarEventItem item, @Nullable OnEventActionListener listener) {
            Context context = binding.getRoot().getContext();

            binding.tvEventTime.setText(item.time.format(TIME_FORMAT));
            if (item.departureTime != null) {
                binding.tvEventDeparture.setText(context.getString(
                        R.string.calendar_departure, item.departureTime.format(DEPARTURE_FORMAT)));
                binding.tvEventDeparture.setVisibility(View.VISIBLE);
            } else {
                binding.tvEventDeparture.setVisibility(View.GONE);
            }
            binding.tvEventDeparture.setAlpha(item.alarmOn ? 1f : DEPARTURE_ALPHA_ALARM_OFF);

            binding.tvEventTitle.setText(item.title);
            binding.tvEventPlace.setText(item.placeName);

            if (item.routineLabel != null) {
                binding.tvRoutineTag.setText(item.routineLabel);
                binding.layoutRoutineTag.setVisibility(View.VISIBLE);
            } else {
                binding.layoutRoutineTag.setVisibility(View.GONE);
            }

            binding.switchAlarm.setOnCheckedChangeListener(null);
            binding.switchAlarm.setChecked(item.alarmOn);

            if (listener == null) {
                binding.btnEdit.setOnClickListener(null);
                binding.btnDelete.setOnClickListener(null);
                return;
            }
            binding.btnEdit.setOnClickListener(v -> listener.onEditClick(item));
            binding.btnDelete.setOnClickListener(v -> listener.onDeleteClick(item));
            binding.switchAlarm.setOnCheckedChangeListener(
                    (button, checked) -> listener.onAlarmToggle(item, checked));
        }
    }

    private static final DiffUtil.ItemCallback<CalendarEventItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CalendarEventItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull CalendarEventItem oldItem,
                                               @NonNull CalendarEventItem newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull CalendarEventItem oldItem,
                                                  @NonNull CalendarEventItem newItem) {
                    return oldItem.equals(newItem);
                }
            };
}
