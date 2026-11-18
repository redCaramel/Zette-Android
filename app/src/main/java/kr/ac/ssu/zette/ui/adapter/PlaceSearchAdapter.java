package kr.ac.ssu.zette.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Objects;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.databinding.ItemPlaceSearchResultBinding;
import kr.ac.ssu.zette.dto.Place;

public class PlaceSearchAdapter extends ListAdapter<Place, PlaceSearchAdapter.ViewHolder> {

    public interface OnPlaceClickListener {
        void onPlaceClick(@NonNull Place place);
    }

    private static final int METERS_PER_KM = 1000;

    @NonNull
    private final OnPlaceClickListener listener;
    @Nullable
    private String selectedPlaceId;

    public PlaceSearchAdapter(@NonNull OnPlaceClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setSelectedPlaceId(@Nullable String placeId) {
        selectedPlaceId = placeId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPlaceSearchResultBinding binding = ItemPlaceSearchResultBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Place place = getItem(position);
        boolean selected = place.id != null && place.id.equals(selectedPlaceId);
        holder.bind(place, position == 0, selected, listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemPlaceSearchResultBinding binding;

        ViewHolder(@NonNull ItemPlaceSearchResultBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Place place, boolean first, boolean selected,
                  @NonNull OnPlaceClickListener listener) {
            Context context = binding.getRoot().getContext();

            binding.viewDivider.setVisibility(first ? View.GONE : View.VISIBLE);
            binding.tvPlaceName.setText(place.name);
            binding.tvPlaceAddress.setText(place.address);

            binding.tvPlaceSelected.setVisibility(selected ? View.VISIBLE : View.GONE);
            if (!selected && place.distance != null) {
                binding.tvPlaceDistance.setText(formatDistance(context, place.distance));
                binding.tvPlaceDistance.setVisibility(View.VISIBLE);
            } else {
                binding.tvPlaceDistance.setVisibility(View.GONE);
            }

            binding.layoutPlaceRow.setOnClickListener(v -> listener.onPlaceClick(place));
        }

        @NonNull
        private static String formatDistance(@NonNull Context context, int meters) {
            if (meters < METERS_PER_KM) {
                return context.getString(R.string.new_schedule_distance_m, meters);
            }
            return context.getString(R.string.new_schedule_distance_km, meters / (float) METERS_PER_KM);
        }
    }

    private static final DiffUtil.ItemCallback<Place> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Place>() {
                @Override
                public boolean areItemsTheSame(@NonNull Place oldItem, @NonNull Place newItem) {
                    return Objects.equals(oldItem.id, newItem.id);
                }

                @Override
                public boolean areContentsTheSame(@NonNull Place oldItem, @NonNull Place newItem) {
                    return Objects.equals(oldItem.name, newItem.name)
                            && Objects.equals(oldItem.address, newItem.address)
                            && Objects.equals(oldItem.distance, newItem.distance);
                }
            };
}
