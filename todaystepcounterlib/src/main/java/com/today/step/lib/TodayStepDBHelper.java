package com.today.step.lib;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 用来记录当天步数列表，传感器回调30次记录一条数据
 * Created by jiahongfei on 2017/10/9.
 */

public class TodayStepDBHelper extends SQLiteOpenHelper {

    private static final String TAG = "TodayStepDBHelper";

    private static final int VERSION = 1;
    private static final String DATABASE_NAME = "TodayStepDB.db";
    private static final String TABLE_NAME = "TodayStepData";
    private static final String PRIMARY_KEY = "_id";
    public static final String TODAY = "today";
    public static final String DATE = "date";
    public static final String STEP = "step";

    private static final String SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
            + PRIMARY_KEY + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + TODAY + " TEXT, "
            + DATE + " long, "
            + STEP + " long);";
    private static final String SQL_DELETE_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
    private static final String SQL_QUERY_ALL = "SELECT * FROM " + TABLE_NAME;
    private static final String SQL_QUERY_STEP = "SELECT * FROM " + TABLE_NAME + " WHERE " + TODAY + " = ? AND " + STEP + " = ?";

    public TodayStepDBHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        Logger.e(TAG, SQL_CREATE_TABLE);
        db.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        deleteTable();
        onCreate(db);
    }

    public synchronized boolean isExist(TodayStepData todayStepData){
        Cursor cursor = getReadableDatabase().rawQuery(SQL_QUERY_STEP,new String[]{todayStepData.getToday(),todayStepData.getStep() + ""});
        boolean exist = cursor.getCount() > 0 ? true : false;
        cursor.close();
        return exist;
    }

    public synchronized void createTable(){
        getWritableDatabase().execSQL(SQL_CREATE_TABLE);
    }

    public synchronized void insert(TodayStepData todayStepData) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(TODAY, todayStepData.getToday());
        contentValues.put(DATE, todayStepData.getDate());
        contentValues.put(STEP, todayStepData.getStep());
        getWritableDatabase().insert(TABLE_NAME, null, contentValues);
    }

    public synchronized List<TodayStepData> getQueryAll() {
        Cursor cursor = getReadableDatabase().rawQuery(SQL_QUERY_ALL, new String[]{});
        List<TodayStepData> todayStepDatas = new ArrayList<>();
        while (cursor.moveToNext()) {
            String today = cursor.getString(cursor.getColumnIndex(TODAY));
            long date = cursor.getLong(cursor.getColumnIndex(DATE));
            long step = cursor.getLong(cursor.getColumnIndex(STEP));
            TodayStepData todayStepData = new TodayStepData();
            todayStepData.setToday(today);
            todayStepData.setDate(date);
            todayStepData.setStep(step);
            todayStepDatas.add(todayStepData);
        }
        cursor.close();
        return todayStepDatas;
    }

    public synchronized void deleteTable() {
        getWritableDatabase().execSQL(SQL_DELETE_TABLE);
    }
}
