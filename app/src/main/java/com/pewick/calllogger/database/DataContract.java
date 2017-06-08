package com.pewick.calllogger.database;

import android.provider.BaseColumns;

/**
 * Created by Chris on 5/16/2017.
 */
public class DataContract {

    public static final String SQL_CREATE_CALL_TABLE =
            "CREATE TABLE " + CallTable.TABLE_NAME + " ("
                    + CallTable.CALL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + CallTable.NUMBER + " INTEGER,"
                    + CallTable.START_TIME + " INTEGER,"
                    + CallTable.END_TIME + " INTEGER,"
                    + CallTable.INCOMING_OUTGOING + " TEXT,"
                    + CallTable.ANSWERED_MISSED + " TEXT)";

    public static final String SQL_CREATE_NUMBERS_TABLE =
            "CREATE TABLE " + NumbersTable.TABLE_NAME + " ("
                    + NumbersTable.NUMBER + " INTEGER PRIMARY KEY,"
                    + NumbersTable.MOST_RECENT + " INTEGER,"
                    + NumbersTable.NOTES + " TEXT)";

//    public static final String SQL_CREATE_INDEX_1_ON_REMINDER_TABLE =
//            "CREATE INDEX IF NOT EXISTS " + CallTable.CALL_ID + " ON "
//                    + CallTable.TABLE_NAME + " (" + CallTable.REMINDER_DATE + ")";

    public static final String SQL_DELETE_CALL_TABLE = "DROP TABLE IF EXISTS " + CallTable.TABLE_NAME;
    public static final String SQL_DELETE_NUMBERS_TABLE = "DROP TABLE IF EXISTS " + NumbersTable.TABLE_NAME;

    public static abstract class CallTable implements BaseColumns {
        public static final String TABLE_NAME = "calls";
        public static final String CALL_ID = "call_id";
        public static final String NUMBER = "number";
        public static final String START_TIME = "start_time";
        public static final String END_TIME = "end_time";
        public static final String INCOMING_OUTGOING = "incoming_outgoing";
        public static final String ANSWERED_MISSED = "answered_missed";
    }

    //I may want to add an id for each number, could use an auto incrementing key
    public static abstract class NumbersTable implements BaseColumns {
        public static final String TABLE_NAME = "numbers";
        public static final String NUMBER = "number";
        public static final String MOST_RECENT = "most_recent";
        public static final String NOTES = "notes";
    }
}
