package com.pewick.calllogger.fragments;

import android.app.DialogFragment;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.pewick.calllogger.R;
import com.pewick.calllogger.activity.MainActivity;
import com.pewick.calllogger.adapters.NumbersListAdapter;
import com.pewick.calllogger.database.DataContract;
import com.pewick.calllogger.database.DbHelper;
import com.pewick.calllogger.models.NumberItem;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Fragment containing a list of all numbers that have called, or been called by this device.
 * From the MainActivity, the list can be searched, or filtered by contacts and non-contacts.
 */
public class NumbersFragment extends Fragment {
    private final String TAG = getClass().getSimpleName();

    private TextView noResults;
    private ListView numbersListView;
    private NumbersListAdapter adapter;

    private ArrayList<NumberItem> numbersList;
    private ArrayList<NumberItem> numbersListOriginal;
    private ArrayList<NumberItem> numberContactList;
    private ArrayList<NumberItem> numberNonContactList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_numbers_list, container, false);
        numbersList = new ArrayList<>();
        numbersListView = (ListView) view.findViewById(R.id.numbers_list);
        adapter = new NumbersListAdapter(getActivity(),numbersList);
        numbersListView.setAdapter(adapter);

        noResults = (TextView) view.findViewById(R.id.no_results);

        this.setListEventListeners();
        this.setOnClickListener();

        return view;
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        this.readNumbersFromDatabase();
        adapter = new NumbersListAdapter(getActivity(),numbersList);
        numbersListView.setAdapter(adapter);
        this.checkNumberOfResults();
    }

    private void setOnClickListener(){
        numbersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

                NumberItem item = numbersList.get(position);

                Bundle args = new Bundle();
                args.putParcelable("number_item", item);

                NumberDialogFragment dialog = new NumberDialogFragment();
                dialog.setStyle(DialogFragment.STYLE_NORMAL, R.style.NewDialog);
                dialog.setArguments(args);

                dialog.show(getActivity().getFragmentManager(), null);
            }
        });
    }

    private void setListEventListeners(){
        numbersListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ((MainActivity) getActivity()).closeKeyboard();
                return false;
            }
        });
    }

    public void filterList(String charText, boolean contacts, boolean nonContacts) {
//        Log.i(TAG, "filterList:");
//        Log.i(TAG, "    chatText: " + charText);
//        Log.i(TAG, "    contacts: " + contacts);
//        Log.i(TAG, "    nonContacts: " + nonContacts);
        ArrayList<NumberItem> temp = new ArrayList<>();
        numbersList.clear();

        if(contacts && nonContacts){
            temp.addAll(numbersListOriginal);
        } else if(contacts){
            temp.addAll(numberContactList);
        } else if(nonContacts){
            temp.addAll(numberNonContactList);
        }

        if(charText.length() == 0){
            numbersList.addAll(temp);
        } else{
            for (int i = 0; i < temp.size(); i++) {
                NumberItem entry = temp.get(i);
                if(Long.toString(entry.getNumber()).contains(charText)
                        || entry.getFormattedNumber().contains(charText)
                        || (entry.getContactName() != null
                        && entry.getContactName().toLowerCase().contains(charText.toLowerCase()))){
                    numbersList.add(entry);
                }
            }
        }
        adapter.notifyDataSetChanged();
        this.checkNumberOfResults();
    }

    private static Bitmap retrieveContactPhoto(Context context, String number) {
        String contactId = null;
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup._ID};
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);

        if (cursor != null && cursor.moveToNext()) {
            contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID));
            cursor.close();
        }

        Bitmap photo = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.contact_phone_icon);

        try {
            InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(),
                    ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.valueOf(contactId)));

            if (inputStream != null) {
                photo = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return photo;
    }

    private void readNumbersFromDatabase(){
        numbersList = new ArrayList<>();
        numbersListOriginal = new ArrayList<>();
        numberContactList = new ArrayList<>();
        numberNonContactList = new ArrayList<>();
        DbHelper dbHelper = DbHelper.getInstance(getActivity());
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        String[] projection = {
                DataContract.NumbersTable.NUMBER,
                DataContract.NumbersTable.MOST_RECENT,
                DataContract.NumbersTable.NOTES,
                DataContract.NumbersTable.OUTGOING_COUNT,
                DataContract.NumbersTable.ANSWERED_COUNT,
                DataContract.NumbersTable.MISSED_COUNT
        };

        //fetch the data from the database as specified
        Cursor cursor = database.query(DataContract.NumbersTable.TABLE_NAME, projection, null, null, null, null, null);
        if(cursor.moveToFirst()){
            do{
                long number = cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.NUMBER));
                NumberItem existingNumber = new NumberItem(number,
                        cursor.getInt(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.MOST_RECENT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.NOTES)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.OUTGOING_COUNT)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.ANSWERED_COUNT)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.MISSED_COUNT)));

                //Need to do this every time, in case the user changes a contact name
                existingNumber.setContactName(((MainActivity)getActivity()).getContactName(String.valueOf(existingNumber.getNumber())));

                if(existingNumber.getContactName() != null){
                    //then the number is in contacts
                    numberContactList.add(existingNumber);
                    //get the contact image
                    Bitmap bitmap = retrieveContactPhoto(getContext(), Long.toString(existingNumber.getNumber()));
                    if(bitmap != null){
                        existingNumber.setContactImage(bitmap);
                    }
                } else{
                    //then the number is NOT in contacts
                    numberNonContactList.add(existingNumber);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        Collections.sort(numberNonContactList);
        Collections.sort(numberContactList);

        //The following ensures that non-contacts will be above contacts
        numbersList.addAll(numberNonContactList);
        numbersList.addAll(numberContactList);
        numbersListOriginal.addAll(numberNonContactList);
        numbersListOriginal.addAll(numberContactList);
    }

    private void checkNumberOfResults(){
        Log.i(TAG, "checkNumberOfResults, empty? "+(numbersList.size() == 0));
        if(numbersList.size() == 0){
            noResults.setVisibility(View.VISIBLE);
            numbersListView.setVisibility(View.GONE);
        } else {
            noResults.setVisibility(View.GONE);
            numbersListView.setVisibility(View.VISIBLE);
        }
    }

    public void refreshList(){
        Log.i(TAG, "Refresh List");
        readNumbersFromDatabase();
        adapter = new NumbersListAdapter(getActivity(),numbersList);
        numbersListView.setAdapter(adapter);
    }
}
