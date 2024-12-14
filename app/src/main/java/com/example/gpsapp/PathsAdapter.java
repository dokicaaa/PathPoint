package com.example.gpsapp;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PathsAdapter extends RecyclerView.Adapter<PathsAdapter.ViewHolder> {
    private List<PathWithWaypoints> pathList;
    private OnPathListener onPathListener;
    private OnDeleteClickListener onDeleteClickListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(int position, int pathId);
    }

    public interface OnPathListener{
        void onPathClick(int pathId);
    }

    public PathsAdapter(List<PathWithWaypoints> pathList,  OnPathListener onPathListener, OnDeleteClickListener onDeleteClickListener) {
        this.pathList = pathList;
        this.onPathListener = onPathListener;
        this.onDeleteClickListener = onDeleteClickListener;

    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.path_view, parent, false);
        return new ViewHolder(view, onPathListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PathWithWaypoints currentItem = pathList.get(position);

        // Formatting the date
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String formattedDate = sdf.format(new Date(currentItem.createdAt));

        holder.txtDate.setText(formattedDate);
        holder.txtAmountMarkers.setText(String.valueOf(currentItem.waypointCount));
        holder.btnDelete.setOnClickListener(view -> {
            // Call the listener's method and pass the clicked item's position and ID
            onDeleteClickListener.onDeleteClick(holder.getAdapterPosition(), currentItem.pID);
        });
    }

    @Override
    public int getItemCount() {
        return pathList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtDate, txtAmountMarkers;
        ImageButton btnDelete;

        ViewHolder(View itemView,OnPathListener onPathListener) {
            super(itemView);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtAmountMarkers = itemView.findViewById(R.id.txtAmountMarkers);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        // Set the click listener for the entire row
            itemView.setOnClickListener(v -> {
               if(onPathListener != null) {
                   int position = getAdapterPosition();
                   if(position != RecyclerView.NO_POSITION){
                       int pathId = pathList.get(position).pID;
                       onPathListener.onPathClick(pathId);
                   }
               }
            });
        }
    }
    // Method to update adapter's dataset
    public void updateData(List<PathWithWaypoints> newPathList) {
        this.pathList = newPathList;
        notifyDataSetChanged();
    }
}