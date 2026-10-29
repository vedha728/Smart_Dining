package com.example.smartdining;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;

import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;

public class CustomerHomeActivity extends AppCompatActivity {

    TextView tvWelcome;
    ListView listHotels;
    DatabaseHelper dbHelper;

    ArrayList<String> hotelNames = new ArrayList<>();
    ArrayList<Integer> hotelIds = new ArrayList<>();
    ArrayList<String> hotelAddresses = new ArrayList<>();

    int userId;
    String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_home);
        AppChrome.install(this, "Explore Hotels", true, true);

        tvWelcome = findViewById(R.id.tvWelcome);
        listHotels = findViewById(R.id.listHotels);

        userId = getIntent().getIntExtra("user_id", -1);
        userName = getIntent().getStringExtra("user_name");

        tvWelcome.setText("Welcome, " + userName);

        dbHelper = new DatabaseHelper(this);
        loadHotels();
        addAiButton();

        listHotels.setOnItemClickListener((parent, view, position, id) -> {
            if (hotelIds.isEmpty() || position >= hotelIds.size()) return;

            int selectedHotelId = hotelIds.get(position);

            Intent intent = new Intent(CustomerHomeActivity.this, HotelDetailsActivity.class);
            intent.putExtra("hotel_id", selectedHotelId);
            intent.putExtra("user_id", userId);          // important
            startActivity(intent);
        });

        Button btnOrderHistory = findViewById(R.id.btnOrderHistory);

        btnOrderHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, OrderHistoryActivity.class);
            intent.putExtra("user_id", userId);
            startActivity(intent);
        });

        Button btnMyCart = findViewById(R.id.btnMyCart);
        btnMyCart.setOnClickListener(v -> {
            Intent intent = new Intent(this, CartActivity.class);
            intent.putExtra("user_id", userId);
            startActivity(intent);
        });
    }


    private void addAiButton() {
        FrameLayout content = findViewById(android.R.id.content);
        if (!(content.getChildAt(0) instanceof LinearLayout)) return;
        LinearLayout wrapper = (LinearLayout) content.getChildAt(0);
        FrameLayout overlay = new FrameLayout(this);
        // Place the button over the customer home content, never on other screens.
        Button fab = new Button(this);
        fab.setText("✦ AI"); fab.setTextSize(14); fab.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        fab.setTextColor(Color.WHITE); fab.setAllCaps(false); fab.setElevation(AppChrome.dp(this, 8));
        GradientDrawable shape = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(104, 74, 210), Color.rgb(37, 135, 190)});
        shape.setShape(GradientDrawable.OVAL); fab.setBackground(shape);
        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(AppChrome.dp(this, 72), AppChrome.dp(this, 72), Gravity.BOTTOM | Gravity.RIGHT);
        fp.setMargins(0,0,AppChrome.dp(this, 18),AppChrome.dp(this, 20));
        overlay.addView(fab, fp);
        wrapper.addView(overlay, new LinearLayout.LayoutParams(-1, 0));
        // Use a foreground overlay sized to the screen by adding it to the root FrameLayout instead.
        wrapper.removeView(overlay);
        content.addView(overlay, new FrameLayout.LayoutParams(-1,-1));
        fab.setOnClickListener(v -> { Intent i = new Intent(this, AiAssistantActivity.class); i.putExtra("user_id", userId); i.putExtra("user_name", userName); startActivity(i); });
    }

    private void loadHotels() {
        hotelNames.clear();
        hotelIds.clear(); hotelAddresses.clear();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT hotel_id, name, address FROM hotels", null);

        if (cursor.moveToFirst()) {
            do {
                hotelIds.add(cursor.getInt(0));
                hotelNames.add(cursor.getString(1));
                hotelAddresses.add(cursor.isNull(2) ? "" : cursor.getString(2));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        if (hotelNames.isEmpty()) {
            hotelNames.add("No hotels registered yet"); hotelAddresses.add("Register a hotel to get started");
        }

        listHotels.setAdapter(new HotelRowAdapter(this, hotelNames, hotelAddresses, hotelIds));
    }
}