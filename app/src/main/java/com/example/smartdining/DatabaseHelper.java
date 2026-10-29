package com.example.smartdining;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "SmartDining.db";
    private static final int DATABASE_VERSION = 3;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // Users
        db.execSQL("CREATE TABLE users (" +
                "user_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "email TEXT UNIQUE," +
                "password TEXT," +
                "phone TEXT," +
                "address TEXT," +
                "preferences TEXT)");

        // Hotels
        db.execSQL("CREATE TABLE hotels (" +
                "hotel_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "email TEXT UNIQUE," +
                "password TEXT," +
                "phone TEXT," +
                "address TEXT)");

        // Food
        db.execSQL("CREATE TABLE food (" +
                "food_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "hotel_id INTEGER," +
                "name TEXT," +
                "category TEXT," +
                "description TEXT," +
                "image_url TEXT," +
                "price REAL," +
                "rating REAL DEFAULT 0," +
                "availability INTEGER DEFAULT 1," +
                "FOREIGN KEY(hotel_id) REFERENCES hotels(hotel_id))");

        // Daily Specials
        db.execSQL("CREATE TABLE daily_specials (" +
                "special_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "hotel_id INTEGER," +
                "food_id INTEGER," +
                "description TEXT," +
                "date TEXT," +
                "FOREIGN KEY(hotel_id) REFERENCES hotels(hotel_id))");

        // Orders
        db.execSQL("CREATE TABLE orders (" +
                "order_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER," +
                "hotel_id INTEGER," +
                "food_id INTEGER," +
                "quantity INTEGER," +
                "total_price REAL," +
                "date TEXT," +
                "time TEXT," +
                "status TEXT DEFAULT 'Placed'," +
                "FOREIGN KEY(user_id) REFERENCES users(user_id)," +
                "FOREIGN KEY(hotel_id) REFERENCES hotels(hotel_id)," +
                "FOREIGN KEY(food_id) REFERENCES food(food_id))");

        // Favorites
        db.execSQL("CREATE TABLE favorites (" +
                "user_id INTEGER," +
                "food_id INTEGER," +
                "PRIMARY KEY(user_id, food_id))");

        // ========== NEW CART TABLES ==========
        db.execSQL("CREATE TABLE cart_clusters (" +
                "cluster_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER," +
                "cluster_name TEXT," +
                "FOREIGN KEY(user_id) REFERENCES users(user_id))");

        db.execSQL("CREATE TABLE cart_items (" +
                "item_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "cluster_id INTEGER," +
                "food_id INTEGER," +
                "hotel_id INTEGER," +
                "quantity INTEGER," +
                "unit_price REAL," +
                "FOREIGN KEY(cluster_id) REFERENCES cart_clusters(cluster_id)," +
                "FOREIGN KEY(food_id) REFERENCES food(food_id)," +
                "FOREIGN KEY(hotel_id) REFERENCES hotels(hotel_id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Preserve existing accounts, menu items, carts and orders during upgrades.
        if (oldVersion < 2) {
            try { db.execSQL("ALTER TABLE food ADD COLUMN image_url TEXT"); }
            catch (Exception ignored) { /* Column already exists in some installs. */ }
        }
        // Cart clusters were introduced after the first database version. Existing installs
        // must receive these tables too; onCreate() alone does not run for existing databases.
        db.execSQL("CREATE TABLE IF NOT EXISTS cart_clusters (" +
                "cluster_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER NOT NULL," +
                "cluster_name TEXT NOT NULL," +
                "FOREIGN KEY(user_id) REFERENCES users(user_id))");
        db.execSQL("CREATE TABLE IF NOT EXISTS cart_items (" +
                "item_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "cluster_id INTEGER NOT NULL," +
                "food_id INTEGER NOT NULL," +
                "hotel_id INTEGER NOT NULL," +
                "quantity INTEGER NOT NULL DEFAULT 1," +
                "unit_price REAL NOT NULL DEFAULT 0," +
                "FOREIGN KEY(cluster_id) REFERENCES cart_clusters(cluster_id)," +
                "FOREIGN KEY(food_id) REFERENCES food(food_id)," +
                "FOREIGN KEY(hotel_id) REFERENCES hotels(hotel_id))");
    }
}
