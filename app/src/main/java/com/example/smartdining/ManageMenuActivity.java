package com.example.smartdining;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;

public class ManageMenuActivity extends AppCompatActivity {

    ListView listManageMenu;
    DatabaseHelper dbHelper;
    int hotelId;

    ArrayList<String> menuItems = new ArrayList<>();
    ArrayList<Integer> foodIds = new ArrayList<>();
    ArrayList<FoodRowAdapter.FoodRow> foodRows = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_menu);
        AppChrome.install(this, "Manage Menu", false, false);

        listManageMenu = findViewById(R.id.listManageMenu);
        hotelId = getIntent().getIntExtra("hotel_id", -1);
        dbHelper = new DatabaseHelper(this);

        loadMenu();

        listManageMenu.setOnItemClickListener((parent, view, position, id) -> {
            if (foodIds.isEmpty()) return;

            int foodId = foodIds.get(position);
            String item = menuItems.get(position);

            new AlertDialog.Builder(this)
                    .setTitle("Manage Item")
                    .setMessage(item)
                    .setPositiveButton("Edit", (d, w) -> {
                        Intent intent = new Intent(this, EditFoodActivity.class);
                        intent.putExtra("food_id", foodId);
                        intent.putExtra("hotel_id", hotelId);
                        startActivity(intent);
                    })
                    .setNegativeButton("Delete", (d, w) -> deleteFood(foodId))
                    .setNeutralButton("Cancel", null)
                    .show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMenu(); // refresh after edit
    }

    private void loadMenu() {
        menuItems.clear();
        foodIds.clear(); foodRows.clear();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT food_id, name, price, category, image_url FROM food WHERE hotel_id=?",
                new String[]{String.valueOf(hotelId)});

        if (c.moveToFirst()) {
            do {
                foodIds.add(c.getInt(0));
                menuItems.add(c.getString(1) + " - ₹" + c.getDouble(2));
                foodRows.add(new FoodRowAdapter.FoodRow(c.getInt(0), c.getString(1), (c.isNull(3)?"Food":c.getString(3))+"  •  ₹"+c.getDouble(2), c.isNull(4)?"":c.getString(4), false));
            } while (c.moveToNext());
        }
        c.close();
        db.close();

        if (menuItems.isEmpty()) {
            menuItems.add("No items found");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, menuItems);
        if (foodRows.isEmpty()) listManageMenu.setAdapter(adapter); else listManageMenu.setAdapter(new FoodRowAdapter(this, foodRows));
    }

    private void deleteFood(int foodId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("food", "food_id=?", new String[]{String.valueOf(foodId)});
        db.close();
        Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show();
        loadMenu();
    }
}