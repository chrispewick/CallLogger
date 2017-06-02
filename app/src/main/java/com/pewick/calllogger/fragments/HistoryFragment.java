package com.pewick.calllogger.fragments;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.pewick.calllogger.R;
import com.pewick.calllogger.activity.MainActivity;
import com.pewick.calllogger.adapters.HistoryListAdapter;
import com.pewick.calllogger.database.DataContract;
import com.pewick.calllogger.database.DbHelper;
import com.pewick.calllogger.models.CallItem;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Chris on 5/17/2017.
 */
public class HistoryFragment extends Fragment {

    private final String TAG = getClass().getSimpleName();

    private ListView callListView;
    private HistoryListAdapter adapter;

    private DbHelper dbHelper;
    private SQLiteDatabase database;

    private ArrayList<CallItem> callList;
    private ArrayList<CallItem> callListOriginal;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history_list, container, false);

        this.readCallsFromDatabase();

        callListView = (ListView) view.findViewById(R.id.history_list);
        adapter = new HistoryListAdapter(getActivity(), callList);
        callListView.setAdapter(adapter);

        this.setListEventListeners();

        return view;
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
//        this.readCallsFromDatabase();
        adapter = new HistoryListAdapter(getActivity(), callList);
        callListView.setAdapter(adapter);
    }

    public void filterList(String charText) {
        Log.i(TAG, "FilterList called: "+charText);
        charText = charText.toLowerCase(Locale.getDefault());
        callList.clear();
        if (charText.length() == 0) {
            callList.addAll(callListOriginal);
        } else {
            for (int i = 0; i < callListOriginal.size(); i++) {
                CallItem entry = callListOriginal.get(i);
                if(Long.toString(entry.getNumber()).contains(charText)
                        || entry.getFormattedNumber().contains(charText)){
//                    Log.i("History", "call matched: "+entry.getNumber());
                    callList.add(entry);
                }
                //TODO: Handle contacts. Here, or in getFormattedNumber()
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void setListEventListeners(){

        callListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ((MainActivity) getActivity()).closeKeyboard();
                return false;
            }
        });
    }

    private String getContactName(Context context, String phoneNumber) {
        String contactName = null;
        if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CONTACTS)
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

    private void readCallsFromDatabase(){
        Log.i(TAG,"readCallsFromDatabase");
        callList = new ArrayList<>();
        callListOriginal = new ArrayList<>();
        dbHelper = DbHelper.getInstance(getActivity());
        database = dbHelper.getReadableDatabase();
        String[] projection = {
                DataContract.CallTable.CALL_ID,
                DataContract.CallTable.NUMBER,
                DataContract.CallTable.START_TIME,
                DataContract.CallTable.END_TIME,
                DataContract.CallTable.INCOMING_OUTGOING,
                DataContract.CallTable.ANSWERED_MISSED
        };

        //specify read order based on number
        //TODO: Handle contacts?
        String sortOrder = DataContract.CallTable.START_TIME + " DESC";

        //fetch the data from the database as specified
        Cursor cursor = database.query(DataContract.CallTable.TABLE_NAME, projection, null, null, null, null, sortOrder);

        if(cursor.moveToFirst()){
            do{
                //public CallItem(int id, long num, long start, long end, String inOut, String ansMiss){

                CallItem existingCall = new CallItem(cursor.getInt(cursor.getColumnIndexOrThrow(DataContract.CallTable.CALL_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.CallTable.NUMBER)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.CallTable.START_TIME)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.CallTable.END_TIME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DataContract.CallTable.INCOMING_OUTGOING)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DataContract.CallTable.ANSWERED_MISSED)));

                existingCall.setContactName(getContactName(getContext(), Long.toString(existingCall.getNumber())));

                callList.add(existingCall);
                callListOriginal.add(existingCall);
            } while (cursor.moveToNext());
        }
        cursor.close();
//        callListOriginal = new ArrayList<>();
//        callListOriginal.addAll(callList);
    }
}
