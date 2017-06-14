package com.pewick.calllogger.models;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Defines the model for a phone number.
 */
public class NumberItem implements Comparable<NumberItem>, Parcelable {

    private final String TAG = getClass().getSimpleName();

    private long number;
    private int mostRecentCallId;
    private int outgoingCount;
    private int answeredCount;
    private int missedCount;
    private String contactName;
    private String notes;

    private Bitmap contactImage;

    public NumberItem(long num, int recent, String contact, String notes, int outgoing, int answered, int missed){
        this.number = num;
        this.mostRecentCallId = recent;
        this.contactName = contact;
        this.notes = notes;
        this.outgoingCount = outgoing;
        this.answeredCount = answered;
        this.missedCount = missed;
    }

    //For earlier versions only
    public NumberItem(long num, int recent, String contact, String notes){
        this.number = num;
        this.mostRecentCallId = recent;
        this.contactName = contact;
        this.notes = notes;
    }

    public String getDisplayText(){
        if(contactName == null){
            return getFormattedNumber();
        } else{
            return contactName;
        }
    }

    public String getFormattedNumber(){
        String temp = ""+this.number;
        if(temp.length() == 10) {
            return String.format("(%s) %s-%s",
                    temp.substring(0, 3),
                    temp.substring(3, 6),
                    temp.substring(6, 10));
        } else{
            return temp;
        }

    }

    public Bitmap getContactImage() {
        return contactImage;
    }

    public void setContactImage(Bitmap contactImage) {
        this.contactImage = contactImage;
    }

    public Uri getPhotoUri(Context context, long number) {
        try {
            Cursor cur = context.getContentResolver().query(
                    ContactsContract.Data.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.NUMBER + "=" + number + " AND "
                            + ContactsContract.Data.MIMETYPE + "='"
                            + ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE + "'", null,
                    null);
            if (cur != null) {
                Log.i(TAG, "cursor NOT null");
                if (!cur.moveToFirst()) {
                    Log.i(TAG, "No photo");
                    return null; // no photo
                }
            } else {
                Log.i(TAG, "cursor null");
                return null; // error in cursor process
            }
        } catch (Exception e) {
            Log.i(TAG, "getPhotoUri Exception!");
            e.printStackTrace();
            return null;
        }
        Uri person = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, number);
        return Uri.withAppendedPath(person, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
    }

    public Uri getContactImage(Context context, String phoneNumber) {
        Uri contactUri = null;
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED){
            ContentResolver cr = context.getContentResolver();
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
//            Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.PHOTO_URI}, null, null, null);

//            contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);

            Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);

            Cursor cursor = context.getContentResolver().query(photoUri,
                    new String[] {ContactsContract.Contacts.Photo.PHOTO}, null, null, null);

            if (cursor == null) {
                return null;
            }
            if(cursor.moveToFirst()) {
                try {
                    contactUri = Uri.parse(cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)));
                }catch (Exception e){
                    e.printStackTrace();
                }
//                contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
            }
            cursor.close();
        }

        return contactUri;
    }

    public InputStream openPhoto(Context context, long contactId) {
        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
        Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
        Cursor cursor = context.getContentResolver().query(photoUri,
                new String[] {ContactsContract.Contacts.Photo.PHOTO}, null, null, null);
        if (cursor == null) {
            return null;
        }
        try {
            if (cursor.moveToFirst()) {
                byte[] data = cursor.getBlob(0);
                if (data != null) {
                    return new ByteArrayInputStream(data);
                }
            }
        } finally {
            cursor.close();
        }
        return null;
    }


    public long getNumber() {
        return number;
    }

    public int getMostRecentCallId() {
        return mostRecentCallId;
    }

    public String getContactName() {
        return contactName;
    }

    public String getNotes() {
        return notes;
    }

    public int getOutgoingCount() {
        return outgoingCount;
    }

    public void setOutgoingCount(int outgoingCount) {
        this.outgoingCount = outgoingCount;
    }

    public int getAnsweredCount() {
        return answeredCount;
    }

    public void setAnsweredCount(int answeredCount) {
        this.answeredCount = answeredCount;
    }

    public int getMissedCount() {
        return missedCount;
    }

    public void setMissedCount(int missedCount) {
        this.missedCount = missedCount;
    }

    @Override
    public int compareTo(NumberItem item){
        if(this.contactName == null && item.getContactName() == null){
            if(this.number > item.getNumber()){
                return 1;
            } else if(this.number < item.getNumber()){
                return -1;
            }
        } else if(this.contactName != null && item.getContactName() != null){
            return contactName.compareTo(item.getContactName());
        }

        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags){
        out.writeLong(this.number);
        out.writeInt(this.mostRecentCallId);
        out.writeStringArray(new String[]{
                this.contactName,
                this.notes
        });
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public NumberItem(Parcel in){
        this.number = in.readLong();
        this.mostRecentCallId = in.readInt();

        String[] stringVals = new String[2];
        in.readStringArray(stringVals);
        this.contactName = stringVals[0];
        this.notes = stringVals[1];
    }

    public static final Parcelable.Creator<NumberItem> CREATOR
            = new Parcelable.Creator<NumberItem>() {

        // This simply calls the Parcel constructor
        @Override
        public NumberItem createFromParcel(Parcel in) {
            return new NumberItem(in);
        }

        @Override
        public NumberItem[] newArray(int size) {
            return new NumberItem[size];
        }
    };
}
