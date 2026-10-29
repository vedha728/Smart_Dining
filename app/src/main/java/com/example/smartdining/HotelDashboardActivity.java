package com.example.smartdining;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import android.support.v7.app.AppCompatActivity;

public class HotelDashboardActivity extends AppCompatActivity {

    TextView tvHotelName;
    Button btnAddFood, btnViewMenu, btnSetSpecial, btnLogout;

    int hotelId;
    String hotelName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hotel_dashboard);
        AppChrome.install(this, "Hotel Dashboard", true, true);

        tvHotelName = findViewById(R.id.tvHotelName);
        btnAddFood = findViewById(R.id.btnAddFood);
        btnViewMenu = findViewById(R.id.btnViewMenu);
        btnSetSpecial = findViewById(R.id.btnSetSpecial);
        btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setVisibility(android.view.View.GONE);

        hotelId = getIntent().getIntExtra("hotel_id", -1);
        hotelName = getIntent().getStringExtra("hotel_name");

        tvHotelName.setText(hotelName + " Dashboard");

        btnAddFood.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddFoodActivity.class);
            intent.putExtra("hotel_id", hotelId);
            startActivity(intent);
        });

        btnViewMenu.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManageMenuActivity.class);
            intent.putExtra("hotel_id", hotelId);
            startActivity(intent);
        });

        btnSetSpecial.setOnClickListener(v -> {
            Intent intent = new Intent(this, SetSpecialActivity.class);
            intent.putExtra("hotel_id", hotelId);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}