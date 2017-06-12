package com.pewick.calllogger.receivers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Toast;

import com.pewick.calllogger.database.DataContract;
import com.pewick.calllogger.database.DbHelper;

import java.util.Date;

/**
 * Created by Chris on 5/15/2017.
 */
public class CallReceiver extends PhoneCallReceiver {

    private final String TAG = getClass().getSimpleName();

    private DbHelper dbHelper;
    private SQLiteDatabase database;

    @Override
    protected void onIncomingCallReceived(Context ctx, String number, Date start)
    {
        number = removeLeadingOne(number);
        dbHelper = DbHelper.getInstance(ctx);
        Toast.makeText(ctx, "Incoming Received", Toast.LENGTH_SHORT).show();
        Log.i(TAG,"Incoming Received: "+number);

        //Don't need to add the call to the tables until call has ended (or was missed)
        //TODO: Confirm that ignoring is counted as missed.
    }

    @Override
    protected void onIncomingCallAnswered(Context ctx, String number, Date start)
    {
        number = removeLeadingOne(number);
        dbHelper = DbHelper.getInstance(ctx);
        Toast.makeText(ctx, "Incoming answered Received", Toast.LENGTH_SHORT).show();
        Log.i(TAG,"Incoming answered Received: "+number);

        //Don't need to add the call to the tables until call has ended
    }

    @Override
    protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end)
    {
        number = removeLeadingOne(number);
        dbHelper = DbHelper.getInstance(ctx);
        Toast.makeText(ctx, "Incoming ended Received", Toast.LENGTH_SHORT).show();
        Log.i(TAG,"Incoming ended Received: "+number);

        //Get the current max call_id
        int maxId = this.getMaxCallId();

        //Insert the call into the CallTable
        addCallToTable(ctx, number, maxId+1, start, end, "incoming", "answered");
        //TODO: confirm that this is not called when call is missed.

        //Check if the number has been logged yet
        if(isNumberInDatabase(number)){
            //Then the number has been logged before, just update most recent
            updateMostRecentCall(ctx, number, start, maxId+1);
            incrementAnsweredCount(ctx, number);
        } else{
            //Then the number has not been logged before, so add to table
            addNumberToTable(ctx, number, maxId+1);
        }
    }

    @Override
    protected void onOutgoingCallStarted(Context ctx, String number, Date start)
    {
        number = removeLeadingOne(number);
        dbHelper = DbHelper.getInstance(ctx);
        Toast.makeText(ctx, "Outgoing started Received", Toast.LENGTH_SHORT).show();
        Log.i(TAG,"Outgoing started Received: "+number);

        //Don't need to add the call to the tables until call has ended
    }

    @Override
    protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end)
    {
        number = removeLeadingOne(number);
        dbHelper = DbHelper.getInstance(ctx);
        Toast.makeText(ctx, "Outgoing ended Received", Toast.LENGTH_SHORT).show();
        Log.i(TAG,"Outgoing ended Received: "+number);

        //Get the current max call_id
        int maxId = this.getMaxCallId();

        //Insert the call into the CallTable
        addCallToTable(ctx, number, maxId+1, start, end, "outgoing", null);

        //Check if the number has been logged yet
        if(isNumberInDatabase(number)){
            //Then the number has been logged before, just update most recent
            updateMostRecentCall(ctx, number, start, maxId+1);
            incrementOutgoingCount(ctx, number);
        } else{
            //Then the number has not been logged before, so add to table
            addNumberToTable(ctx, number, maxId+1);
        }
    }

    @Override
    protected void onMissedCall(Context ctx, String number, Date start)
    {
        number = removeLeadingOne(number);
        dbHelper = DbHelper.getInstance(ctx);
        Toast.makeText(ctx, "Missed Received", Toast.LENGTH_SHORT).show();
        Log.i(TAG,"Missed Received: "+number);

        //Get the current max call_id
        int maxId = this.getMaxCallId();

        //Insert the call into the CallTable
        addCallToTable(ctx, number, maxId+1, start, null, "incoming", "missed");

        //Check if the number has been logged yet
        if(isNumberInDatabase(number)){
            //Then the number has been logged before, just update most recent
            updateMostRecentCall(ctx, number, start, maxId+1);
            incrementMissedCount(ctx, number);
        } else{
            //Then the number has not been logged before, so add to table
            addNumberToTable(ctx, number, maxId+1);
        }
    }

    private String removeLeadingOne(String number){
        //TODO: handle area code if the user does not add dial it. Probably need to use location.
        //Is this safe? Would it interfere with non-local numbers? Maybe just out of country?
        if(number.length() == 11){
            return number.substring(1);
        } else{
            return number;
        }
    }

    private void addNumberToTable(Context ctx, String number, long callId){
        long num = Long.parseLong(number);
        ContentValues sqlValuesStatement = new ContentValues();
        sqlValuesStatement.put(DataContract.NumbersTable.NUMBER, num);
        sqlValuesStatement.put(DataContract.NumbersTable.MOST_RECENT, callId);
        sqlValuesStatement.putNull(DataContract.NumbersTable.NOTES);

        dbHelper = DbHelper.getInstance(ctx);
        database = dbHelper.getWritableDatabase();
        database.beginTransaction();
        database.insert(DataContract.NumbersTable.TABLE_NAME, null, sqlValuesStatement);
        database.setTransactionSuccessful();
        database.endTransaction();
        database.close();
    }

    private long addCallToTable(Context ctx, String number, int callId, Date start, Date end, String inOut, String ansMiss){

        long num = Long.parseLong(number);
        ContentValues sqlValuesStatement = new ContentValues();
        sqlValuesStatement.put(DataContract.CallTable.NUMBER, num);
        sqlValuesStatement.put(DataContract.CallTable.START_TIME, start.getTime());
        if(end != null) { //Can, and likely will be, NULL
            sqlValuesStatement.put(DataContract.CallTable.END_TIME, end.getTime());
        }
        sqlValuesStatement.put(DataContract.CallTable.INCOMING_OUTGOING, inOut);
        sqlValuesStatement.put(DataContract.CallTable.ANSWERED_MISSED, ansMiss);
        sqlValuesStatement.put(DataContract.CallTable.CALL_ID, callId);

        dbHelper = DbHelper.getInstance(ctx);
        database = dbHelper.getWritableDatabase();
        database.beginTransaction();
        long rowID = database.insert(DataContract.CallTable.TABLE_NAME, null, sqlValuesStatement);
        database.setTransactionSuccessful();
        database.endTransaction();
        database.close();

        return rowID;
    }



    private void updateMostRecentCall(Context ctx, String number, Date startDate, long callId){
        long num = Long.parseLong(number);
        dbHelper = DbHelper.getInstance(ctx);
        database = dbHelper.getWritableDatabase();

        ContentValues sqlValuesStatement = new ContentValues();

        //create insert statement
        sqlValuesStatement.clear();

        sqlValuesStatement.put(DataContract.NumbersTable.MOST_RECENT, callId);

        String whereClause = String.format("%s = %s" , DataContract.NumbersTable.NUMBER, num);

        database.beginTransaction();
        database.update(DataContract.NumbersTable.TABLE_NAME, sqlValuesStatement, whereClause, null); //whereArgs
        database.setTransactionSuccessful();
        database.endTransaction();
        database.close();
    }

    private boolean isNumberInDatabase(String number){
        long num = Long.parseLong(number);
        database = dbHelper.getReadableDatabase();

        String searchQuery = "SELECT " + DataContract.NumbersTable.NUMBER + " FROM "
                + DataContract.NumbersTable.TABLE_NAME
                + " WHERE " + DataContract.NumbersTable.NUMBER
                + "= '"+ num + "'";

//        Cursor cur = database.query(DataContract.NumbersTable.TABLE_NAME, )
        Cursor cursor = database.rawQuery(searchQuery, null);
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        database.close();

        return exists;
    }

    private int getMaxCallId(){
        database = dbHelper.getReadableDatabase();

        String query = "SELECT MAX("+ DataContract.CallTable.CALL_ID + ") FROM " + DataContract.CallTable.TABLE_NAME;
        SQLiteStatement stmt = database.compileStatement("SELECT MAX("
                + DataContract.CallTable.CALL_ID + ") FROM " + DataContract.CallTable.TABLE_NAME);

        int id = (int) stmt.simpleQueryForLong();

//        String query = "SELECT MAX(call_id) AS _id FROM db_table";
        Cursor cursor = database.rawQuery(query, null);

//        int id = 0;
//        if(cursor.moveToFirst()){
//            id = cursor.getInt(cursor.getColumnIndex(DataContract.CallTable.CALL_ID));
//        }

//        if(cursor.getCount() > 0){
//            cursor.moveToFirst();
//            id = cursor.getInt(cursor.getColumnIndex(DataContract.CallTable.CALL_ID));
//        }
        cursor.close();
        database.close();

        Log.i("Receiver","MaxId: "+id);

        return id;
    }

    private void incrementOutgoingCount(Context ctx, String number){
        int count = this.getOutgoingCount(ctx, number);
        long num = Long.parseLong(number);
        dbHelper = DbHelper.getInstance(ctx);
        database = dbHelper.getWritableDatabase();

        ContentValues sqlValuesStatement = new ContentValues();

        //create insert statement
        sqlValuesStatement.clear();
        sqlValuesStatement.put(DataContract.NumbersTable.OUTGOING_COUNT, count);

        String whereClause = String.format("%s = %s" , DataContract.NumbersTable.NUMBER, num);

        database.beginTransaction();
        database.update(DataContract.NumbersTable.TABLE_NAME, sqlValuesStatement, whereClause, null); //whereArgs
        database.setTransactionSuccessful();
        database.endTransaction();
        database.close();
    }

    private int getOutgoingCount(Context ctx, String number){
        dbHelper = DbHelper.getInstance(ctx);
        database = dbHelper.getReadableDatabase();
        String whereArgs = String.format("%s = %s", DataContract.NumbersTable.NUMBER, number);
        Cursor cursor = database.query(DataContract.NumbersTable.TABLE_NAME,
                new String[]{DataContract.NumbersTable.OUTGOING_COUNT}, whereArgs, null, null, null, null);

        if(cursor.moveToFirst()){
            return cursor.getInt(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.OUTGOING_COUNT));
        } else{
            Log.i(TAG,"Couldn't find number!"); //Shouldn't happen
            return -1;
        }
    }

    private void incrementAnsweredCount(Context ctx, String number){
        int count = this.getAnsweredCount(ctx, number);
        long num = Long.parseLong(number);
        dbHelper = DbHelper.getInstance(ctx);
        database = dbHelper.getWritableDatabase();

        ContentValues sqlValuesStatement = new ContentValues();

        //create insert statement
        sqlValuesStatement.clear();
        sqlValuesStatement.put(DataContract.NumbersTable.ANSWERED_COUNT, count);

        String whereClause = String.format("%s = %s" , DataContract.NumbersTable.NUMBER, num);

        database.beginTransaction();
        database.update(DataContract.NumbersTable.TABLE_NAME, sqlValuesStatement, whereClause, null); //whereArgs
        database.setTransactionSuccessful();
        database.endTransaction();
        database.close();
    }

    private int getAnsweredCount(Context ctx, String number){
        dbHelper = DbHelper.getInstance(ctx);
        database = dbHelper.getReadableDatabase();
        String whereArgs = String.format("%s = %s", DataContract.NumbersTable.NUMBER, number);
        Cursor cursor = database.query(DataContract.NumbersTable.TABLE_NAME,
                new String[]{DataContract.NumbersTable.ANSWERED_COUNT}, whereArgs, null, null, null, null);

        if(cursor.moveToFirst()){
            return cursor.getInt(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.ANSWERED_COUNT));
        } else{
            Log.i(TAG,"Couldn't find number!"); //Shouldn't happen
            return -1;
        }
    }

    private void incrementMissedCount(Context ctx, String number){
        int count = this.getMissedCount(ctx, number);
        long num = Long.parseLong(number);
        dbHelper = DbHelper.getInstance(ctx);
        database = dbHelper.getWritableDatabase();

        ContentValues sqlValuesStatement = new ContentValues();

        //create insert statement
        sqlValuesStatement.clear();
        sqlValuesStatement.put(DataContract.NumbersTable.MISSED_COUNT, count);

        String whereClause = String.format("%s = %s" , DataContract.NumbersTable.NUMBER, num);

        database.beginTransaction();
        database.update(DataContract.NumbersTable.TABLE_NAME, sqlValuesStatement, whereClause, null); //whereArgs
        database.setTransactionSuccessful();
        database.endTransaction();
        database.close();
    }

    private int getMissedCount(Context ctx, String number){
        dbHelper = DbHelper.getInstance(ctx);
        database = dbHelper.getReadableDatabase();
        String whereArgs = String.format("%s = %s", DataContract.NumbersTable.NUMBER, number);
        Cursor cursor = database.query(DataContract.NumbersTable.TABLE_NAME,
                new String[]{DataContract.NumbersTable.MISSED_COUNT}, whereArgs, null, null, null, null);

        if(cursor.moveToFirst()){
            return cursor.getInt(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.MISSED_COUNT));
        } else{
            Log.i(TAG,"Couldn't find number!"); //Shouldn't happen
            return -1;
        }
    }
}
