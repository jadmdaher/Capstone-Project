package com.example.capstoneprojectv10.ui.availablerides;

import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.capstoneprojectv10.R;
import com.example.capstoneprojectv10.data.model.RideItem;
import com.example.capstoneprojectv10.databinding.RecyclerviewAvailableRideTitleBinding;
import com.example.capstoneprojectv10.ui.availablerides.placeholder.PlaceholderContent.PlaceholderItem;
import com.example.capstoneprojectv10.databinding.FragmentAvailableRideItemBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * {@link RecyclerView.Adapter} that can display a {@link PlaceholderItem}.
 * TODO: Replace the implementation with code for your data type.
 */
public class AvailableRideRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_TITLE = 0;
    private static final int VIEW_TYPE_RIDE = 1;

    //private final List<PlaceholderItem> mValues;

    private final List<RideItem> rideList;
    private final onRideClickListener listener;

    public AvailableRideRecyclerViewAdapter(List<RideItem> items, onRideClickListener listener) {
        this.rideList = items;
        this.listener = listener;
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
            ((AvailableRidesViewHolder) holder).bind(rideList.get(position - 1), listener); // Adjust index for rides
        }
    }

    @Override
    public int getItemCount() {
        return rideList.size() + 1; // Extra count for title item
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

        public void bind(RideItem item, onRideClickListener listener) {
            //mIdView.setText(item.id);
            //mContentView.setText(item.content);

            String username = "@" + item.driverName;
            mIdView.setText(username);
            //mContentView.setText(item.rideDate);

            // Format ride date nicely
            try {
                // Parse the original string to a Date object
                SimpleDateFormat inputFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
                Date date = inputFormat.parse(item.rideDate);

                // Format it to only show the date (e.g. "28 Mar 2025")
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                mContentView.setText(outputFormat.format(date));
            } catch (Exception e) {
                mContentView.setText(item.rideDate); // fallback if parsing fails
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

            itemView.setOnClickListener(v -> listener.onRideClick(item));
        }
    }
}