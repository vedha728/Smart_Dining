package com.example.smartdining;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;

public class CartActivity extends AppCompatActivity {

    ListView listClusters;
    DatabaseHelper dbHelper;
    int userId;

    ArrayList<String> clusterNames = new ArrayList<>();
    ArrayList<Integer> clusterIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);
        AppChrome.install(this, "My Cart", false, false);

        listClusters = findViewById(R.id.listClusters);
        userId = getIntent().getIntExtra("user_id", -1);
        dbHelper = new DatabaseHelper(this);

        loadClusters();

        listClusters.setOnItemClickListener((parent, view, position, id) -> {
            if (clusterIds.isEmpty()) return;

            Intent intent = new Intent(this, ClusterDetailsActivity.class);
            intent.putExtra("cluster_id", clusterIds.get(position));
            intent.putExtra("cluster_name", clusterNames.get(position));
            intent.putExtra("user_id", userId);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadClusters();
    }

    private void loadClusters() {
        clusterNames.clear();
        clusterIds.clear();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT cluster_id, cluster_name FROM cart_clusters WHERE user_id=?",
                new String[]{String.valueOf(userId)});

        while (c.moveToNext()) {
            clusterIds.add(c.getInt(0));
            clusterNames.add(c.getString(1));
        }
        c.close();
        db.close();

        if (clusterNames.isEmpty()) {
            clusterNames.add("No clusters yet. Add items from a hotel menu.");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, clusterNames);
        listClusters.setAdapter(adapter);
    }
}