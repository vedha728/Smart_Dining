package com.example.smartdining;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import android.support.v7.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ClusterDetailsActivity extends AppCompatActivity {

    TextView tvClusterName;
    ListView listClusterItems;
    Button btnPlaceOrder;
    DatabaseHelper dbHelper;

    int clusterId, userId;
    String clusterName;

    ArrayList<String> itemTexts = new ArrayList<>();
    ArrayList<Integer> itemIds = new ArrayList<>();
    ArrayList<FoodRowAdapter.FoodRow> itemRows = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cluster_details);
        AppChrome.install(this, "Cart Cluster", false, false);

        tvClusterName = findViewById(R.id.tvClusterName);
        listClusterItems = findViewById(R.id.listClusterItems);
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);

        clusterId = getIntent().getIntExtra("cluster_id", -1);
        userId = getIntent().getIntExtra("user_id", -1);
        clusterName = getIntent().getStringExtra("cluster_name");

        tvClusterName.setText(clusterName);
        dbHelper = new DatabaseHelper(this);

        loadItems();

        btnPlaceOrder.setOnClickListener(v -> confirmPlaceOrder());
    }

    private void loadItems() {
        itemTexts.clear();
        itemIds.clear(); itemRows.clear();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT ci.item_id, f.name, h.name, ci.quantity, ci.unit_price, f.image_url, ci.food_id " +
                        "FROM cart_items ci " +
                        "JOIN food f ON ci.food_id = f.food_id " +
                        "JOIN hotels h ON ci.hotel_id = h.hotel_id " +
                        "WHERE ci.cluster_id=?",
                new String[]{String.valueOf(clusterId)});

        while (c.moveToNext()) {
            itemIds.add(c.getInt(0));
            double total = c.getInt(3) * c.getDouble(4);
            itemTexts.add(c.getString(1) + " x" + c.getInt(3) +
                    "\n" + c.getString(2) + " | ₹" + total);
            itemRows.add(new FoodRowAdapter.FoodRow(c.getInt(6), c.getString(1), c.getString(2)+"  •  "+c.getInt(3)+" ×  ₹"+String.format(Locale.getDefault(),"%.2f",c.getDouble(4))+" = ₹"+String.format(Locale.getDefault(),"%.2f",total), c.isNull(5)?"":c.getString(5), false));
        }
        c.close();
        db.close();

        if (itemTexts.isEmpty()) {
            itemTexts.add("No items in this cluster");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, itemTexts);
        if (itemRows.isEmpty()) listClusterItems.setAdapter(adapter); else listClusterItems.setAdapter(new FoodRowAdapter(this, itemRows));
    }

    private void confirmPlaceOrder() {
        if (itemIds.isEmpty()) {
            Toast.makeText(this, "Cluster is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Place Order")
                .setMessage("Place order for all items in \"" + clusterName + "\"?")
                .setPositiveButton("Yes, Place Order", (d, w) -> placeOrder())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void placeOrder() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        Cursor c = db.rawQuery(
                "SELECT food_id, hotel_id, quantity, unit_price FROM cart_items WHERE cluster_id=?",
                new String[]{String.valueOf(clusterId)});

        while (c.moveToNext()) {
            ContentValues values = new ContentValues();
            values.put("user_id", userId);
            values.put("hotel_id", c.getInt(1));
            values.put("food_id", c.getInt(0));
            values.put("quantity", c.getInt(2));
            values.put("total_price", c.getInt(2) * c.getDouble(3));
            values.put("date", date);
            values.put("time", time);
            values.put("status", "Placed");
            db.insert("orders", null, values);
        }
        c.close();

        // Clear the cluster
        db.delete("cart_items", "cluster_id=?", new String[]{String.valueOf(clusterId)});
        db.delete("cart_clusters", "cluster_id=?", new String[]{String.valueOf(clusterId)});
        db.close();

        Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_LONG).show();
        finish();
    }
}