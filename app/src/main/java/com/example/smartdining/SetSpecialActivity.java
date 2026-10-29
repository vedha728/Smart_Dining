package com.example.smartdining;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import android.support.v7.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class SetSpecialActivity extends AppCompatActivity {

    ListView listSpecial;
    DatabaseHelper dbHelper;
    int hotelId;

    ArrayList<String> menuItems = new ArrayList<>();
    ArrayList<Integer> foodIds = new ArrayList<>();
    ArrayList<String> foodNames = new ArrayList<>();
    ArrayList<Double> foodPrices = new ArrayList<>();
    ArrayList<FoodRowAdapter.FoodRow> foodRows = new ArrayList<>();
    java.util.HashSet<Integer> selectedSpecialIds = new java.util.HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_special);
        AppChrome.install(this, "Daily Specials", false, false);

        listSpecial = findViewById(R.id.listSpecial);
        hotelId = getIntent().getIntExtra("hotel_id", -1);
        dbHelper = new DatabaseHelper(this);

        loadMenu();

        listSpecial.setOnItemClickListener((parent, view, position, id) -> {
            if (foodIds.isEmpty()) return;

            String name = foodNames.get(position);
            double price = foodPrices.get(position);
            int foodId = foodIds.get(position);

            boolean selected = selectedSpecialIds.contains(foodId);
            new AlertDialog.Builder(this)
                    .setTitle(selected ? "Remove Daily Special?" : "Set Daily Special")
                    .setMessage(name + " - ₹" + price + (selected ? "\n\nRemove this item from today's specials?" : "\n\nAdd this item to today's specials? Multiple specials are allowed."))
                    .setPositiveButton(selected ? "Remove" : "Add special", (d, w) -> toggleSpecial(foodId, name, price, !selected))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @Override protected void onResume() { super.onResume(); if (dbHelper != null && listSpecial != null) loadMenu(); }

    private void loadMenu() {
        menuItems.clear(); foodIds.clear(); foodNames.clear(); foodPrices.clear(); foodRows.clear(); selectedSpecialIds.clear();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor sp = db.rawQuery("SELECT food_id FROM daily_specials WHERE hotel_id=? AND date=?", new String[]{String.valueOf(hotelId),today});
        while(sp.moveToNext()) selectedSpecialIds.add(sp.getInt(0)); sp.close();
        Cursor c = db.rawQuery("SELECT food_id, name, price, category, image_url FROM food WHERE hotel_id=? AND availability=1 ORDER BY name", new String[]{String.valueOf(hotelId)});
        while (c.moveToNext()) {
            int id=c.getInt(0); String name=c.getString(1); double price=c.getDouble(2);
            foodIds.add(id); foodNames.add(name); foodPrices.add(price);
            foodRows.add(new FoodRowAdapter.FoodRow(id,name,(selectedSpecialIds.contains(id)?"✓ DAILY SPECIAL  •  ":"")+"₹"+String.format(Locale.getDefault(),"%.2f",price),c.isNull(4)?"":c.getString(4),selectedSpecialIds.contains(id)));
        }
        c.close(); db.close();
        if (foodRows.isEmpty()) { menuItems.add("No food items available. Add food first."); listSpecial.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,menuItems)); }
        else listSpecial.setAdapter(new FoodRowAdapter(this,foodRows));
    }

    private void toggleSpecial(int foodId, String name, double price, boolean makeSpecial) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (makeSpecial) {
            Cursor existing=db.rawQuery("SELECT special_id FROM daily_specials WHERE hotel_id=? AND food_id=? AND date=?",new String[]{String.valueOf(hotelId),String.valueOf(foodId),today});
            boolean found=existing.moveToFirst(); existing.close();
            if (!found) {
                ContentValues values=new ContentValues(); values.put("hotel_id",hotelId); values.put("food_id",foodId); values.put("description",name+" - ₹"+price); values.put("date",today);
                db.insert("daily_specials",null,values);
            }
        } else db.delete("daily_specials","hotel_id=? AND food_id=? AND date=?",new String[]{String.valueOf(hotelId),String.valueOf(foodId),today});
        db.close();
        Toast.makeText(this, makeSpecial ? "Added to today's specials" : "Removed from today's specials", Toast.LENGTH_SHORT).show();
        loadMenu();
    }

}
