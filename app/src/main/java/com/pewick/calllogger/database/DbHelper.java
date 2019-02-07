package com.pewick.calllogger.database;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.ContactsContract;
import androidx.core.content.ContextCompat;
import android.util.Log;

import com.pewick.calllogger.models.CallItem;
import com.pewick.calllogger.models.NumberItem;

import java.util.ArrayList;

/**
 * A helper class to manage database creation and version management.
 */
public class DbHelper extends SQLiteOpenHelper {
    private final String TAG = getClass().getSimpleName();

    // NOTE: if the database schema is updated, this version number MUST be incremented
    public static final int DATABASE_VERSION = 4;
    public static final String DATABASE_NAME = "CallLogger.db";

    private static DbHelper dbHelperInstance;
    private Context context;
    private ArrayList<NumberItem> numbersList;
    private ArrayList<CallItem> callsList;

    /**
     * Create a helper object to create, open, and/or manage a database. Uses the version number
     * hardcoded in this class.
     * @param context the context of the application.
     */
    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    /**
     * Ensures that only one DbHelper will exist at any given time.
     * @param context uses this context to get the application context, to further prevent leaks.
     * @return the current instance of DbHelper if one exists, otherwise a new instance.
     */
    public static synchronized DbHelper getInstance(Context context) {
        if (dbHelperInstance == null) {
            dbHelperInstance = new DbHelper(context.getApplicationContext());
        }
        return dbHelperInstance;
    }

    /**
     * Called only when the database is created for the first time. Creates the tables needed by
     * the application.
     * @param db the SQLite database containing the application's tables.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "onCreate, version: "+db.getVersion());
        //Delete
        db.execSQL(DataContract.SQL_DELETE_CALL_TABLE);
        db.execSQL(DataContract.SQL_DELETE_NUMBERS_TABLE);

        //Create
        db.execSQL(DataContract.SQL_CREATE_CALL_TABLE);
        db.execSQL(DataContract.SQL_CREATE_NUMBERS_TABLE);
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
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "onUpgrade");
        // Write logic to preserve the data currently in the user's database, then implement the
        // changes to the database tables, finally, copy the original data back into the new tables.

        if(oldVersion < 4) {
            //From this version on, I maintained a count of all calls for each number.
            // Earlier versions did not have this feature, so I need to save that earlier data,
            // then calculate the counts, then store it all back in the database.

            //Retrieve all data from the tables in the existing database
            this.readNumbersFromDatabaseVersion3(db);
            this.readCallsFromDatabase(db);

            //Delete the old tables
            db.execSQL(DataContract.SQL_DELETE_CALL_TABLE);
            db.execSQL(DataContract.SQL_DELETE_NUMBERS_TABLE);
            //Create the new tables
            db.execSQL(DataContract.SQL_CREATE_CALL_TABLE);
            db.execSQL(DataContract.SQL_CREATE_NUMBERS_TABLE);

            for (CallItem callItem : callsList) {
                this.addCallToTable(db, callItem);
                //Now, find the corresponding number and increment the appropriate count
                for (NumberItem numItem : numbersList) {
                    if (callItem.getNumber() == numItem.getNumber()) {
                        Log.i(TAG, "Found matching number for call item");
                        switch (callItem.getCallType()) {
                            case 1:
                                //Outgoing
                                numItem.setOutgoingCount(numItem.getOutgoingCount() + 1);
                                break;
                            case 2:
                                //Answered
                                numItem.setAnsweredCount(numItem.getAnsweredCount() + 1);
                                break;
                            case 3:
                                //Missed
                                numItem.setMissedCount(numItem.getMissedCount() + 1);
                                break;
                        }
                        break;
                    }
                }
            }
            for (NumberItem numberItem : numbersList) {
                this.addNumberToTable(db, numberItem);
            }
        }
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        onUpgrade(db, oldVersion, newVersion);
    }

    private void readNumbersFromDatabase(SQLiteDatabase database){
        numbersList = new ArrayList<>();

        String[] projection = {
                DataContract.NumbersTable.NUMBER,
                DataContract.NumbersTable.MOST_RECENT,
                DataContract.NumbersTable.NOTES,
                DataContract.NumbersTable.OUTGOING_COUNT,
                DataContract.NumbersTable.ANSWERED_COUNT,
                DataContract.NumbersTable.MISSED_COUNT
        };

        //fetch the data from the database as specified
        database.beginTransaction();
        Cursor cursor = database.query(DataContract.NumbersTable.TABLE_NAME,
                projection,
                null, null, null, null, null);
        database.setTransactionSuccessful();
        database.endTransaction();
        if(cursor.moveToFirst()){
            do{
                long number = cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.NUMBER));
//                Log.i(TAG, "Number: "+number);

                String contact = getContactName(context, Long.toString(number));

                NumberItem existingNumber = new NumberItem(number,
                        cursor.getInt(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.MOST_RECENT)),
                        contact,
                        cursor.getString(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.NOTES)));

                numbersList.add(existingNumber);
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    /**
     * This is an old version. In this version, there were no count fields stored in the table.
     * Used to upgrade installed apps to version 4's new database schema.
     * @param database
     */
    private void readNumbersFromDatabaseVersion3(SQLiteDatabase database){
        numbersList = new ArrayList<>();
        String[] projection = {
                DataContract.NumbersTable.NUMBER,
                DataContract.NumbersTable.MOST_RECENT,
                DataContract.NumbersTable.NOTES
        };

        //fetch the data from the database as specified
        database.beginTransaction();
        Cursor cursor = database.query(DataContract.NumbersTable.TABLE_NAME,
                projection,
                null, null, null, null, null);
        database.setTransactionSuccessful();
        database.endTransaction();
        if(cursor.moveToFirst()){
            do{
                long number = cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.NUMBER));
//                Log.i(TAG, "Number: "+number);

                String contact = getContactName(context, Long.toString(number));

                NumberItem existingNumber = new NumberItem(number,
                        cursor.getInt(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.MOST_RECENT)),
                        contact,
                        cursor.getString(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.NOTES)));

                numbersList.add(existingNumber);
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    private String getContactName(Context context, String phoneNumber) {
        String contactName = null;
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED){
            ContentResolver cr = context.getContentResolver();
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
            Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
            if (cursor == null) {
                return null;
            }
            if(cursor.moveToFirst()) {
                contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
            }
            cursor.close();
        }
        return contactName;
    }

    private void readCallsFromDatabase(SQLiteDatabase database){
        Log.i(TAG,"readCallsFromDatabase");
        callsList = new ArrayList<>();
        String[] projection = {
                DataContract.CallTable.CALL_ID,
                DataContract.CallTable.NUMBER,
                DataContract.CallTable.START_TIME,
                DataContract.CallTable.END_TIME,
                DataContract.CallTable.INCOMING_OUTGOING,
                DataContract.CallTable.ANSWERED_MISSED
        };

        //specify read order based on number
        String sortOrder = DataContract.CallTable.START_TIME + " DESC";

        //fetch the data from the database as specified
        Cursor cursor = database.query(DataContract.CallTable.TABLE_NAME, projection, null, null, null, null, sortOrder);
        if(cursor.moveToFirst()){
            do{
                CallItem existingCall = new CallItem(cursor.getInt(cursor.getColumnIndexOrThrow(DataContract.CallTable.CALL_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.CallTable.NUMBER)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.CallTable.START_TIME)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.CallTable.END_TIME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DataContract.CallTable.INCOMING_OUTGOING)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DataContract.CallTable.ANSWERED_MISSED)));

                existingCall.setContactName(getContactName(context, Long.toString(existingCall.getNumber())));
                callsList.add(existingCall);
            } while (cursor.moveToNext());
        }
        cursor.close();

    }

    private void addNumberToTable(SQLiteDatabase database, NumberItem numberItem){
        ContentValues sqlValuesStatement = new ContentValues();
        sqlValuesStatement.put(DataContract.NumbersTable.NUMBER, numberItem.getNumber());
        sqlValuesStatement.put(DataContract.NumbersTable.MOST_RECENT, numberItem.getMostRecentCallId());
        sqlValuesStatement.put(DataContract.NumbersTable.NOTES, numberItem.getNotes());
        sqlValuesStatement.put(DataContract.NumbersTable.OUTGOING_COUNT, numberItem.getOutgoingCount());
        sqlValuesStatement.put(DataContract.NumbersTable.ANSWERED_COUNT, numberItem.getAnsweredCount());
        sqlValuesStatement.put(DataContract.NumbersTable.MISSED_COUNT, numberItem.getMissedCount());

        database.beginTransaction();
        database.insert(DataContract.NumbersTable.TABLE_NAME, null, sqlValuesStatement);
        database.setTransactionSuccessful();
        database.endTransaction();
    }

    private long addCallToTable(SQLiteDatabase database, CallItem callItem){
        ContentValues sqlValuesStatement = new ContentValues();
        sqlValuesStatement.put(DataContract.CallTable.NUMBER, callItem.getNumber());
        sqlValuesStatement.put(DataContract.CallTable.START_TIME, callItem.getStartTime());
        sqlValuesStatement.put(DataContract.CallTable.END_TIME, callItem.getEndTime());

        sqlValuesStatement.put(DataContract.CallTable.INCOMING_OUTGOING, callItem.getInOut());
        sqlValuesStatement.put(DataContract.CallTable.ANSWERED_MISSED, callItem.getAnsMiss());
        sqlValuesStatement.put(DataContract.CallTable.CALL_ID, callItem.getCallId());

        database.beginTransaction();
        long rowID = database.insert(DataContract.CallTable.TABLE_NAME, null, sqlValuesStatement);
        database.setTransactionSuccessful();
        database.endTransaction();

        return rowID;
    }
}
