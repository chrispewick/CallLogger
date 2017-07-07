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
 * Custom receiver to handle call events.
 */
public class CallReceiver extends PhoneCallReceiver {

    private final String TAG = getClass().getSimpleName();
    private final String INCOMING = "incoming";
    private final String OUTGOING = "outgoing";
    private final String ANSWERED = "answered";
    private final String MISSED = "missed";

    private DbHelper dbHelper;
    private SQLiteDatabase database;

    @Override
    protected void onIncomingCallReceived(Context ctx, String number, Date start){
//        Toast.makeText(ctx, "Incoming Received", Toast.LENGTH_SHORT).show();
        Log.i(TAG,"Incoming Received: "+number);

        //Don't need to add the call to the tables until call has ended (or was missed)
    }

    @Override
    protected void onIncomingCallAnswered(Context ctx, String number, Date start){
//        Toast.makeText(ctx, "Incoming answered Received", Toast.LENGTH_SHORT).show();
        Log.i(TAG,"Incoming answered Received: "+number);

        //Don't need to add the call to the tables until call has ended
    }

    @Override
    protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end){
        number = removeLeadingOne(number);
        dbHelper = DbHelper.getInstance(ctx);
//        Toast.makeText(ctx, "Incoming ended Received", Toast.LENGTH_SHORT).show();
        Log.i(TAG,"Incoming ended Received: "+number);

        //Get the current max call_id
        int maxId = this.getMaxCallId();

        //Insert the call into the CallTable
        addCallToTable(ctx, number, maxId+1, start, end, INCOMING, ANSWERED);

        //Check if the number has been logged yet
        if(isNumberInDatabase(number)){
            //Then the number has been logged before, just update most recent
            updateMostRecentCall(ctx, number, start, maxId+1, ANSWERED);
        } else{
            //Then the number has not been logged before, so add to table
            addNumberToTable(ctx, number, maxId+1, ANSWERED);
        }
    }

    @Override
    protected void onMissedCall(Context ctx, String number, Date start){
        number = removeLeadingOne(number);
        dbHelper = DbHelper.getInstance(ctx);
//        Toast.makeText(ctx, "Missed Received", Toast.LENGTH_SHORT).show();
        Log.i(TAG,"Missed Received: "+number);

        //Get the current max call_id
        int maxId = this.getMaxCallId();

        //Insert the call into the CallTable
        addCallToTable(ctx, number, maxId+1, start, null, INCOMING, MISSED);

        //Check if the number has been logged yet
        if(isNumberInDatabase(number)){
            //Then the number has been logged before, just update most recent
            updateMostRecentCall(ctx, number, start, maxId+1, MISSED);
        } else{
            //Then the number has not been logged before, so add to table
            addNumberToTable(ctx, number, maxId+1, MISSED);
        }
    }

    @Override
    protected void onOutgoingCallStarted(Context ctx, String number, Date start){
//        Toast.makeText(ctx, "Outgoing started Received", Toast.LENGTH_SHORT).show();
        Log.i(TAG,"Outgoing started Received: "+number);

        //Don't need to add the call to the tables until call has ended
    }

    @Override
    protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end){
        number = removeLeadingOne(number);
        dbHelper = DbHelper.getInstance(ctx);
        Toast.makeText(ctx, "Outgoing ended Received", Toast.LENGTH_SHORT).show();
        Log.i(TAG,"Outgoing ended Received: "+number);

        //Get the current max call_id
        int maxId = this.getMaxCallId();

        //Insert the call into the CallTable
        addCallToTable(ctx, number, maxId+1, start, end, OUTGOING, null);

        //Check if the number has been logged yet
        if(isNumberInDatabase(number)){
            //Then the number has been logged before, just update most recent
            updateMostRecentCall(ctx, number, start, maxId+1, OUTGOING);
        } else{
            //Then the number has not been logged before, so add to table
            addNumberToTable(ctx, number, maxId+1, OUTGOING);
        }
    }

    private String removeLeadingOne(String number){
        Log.i(TAG, "removeLeadingOne, num length: "+number.length() );
        //Is this safe? Would it interfere with non-local numbers? Maybe just out of country?
        if(number.length() == 11){//Then there is a "1" to remove
            return number.substring(1);
        } else if(number.length() == 12){//Then there is a "+1" to remove
            return number.substring(2);
        } else{
            return number;
        }
    }

    private void addNumberToTable(Context ctx, String number, long callId, String callType){
        long num = Long.parseLong(number);

        String countColumn = "";
        if(callType.equals(ANSWERED)){
            countColumn = DataContract.NumbersTable.ANSWERED_COUNT;
        } else if(callType.equals(MISSED)){
            countColumn = DataContract.NumbersTable.MISSED_COUNT;
        } else if(callType.equals(OUTGOING)){
            countColumn = DataContract.NumbersTable.OUTGOING_COUNT;
        } else{
            //Should never happen, will cause crash
            Log.e(TAG, "Invalid call type in updateMostRecentCall");
        }

        ContentValues sqlValuesStatement = new ContentValues();
        sqlValuesStatement.put(DataContract.NumbersTable.NUMBER, num);
        sqlValuesStatement.put(DataContract.NumbersTable.MOST_RECENT, callId);
        sqlValuesStatement.put(countColumn, 1);
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
        if(end != null) { //Will be null if the call was missed
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



    private void updateMostRecentCall(Context ctx, String number, Date startDate, long callId, String callType ){
        long num = Long.parseLong(number);
        dbHelper = DbHelper.getInstance(ctx);
        database = dbHelper.getWritableDatabase();

        ContentValues sqlValuesStatement = new ContentValues();

        //create insert statement
        sqlValuesStatement.clear();
        sqlValuesStatement.put(DataContract.NumbersTable.MOST_RECENT, callId);

        String whereClause = String.format("%s = %s" , DataContract.NumbersTable.NUMBER, num);

        String countColumn = "";
        if(callType.equals(ANSWERED)){
            countColumn = DataContract.NumbersTable.ANSWERED_COUNT;
        } else if(callType.equals(MISSED)){
            countColumn = DataContract.NumbersTable.MISSED_COUNT;
        } else if(callType.equals(OUTGOING)){
            countColumn = DataContract.NumbersTable.OUTGOING_COUNT;
        } else{
            //Should never happen, will cause crash
            Log.e(TAG, "Invalid call type in updateMostRecentCall");
        }

        database.beginTransaction();
        database.update(DataContract.NumbersTable.TABLE_NAME, sqlValuesStatement, whereClause, null); //whereArgs
        database.execSQL("UPDATE " + DataContract.NumbersTable.TABLE_NAME
                + " SET " + countColumn + " = "
                + countColumn + " + 1 "
                + "WHERE "+ DataContract.NumbersTable.NUMBER + " = " + num );
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
        Cursor cursor = database.rawQuery(query, null);

        cursor.close();
        database.close();

        Log.i("Receiver","MaxId: "+id);
        return id;
    }
}
