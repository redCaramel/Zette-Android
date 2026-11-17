package kr.ac.ssu.zette.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.databinding.ItemDateCellBinding;

public class DateCellAdapter extends RecyclerView.Adapter<DateCellAdapter.ViewHolder> {

    public interface OnDateClickListener {
        void onDateClick(@NonNull LocalDate date);
    }

    private final OnDateClickListener listener;
    private final List<LocalDate> dates = new ArrayList<>();
    private final Set<LocalDate> eventDates = new HashSet<>();
    @Nullable
    private YearMonth displayedMonth;
    @NonNull
    private LocalDate selectedDate = LocalDate.now();
    @NonNull
    private LocalDate today = LocalDate.now();

    public DateCellAdapter(@NonNull OnDateClickListener listener) {
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void submit(@NonNull List<LocalDate> newDates, @Nullable YearMonth month,
                       @NonNull LocalDate selected, @NonNull LocalDate todayDate) {
        dates.clear();
        dates.addAll(newDates);
        displayedMonth = month;
        selectedDate = selected;
        today = todayDate;
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setEventDates(@NonNull Set<LocalDate> newEventDates) {
        eventDates.clear();
        eventDates.addAll(newEventDates);
        notifyDataSetChanged();
    }

    @NonNull
    public List<LocalDate> getDates() {
        return Collections.unmodifiableList(dates);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDateCellBinding binding = ItemDateCellBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LocalDate date = dates.get(position);
        boolean outside = displayedMonth != null && !YearMonth.from(date).equals(displayedMonth);
        holder.bind(date, date.equals(selectedDate), date.equals(today), outside,
                eventDates.contains(date), listener);
    }

    @Override
    public int getItemCount() {
        return dates.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemDateCellBinding binding;

        ViewHolder(@NonNull ItemDateCellBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull LocalDate date, boolean selected, boolean today, boolean outside,
                  boolean hasEvent, @NonNull OnDateClickListener listener) {
            Context context = binding.getRoot().getContext();
            binding.tvDate.setText(String.valueOf(date.getDayOfMonth()));

            if (selected) {
                applyCell(context, R.drawable.bg_round_full, R.color.green_accent,
                        R.style.TextAppearance_Zette_Subtitle, R.color.text_on_accent);
            } else if (today) {
                applyCell(context, R.drawable.bg_round_full_stroke, R.color.green_accent,
                        R.style.TextAppearance_Zette_Subtitle, R.color.green_brand);
            } else {
                applyCell(context, 0, 0, R.style.TextAppearance_Zette_BodyLarge,
                        outside ? R.color.text_disabled : R.color.text_primary);
            }

            binding.viewDot.setVisibility(hasEvent && !outside ? View.VISIBLE : View.INVISIBLE);
            binding.getRoot().setOnClickListener(v -> listener.onDateClick(date));
        }

        private void applyCell(@NonNull Context context, int backgroundRes, @ColorRes int backgroundTint,
                               @StyleRes int textAppearance, @ColorRes int textColor) {
            if (backgroundRes == 0) {
                binding.tvDate.setBackground(null);
            } else {
                binding.tvDate.setBackgroundResource(backgroundRes);
                binding.tvDate.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(context, backgroundTint)));
            }
            binding.tvDate.setTextAppearance(textAppearance);
            binding.tvDate.setTextColor(ContextCompat.getColor(context, textColor));
        }
    }
}
