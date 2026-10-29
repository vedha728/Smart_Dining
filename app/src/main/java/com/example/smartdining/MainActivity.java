package com.example.smartdining;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Spinner spAccountType;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        spAccountType = findViewById(R.id.spAccountType);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvRegister);
        dbHelper = new DatabaseHelper(this);

        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Customer", "Hotel"});
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spAccountType.setAdapter(roleAdapter);

        btnLogin.setOnClickListener(v -> login());
        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, CustomerRegisterActivity.class)));
    }

    private void login() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String role = spAccountType.getSelectedItem().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor;
        if ("Customer".equals(role)) {
            cursor = db.rawQuery("SELECT user_id, name FROM users WHERE email=? AND password=?",
                    new String[]{email, password});
        } else {
            cursor = db.rawQuery("SELECT hotel_id, name FROM hotels WHERE email=? AND password=?",
                    new String[]{email, password});
        }

        if (cursor.moveToFirst()) {
            int accountId = cursor.getInt(0);
            String accountName = cursor.getString(1);
            cursor.close();
            db.close();

            if ("Customer".equals(role)) {
                Intent intent = new Intent(this, CustomerHomeActivity.class);
                intent.putExtra("user_id", accountId);
                intent.putExtra("user_name", accountName);
                startActivity(intent);
            } else {
                Intent intent = new Intent(this, HotelDashboardActivity.class);
                intent.putExtra("hotel_id", accountId);
                intent.putExtra("hotel_name", accountName);
                startActivity(intent);
            }
            finish();
        } else {
            cursor.close();
            db.close();
            Toast.makeText(this, "Invalid " + role.toLowerCase() + " email or password", Toast.LENGTH_SHORT).show();
        }
    }
}
