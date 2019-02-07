package com.pewick.calllogger.database

import android.provider.BaseColumns

/**
 * Defines the database tables used by the application, and the strings used to create or delete them.
 */
object DataContract {

    val SQL_CREATE_CALL_TABLE = (
            "CREATE TABLE " + CallTable.TABLE_NAME + " ("
                    + CallTable.CALL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + CallTable.NUMBER + " INTEGER,"
                    + CallTable.START_TIME + " INTEGER,"
                    + CallTable.END_TIME + " INTEGER,"
                    + CallTable.INCOMING_OUTGOING + " TEXT,"
                    + CallTable.ANSWERED_MISSED + " TEXT)")

    val SQL_CREATE_NUMBERS_TABLE = (
            "CREATE TABLE " + NumbersTable.TABLE_NAME + " ("
                    + NumbersTable.NUMBER + " INTEGER PRIMARY KEY,"
                    + NumbersTable.MOST_RECENT + " INTEGER,"
                    + NumbersTable.NOTES + " TEXT,"
                    + NumbersTable.OUTGOING_COUNT + " INTEGER,"
                    + NumbersTable.ANSWERED_COUNT + " INTEGER,"
                    + NumbersTable.MISSED_COUNT + " INTEGER)")

    val SQL_DELETE_CALL_TABLE = "DROP TABLE IF EXISTS " + CallTable.TABLE_NAME
    val SQL_DELETE_NUMBERS_TABLE = "DROP TABLE IF EXISTS " + NumbersTable.TABLE_NAME

    abstract class CallTable : BaseColumns {
        companion object {
            val TABLE_NAME = "calls"
            val CALL_ID = "call_id"
            val NUMBER = "number"
            val START_TIME = "start_time"
            val END_TIME = "end_time"
            val INCOMING_OUTGOING = "incoming_outgoing"
            val ANSWERED_MISSED = "answered_missed"
        }
    }

    //I may want to add an id for each number, could use an auto incrementing key
    abstract class NumbersTable : BaseColumns {
        companion object {
            val TABLE_NAME = "numbers"
            val NUMBER = "number"
            val MOST_RECENT = "most_recent"
            val NOTES = "notes"
            val OUTGOING_COUNT = "outgoing_count"
            val ANSWERED_COUNT = "answered_count"
            val MISSED_COUNT = "missed_count"
        }
    }
}
