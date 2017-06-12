package com.pewick.calllogger.fragments;

import android.Manifest;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.pewick.calllogger.R;
import com.pewick.calllogger.activity.MainActivity;
import com.pewick.calllogger.adapters.NumbersListAdapter;
import com.pewick.calllogger.database.DataContract;
import com.pewick.calllogger.database.DbHelper;
import com.pewick.calllogger.models.NumberItem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

/**
 * Created by Chris on 5/17/2017.
 */
public class NumbersFragment extends Fragment {

    private final String TAG = getClass().getSimpleName();

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

//        this.readNumbersFromDatabase();

        numbersList = new ArrayList<>();
        numbersListView = (ListView) view.findViewById(R.id.numbers_list);
        adapter = new NumbersListAdapter(getActivity(),numbersList);
        numbersListView.setAdapter(adapter);

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
    }

    private void setOnClickListener(){
        numbersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

                NumberItem item = (NumberItem) numbersList.get(position);

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

    //Need to do this evertime, in case the user changes a contact name
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

        //specify read order based on number
//        String sortOrder = DataContract.NumbersTable.NUMBER + " ASC";

        //fetch the data from the database as specified
        database.beginTransaction();
        Cursor cursor = database.query(DataContract.NumbersTable.TABLE_NAME, projection, null, null, null, null, null);
        database.setTransactionSuccessful();
        database.endTransaction();
        if(cursor.moveToFirst()){
            do{
                //NumberItem(long num, int recent, String contact, String notes)

                long number = cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.NUMBER));
//                Log.i(TAG, "Number: "+number);

                String contact = getContactName(getContext(), Long.toString(number));

                NumberItem existingNumber = new NumberItem(number,
                        cursor.getInt(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.MOST_RECENT)),
                        contact,
                        cursor.getString(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.NOTES)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.OUTGOING_COUNT)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.ANSWERED_COUNT)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.MISSED_COUNT)));

                if(existingNumber.getContactName() != null){
                    //then the number is in contacts
                    numberContactList.add(existingNumber);

                    //And get the contact image
                    Bitmap bitmap = retrieveContactPhoto(getContext(), Long.toString(existingNumber.getNumber()));
                    if(bitmap != null){
                        Log.i(TAG, "Bitmap NOT null");
                        existingNumber.setContactImage(bitmap);
                    }
                } else{
                    //then the number is NOT in contacts
                    numberNonContactList.add(existingNumber);
                }

                //cursor.getString(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.CONTACT_NAME))

//                numbersList.add(existingNumber);
//                numbersListOriginal.add(existingNumber);
            } while (cursor.moveToNext());
        }

        Collections.sort(numberNonContactList);
        Collections.sort(numberContactList);


        //The following ensures that non-contacts will be above contacts
        numbersList.addAll(numberNonContactList);
        numbersList.addAll(numberContactList);
        numbersListOriginal.addAll(numberNonContactList);
        numbersListOriginal.addAll(numberContactList);

        cursor.close();
    }
}
