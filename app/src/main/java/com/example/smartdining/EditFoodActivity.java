package com.example.smartdining;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import android.support.v7.app.AppCompatActivity;

public class EditFoodActivity extends AppCompatActivity {

    EditText etFoodName, etCategory, etDescription, etPrice, etImageUrl;
    Button btnUpdateFood;
    DatabaseHelper dbHelper;
    int foodId, hotelId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_food);
        AppChrome.install(this, "Edit Food Item", false, false);

        etFoodName = findViewById(R.id.etFoodName);
        etCategory = findViewById(R.id.etCategory);
        etDescription = findViewById(R.id.etDescription);
        etPrice = findViewById(R.id.etPrice);
        etImageUrl = findViewById(R.id.etImageUrl);
        btnUpdateFood = findViewById(R.id.btnUpdateFood);

        foodId = getIntent().getIntExtra("food_id", -1);
        hotelId = getIntent().getIntExtra("hotel_id", -1);
        dbHelper = new DatabaseHelper(this);

        loadFoodData();

        btnUpdateFood.setOnClickListener(v -> updateFood());
    }

    private void loadFoodData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT name, category, description, price, image_url FROM food WHERE food_id=?",
                new String[]{String.valueOf(foodId)});

        if (c.moveToFirst()) {
            etFoodName.setText(c.getString(0));
            etCategory.setText(c.getString(1));
            etDescription.setText(c.getString(2));
            etPrice.setText(String.valueOf(c.getDouble(3)));
            etImageUrl.setText(c.isNull(4) ? "" : c.getString(4));
        }
        c.close();
        db.close();
    }

    private void updateFood() {
        String name = etFoodName.getText().toString().trim();
        String category = etCategory.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();

        if (name.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "Name and Price required", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        try { price = Double.parseDouble(priceStr); if (price < 0) throw new NumberFormatException(); }
        catch (NumberFormatException e) { Toast.makeText(this, "Enter a valid non-negative price", Toast.LENGTH_SHORT).show(); return; }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("category", category);
        values.put("description", description);
        values.put("image_url", etImageUrl.getText().toString().trim());
        values.put("price", price);

        int rows = db.update("food", values, "food_id=?", new String[]{String.valueOf(foodId)});
        db.close();

        if (rows > 0) {
            Toast.makeText(this, "Food updated successfully", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
        }
    }
}
