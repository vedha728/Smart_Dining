package com.example.smartdining;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import android.support.v7.app.AppCompatActivity;

public class HotelLoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin;
    TextView tvGoToRegister;
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hotel_login);
        AppChrome.install(this, "Hotel Login", false, false);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);

        dbHelper = new DatabaseHelper(this);

        btnLogin.setOnClickListener(v -> loginHotel());

        tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, HotelRegisterActivity.class));
            finish();
        });
    }

    private void loginHotel() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT hotel_id, name FROM hotels WHERE email=? AND password=?",
                new String[]{email, password});

        if (cursor.moveToFirst()) {
            int hotelId = cursor.getInt(0);
            String hotelName = cursor.getString(1);
            cursor.close();
            db.close();

            Toast.makeText(this, "Welcome " + hotelName, Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(this, HotelDashboardActivity.class);
            intent.putExtra("hotel_id", hotelId);
            intent.putExtra("hotel_name", hotelName);
            startActivity(intent);
            finish();
        } else {
            cursor.close();
            db.close();
            Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show();
        }
    }
}