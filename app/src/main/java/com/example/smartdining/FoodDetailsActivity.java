package com.example.smartdining;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;

public class FoodDetailsActivity extends AppCompatActivity {

    TextView tvFoodName, tvHotelName, tvCategory, tvPrice, tvDescription;
    ImageView ivFoodImage;
    Button btnAddToCart, btnIncreaseQuantity, btnDecreaseQuantity;
    EditText etQuantity;
    DatabaseHelper dbHelper;

    int foodId = -1, hotelId = -1, userId = -1;
    double price = 0;
    String foodName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_details);
        AppChrome.install(this, "Food Details", false, false);

        try {
            tvFoodName = findViewById(R.id.tvFoodName);
            tvHotelName = findViewById(R.id.tvHotelName);
            tvCategory = findViewById(R.id.tvCategory);
            tvPrice = findViewById(R.id.tvPrice);
            tvDescription = findViewById(R.id.tvDescription);
            ivFoodImage = findViewById(R.id.ivFoodImage);
            btnAddToCart = findViewById(R.id.btnAddToCart);
            btnIncreaseQuantity = findViewById(R.id.btnIncreaseQuantity);
            btnDecreaseQuantity = findViewById(R.id.btnDecreaseQuantity);
            etQuantity = findViewById(R.id.etQuantity);

            foodId = getIntent().getIntExtra("food_id", -1);
            hotelId = getIntent().getIntExtra("hotel_id", -1);
            userId = getIntent().getIntExtra("user_id", -1);

            if (foodId == -1 || userId == -1 || hotelId == -1) {
                Toast.makeText(this, "Error: Missing IDs", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            dbHelper = new DatabaseHelper(this);
            loadFoodDetails();

            btnIncreaseQuantity.setOnClickListener(v -> {
                int quantity = readQuantity();
                etQuantity.setText(String.valueOf(Math.min(99, quantity + 1)));
            });

            btnDecreaseQuantity.setOnClickListener(v -> {
                int quantity = readQuantity();
                if (quantity > 1) etQuantity.setText(String.valueOf(quantity - 1));
            });

            btnAddToCart.setOnClickListener(v -> showClusterDialog());

        } catch (Exception e) {
            Toast.makeText(this, "onCreate error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void loadFoodDetails() {
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor c = db.rawQuery(
                    "SELECT f.name, f.category, f.description, f.price, h.name, f.image_url " +
                            "FROM food f JOIN hotels h ON f.hotel_id = h.hotel_id " +
                            "WHERE f.food_id = ?",
                    new String[]{String.valueOf(foodId)});

            if (c.moveToFirst()) {
                foodName = c.getString(0);
                tvFoodName.setText(foodName);
                tvCategory.setText("Category: " + (c.getString(1) != null ? c.getString(1) : "-"));
                tvDescription.setText(c.getString(2) != null && !c.getString(2).isEmpty() ? c.getString(2) : "No description");
                price = c.getDouble(3);
                tvPrice.setText("₹" + price);
                tvHotelName.setText("Hotel: " + c.getString(4));
                FoodImageLoader.load(c.isNull(5) ? "" : c.getString(5), ivFoodImage, false);
            }
            c.close();
            db.close();
        } catch (Exception e) {
            Toast.makeText(this, "Load error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showClusterDialog() {
        ArrayList<String> clusterNames = new ArrayList<>();
        ArrayList<Integer> clusterIds = new ArrayList<>();

        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor c = db.rawQuery(
                    "SELECT cluster_id, cluster_name FROM cart_clusters WHERE user_id = ?",
                    new String[]{String.valueOf(userId)});

            while (c.moveToNext()) {
                clusterIds.add(c.getInt(0));
                clusterNames.add(c.getString(1));
            }
            c.close();
            db.close();
        } catch (Exception e) {
            Toast.makeText(this, "DB Error (tables missing?): " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        clusterNames.add("+ Create New Cluster");

        new AlertDialog.Builder(this)
                .setTitle("Add to which Cluster?")
                .setItems(clusterNames.toArray(new String[0]), (dialog, which) -> {
                    if (which == clusterNames.size() - 1) {
                        askNewClusterName();
                    } else {
                        addToCart(clusterIds.get(which), readQuantity());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void askNewClusterName() {
        final EditText input = new EditText(this);
        input.setHint("e.g. Lunch, Dinner, Party");

        new AlertDialog.Builder(this)
                .setTitle("New Cluster Name")
                .setView(input)
                .setPositiveButton("Create", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Name required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int newId = createCluster(name);
                    if (newId > 0) {
                        addToCart(newId, readQuantity());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private int createCluster(String name) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("user_id", userId);
            values.put("cluster_name", name);
            long id = db.insert("cart_clusters", null, values);
            db.close();
            return (int) id;
        } catch (Exception e) {
            Toast.makeText(this, "Create cluster failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return -1;
        }
    }

    private int readQuantity() {
        String value = etQuantity.getText().toString().trim();
        if (value.isEmpty()) return 1;
        try {
            return Math.min(99, Math.max(1, Integer.parseInt(value)));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private void addToCart(int clusterId, int quantity) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            Cursor existing = db.rawQuery(
                    "SELECT item_id, quantity FROM cart_items WHERE cluster_id=? AND food_id=? AND hotel_id=?",
                    new String[]{String.valueOf(clusterId), String.valueOf(foodId), String.valueOf(hotelId)});
            long result;
            if (existing.moveToFirst()) {
                int itemId = existing.getInt(0);
                int updatedQuantity = existing.getInt(1) + quantity;
                existing.close();
                ContentValues update = new ContentValues();
                update.put("quantity", updatedQuantity);
                result = db.update("cart_items", update, "item_id=?", new String[]{String.valueOf(itemId)});
            } else {
                existing.close();
                ContentValues values = new ContentValues();
                values.put("cluster_id", clusterId);
                values.put("food_id", foodId);
                values.put("hotel_id", hotelId);
                values.put("quantity", quantity);
                values.put("unit_price", price);
                result = db.insert("cart_items", null, values);
            }
            db.close();

            if (result > 0) {
                Toast.makeText(this, quantity + " × " + foodName + " added to cluster!", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(this, "Could not add item to cart", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Add to cart error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
}