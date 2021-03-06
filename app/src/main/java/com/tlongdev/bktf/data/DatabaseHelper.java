package com.tlongdev.bktf.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;

import com.tlongdev.bktf.R;
import com.tlongdev.bktf.data.DatabaseContract.*;

public class DatabaseHelper extends SQLiteOpenHelper {

    /**
     * Log tag for logging.
     */
    @SuppressWarnings("unused")
    private static final String LOG_TAG = DatabaseHelper.class.getSimpleName();

    private static DatabaseHelper ourInstance;

    public static DatabaseHelper getInstance(Context context) {
        if (ourInstance == null) {
            ourInstance = new DatabaseHelper(context);
        }
        return ourInstance;
    }

    private static final int DATABASE_VERSION = 10;
    private static final String DATABASE_NAME = "bptf.db";

    private final Context mContext;

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        if (mContext.databaseList().length > 1) {
            mContext.deleteDatabase("pricelist.db");
            mContext.deleteDatabase("items.db");
            mContext.deleteDatabase("backpack.db");
        }

        final String SQL_CREATE_PRICE_LIST_TABLE =
                "CREATE TABLE " + PriceEntry.TABLE_NAME + " (" +
                        PriceEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +

                        PriceEntry.COLUMN_DEFINDEX + " INTEGER NOT NULL, " +
                        PriceEntry.COLUMN_ITEM_QUALITY + " INTEGER NOT NULL, " +
                        PriceEntry.COLUMN_ITEM_TRADABLE + " INTEGER NOT NULL, " +
                        PriceEntry.COLUMN_ITEM_CRAFTABLE + " INTEGER NOT NULL, " +
                        PriceEntry.COLUMN_PRICE_INDEX + " INTEGER NOT NULL, " +
                        PriceEntry.COLUMN_AUSTRALIUM + " INTEGER NOT NULL, " +
                        PriceEntry.COLUMN_CURRENCY + " TEXT NOT NULL, " +
                        PriceEntry.COLUMN_PRICE + " REAL NOT NULL, " +
                        PriceEntry.COLUMN_PRICE_HIGH + " REAL, " +
                        PriceEntry.COLUMN_LAST_UPDATE + " INTEGER NOT NULL, " +
                        PriceEntry.COLUMN_DIFFERENCE + " REAL NOT NULL, " +
                        PriceEntry.COLUMN_WEAPON_WEAR + " INTEGER NOT NULL, " +

                        " UNIQUE (" + PriceEntry.COLUMN_DEFINDEX + ", " +
                        PriceEntry.COLUMN_ITEM_QUALITY + ", " +
                        PriceEntry.COLUMN_ITEM_TRADABLE + ", " +
                        PriceEntry.COLUMN_ITEM_CRAFTABLE + ", " +
                        PriceEntry.COLUMN_PRICE_INDEX + ", " +
                        PriceEntry.COLUMN_AUSTRALIUM + ", " +
                        PriceEntry.COLUMN_WEAPON_WEAR + ") ON CONFLICT REPLACE);";

        final String SQL_CREATE_ITEM_SCHEMA_TABLE =
                "CREATE TABLE " + ItemSchemaEntry.TABLE_NAME + " (" +
                        ItemSchemaEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +

                        ItemSchemaEntry.COLUMN_DEFINDEX + " INTEGER NOT NULL, " +
                        ItemSchemaEntry.COLUMN_ITEM_NAME + " INTEGER NOT NULL, " +
                        ItemSchemaEntry.COLUMN_DESCRIPTION + " TEXT, " +
                        ItemSchemaEntry.COLUMN_TYPE_NAME + " INTEGER NOT NULL, " +
                        ItemSchemaEntry.COLUMN_PROPER_NAME + " INTEGER NOT NULL, " +
                        ItemSchemaEntry.COLUMN_IMAGE_LARGE + " TEXT, " +
                        ItemSchemaEntry.COLUMN_IMAGE + " TEXT, " +

                        " UNIQUE (" + PriceEntry.COLUMN_DEFINDEX + ") ON CONFLICT REPLACE);";

        final String SQL_CREATE_UNUSUAL_SCHEMA_TABLE =
                "CREATE TABLE " + UnusualSchemaEntry.TABLE_NAME + " (" +
                        UnusualSchemaEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +

                        UnusualSchemaEntry.COLUMN_ID + " INTEGER NOT NULL, " +
                        UnusualSchemaEntry.COLUMN_NAME + " TEXT NOT NULL, " +

                        " UNIQUE (" + UnusualSchemaEntry.COLUMN_ID + ") ON CONFLICT REPLACE);";

        final String SQL_CREATE_ORIGIN_NAMES_TABLE =
                "CREATE TABLE " + OriginEntry.TABLE_NAME + " (" +
                        OriginEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +

                        OriginEntry.COLUMN_ID + " INTEGER NOT NULL, " +
                        OriginEntry.COLUMN_NAME + " TEXT NOT NULL, " +

                        " UNIQUE (" + OriginEntry.COLUMN_ID + ") ON CONFLICT REPLACE);";

        final String SQL_CREATE_DECORATED_WEAPONS_TABLE_TABLE =
                "CREATE TABLE " + DecoratedWeaponEntry.TABLE_NAME + " (" +
                        DecoratedWeaponEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +

                        DecoratedWeaponEntry.COLUMN_DEFINDEX + " INTEGER NOT NULL, " +
                        DecoratedWeaponEntry.COLUMN_GRADE + " INTEGER NOT NULL, " +

                        " UNIQUE (" + DecoratedWeaponEntry.COLUMN_DEFINDEX + ") ON CONFLICT REPLACE);";

        final String SQL_CREATE_FAVORITES_TABLE =
                "CREATE TABLE " + FavoritesEntry.TABLE_NAME + " (" +
                        FavoritesEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +

                        FavoritesEntry.COLUMN_DEFINDEX + " INTEGER NOT NULL, " +
                        FavoritesEntry.COLUMN_ITEM_QUALITY + " INTEGER NOT NULL, " +
                        FavoritesEntry.COLUMN_ITEM_TRADABLE + " INTEGER NOT NULL, " +
                        FavoritesEntry.COLUMN_ITEM_CRAFTABLE + " INTEGER NOT NULL, " +
                        FavoritesEntry.COLUMN_PRICE_INDEX + " INTEGER NOT NULL, " +
                        FavoritesEntry.COLUMN_AUSTRALIUM + " INTEGER NOT NULL, " +
                        FavoritesEntry.COLUMN_WEAPON_WEAR + " INTEGER NOT NULL, " +

                        " UNIQUE (" + FavoritesEntry.COLUMN_DEFINDEX + ", " +
                        FavoritesEntry.COLUMN_ITEM_QUALITY + ", " +
                        FavoritesEntry.COLUMN_ITEM_TRADABLE + ", " +
                        FavoritesEntry.COLUMN_ITEM_CRAFTABLE + ", " +
                        FavoritesEntry.COLUMN_PRICE_INDEX + ", " +
                        FavoritesEntry.COLUMN_AUSTRALIUM + ", " +
                        FavoritesEntry.COLUMN_WEAPON_WEAR + ") ON CONFLICT REPLACE);";

        final String SQL_CREATE_CALCULATOR_TABLE =
                "CREATE TABLE " + CalculatorEntry.TABLE_NAME + " (" +
                        CalculatorEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +

                        CalculatorEntry.COLUMN_DEFINDEX + " INTEGER NOT NULL, " +
                        CalculatorEntry.COLUMN_ITEM_QUALITY + " INTEGER NOT NULL, " +
                        CalculatorEntry.COLUMN_ITEM_TRADABLE + " INTEGER NOT NULL, " +
                        CalculatorEntry.COLUMN_ITEM_CRAFTABLE + " INTEGER NOT NULL, " +
                        CalculatorEntry.COLUMN_PRICE_INDEX + " INTEGER NOT NULL, " +
                        CalculatorEntry.COLUMN_AUSTRALIUM + " INTEGER NOT NULL, " +
                        CalculatorEntry.COLUMN_WEAPON_WEAR + " INTEGER NOT NULL, " +
                        CalculatorEntry.COLUMN_COUNT + " INTEGER NOT NULL, " +

                        " UNIQUE (" + CalculatorEntry.COLUMN_DEFINDEX + ", " +
                        CalculatorEntry.COLUMN_ITEM_QUALITY + ", " +
                        CalculatorEntry.COLUMN_ITEM_TRADABLE + ", " +
                        CalculatorEntry.COLUMN_ITEM_CRAFTABLE + ", " +
                        CalculatorEntry.COLUMN_PRICE_INDEX + ", " +
                        CalculatorEntry.COLUMN_AUSTRALIUM + ", " +
                        CalculatorEntry.COLUMN_WEAPON_WEAR + ") ON CONFLICT REPLACE);";

        db.execSQL(SQL_CREATE_PRICE_LIST_TABLE);
        db.execSQL(SQL_CREATE_ITEM_SCHEMA_TABLE);
        db.execSQL(SQL_CREATE_UNUSUAL_SCHEMA_TABLE);
        db.execSQL(SQL_CREATE_ORIGIN_NAMES_TABLE);
        db.execSQL(SQL_CREATE_DECORATED_WEAPONS_TABLE_TABLE);
        db.execSQL(SQL_CREATE_FAVORITES_TABLE);
        db.execSQL(SQL_CREATE_CALCULATOR_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + PriceEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ItemSchemaEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + UnusualSchemaEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + OriginEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DecoratedWeaponEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + FavoritesEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + CalculatorEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS backpack");
        db.execSQL("DROP TABLE IF EXISTS backpack_guest");

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();

        editor.remove(mContext.getString(R.string.pref_last_item_schema_update));
        editor.remove(mContext.getString(R.string.pref_last_price_list_update));

        editor.apply();

        if (oldVersion < 7) {
            mContext.deleteDatabase("pricelist.db");
            mContext.deleteDatabase("items.db");
            mContext.deleteDatabase("backpack.db");
        }

        onCreate(db);
    }
}
