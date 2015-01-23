package com.unitloadsystem.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by KSC on 2015-01-24.
 */
public class MySQLiteOpenHelper extends SQLiteOpenHelper{
    private final String palletDBName = "palletdb";
    private final String containerDBName = "containerdb";

    public MySQLiteOpenHelper(Context context, String  name, SQLiteDatabase.CursorFactory factory, int version){
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        String sql = "create table " + palletDBName + " (" +
                "name text primary key, " +
                "width integer, " +
                "height integer, " +
                "unit text);";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        String sql = "drop table if exists " + palletDBName;
        db.execSQL(sql);

        onCreate(db);
    }
}
