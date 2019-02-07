package com.pewick.calllogger.fragments

import android.app.DialogFragment
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.ContactsContract
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import android.widget.TextView

import com.pewick.calllogger.R
import com.pewick.calllogger.activity.MainActivity
import com.pewick.calllogger.adapters.NumbersListAdapter
import com.pewick.calllogger.database.DataContract
import com.pewick.calllogger.database.DbHelper
import com.pewick.calllogger.models.NumberItem

import java.io.IOException
import java.io.InputStream
import java.util.ArrayList
import java.util.Collections

/**
 * Fragment containing a list of all numbers that have called, or been called by this device.
 * From the MainActivity, the list can be searched, or filtered by contacts and non-contacts.
 */
class NumbersFragment : Fragment() {
    private val TAG = javaClass.simpleName

    private var noResults: TextView? = null
    private var numbersListView: ListView? = null
    private var adapter: NumbersListAdapter? = null

    private var numbersList: ArrayList<NumberItem>? = null
    private var numbersListOriginal: ArrayList<NumberItem>? = null
    private var numberContactList: ArrayList<NumberItem>? = null
    private var numberNonContactList: ArrayList<NumberItem>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_numbers_list, container, false)
        numbersList = ArrayList()
        numbersListView = view.findViewById<View>(R.id.numbers_list) as ListView
        adapter = NumbersListAdapter(activity!!, numbersList!!)
        numbersListView!!.adapter = adapter

        noResults = view.findViewById<View>(R.id.no_results) as TextView

        this.setListEventListeners()
        this.setOnClickListener()

        return view
    }

    override fun onResume() {
        Log.i(TAG, "onResume")
        super.onResume()
        this.readNumbersFromDatabase()
        adapter = NumbersListAdapter(activity!!, numbersList!!)
        numbersListView!!.adapter = adapter
        this.checkNumberOfResults()
    }

    private fun setOnClickListener() {
        numbersListView!!.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, position, id ->
            val item = numbersList!![position]

//            val args = Bundle().apply {
//                put
//            }

            val args = Bundle()
            args.putParcelable("number_item", item)

            val dialog = NumberDialogFragment()
            dialog.setStyle(DialogFragment.STYLE_NORMAL, R.style.NewDialog)
            dialog.arguments = args

            dialog.show(activity!!.fragmentManager, null)
        }
    }

    private fun setListEventListeners() {
        numbersListView!!.setOnTouchListener { v, event ->
            (activity as MainActivity).closeKeyboard()
            false
        }
    }

    fun filterList(charText: String, contacts: Boolean, nonContacts: Boolean) {
        //        Log.i(TAG, "filterList:");
        //        Log.i(TAG, "    chatText: " + charText);
        //        Log.i(TAG, "    contacts: " + contacts);
        //        Log.i(TAG, "    nonContacts: " + nonContacts);
        val temp = ArrayList<NumberItem>()
        numbersList!!.clear()

        if (contacts && nonContacts) {
            temp.addAll(numbersListOriginal!!)
        } else if (contacts) {
            temp.addAll(numberContactList!!)
        } else if (nonContacts) {
            temp.addAll(numberNonContactList!!)
        }

        if (charText.length == 0) {
            numbersList!!.addAll(temp)
        } else {
            for (i in temp.indices) {
                val entry = temp[i]
                if (java.lang.Long.toString(entry.number).contains(charText)
                        || entry.formattedNumber.contains(charText)
                        || entry.contactName != null && entry.contactName!!.toLowerCase().contains(charText.toLowerCase())) {
                    numbersList!!.add(entry)
                }
            }
        }
        adapter!!.notifyDataSetChanged()
        this.checkNumberOfResults()
    }

    private fun retrieveContactPhoto(context: Context, number: String): Bitmap? {
        var contactId: String? = null
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup._ID)
        val cursor = context.contentResolver.query(uri, projection, null, null, null)

        if (cursor != null && cursor.moveToNext()) {
            contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID))
            cursor.close()
        }

        var photo = BitmapFactory.decodeResource(context.resources,
                R.drawable.contact_phone_icon)

        try {
            val inputStream = ContactsContract.Contacts.openContactPhotoInputStream(context.contentResolver,
                    ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, java.lang.Long.valueOf(contactId!!)))

            if (inputStream != null) {
                photo = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }

        return photo
    }

    private fun readNumbersFromDatabase() {
        numbersList = ArrayList()
        numbersListOriginal = ArrayList()
        numberContactList = ArrayList()
        numberNonContactList = ArrayList()
        val dbHelper = DbHelper.getInstance(activity!!)
        val database = dbHelper.readableDatabase
        val projection = arrayOf(DataContract.NumbersTable.NUMBER, DataContract.NumbersTable.MOST_RECENT, DataContract.NumbersTable.NOTES, DataContract.NumbersTable.OUTGOING_COUNT, DataContract.NumbersTable.ANSWERED_COUNT, DataContract.NumbersTable.MISSED_COUNT)

        //fetch the data from the database as specified
        val cursor = database.query(DataContract.NumbersTable.TABLE_NAME, projection, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            do {
                val number = cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.NUMBER))
                val existingNumber = NumberItem(number,
                        cursor.getInt(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.MOST_RECENT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.NOTES)) ?: "",
                        cursor.getInt(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.OUTGOING_COUNT)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.ANSWERED_COUNT)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.MISSED_COUNT)))

                //Need to do this every time, in case the user changes a contact name
                existingNumber.contactName = (activity as MainActivity).getContactName(existingNumber.number.toString())

                if (existingNumber.contactName != null) {
                    //then the number is in contacts
                    numberContactList!!.add(existingNumber)
                    //get the contact image
                    val bitmap = retrieveContactPhoto(context!!, java.lang.Long.toString(existingNumber.number))
                    if (bitmap != null) {
                        existingNumber.contactImage = bitmap
                    }
                } else {
                    //then the number is NOT in contacts
                    numberNonContactList!!.add(existingNumber)
                }
            } while (cursor.moveToNext())
        }
        cursor.close()

        Collections.sort(numberNonContactList)
        Collections.sort(numberContactList)

        //The following ensures that non-contacts will be above contacts
        numbersList!!.addAll(numberNonContactList!!)
        numbersList!!.addAll(numberContactList!!)
        numbersListOriginal!!.addAll(numberNonContactList!!)
        numbersListOriginal!!.addAll(numberContactList!!)
    }

    private fun checkNumberOfResults() {
        Log.i(TAG, "checkNumberOfResults, empty? " + (numbersList!!.size == 0))
        if (numbersList!!.size == 0) {
            noResults!!.visibility = View.VISIBLE
            numbersListView!!.visibility = View.GONE
        } else {
            noResults!!.visibility = View.GONE
            numbersListView!!.visibility = View.VISIBLE
        }
    }

    fun refreshList() {
        Log.i(TAG, "Refresh List")
        readNumbersFromDatabase()
        adapter = NumbersListAdapter(activity!!, numbersList!!)
        numbersListView!!.adapter = adapter
    }
}
