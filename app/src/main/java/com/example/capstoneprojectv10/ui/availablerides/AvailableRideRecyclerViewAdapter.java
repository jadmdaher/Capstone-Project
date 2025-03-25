package com.example.capstoneprojectv10.ui.availablerides;

import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.capstoneprojectv10.databinding.RecyclerviewAvailableRideTitleBinding;
import com.example.capstoneprojectv10.ui.availablerides.placeholder.PlaceholderContent.PlaceholderItem;
import com.example.capstoneprojectv10.databinding.FragmentAvailableRideItemBinding;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link PlaceholderItem}.
 * TODO: Replace the implementation with code for your data type.
 */
public class AvailableRideRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_TITLE = 0;
    private static final int VIEW_TYPE_RIDE = 1;

    private final List<PlaceholderItem> mValues;

    public AvailableRideRecyclerViewAdapter(List<PlaceholderItem> items) {
        mValues = items;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0) ? VIEW_TYPE_TITLE : VIEW_TYPE_RIDE;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_TITLE) {
            RecyclerviewAvailableRideTitleBinding binding = RecyclerviewAvailableRideTitleBinding.inflate(inflater, parent, false);
            return new AvailableRidesTitleViewHolder(binding);
        } else {
            FragmentAvailableRideItemBinding binding = FragmentAvailableRideItemBinding.inflate(inflater, parent, false);
            return new AvailableRidesViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof AvailableRidesTitleViewHolder) {
            ((AvailableRidesTitleViewHolder) holder).bind();
        } else if (holder instanceof AvailableRidesViewHolder) {
            ((AvailableRidesViewHolder) holder).bind(mValues.get(position - 1)); // Adjust index for rides
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size() + 1; // Extra count for title item
    }

    public static class AvailableRidesTitleViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;

        public AvailableRidesTitleViewHolder(RecyclerviewAvailableRideTitleBinding binding) {
            super(binding.getRoot());
            titleTextView = binding.tvTitle;
        }

        public void bind() {
            titleTextView.setText("Available Rides");
        }
    }

    public static class AvailableRidesViewHolder extends RecyclerView.ViewHolder {
        private final TextView mIdView;
        private final TextView mContentView;

        public AvailableRidesViewHolder(FragmentAvailableRideItemBinding binding) {
            super(binding.getRoot());
            mIdView = binding.rideName;
            mContentView = binding.rideDate;
        }

        public void bind(PlaceholderItem item) {
            mIdView.setText(item.id);
            mContentView.setText(item.content);
        }
    }
}