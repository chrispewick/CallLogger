package com.pewick.calllogger.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pewick.calllogger.R;
import com.pewick.calllogger.database.DataContract;
import com.pewick.calllogger.database.DbHelper;
import com.pewick.calllogger.models.CallItem;
import com.pewick.calllogger.models.NumberItem;
import com.pewick.calllogger.views.EditTextPlus;

/**
 * Created by Chris on 6/9/2017.
 */
public class NumberDialogFragment extends DialogFragment {
    private final String TAG = getClass().getSimpleName();

    private AlertDialog dialog;

    private NumberItem numberItem;

    private EditTextPlus notes;
    TextView titleContact;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.NewCustomDialog);
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_number_dialog, null);

        Bundle args = getArguments();
        this.numberItem = (NumberItem) args.get("number_item");

        view.requestFocus();
        builder.setTitle(null).setView(view);
        dialog = builder.create();

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        //I could use the view in onCreate to access these view items from onCreate instead, I believe
        this.configureDialogTextContent();
        this.configureCallButton();
        this.configureDoneButton();
    }

    private void configureDialogTextContent(){
        titleContact = (TextView) dialog.findViewById(R.id.title_contact_name);
        if(numberItem.getContactName() != null){
            titleContact.setText(numberItem.getContactName());
        } else {
            //This causes a crash, b/c the view is null during onCreate
            titleContact.setVisibility(View.GONE);
        }

        TextView titleNumber = (TextView) dialog.findViewById(R.id.title_number);
        titleNumber.setText(numberItem.getFormattedNumber());

        TextView mostRecentDate = (TextView) dialog.findViewById(R.id.recent_date_time);
        ImageView mostRecentIcon = (ImageView) dialog.findViewById(R.id.recent_call_type_icon);
        CallItem callItem = this.readMostRecentCallFromDatabase();
        if(callItem != null){
            mostRecentDate.setText(callItem.getFormattedDateTime());
            switch(callItem.getCallType()){
                case 1:
                    //outgoing
                    mostRecentIcon.setImageResource(R.drawable.outgoing_made_icon);
                    mostRecentIcon.setColorFilter(ContextCompat.getColor(getActivity(),R.color.blue));
                    break;
                case 2:
                    //answered
                    mostRecentIcon.setImageResource(R.drawable.incoming_answered_icon);
                    mostRecentIcon.setColorFilter(ContextCompat.getColor(getActivity(),R.color.green));
                    break;
                case 3:
                    //missed
                    mostRecentIcon.setImageResource(R.drawable.incoming_missed_icon);
                    mostRecentIcon.setColorFilter(ContextCompat.getColor(getActivity(),R.color.red));
                    break;
            }
        }

        TextView incomingTotal = (TextView) dialog.findViewById(R.id.total_incoming);
        TextView outgoingTotal = (TextView) dialog.findViewById(R.id.total_outgoing);
        String incomingTotalStr = getResources().getString(R.string.total_in) + " " + (numberItem.getAnsweredCount() + numberItem.getMissedCount());
        String outgoingTotalStr = getResources().getString(R.string.total_out) + " " + numberItem.getOutgoingCount();
        incomingTotal.setText(incomingTotalStr);
        outgoingTotal.setText(outgoingTotalStr);

        notes = (EditTextPlus) dialog.findViewById(R.id.notes_field);
        if(numberItem.getNotes() != null){
            notes.setText(numberItem.getNotes());
        }

        notes = (EditTextPlus) dialog.findViewById(R.id.notes_field);
        notes.setText(this.readNotesFormDatabase());
    }

    private CallItem readMostRecentCallFromDatabase(){
        DbHelper dbHelper = DbHelper.getInstance(getActivity());
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        String[] projection = {
                DataContract.CallTable.CALL_ID,
                DataContract.CallTable.NUMBER,
                DataContract.CallTable.START_TIME,
                DataContract.CallTable.END_TIME,
                DataContract.CallTable.INCOMING_OUTGOING,
                DataContract.CallTable.ANSWERED_MISSED
        };

        String whereArgs = String.format("%s = %s", DataContract.CallTable.CALL_ID, numberItem.getMostRecentCallId());
        Cursor cursor = database.query(DataContract.CallTable.TABLE_NAME, projection, whereArgs, null, null, null, null);

        CallItem callItem = null;
        if(cursor.moveToFirst()){
            callItem = new CallItem(cursor.getInt(cursor.getColumnIndexOrThrow(DataContract.CallTable.CALL_ID)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.CallTable.NUMBER)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.CallTable.START_TIME)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.CallTable.END_TIME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DataContract.CallTable.INCOMING_OUTGOING)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DataContract.CallTable.ANSWERED_MISSED)));
        } else{
            Log.i(TAG,"Most recent was null!"); //This could happen if I decide to allow users to delete calls from app
        }
        cursor.close();
        database.close();
        dbHelper.close();
        return callItem;
    }

    private String readNotesFormDatabase(){
        //Needed b/c: when user edits the notes, then closes the dialog, the fragment below is still
        //holding the NumberItem with the old values. Since NOTES is the only value that can be
        //changed, I will simply read the notes from the database each time.

        String notes = "";

        DbHelper dbHelper = DbHelper.getInstance(getActivity());
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        String[] projection = {
                DataContract.NumbersTable.NOTES
        };

        String whereArgs = String.format("%s = %s", DataContract.NumbersTable.NUMBER, numberItem.getNumber());
        Cursor cursor = database.query(DataContract.NumbersTable.TABLE_NAME, projection, whereArgs, null, null, null, null);
        if(cursor.moveToFirst()){
            notes = cursor.getString(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.NOTES));
        }
        cursor.close();
        return notes;
    }

    private void configureDoneButton(){
        LinearLayout doneButton = (LinearLayout) dialog.findViewById(R.id.done_button);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DbHelper dbHelper = DbHelper.getInstance(getActivity());
                SQLiteDatabase database = dbHelper.getReadableDatabase();
                ContentValues cv = new ContentValues();
                cv.put(DataContract.NumbersTable.NOTES, notes.getText().toString());
                database.update(DataContract.NumbersTable.TABLE_NAME, cv, DataContract.NumbersTable.NUMBER + "= ?", new String[] {Long.toString(numberItem.getNumber())});

                database.close();
                dbHelper.close();
                dialog.dismiss();
            }
        });

    }

    private void configureCallButton(){
        LinearLayout callButton = (LinearLayout) dialog.findViewById(R.id.call_button);
        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + numberItem.getNumber()));

                try {
                    startActivity(intent);
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
    }
}