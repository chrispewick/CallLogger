package com.pewick.calllogger.receivers

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast

import com.pewick.calllogger.database.DataContract
import com.pewick.calllogger.database.DbHelper

import java.util.Date

/**
 * Custom receiver to handle call events.
 */
class CallReceiver : PhoneCallReceiver() {

    private val TAG = javaClass.simpleName
    private val INCOMING = "incoming"
    private val OUTGOING = "outgoing"
    private val ANSWERED = "answered"
    private val MISSED = "missed"

    private var dbHelper: DbHelper? = null
    private var database: SQLiteDatabase? = null

    private val maxCallId: Int
        get() {
            database = dbHelper!!.readableDatabase

            val query = "SELECT MAX(" + DataContract.CallTable.CALL_ID + ") FROM " + DataContract.CallTable.TABLE_NAME
            val stmt = database!!.compileStatement("SELECT MAX("
                    + DataContract.CallTable.CALL_ID + ") FROM " + DataContract.CallTable.TABLE_NAME)

            val id = stmt.simpleQueryForLong().toInt()
            val cursor = database!!.rawQuery(query, null)

            cursor.close()
            database!!.close()

            Log.i("Receiver", "MaxId: $id")
            return id
        }

    override fun onIncomingCallReceived(ctx: Context, number: String?, start: Date) {
        //        Toast.makeText(ctx, "Incoming Received", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Incoming Received: " + number!!)

        //Don't need to add the call to the tables until call has ended (or was missed)
    }

    override fun onIncomingCallAnswered(ctx: Context, number: String?, start: Date) {
        //        Toast.makeText(ctx, "Incoming answered Received", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Incoming answered Received: " + number!!)

        //Don't need to add the call to the tables until call has ended
    }

    override fun onIncomingCallEnded(ctx: Context, number: String?, start: Date?, end: Date) {
        var number = number
        number = removeLeadingOne(number!!)
        dbHelper = DbHelper.getInstance(ctx)
        //        Toast.makeText(ctx, "Incoming ended Received", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Incoming ended Received: $number")

        //Get the current max call_id
        val maxId = this.maxCallId

        //Insert the call into the CallTable
        addCallToTable(ctx, number, maxId + 1, start!!, end, INCOMING, ANSWERED)

        //Check if the number has been logged yet
        if (isNumberInDatabase(number)) {
            //Then the number has been logged before, just update most recent
            updateMostRecentCall(ctx, number, start, (maxId + 1).toLong(), ANSWERED)
        } else {
            //Then the number has not been logged before, so add to table
            addNumberToTable(ctx, number, (maxId + 1).toLong(), ANSWERED)
        }
    }

    override fun onMissedCall(ctx: Context, number: String?, start: Date?) {
        var number = number
        number = removeLeadingOne(number!!)
        dbHelper = DbHelper.getInstance(ctx)
        //        Toast.makeText(ctx, "Missed Received", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Missed Received: $number")

        //Get the current max call_id
        val maxId = this.maxCallId

        //Insert the call into the CallTable
        addCallToTable(ctx, number, maxId + 1, start!!, null, INCOMING, MISSED)

        //Check if the number has been logged yet
        if (isNumberInDatabase(number)) {
            //Then the number has been logged before, just update most recent
            updateMostRecentCall(ctx, number, start, (maxId + 1).toLong(), MISSED)
        } else {
            //Then the number has not been logged before, so add to table
            addNumberToTable(ctx, number, (maxId + 1).toLong(), MISSED)
        }
    }

    override fun onOutgoingCallStarted(ctx: Context, number: String?, start: Date) {
        //        Toast.makeText(ctx, "Outgoing started Received", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Outgoing started Received: " + number!!)

        //Don't need to add the call to the tables until call has ended
    }

    override fun onOutgoingCallEnded(ctx: Context, number: String?, start: Date?, end: Date) {
        var number = number
        number = removeLeadingOne(number!!)
        dbHelper = DbHelper.getInstance(ctx)
        Toast.makeText(ctx, "Outgoing ended Received", Toast.LENGTH_SHORT).show()
        Log.i(TAG, "Outgoing ended Received: $number")

        //Get the current max call_id
        val maxId = this.maxCallId

        //Insert the call into the CallTable
        addCallToTable(ctx, number, maxId + 1, start!!, end, OUTGOING, null)

        //Check if the number has been logged yet
        if (isNumberInDatabase(number)) {
            //Then the number has been logged before, just update most recent
            updateMostRecentCall(ctx, number, start, (maxId + 1).toLong(), OUTGOING)
        } else {
            //Then the number has not been logged before, so add to table
            addNumberToTable(ctx, number, (maxId + 1).toLong(), OUTGOING)
        }
    }

    private fun removeLeadingOne(number: String): String {
        Log.i(TAG, "removeLeadingOne, num length: " + number.length)
        //Is this safe? Would it interfere with non-local numbers? Maybe just out of country?
        return if (number.length == 11) {//Then there is a "1" to remove
            number.substring(1)
        } else if (number.length == 12) {//Then there is a "+1" to remove
            number.substring(2)
        } else {
            number
        }
    }

    private fun addNumberToTable(ctx: Context, number: String?, callId: Long, callType: String) {
        val num = java.lang.Long.parseLong(number!!)

        var countColumn = ""
        if (callType == ANSWERED) {
            countColumn = DataContract.NumbersTable.ANSWERED_COUNT
        } else if (callType == MISSED) {
            countColumn = DataContract.NumbersTable.MISSED_COUNT
        } else if (callType == OUTGOING) {
            countColumn = DataContract.NumbersTable.OUTGOING_COUNT
        } else {
            //Should never happen, will cause crash
            Log.e(TAG, "Invalid call type in updateMostRecentCall")
        }

        val sqlValuesStatement = ContentValues()
        sqlValuesStatement.put(DataContract.NumbersTable.NUMBER, num)
        sqlValuesStatement.put(DataContract.NumbersTable.MOST_RECENT, callId)
        sqlValuesStatement.put(countColumn, 1)
        sqlValuesStatement.putNull(DataContract.NumbersTable.NOTES)

        dbHelper = DbHelper.getInstance(ctx)
        database = dbHelper!!.writableDatabase
        database!!.beginTransaction()
        database!!.insert(DataContract.NumbersTable.TABLE_NAME, null, sqlValuesStatement)
        database!!.setTransactionSuccessful()
        database!!.endTransaction()
        database!!.close()
    }

    private fun addCallToTable(ctx: Context, number: String?, callId: Int, start: Date, end: Date?, inOut: String, ansMiss: String?): Long {
        val num = java.lang.Long.parseLong(number!!)
        val sqlValuesStatement = ContentValues()
        sqlValuesStatement.put(DataContract.CallTable.NUMBER, num)
        sqlValuesStatement.put(DataContract.CallTable.START_TIME, start.time)
        if (end != null) { //Will be null if the call was missed
            sqlValuesStatement.put(DataContract.CallTable.END_TIME, end.time)
        }
        sqlValuesStatement.put(DataContract.CallTable.INCOMING_OUTGOING, inOut)
        sqlValuesStatement.put(DataContract.CallTable.ANSWERED_MISSED, ansMiss)
        sqlValuesStatement.put(DataContract.CallTable.CALL_ID, callId)

        dbHelper = DbHelper.getInstance(ctx)
        database = dbHelper!!.writableDatabase
        database!!.beginTransaction()
        val rowID = database!!.insert(DataContract.CallTable.TABLE_NAME, null, sqlValuesStatement)
        database!!.setTransactionSuccessful()
        database!!.endTransaction()
        database!!.close()

        return rowID
    }


    private fun updateMostRecentCall(ctx: Context, number: String?, startDate: Date, callId: Long, callType: String) {
        val num = java.lang.Long.parseLong(number!!)
        dbHelper = DbHelper.getInstance(ctx)
        database = dbHelper!!.writableDatabase

        val sqlValuesStatement = ContentValues()

        //create insert statement
        sqlValuesStatement.clear()
        sqlValuesStatement.put(DataContract.NumbersTable.MOST_RECENT, callId)

        val whereClause = String.format("%s = %s", DataContract.NumbersTable.NUMBER, num)

        var countColumn = ""
        if (callType == ANSWERED) {
            countColumn = DataContract.NumbersTable.ANSWERED_COUNT
        } else if (callType == MISSED) {
            countColumn = DataContract.NumbersTable.MISSED_COUNT
        } else if (callType == OUTGOING) {
            countColumn = DataContract.NumbersTable.OUTGOING_COUNT
        } else {
            //Should never happen, will cause crash
            Log.e(TAG, "Invalid call type in updateMostRecentCall")
        }

        database!!.beginTransaction()
        database!!.update(DataContract.NumbersTable.TABLE_NAME, sqlValuesStatement, whereClause, null) //whereArgs
        database!!.execSQL("UPDATE " + DataContract.NumbersTable.TABLE_NAME
                + " SET " + countColumn + " = "
                + countColumn + " + 1 "
                + "WHERE " + DataContract.NumbersTable.NUMBER + " = " + num)
        database!!.setTransactionSuccessful()
        database!!.endTransaction()
        database!!.close()
    }

    private fun isNumberInDatabase(number: String?): Boolean {
        val num = java.lang.Long.parseLong(number!!)
        database = dbHelper!!.readableDatabase

        val searchQuery = ("SELECT " + DataContract.NumbersTable.NUMBER + " FROM "
                + DataContract.NumbersTable.TABLE_NAME
                + " WHERE " + DataContract.NumbersTable.NUMBER
                + "= '" + num + "'")

        val cursor = database!!.rawQuery(searchQuery, null)
        val exists = cursor.count > 0
        cursor.close()
        database!!.close()

        return exists
    }
}
