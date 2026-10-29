package com.example.smartdining;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import android.support.v7.app.AppCompatActivity;

public class HotelRegisterActivity extends AppCompatActivity {

    EditText etHotelName, etEmail, etPassword, etPhone, etAddress;
    Button btnRegister;
    TextView tvGoToLogin;
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hotel_register);
        AppChrome.install(this, "Hotel Registration", false, false);

        etHotelName = findViewById(R.id.etHotelName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        dbHelper = new DatabaseHelper(this);

        btnRegister.setOnClickListener(v -> registerHotel());

        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, HotelLoginActivity.class));
            finish();
        });
    }

    private void registerHotel() {
        String name = etHotelName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("email", email);
        values.put("password", password);
        values.put("phone", phone);
        values.put("address", address);

        long result = db.insert("hotels", null, values);
        db.close();

        if (result != -1) {
            Toast.makeText(this, "Hotel Registered Successfully!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, HotelLoginActivity.class));
            finish();
        } else {
            Toast.makeText(this, "Email already exists", Toast.LENGTH_SHORT).show();
        }
    }
}