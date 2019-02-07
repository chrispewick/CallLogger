package com.pewick.calllogger.database

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import android.util.Log

import com.pewick.calllogger.models.CallItem
import com.pewick.calllogger.models.NumberItem

import java.util.ArrayList

/**
 * A helper class to manage database creation and version management.
 */
class DbHelper
/**
 * Create a helper object to create, open, and/or manage a database. Uses the version number
 * hardcoded in this class.
 * @param context the context of the application.
 */
(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    private val TAG = javaClass.simpleName
    private var numbersList: ArrayList<NumberItem>? = null
    private var callsList: ArrayList<CallItem>? = null

    /**
     * Called only when the database is created for the first time. Creates the tables needed by
     * the application.
     * @param db the SQLite database containing the application's tables.
     */
    override fun onCreate(db: SQLiteDatabase) {
        Log.i(TAG, "onCreate, version: " + db.version)
        //Delete
        db.execSQL(DataContract.SQL_DELETE_CALL_TABLE)
        db.execSQL(DataContract.SQL_DELETE_NUMBERS_TABLE)

        //Create
        db.execSQL(DataContract.SQL_CREATE_CALL_TABLE)
        db.execSQL(DataContract.SQL_CREATE_NUMBERS_TABLE)
    }

    /**
     * Called when the database needs to be upgraded, indicated by a new version number. The
     * implementation of this method should drop tables, add tables, or do anything else it needs
     * to upgrade to the new schema version.
     *
     * @param db the SQLite database.
     * @param oldVersion the old version number.
     * @param newVersion the new version number.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.i(TAG, "onUpgrade")
        // Write logic to preserve the data currently in the user's database, then implement the
        // changes to the database tables, finally, copy the original data back into the new tables.

        if (oldVersion < 4) {
            //From this version on, I maintained a count of all calls for each number.
            // Earlier versions did not have this feature, so I need to save that earlier data,
            // then calculate the counts, then store it all back in the database.

            //Retrieve all data from the tables in the existing database
            this.readNumbersFromDatabaseVersion3(db)
            this.readCallsFromDatabase(db)

            //Delete the old tables
            db.execSQL(DataContract.SQL_DELETE_CALL_TABLE)
            db.execSQL(DataContract.SQL_DELETE_NUMBERS_TABLE)
            //Create the new tables
            db.execSQL(DataContract.SQL_CREATE_CALL_TABLE)
            db.execSQL(DataContract.SQL_CREATE_NUMBERS_TABLE)

            for (callItem in callsList!!) {
                this.addCallToTable(db, callItem)
                //Now, find the corresponding number and increment the appropriate count
                for (numItem in numbersList!!) {
                    if (callItem.number == numItem.number) {
                        Log.i(TAG, "Found matching number for call item")
                        when (callItem.callType) {
                            1 ->
                                //Outgoing
                                numItem.outgoingCount = numItem.outgoingCount + 1
                            2 ->
                                //Answered
                                numItem.answeredCount = numItem.answeredCount + 1
                            3 ->
                                //Missed
                                numItem.missedCount = numItem.missedCount + 1
                        }
                        break
                    }
                }
            }
            for (numberItem in numbersList!!) {
                this.addNumberToTable(db, numberItem)
            }
        }
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        //        onUpgrade(db, oldVersion, newVersion);
    }

    private fun readNumbersFromDatabase(database: SQLiteDatabase) {
        numbersList = ArrayList()

        val projection = arrayOf(DataContract.NumbersTable.NUMBER, DataContract.NumbersTable.MOST_RECENT, DataContract.NumbersTable.NOTES, DataContract.NumbersTable.OUTGOING_COUNT, DataContract.NumbersTable.ANSWERED_COUNT, DataContract.NumbersTable.MISSED_COUNT)

        //fetch the data from the database as specified
        database.beginTransaction()
        val cursor = database.query(DataContract.NumbersTable.TABLE_NAME,
                projection, null, null, null, null, null)
        database.setTransactionSuccessful()
        database.endTransaction()
        if (cursor.moveToFirst()) {
            do {
                val number = cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.NUMBER))
                //                Log.i(TAG, "Number: "+number);

                val contact = getContactName(context, java.lang.Long.toString(number))

                val existingNumber = NumberItem(number,
                        cursor.getInt(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.MOST_RECENT)),
                        contact!!,
                        cursor.getString(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.NOTES)))

                numbersList!!.add(existingNumber)
            } while (cursor.moveToNext())
        }
        cursor.close()
    }

    /**
     * This is an old version. In this version, there were no count fields stored in the table.
     * Used to upgrade installed apps to version 4's new database schema.
     * @param database
     */
    private fun readNumbersFromDatabaseVersion3(database: SQLiteDatabase) {
        numbersList = ArrayList()
        val projection = arrayOf(DataContract.NumbersTable.NUMBER, DataContract.NumbersTable.MOST_RECENT, DataContract.NumbersTable.NOTES)

        //fetch the data from the database as specified
        database.beginTransaction()
        val cursor = database.query(DataContract.NumbersTable.TABLE_NAME,
                projection, null, null, null, null, null)
        database.setTransactionSuccessful()
        database.endTransaction()
        if (cursor.moveToFirst()) {
            do {
                val number = cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.NUMBER))
                //                Log.i(TAG, "Number: "+number);

                val contact = getContactName(context, java.lang.Long.toString(number))

                val existingNumber = NumberItem(number,
                        cursor.getInt(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.MOST_RECENT)),
                        contact!!,
                        cursor.getString(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.NOTES)))

                numbersList!!.add(existingNumber)
            } while (cursor.moveToNext())
        }
        cursor.close()
    }

    private fun getContactName(context: Context, phoneNumber: String): String? {
        var contactName: String? = null
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            val cr = context.contentResolver
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
            val cursor = cr.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)
                    ?: return null
            if (cursor.moveToFirst()) {
                contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME))
            }
            cursor.close()
        }
        return contactName
    }

    private fun readCallsFromDatabase(database: SQLiteDatabase) {
        Log.i(TAG, "readCallsFromDatabase")
        callsList = ArrayList()
        val projection = arrayOf(DataContract.CallTable.CALL_ID, DataContract.CallTable.NUMBER, DataContract.CallTable.START_TIME, DataContract.CallTable.END_TIME, DataContract.CallTable.INCOMING_OUTGOING, DataContract.CallTable.ANSWERED_MISSED)

        //specify read order based on number
        val sortOrder = DataContract.CallTable.START_TIME + " DESC"

        //fetch the data from the database as specified
        val cursor = database.query(DataContract.CallTable.TABLE_NAME, projection, null, null, null, null, sortOrder)
        if (cursor.moveToFirst()) {
            do {
                val existingCall = CallItem(cursor.getInt(cursor.getColumnIndexOrThrow(DataContract.CallTable.CALL_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.CallTable.NUMBER)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.CallTable.START_TIME)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.CallTable.END_TIME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DataContract.CallTable.INCOMING_OUTGOING)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DataContract.CallTable.ANSWERED_MISSED)))

                existingCall.contactName = getContactName(context, java.lang.Long.toString(existingCall.number))
                callsList!!.add(existingCall)
            } while (cursor.moveToNext())
        }
        cursor.close()

    }

    private fun addNumberToTable(database: SQLiteDatabase, numberItem: NumberItem) {
        val sqlValuesStatement = ContentValues()
        sqlValuesStatement.put(DataContract.NumbersTable.NUMBER, numberItem.number)
        sqlValuesStatement.put(DataContract.NumbersTable.MOST_RECENT, numberItem.mostRecentCallId)
        sqlValuesStatement.put(DataContract.NumbersTable.NOTES, numberItem.notes)
        sqlValuesStatement.put(DataContract.NumbersTable.OUTGOING_COUNT, numberItem.outgoingCount)
        sqlValuesStatement.put(DataContract.NumbersTable.ANSWERED_COUNT, numberItem.answeredCount)
        sqlValuesStatement.put(DataContract.NumbersTable.MISSED_COUNT, numberItem.missedCount)

        database.beginTransaction()
        database.insert(DataContract.NumbersTable.TABLE_NAME, null, sqlValuesStatement)
        database.setTransactionSuccessful()
        database.endTransaction()
    }

    private fun addCallToTable(database: SQLiteDatabase, callItem: CallItem): Long {
        val sqlValuesStatement = ContentValues()
        sqlValuesStatement.put(DataContract.CallTable.NUMBER, callItem.number)
        sqlValuesStatement.put(DataContract.CallTable.START_TIME, callItem.startTime)
        sqlValuesStatement.put(DataContract.CallTable.END_TIME, callItem.endTime)

        sqlValuesStatement.put(DataContract.CallTable.INCOMING_OUTGOING, callItem.inOut)
        sqlValuesStatement.put(DataContract.CallTable.ANSWERED_MISSED, callItem.ansMiss)
        sqlValuesStatement.put(DataContract.CallTable.CALL_ID, callItem.callId)

        database.beginTransaction()
        val rowID = database.insert(DataContract.CallTable.TABLE_NAME, null, sqlValuesStatement)
        database.setTransactionSuccessful()
        database.endTransaction()

        return rowID
    }

    companion object {

        // NOTE: if the database schema is updated, this version number MUST be incremented
        val DATABASE_VERSION = 4
        val DATABASE_NAME = "CallLogger.db"

        private var dbHelperInstance: DbHelper? = null

        /**
         * Ensures that only one DbHelper will exist at any given time.
         * @param context uses this context to get the application context, to further prevent leaks.
         * @return the current instance of DbHelper if one exists, otherwise a new instance.
         */
        @Synchronized
        fun getInstance(context: Context): DbHelper {
            if (dbHelperInstance == null) {
                dbHelperInstance = DbHelper(context.applicationContext)
            }
            return dbHelperInstance!!
        }
    }
}
