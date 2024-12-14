package com.example.gpsapp;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import java.util.ArrayList;
import java.util.List;

public class PathsActivity extends AppCompatActivity implements PathsAdapter.OnPathListener, PathsAdapter.OnDeleteClickListener{
    public RecyclerView recyclerView;
    private PathsAdapter adapter;
    public List<PathWithWaypoints> paths = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.saved_paths); // Make sure this layout has the RecyclerView

        recyclerView = findViewById(R.id.recycleView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PathsAdapter(paths,this, this);
        recyclerView.setAdapter(adapter);

        populateRecyleview();
    }

public void populateRecyleview(){
    AppDatabase db = AppDatabase.getInstance(getApplicationContext());

    // Observe the LiveData from the database
    db.pathDao().getAllPathsWithWaypoints().observe(this, newPaths -> {
        //Update the adapter's data when the observed data changes
        Log.d("PathsActivity", "Received paths update: " + newPaths.size());
        adapter.updateData(newPaths);
    });
}

    public void popUp(){
        Dialog myDialog = new Dialog(PathsActivity.this);
        myDialog.setContentView(R.layout.delete_popup);
        myDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        myDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        myDialog.setCancelable(true);
        myDialog.show();

        // Make sure to call findViewById on the dialog's view
        ImageButton btnConfirmSave = myDialog.findViewById(R.id.btnYes);
        ImageButton btnCancelSave = myDialog.findViewById(R.id.btnNo);

        btnConfirmSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myDialog.dismiss();
            }
        });

        btnCancelSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myDialog.dismiss();
            }
        });
    }

    @Override
    public void onPathClick(int pathId) {
        // Start MainActivity and pass the clicked path's ID
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("PATH_ID", pathId); // Use this extra to pass the path ID
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(int position, int pathId) {
        // Perform the delete operation
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        Path pathToDelete = new Path();
        pathToDelete.pID = pathId;

        new Thread(() -> {
            db.pathDao().deletePath(pathToDelete);
            // After deleting, refresh the RecyclerView on the UI thread
            runOnUiThread(() -> {
                paths.remove(position);
                adapter.notifyItemRemoved(position);
            });
        }).start();
    }

}
