package com.example.smartdining;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import android.support.v7.app.AppCompatActivity;

public class AddFoodActivity extends AppCompatActivity {

    EditText etFoodName, etCategory, etDescription, etPrice, etImageUrl;
    Button btnSaveFood;
    DatabaseHelper dbHelper;
    int hotelId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_food);
        AppChrome.install(this, "Add Food Item", false, false);

        etFoodName = findViewById(R.id.etFoodName);
        etCategory = findViewById(R.id.etCategory);
        etDescription = findViewById(R.id.etDescription);
        etPrice = findViewById(R.id.etPrice);
        etImageUrl = findViewById(R.id.etImageUrl);
        btnSaveFood = findViewById(R.id.btnSaveFood);

        hotelId = getIntent().getIntExtra("hotel_id", -1);
        dbHelper = new DatabaseHelper(this);

        btnSaveFood.setOnClickListener(v -> saveFood());
    }

    private void saveFood() {
        String name = etFoodName.getText().toString().trim();
        String category = etCategory.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();

        if (name.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "Name and Price are required", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        try { price = Double.parseDouble(priceStr); if (price < 0) throw new NumberFormatException(); }
        catch (NumberFormatException e) { Toast.makeText(this, "Enter a valid non-negative price", Toast.LENGTH_SHORT).show(); return; }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("hotel_id", hotelId);
        values.put("name", name);
        values.put("category", category);
        values.put("description", description);
        values.put("image_url", etImageUrl.getText().toString().trim());
        values.put("price", price);
        values.put("rating", 0);
        values.put("availability", 1);

        long result = db.insert("food", null, values);
        db.close();

        if (result != -1) {
            Toast.makeText(this, "Food item added successfully!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to add food", Toast.LENGTH_SHORT).show();
        }
    }
}
