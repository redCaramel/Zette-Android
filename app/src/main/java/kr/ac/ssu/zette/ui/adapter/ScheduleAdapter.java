package kr.ac.ssu.zette.ui.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.databinding.ItemScheduleCardBinding;
import kr.ac.ssu.zette.dto.Schedule;
import kr.ac.ssu.zette.ui.item.ScheduleCardItem;

public class ScheduleAdapter extends ListAdapter<ScheduleCardItem, ScheduleAdapter.ViewHolder> {

    public interface OnScheduleActionListener {
        void onItemClick(@NonNull ScheduleCardItem item);

        void onStatusToggle(@NonNull ScheduleCardItem item);

        void onCompleteClick(@NonNull ScheduleCardItem item);
    }

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN);
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN);

    @Nullable
    private final OnScheduleActionListener listener;

    public ScheduleAdapter(@Nullable OnScheduleActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemScheduleCardBinding binding = ItemScheduleCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemScheduleCardBinding binding;

        ViewHolder(@NonNull ItemScheduleCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull ScheduleCardItem item, @Nullable OnScheduleActionListener listener) {
            Context context = binding.getRoot().getContext();
            Schedule schedule = item.schedule;

            binding.tvTitle.setText(schedule.name);
            binding.tvTime.setText(formatScheduleTime(context, schedule.date, schedule.arrivalTime));
            binding.tvPlace.setText(schedule.place == null ? "" : schedule.place.name);

            bindStatus(context, item);
            bindMeta(context, item);
            bindListeners(item, listener);
        }

        private void bindStatus(@NonNull Context context, @NonNull ScheduleCardItem item) {
            String time = formatTime(item.statusTime);

            switch (item.status) {
                case DONE:
                    applyBadge(context, R.string.schedule_status_done,
                            R.color.surface_ceramic, R.color.text_secondary, false);
                    applyDeparture(context, R.drawable.ic_check_circle, R.color.green_accent,
                            context.getString(R.string.schedule_message_done, time), R.color.green_brand);
                    setCompleteActionVisible(false);
                    break;
                case MOVING:
                    applyBadge(context, R.string.schedule_status_moving,
                            R.color.green_mint, R.color.green_brand, false);
                    applyDeparture(context, R.drawable.ic_directions_bus, R.color.green_accent,
                            context.getString(R.string.schedule_message_moving, time), R.color.green_brand);
                    setCompleteActionVisible(true);
                    break;
                case LATE:
                    applyBadge(context, R.string.schedule_status_late,
                            R.color.semantic_error_tint, R.color.semantic_error, false);
                    applyDeparture(context, R.drawable.ic_warning, R.color.semantic_error,
                            context.getString(R.string.schedule_message_late), R.color.semantic_error);
                    setCompleteActionVisible(true);
                    break;
                case SCHEDULED:
                    applyBadge(context, R.string.schedule_status_scheduled,
                            R.color.green_mint, R.color.green_brand, true);
                    applyDeparture(context, R.drawable.ic_notifications_active, R.color.green_accent,
                            context.getString(R.string.schedule_message_scheduled, time), R.color.green_brand);
                    setCompleteActionVisible(false);
                    break;
                case UNRESERVED:
                    applyBadge(context, R.string.schedule_status_unreserved,
                            R.color.surface_ceramic, R.color.text_secondary, true);
                    applyDeparture(context, R.drawable.ic_notifications_active, R.color.text_disabled,
                            context.getString(R.string.schedule_message_unreserved), R.color.text_secondary);
                    setCompleteActionVisible(false);
                    break;
                case MISSED:
                    applyBadge(context, R.string.schedule_status_missed,
                            R.color.semantic_error_tint, R.color.semantic_error, false);
                    applyDeparture(context, R.drawable.ic_notifications_active, R.color.semantic_error,
                            context.getString(R.string.schedule_message_missed), R.color.semantic_error);
                    setCompleteActionVisible(false);
                    break;
            }
        }

        private void applyBadge(@NonNull Context context, @StringRes int label,
                                @ColorRes int background, @ColorRes int content, boolean toggleable) {
            binding.layoutStatus.setBackgroundTintList(ContextCompat.getColorStateList(context, background));
            binding.tvStatus.setText(label);
            binding.tvStatus.setTextColor(ContextCompat.getColor(context, content));
            binding.ivStatusToggle.setVisibility(toggleable ? View.VISIBLE : View.GONE);
            ImageViewCompat.setImageTintList(binding.ivStatusToggle,
                    ColorStateList.valueOf(ContextCompat.getColor(context, content)));
            binding.layoutStatus.setPaddingRelative(
                    binding.layoutStatus.getPaddingStart(), 0,
                    context.getResources().getDimensionPixelSize(
                            toggleable ? R.dimen.schedule_badge_padding_end_toggle : R.dimen.space_2),
                    0);
        }

        private void applyDeparture(@NonNull Context context, @DrawableRes int icon, @ColorRes int iconTint,
                                    @NonNull String message, @ColorRes int messageColor) {
            binding.ivDeparture.setImageResource(icon);
            ImageViewCompat.setImageTintList(binding.ivDeparture,
                    ColorStateList.valueOf(ContextCompat.getColor(context, iconTint)));
            binding.tvDepartureMessage.setText(message);
            binding.tvDepartureMessage.setTextColor(ContextCompat.getColor(context, messageColor));
        }

        private void setCompleteActionVisible(boolean visible) {
            binding.tvCompleteAction.setVisibility(visible ? View.VISIBLE : View.GONE);
            binding.layoutHeader.setGravity(visible ? Gravity.TOP : Gravity.CENTER_VERTICAL);
        }

        private void bindMeta(@NonNull Context context, @NonNull ScheduleCardItem item) {
            String meta = null;
            if (item.status == ScheduleCardItem.Status.MOVING && item.boardingRoute != null) {
                meta = context.getString(R.string.schedule_meta_boarding, item.boardingRoute);
            } else if (item.travelMinutes != null) {
                meta = context.getString(R.string.schedule_meta_travel, item.travelMinutes);
            }
            binding.tvDepartureMeta.setText(meta);
            binding.tvDepartureMeta.setVisibility(meta == null ? View.GONE : View.VISIBLE);
        }

        private void bindListeners(@NonNull ScheduleCardItem item, @Nullable OnScheduleActionListener listener) {
            boolean toggleable = item.status == ScheduleCardItem.Status.SCHEDULED
                    || item.status == ScheduleCardItem.Status.UNRESERVED;

            if (listener == null) {
                binding.getRoot().setOnClickListener(null);
                binding.layoutStatus.setOnClickListener(null);
                binding.tvCompleteAction.setOnClickListener(null);
                binding.layoutStatus.setClickable(false);
                return;
            }

            binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
            binding.tvCompleteAction.setOnClickListener(v -> listener.onCompleteClick(item));
            if (toggleable) {
                binding.layoutStatus.setOnClickListener(v -> listener.onStatusToggle(item));
            } else {
                binding.layoutStatus.setOnClickListener(null);
                binding.layoutStatus.setClickable(false);
            }
        }

        @NonNull
        private static String formatScheduleTime(@NonNull Context context,
                                                 @Nullable LocalDate date, @Nullable LocalTime time) {
            String timeText = formatTime(time);
            if (date == null) return timeText;

            LocalDate today = LocalDate.now();
            if (date.equals(today)) {
                return context.getString(R.string.schedule_time_today, timeText);
            } else if (date.equals(today.plusDays(1))) {
                return context.getString(R.string.schedule_time_tomorrow, timeText);
            }
            return context.getString(R.string.schedule_time_date, date.format(DATE_FORMAT), timeText);
        }

        @NonNull
        private static String formatTime(@Nullable LocalTime time) {
            return time == null ? "" : time.format(TIME_FORMAT);
        }
    }

    private static final DiffUtil.ItemCallback<ScheduleCardItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ScheduleCardItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull ScheduleCardItem oldItem,
                                               @NonNull ScheduleCardItem newItem) {
                    return oldItem.schedule.id == newItem.schedule.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull ScheduleCardItem oldItem,
                                                  @NonNull ScheduleCardItem newItem) {
                    return oldItem.equals(newItem);
                }
            };
}
