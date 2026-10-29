package com.example.smartdining;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import android.support.v7.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class HotelDetailsActivity extends AppCompatActivity {

    TextView tvHotelName, tvHotelAddress, tvDailySpecial;
    ListView listMenu;
    DatabaseHelper dbHelper;

    int hotelId;
    int userId = -1;               // will be received from CustomerHome
    ArrayList<String> menuItems = new ArrayList<>();
    ArrayList<Integer> foodIds = new ArrayList<>();
    ArrayList<Double> foodPrices = new ArrayList<>();
    ArrayList<String> foodNames = new ArrayList<>();
    ArrayList<FoodRowAdapter.FoodRow> foodRows = new ArrayList<>();
    java.util.HashSet<Integer> specialFoodIds = new java.util.HashSet<>();
    int specialFoodId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hotel_details);
        AppChrome.install(this, "Hotel & Menu", false, false);

        tvHotelName = findViewById(R.id.tvHotelName);
        tvHotelAddress = findViewById(R.id.tvHotelAddress);
        tvDailySpecial = findViewById(R.id.tvDailySpecial);
        listMenu = findViewById(R.id.listMenu);

        hotelId = getIntent().getIntExtra("hotel_id", -1);
        userId = getIntent().getIntExtra("user_id", -1);   // important for ordering

        dbHelper = new DatabaseHelper(this);

        loadHotelInfo();
        loadDailySpecial();
        loadMenu();

        // When customer clicks a food item → place order
        listMenu.setOnItemClickListener((parent, view, position, id) -> {
            if (userId == -1) {
                Toast.makeText(this, "You are viewing as Hotel", Toast.LENGTH_SHORT).show();
                return;
            }
            if (foodIds.isEmpty()) return;

            Intent intent = new Intent(this, FoodDetailsActivity.class);
            intent.putExtra("food_id", foodIds.get(position));
            intent.putExtra("hotel_id", hotelId);
            intent.putExtra("user_id", userId);
            startActivity(intent);
        });
    }

    private void placeOrder(int foodId, double price, int quantity) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        double total = price * quantity;

        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("hotel_id", hotelId);
        values.put("food_id", foodId);
        values.put("quantity", quantity);
        values.put("total_price", total);
        values.put("date", currentDate);
        values.put("time", currentTime);
        values.put("status", "Placed");

        long result = db.insert("orders", null, values);
        db.close();

        if (result != -1) {
            Toast.makeText(this, "Ordered " + quantity + " item(s) successfully!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to place order", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadHotelInfo() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT name, address FROM hotels WHERE hotel_id=?",
                new String[]{String.valueOf(hotelId)});
        if (c.moveToFirst()) {
            tvHotelName.setText(c.getString(0));
            tvHotelAddress.setText(c.getString(1));
        }
        c.close();
        db.close();
    }

    private void loadDailySpecial() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        specialFoodIds.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT ds.food_id, f.name, f.price FROM daily_specials ds JOIN food f ON ds.food_id=f.food_id WHERE ds.hotel_id=? AND ds.date=? ORDER BY f.name",
                new String[]{String.valueOf(hotelId), today});
        StringBuilder specials = new StringBuilder("★ TODAY'S SPECIALS\n");
        while (c.moveToNext()) {
            int id=c.getInt(0); specialFoodIds.add(id);
            if (specials.charAt(specials.length()-1)!='\n') specials.append("\n");
            specials.append("• ").append(c.getString(1)).append(" — ₹").append(c.getDouble(2));
        }
        c.close(); db.close();
        if (specialFoodIds.isEmpty()) specials = new StringBuilder("No daily special set today");
        tvDailySpecial.setText(specials.toString());
    }

    private void loadMenu() {
        menuItems.clear(); foodIds.clear(); foodPrices.clear(); foodNames.clear(); foodRows.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT food_id, name, price, category, image_url FROM food WHERE hotel_id=? AND availability=1 ORDER BY name",
                new String[]{String.valueOf(hotelId)});
        while (c.moveToNext()) {
            int id=c.getInt(0); String name=c.getString(1); double price=c.getDouble(2);
            foodIds.add(id); foodNames.add(name); foodPrices.add(price);
            String category=c.isNull(3)?"Food":c.getString(3);
            foodRows.add(new FoodRowAdapter.FoodRow(id,name,category+"  •  ₹"+String.format(Locale.getDefault(),"%.2f",price),c.isNull(4)?"":c.getString(4),specialFoodIds.contains(id)));
        }
        c.close(); db.close();
        if (foodRows.isEmpty()) { menuItems.clear(); menuItems.add("No food items available yet"); listMenu.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,menuItems)); }
        else listMenu.setAdapter(new FoodRowAdapter(this,foodRows));
    }
}
