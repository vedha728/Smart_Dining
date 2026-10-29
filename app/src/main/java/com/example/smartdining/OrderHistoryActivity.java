package com.example.smartdining;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Locale;

public class OrderHistoryActivity extends AppCompatActivity {

    ListView listOrders;
    DatabaseHelper dbHelper;
    int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);
        AppChrome.install(this, "Order History", false, false);

        listOrders = findViewById(R.id.listOrders);
        userId = getIntent().getIntExtra("user_id", -1);
        dbHelper = new DatabaseHelper(this);

        loadOrders();
    }

    private void loadOrders() {
        ArrayList<String> orders = new ArrayList<>();
        ArrayList<FoodRowAdapter.FoodRow> orderRows = new ArrayList<>();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT o.quantity, o.total_price, o.date, o.time, o.status, f.name, h.name, f.image_url, o.order_id, f.food_id " +
                        "FROM orders o " +
                        "JOIN food f ON o.food_id = f.food_id " +
                        "JOIN hotels h ON o.hotel_id = h.hotel_id " +
                        "WHERE o.user_id=? ORDER BY o.order_id DESC",
                new String[]{String.valueOf(userId)});

        if (c.moveToFirst()) {
            do {
                String line = c.getString(5) + " x" + c.getInt(0) +
                        "\nHotel: " + c.getString(6) +
                        "\n₹" + c.getDouble(1) + " | " + c.getString(2) + " " + c.getString(3) +
                        " | " + c.getString(4);
                orders.add(line);
                orderRows.add(new FoodRowAdapter.FoodRow(c.getInt(9), c.getString(5), c.getString(6)+"  •  "+c.getInt(0)+" ×  ₹"+String.format(Locale.getDefault(),"%.2f",c.getDouble(1))+"\n"+c.getString(2)+" "+c.getString(3)+"  •  "+c.getString(4), c.isNull(7)?"":c.getString(7), false));
            } while (c.moveToNext());
        }
        c.close();
        db.close();

        if (orders.isEmpty()) {
            orders.add("No orders yet");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, orders);
        if (orderRows.isEmpty()) listOrders.setAdapter(adapter); else listOrders.setAdapter(new FoodRowAdapter(this, orderRows));
    }
}