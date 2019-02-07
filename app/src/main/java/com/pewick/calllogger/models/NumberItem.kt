package com.pewick.calllogger.models

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import android.util.Log

import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * Defines the model for a phone number.
 */
class NumberItem : Comparable<NumberItem>, Parcelable {

    private val TAG = javaClass.simpleName

    var number: Long = 0
        private set
    var mostRecentCallId: Int = 0
        private set
    var outgoingCount: Int = 0
    var answeredCount: Int = 0
    var missedCount: Int = 0
    var contactName: String? = null
    var notes: String = ""
        private set

    var contactImage: Bitmap? = null

    val displayText: String
        get() = if (contactName == null) {
            formattedNumber
        } else {
            contactName!!
        }

    val formattedNumber: String
        get() {
            val temp = "" + this.number
            return if (temp.length == 10) {
                String.format("(%s) %s-%s",
                        temp.substring(0, 3),
                        temp.substring(3, 6),
                        temp.substring(6, 10))
            } else if (temp.length == 7) {
                String.format("%s-%s",
                        temp.substring(0, 3),
                        temp.substring(3))
            } else {
                temp
            }
        }

    constructor(num: Long, recent: Int, notes: String, outgoing: Int, answered: Int, missed: Int) {
        this.number = num
        this.mostRecentCallId = recent
        this.notes = notes
        this.outgoingCount = outgoing
        this.answeredCount = answered
        this.missedCount = missed
    }

    //For earlier versions only
    constructor(num: Long, recent: Int, contact: String, notes: String) {
        this.number = num
        this.mostRecentCallId = recent
        this.contactName = contact
        this.notes = notes
    }

    fun getPhotoUri(context: Context, number: Long): Uri? {
        try {
            val cur = context.contentResolver.query(
                    ContactsContract.Data.CONTENT_URI, null,
                    ContactsContract.CommonDataKinds.Phone.NUMBER + "=" + number + " AND "
                            + ContactsContract.Data.MIMETYPE + "='"
                            + ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE + "'", null, null)
            if (cur != null) {
                Log.i(TAG, "cursor NOT null")
                if (!cur.moveToFirst()) {
                    Log.i(TAG, "No photo")
                    return null // no photo
                }
            } else {
                Log.i(TAG, "cursor null")
                return null // error in cursor process
            }
        } catch (e: Exception) {
            Log.i(TAG, "getPhotoUri Exception!")
            e.printStackTrace()
            return null
        }

        val person = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, number)
        return Uri.withAppendedPath(person, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY)
    }

    fun getContactImage(context: Context, phoneNumber: String): Uri? {
        var contactUri: Uri? = null
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            val cr = context.contentResolver
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
            //            Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.PHOTO_URI}, null, null, null);

            //            contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);

            val photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY)

            val cursor = context.contentResolver.query(photoUri,
                    arrayOf(ContactsContract.Contacts.Photo.PHOTO), null, null, null) ?: return null

            if (cursor.moveToFirst()) {
                try {
                    contactUri = Uri.parse(cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)))
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                //                contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
            }
            cursor.close()
        }

        return contactUri
    }

    fun openPhoto(context: Context, contactId: Long): InputStream? {
        val contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
        val photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY)
        val cursor = context.contentResolver.query(photoUri,
                arrayOf(ContactsContract.Contacts.Photo.PHOTO), null, null, null) ?: return null
        try {
            if (cursor.moveToFirst()) {
                val data = cursor.getBlob(0)
                if (data != null) {
                    return ByteArrayInputStream(data)
                }
            }
        } finally {
            cursor.close()
        }
        return null
    }

    override fun compareTo(item: NumberItem): Int {
        if (this.contactName == null && item.contactName == null) {
            if (this.number > item.number) {
                return 1
            } else if (this.number < item.number) {
                return -1
            }
        } else if (this.contactName != null && item.contactName != null) {
            return contactName!!.compareTo(item.contactName!!)
        }

        return 0
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeLong(this.number)
        out.writeInt(this.mostRecentCallId)
        out.writeStringArray(arrayOf<String>(this.contactName ?: "", this.notes))
    }

    override fun describeContents(): Int {
        return 0
    }

    constructor(`in`: Parcel) {
        this.number = `in`.readLong()
        this.mostRecentCallId = `in`.readInt()

        val stringVals = arrayOfNulls<String>(2)
        `in`.readStringArray(stringVals)
        this.contactName = stringVals[0]
        this.notes = stringVals[1]!!
    }

    companion object {

        @JvmField val CREATOR: Parcelable.Creator<NumberItem> = object : Parcelable.Creator<NumberItem> {

            // This simply calls the Parcel constructor
            override fun createFromParcel(`in`: Parcel): NumberItem {
                return NumberItem(`in`)
            }

            override fun newArray(size: Int): Array<NumberItem??> {
                return arrayOfNulls(size)
            }
        }
    }
}
