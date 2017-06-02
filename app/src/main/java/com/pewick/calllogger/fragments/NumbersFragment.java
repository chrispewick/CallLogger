package com.pewick.calllogger.fragments;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.pewick.calllogger.R;
import com.pewick.calllogger.adapters.NumbersListAdapter;
import com.pewick.calllogger.database.DataContract;
import com.pewick.calllogger.database.DbHelper;
import com.pewick.calllogger.models.NumberItem;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Chris on 5/17/2017.
 */
public class NumbersFragment extends Fragment {

    private final String TAG = getClass().getSimpleName();

    private ListView numbersListView;
    private NumbersListAdapter adapter;

    private DbHelper dbHelper;
    private SQLiteDatabase database;

    private ArrayList<NumberItem> numbersList;
    private ArrayList<NumberItem> numbersListOriginal;
    private ArrayList<NumberItem> numberContactList;
    private ArrayList<NumberItem> numberNonContactList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history_list, container, false);

//        this.readNumbersFromDatabase();

        numbersList = new ArrayList<>();
        numbersListView = (ListView) view.findViewById(R.id.history_list);
        adapter = new NumbersListAdapter(getActivity(),numbersList);
        numbersListView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        this.readNumbersFromDatabase();
        adapter = new NumbersListAdapter(getActivity(),numbersList);
        numbersListView.setAdapter(adapter);
    }

    public void filterList(String charText, boolean contacts, boolean nonContacts) {
        Log.i(TAG, "filterList:");
        Log.i(TAG, "    chatText: " + charText);
        Log.i(TAG, "    contacts: " + contacts);
        Log.i(TAG, "    nonContacts: " + nonContacts);
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
                        || (entry.getContactName() != null
                        && entry.getContactName().toLowerCase().contains(charText.toLowerCase()))){
//                    Log.i("Number", "number matched: "+entry.getNumber());
                    numbersList.add(entry);
                }
            }
        }
        adapter.notifyDataSetChanged();
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

//    private Uri getContactImage(Context context, String phoneNumber) {
//        Uri contactImage = null;
//        if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CONTACTS)
//                == PackageManager.PERMISSION_GRANTED){
//            ContentResolver cr = context.getContentResolver();
//            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
//            Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI}, null, null, null);
//            if (cursor == null) {
//                return null;
//            }
//            if(cursor.moveToFirst()) {
//                contactImage = cursor.get(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
//            }
//            cursor.close();
//        }
//
//        return contactImage;
//    }

    public Uri getPhotoUri(Context context, String number) {
        try {
            Cursor cur = context.getContentResolver().query(
                    ContactsContract.Data.CONTENT_URI,
                    null,
                    ContactsContract.Data.CONTACT_ID + "=" + this.getId() + " AND "
                            + ContactsContract.Data.MIMETYPE + "='"
                            + ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE + "'", null,
                    null);
            if (cur != null) {
                if (!cur.moveToFirst()) {
                    return null; // no photo
                }
            } else {
                return null; // error in cursor process
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        Uri person = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long
                .parseLong(number));
        return Uri.withAppendedPath(person, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
    }

    private void readNumbersFromDatabase(){
        numbersList = new ArrayList<>();
        numbersListOriginal = new ArrayList<>();
        numberContactList = new ArrayList<>();
        numberNonContactList = new ArrayList<>();
        dbHelper = DbHelper.getInstance(getActivity());
        database = dbHelper.getReadableDatabase();
        String[] projection = {
                DataContract.NumbersTable.NUMBER,
                DataContract.NumbersTable.MOST_RECENT,
                DataContract.NumbersTable.NOTES
        };

        //specify read order based on number
        //TODO: Handle contacts? - may need to sort them myself
        String sortOrder = DataContract.NumbersTable.NUMBER + " ASC";

        //fetch the data from the database as specified
        Cursor cursor = database.query(DataContract.NumbersTable.TABLE_NAME, projection, null, null, null, null, sortOrder);

        if(cursor.moveToFirst()){
            do{
                //NumberItem(long num, int recent, String contact, String notes)

                long number = cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.NUMBER));
//                Log.i(TAG, "Number: "+number);

                String contact = getContactName(getContext(), Long.toString(number));

                NumberItem existingNumber = new NumberItem(number,
                        cursor.getInt(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.MOST_RECENT)),
                        contact,
                        cursor.getString(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.NOTES)));

                if(existingNumber.getContactName() != null){
                    //then the number is in contacts
                    numberContactList.add(existingNumber);
                } else{
                    //then the number is NOT in contacts
                    numberNonContactList.add(existingNumber);
                }

                //cursor.getString(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.CONTACT_NAME))

                numbersList.add(existingNumber);
                numbersListOriginal.add(existingNumber);
            } while (cursor.moveToNext());
        }

        cursor.close();
    }
}
