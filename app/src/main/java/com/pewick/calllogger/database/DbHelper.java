package com.pewick.calllogger.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Chris on 5/16/2017.
 */
public class DbHelper extends SQLiteOpenHelper {
    // NOTE: if the database schema is updated, this version number MUST be incremented
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "MusicPlayer.db";

    private static DbHelper dbHelperInstance;

    /**
     * Create a helper object to create, open, and/or manage a database. Uses the version number
     * hardcoded in this class.
     * @param context the context of the application.
     */
    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
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
        // discard data and start over - probably not a good idea for this app in particular
//        onCreate(db);

        //Write logic to preserve the data currently in the user's database, then implement the
        // changes to the database tables, finally, copy the original data back into the new tables.
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Again, just discard everything
        onUpgrade(db, oldVersion, newVersion);
    }
}
