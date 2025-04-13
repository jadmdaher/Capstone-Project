package com.example.capstoneprojectv10.ui.ridehistory;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.capstoneprojectv10.R;
import com.example.capstoneprojectv10.data.model.RideItem;
import com.example.capstoneprojectv10.databinding.FragmentAvailableRideItemBinding;
import com.example.capstoneprojectv10.databinding.FragmentRideHistoryItemBinding;
import com.example.capstoneprojectv10.databinding.RecyclerviewRideHistoryTitleBinding;
import com.example.capstoneprojectv10.ui.ridehistory.placeholder.PlaceholderContent.PlaceholderItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RideHistoryRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_TITLE = 0;
    private static final int VIEW_TYPE_RIDE = 1;

    //private final List<PlaceholderItem> mValues;
    private final List<RideItem> rideList;

    public RideHistoryRecyclerViewAdapter(List<RideItem> items) {
        this.rideList = items;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0) ? VIEW_TYPE_TITLE : VIEW_TYPE_RIDE;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_TITLE) {
            RecyclerviewRideHistoryTitleBinding binding = RecyclerviewRideHistoryTitleBinding.inflate(inflater, parent, false);
            return new RideHistoryTitleViewHolder(binding);
        } else {
            FragmentRideHistoryItemBinding binding = FragmentRideHistoryItemBinding.inflate(inflater, parent, false);
            return new RideHistoryViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof RideHistoryTitleViewHolder) {
            ((RideHistoryTitleViewHolder) holder).bind();
        } else if (holder instanceof RideHistoryViewHolder) {
            ((RideHistoryViewHolder) holder).bind(rideList.get(position - 1)); // Adjust index for rides
        }
    }

    @Override
    public int getItemCount() {
        return rideList.size() + 1; // Extra count for title item
    }

    public static class RideHistoryTitleViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;

        public RideHistoryTitleViewHolder(RecyclerviewRideHistoryTitleBinding binding) {
            super(binding.getRoot());
            titleTextView = binding.tvTitle;
        }

        public void bind() {
            titleTextView.setText("Ride History");
        }
    }

    public static class RideHistoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView mIdView;
        private final TextView mContentView;

        public RideHistoryViewHolder(FragmentRideHistoryItemBinding binding) {
            super(binding.getRoot());
            mIdView = binding.rideName;
            mContentView = binding.rideDate;
        }

        public void bind(RideItem item) {
            mIdView.setText("@" + item.driverName);

            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
                Date date = inputFormat.parse(item.rideDate);
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                mContentView.setText(outputFormat.format(date));
            } catch (Exception e) {
                mContentView.setText(item.rideDate);
            }

            FragmentAvailableRideItemBinding binding = FragmentAvailableRideItemBinding.bind(itemView);

            // Load profile image with fallback
            if (item.profileImageUrl != null && !item.profileImageUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(item.profileImageUrl)
                        .circleCrop()
                        .into(binding.profileImage);
            } else {
                binding.profileImage.setImageResource(R.drawable.ic_profile_placeholder);
            }
        }

    }
}
