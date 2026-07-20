package com.unitloadsystem.db;

import android.content.Context;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by KSC on 2015-01-24.
 */
public class MySQLiteOpenHelper extends SQLiteOpenHelper{
    public static final int DB_VERSION = 6;

    private final String palletDBName = "palletdb";
    private final String boxDBName = "boxdb";

    public MySQLiteOpenHelper(Context context, String  name, SQLiteDatabase.CursorFactory factory, int version){
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        createPalletTable(db);
        createBoxTable(db);
        insertStandardPallets(db);
    }

    private void createPalletTable(SQLiteDatabase db){
        String sql = "create table if not exists " + palletDBName + " (" +
                "name text primary key, " +
                "width integer, " +
                "height integer, " +
                "pallet_height integer not null default 150, " +
                "unit text);";
        db.execSQL(sql);
    }

    private void createBoxTable(SQLiteDatabase db){
        String sql = "create table if not exists " + boxDBName + " (" +
                "name text primary key, " +
                "box_length real, " +
                "box_width real, " +
                "box_height real, " +
                "unit text, " +
                "weight real, " +
                "weight_unit text);";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        if(oldVersion < 2){
            createBoxTable(db);
        }
        if(oldVersion < 3){
            db.execSQL("alter table " + palletDBName
                    + " add column pallet_height integer not null default 150");
            db.execSQL("update " + palletDBName
                    + " set pallet_height = 6 where lower(unit) = 'inch'");
        }
        if (oldVersion < 4) {
            insertStandardPallets(db);
        }
        if (oldVersion < 5) {
            insertKoreanPallets(db);
        }
        if (oldVersion < 6) {
            removeRetiredDefaultPallets(db);
        }
    }

    private void insertStandardPallets(SQLiteDatabase db) {
        insertPalletIfMissing(db, "EPAL 1 (EUR) - 1200 x 800 mm", 1200, 800, 144);
        insertPalletIfMissing(db, "GMA (US) - 1219 x 1016 mm", 1219, 1016, 150);
        insertKoreanPallets(db);
    }

    private void insertKoreanPallets(SQLiteDatabase db) {
        insertPalletIfMissing(db, "Korea Common (1111) - 1100 x 1100 mm", 1100, 1100, 120);
        insertPalletIfMissing(db, "Korea Export (1210) - 1200 x 1000 mm", 1200, 1000, 120);
    }

    private void removeRetiredDefaultPallets(SQLiteDatabase db) {
        db.delete(palletDBName, "name IN (?, ?, ?)", new String[]{
                "ISO - 1200 x 1000 mm",
                "JIS (Japan) - 1100 x 1100 mm",
                "Australian - 1165 x 1165 mm"
        });
    }

    private void insertPalletIfMissing(SQLiteDatabase db, String name, int width, int length,
                                       int palletHeight) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("width", width);
        values.put("height", length);
        values.put("pallet_height", palletHeight);
        values.put("unit", "mm");
        db.insertWithOnConflict(palletDBName, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }
}
