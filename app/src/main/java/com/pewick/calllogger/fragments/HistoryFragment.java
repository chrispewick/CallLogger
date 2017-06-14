package com.pewick.calllogger.fragments;

import android.Manifest;
import android.app.DialogFragment;
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
import android.telecom.Call;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.pewick.calllogger.R;
import com.pewick.calllogger.activity.MainActivity;
import com.pewick.calllogger.adapters.HistoryListAdapter;
import com.pewick.calllogger.database.DataContract;
import com.pewick.calllogger.database.DbHelper;
import com.pewick.calllogger.models.CallItem;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;

/**
 * Created by Chris on 5/17/2017.
 */
public class HistoryFragment extends Fragment {

    private final String TAG = getClass().getSimpleName();

    private TextView noResults;
    private TextView numberResults;
    private ListView callListView;
    private HistoryListAdapter adapter;

    private ArrayList<CallItem> callList;
    private ArrayList<CallItem> callListOriginal;
    private ArrayList<CallItem> contactsCallList;
    private ArrayList<CallItem> noncontactsCallList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history_list, container, false);

//        this.readCallsFromDatabase();

        callList = new ArrayList<>();
        callListView = (ListView) view.findViewById(R.id.history_list);
        adapter = new HistoryListAdapter(getActivity(), callList);
        callListView.setAdapter(adapter);

        noResults = (TextView) view.findViewById(R.id.no_results);
        numberResults = (TextView) view .findViewById(R.id.number_results);

        this.setListEventListeners();

        return view;
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        this.readCallsFromDatabase();
        adapter = new HistoryListAdapter(getActivity(), callList);
        callListView.setAdapter(adapter);
        this.ifListEmptyShowNoResults();
    }

    public void filterList(String charText, boolean contacts, boolean nonContacts, boolean incoming, boolean outgoing){
        Log.i(TAG, "filterList:");
        Log.i(TAG, "    chatText: "+charText);
        Log.i(TAG, "    contacts: "+contacts);
        Log.i(TAG, "    nonContacts: "+nonContacts);
        Log.i(TAG, "    incoming: "+incoming);
        Log.i(TAG, "    outgoing: "+outgoing);
        ArrayList<CallItem> temp = new ArrayList<>();
        callList.clear();

        if(contacts && nonContacts){
            if(incoming && outgoing){
                //add all in original list
                temp.addAll(callListOriginal);
            } else if(incoming){
                //add incoming calls from original list
                for(CallItem item : callListOriginal){
                    if(item.getCallType() == 2 || item.getCallType() == 3){
                        temp.add(item);
                    }
                }
            } else if(outgoing){
                //add outgoing calls from original list
                for(CallItem item : callListOriginal){
                    if(item.getCallType() == 1){
                        temp.add(item);
                    }
                }
            } else{
                //add NONE from original list
            }
        }
        else if(contacts){
            if(incoming && outgoing){
                //add all in contacts list
                temp.addAll(contactsCallList);
            } else if(incoming){
                //add incoming calls from contacts list
                for(CallItem item : contactsCallList){
                    if(item.getCallType() == 2 || item.getCallType() == 3){
                        temp.add(item);
                    }
                }
            } else if(outgoing){
                //add outgoing calls from contacts list
                for(CallItem item : contactsCallList){
                    if(item.getCallType() == 1){
                        temp.add(item);
                    }
                }
            } else{
                //add NONE from contacts list
            }
        }
        else if(nonContacts){
            if(incoming && outgoing){
                //add all in non-contacts list
                temp.addAll(noncontactsCallList);
            } else if(incoming){
                //add incoming calls from non-contacts list
                for(CallItem item : noncontactsCallList){
                    if(item.getCallType() == 2 || item.getCallType() == 3){
                        temp.add(item);
                    }
                }
            } else if(outgoing){
                //add outgoing calls from non-contacts list
                for(CallItem item : noncontactsCallList){
                    if(item.getCallType() == 1){
                        temp.add(item);
                    }
                }
            } else{
                //add NONE from non-contacts list
            }
        }

        if(charText.length() == 0){
            callList.addAll(temp);
        } else{
            for (int i = 0; i < temp.size(); i++) {
                CallItem entry = temp.get(i);
//                if(Long.toString(entry.getNumber()).contains(charText)
//                        || entry.getFormattedNumber().contains(charText)
//                        || entry.getContactName().contains(charText)){
////                    Log.i("History", "call matched: "+entry.getNumber());
//                    callList.add(entry);
//                }
                if(Long.toString(entry.getNumber()).contains(charText)
                        || (entry.getContactName() != null
                        && entry.getContactName().toLowerCase().contains(charText.toLowerCase()))){
//                    Log.i("History", "call matched: "+entry.getNumber());
                    callList.add(entry);
                }
            }
        }
        adapter.notifyDataSetChanged();
        this.ifListEmptyShowNoResults();
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
        if(ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_CONTACTS)
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

    private void readInitialCallsFromDatabase(){
        Log.i(TAG,"readInitialCallsFromDatabase");
        Calendar startTime = Calendar.getInstance();
        callList = new ArrayList<>();
        callListOriginal = new ArrayList<>();
        contactsCallList = new ArrayList<>();
        noncontactsCallList = new ArrayList<>();
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

        //specify read order based on number
        String sortOrder = DataContract.CallTable.START_TIME + " DESC";

        //fetch the data from the database as specified
//        database.beginTransaction();
        Cursor cursor = database.query(DataContract.CallTable.TABLE_NAME, projection, null, null, null, null, sortOrder);
//        database.setTransactionSuccessful();
//        database.endTransaction();
        int count = 0;
        if(cursor.moveToFirst()){
            do{
                //public CallItem(int id, long num, long start, long end, String inOut, String ansMiss){

                CallItem existingCall = new CallItem(cursor.getInt(cursor.getColumnIndexOrThrow(DataContract.CallTable.CALL_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.CallTable.NUMBER)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.CallTable.START_TIME)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.CallTable.END_TIME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DataContract.CallTable.INCOMING_OUTGOING)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DataContract.CallTable.ANSWERED_MISSED)));

                existingCall.setContactName(getContactName(getActivity(), Long.toString(existingCall.getNumber())));

                if(existingCall.getContactName() != null){
//                    Log.i(TAG, "contactName null");
                    //Then this call was from a contact
                    contactsCallList.add(existingCall);
                } else{
//                    Log.i(TAG, "contactName NOT null");
                    //Then this call was not from a contact
                    noncontactsCallList.add(existingCall);
                }

                callList.add(existingCall);
                callListOriginal.add(existingCall);
                count++;
            } while (cursor.moveToNext() && count < 50);
        }
//        database.setTransactionSuccessful();
//        database.endTransaction();
        cursor.close();

//        Collections.sort(callList);
//        Collections.sort(callListOriginal);
//        callListOriginal.addAll(callList);

        Calendar endTime = Calendar.getInstance();
        Log.i(TAG, "Time: "+ (endTime.getTimeInMillis() - startTime.getTimeInMillis()));
        Log.i(TAG, "callList size: "+callList.size());
        Log.i(TAG, "contacts size: "+contactsCallList.size());
        Log.i(TAG, "non-contacts size: "+noncontactsCallList.size());

//        callListOriginal = new ArrayList<>();
//        callListOriginal.addAll(callList);
    }

    private void readCallsFromDatabase(){
        Log.i(TAG,"readCallsFromDatabase");
        Calendar startTime = Calendar.getInstance();
        callList = new ArrayList<>();
        callListOriginal = new ArrayList<>();
        contactsCallList = new ArrayList<>();
        noncontactsCallList = new ArrayList<>();
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

        //specify read order based on number
        String sortOrder = DataContract.CallTable.START_TIME + " DESC";

        //fetch the data from the database as specified
//        database.beginTransaction();
        Cursor cursor = database.query(DataContract.CallTable.TABLE_NAME, projection, null, null, null, null, sortOrder);
//        database.setTransactionSuccessful();
//        database.endTransaction();
        if(cursor.moveToFirst()){
            do{
                //public CallItem(int id, long num, long start, long end, String inOut, String ansMiss){

                CallItem existingCall = new CallItem(cursor.getInt(cursor.getColumnIndexOrThrow(DataContract.CallTable.CALL_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.CallTable.NUMBER)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.CallTable.START_TIME)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.CallTable.END_TIME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DataContract.CallTable.INCOMING_OUTGOING)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DataContract.CallTable.ANSWERED_MISSED)));

                existingCall.setContactName(getContactName(getActivity(), Long.toString(existingCall.getNumber())));

                if(existingCall.getContactName() != null){
//                    Log.i(TAG, "contactName null");
                    //Then this call was from a contact
                    contactsCallList.add(existingCall);
                } else{
//                    Log.i(TAG, "contactName NOT null");
                    //Then this call was not from a contact
                    noncontactsCallList.add(existingCall);
                }

                callList.add(existingCall);
                callListOriginal.add(existingCall);
            } while (cursor.moveToNext());
        }
//        database.setTransactionSuccessful();
//        database.endTransaction();
        cursor.close();

//        Collections.sort(callList);
//        Collections.sort(callListOriginal);
//        callListOriginal.addAll(callList);

        Calendar endTime = Calendar.getInstance();
        Log.i(TAG, "Time: "+ (endTime.getTimeInMillis() - startTime.getTimeInMillis()));
        Log.i(TAG, "callList size: "+callList.size());
        Log.i(TAG, "contacts size: "+contactsCallList.size());
        Log.i(TAG, "non-contacts size: "+noncontactsCallList.size());

//        callListOriginal = new ArrayList<>();
//        callListOriginal.addAll(callList);
    }

    private void ifListEmptyShowNoResults(){
        Log.i(TAG, "ifListEmptyShowNoResults, empty? "+(callList.size() == 0));
        if(callList.size() == 0){
            noResults.setVisibility(View.VISIBLE);
            callListView.setVisibility(View.GONE);
            numberResults.setVisibility(View.GONE);
        } else {
            noResults.setVisibility(View.GONE);
            callListView.setVisibility(View.VISIBLE);
            numberResults.setVisibility(View.VISIBLE);
            this.showNumberOfResults();
        }
    }

    private void showNumberOfResults(){
        String numResults = callList.size() +" "+ getResources().getString(R.string.results);
        numberResults.setText(numResults);
    }
}
