package com.pewick.calllogger.fragments

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle

import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat

import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView

import com.pewick.calllogger.R
import com.pewick.calllogger.activity.MainActivity
import com.pewick.calllogger.adapters.HistoryListAdapter
import com.pewick.calllogger.database.DataContract
import com.pewick.calllogger.database.DbHelper
import com.pewick.calllogger.models.CallItem

import java.util.ArrayList
import java.util.Calendar

/**
 * Fragment containing a list of all calls. From the MainActivity, the list can be searched, or
 * filtered by contacts, non-contacts, answered, missed, and outgoing.
 */
class HistoryFragment : Fragment() {
    private val TAG = javaClass.getSimpleName()

    private var noResults: TextView? = null
    private var numberResults: TextView? = null
    private var callListView: ListView? = null
    private var adapter: HistoryListAdapter? = null

    private var callList: ArrayList<CallItem>? = null
    private var callListOriginal: ArrayList<CallItem>? = null
    private var contactsCallList: ArrayList<CallItem>? = null
    private var nonContactsCallList: ArrayList<CallItem>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        Log.i(TAG, "onCreateView")
        val view = inflater.inflate(R.layout.fragment_history_list, container, false)
        callList = ArrayList()
        callListView = view.findViewById<View>(R.id.history_list) as ListView
        noResults = view.findViewById<View>(R.id.no_results) as TextView
        numberResults = view.findViewById<View>(R.id.number_results) as TextView

        this.setListEventListeners()

        return view
    }

    override fun onResume() {
        Log.i(TAG, "onResume")
        super.onResume()
        this.readCallsFromDatabase()
        adapter = HistoryListAdapter(activity!!, callList!!)
        callListView!!.adapter = adapter
        this.checkNumberOfResults()
    }

    fun filterList(charText: String, contacts: Boolean, nonContacts: Boolean, incoming: Boolean, outgoing: Boolean) {
        //        Log.i(TAG, "filterList:");
        //        Log.i(TAG, "    chatText: "+charText);
        //        Log.i(TAG, "    contacts: "+contacts);
        //        Log.i(TAG, "    nonContacts: "+nonContacts);
        //        Log.i(TAG, "    incoming: "+incoming);
        //        Log.i(TAG, "    outgoing: "+outgoing);
        val temp = ArrayList<CallItem>()
        callList!!.clear()

        if (contacts && nonContacts) {
            if (incoming && outgoing) {
                //add all in original list
                temp.addAll(callListOriginal!!)
            } else if (incoming) {
                //add incoming calls from original list
                for (item in callListOriginal!!) {
                    if (item.callType == 2 || item.callType == 3) {
                        temp.add(item)
                    }
                }
            } else if (outgoing) {
                //add outgoing calls from original list
                for (item in callListOriginal!!) {
                    if (item.callType == 1) {
                        temp.add(item)
                    }
                }
            }
            //Otherwise, add NONE from original list
        } else if (contacts) {
            if (incoming && outgoing) {
                //add all in contacts list
                temp.addAll(contactsCallList!!)
            } else if (incoming) {
                //add incoming calls from contacts list
                for (item in contactsCallList!!) {
                    if (item.callType == 2 || item.callType == 3) {
                        temp.add(item)
                    }
                }
            } else if (outgoing) {
                //add outgoing calls from contacts list
                for (item in contactsCallList!!) {
                    if (item.callType == 1) {
                        temp.add(item)
                    }
                }
            }
            //Otherwise, add NONE from contacts list
        } else if (nonContacts) {
            if (incoming && outgoing) {
                //add all in non-contacts list
                temp.addAll(nonContactsCallList!!)
            } else if (incoming) {
                //add incoming calls from non-contacts list
                for (item in nonContactsCallList!!) {
                    if (item.callType == 2 || item.callType == 3) {
                        temp.add(item)
                    }
                }
            } else if (outgoing) {
                //add outgoing calls from non-contacts list
                for (item in nonContactsCallList!!) {
                    if (item.callType == 1) {
                        temp.add(item)
                    }
                }
            }
            //Otherwise, add NONE from non-contacts list
        }

        if (charText.length == 0) {
            callList!!.addAll(temp)
        } else {
            for (i in temp.indices) {
                val entry = temp[i]
                if (java.lang.Long.toString(entry.number).contains(charText)
                        || entry.formattedNumber.contains(charText)
                        || entry.contactName != null && entry.contactName!!.toLowerCase().contains(charText.toLowerCase())) {
                    callList!!.add(entry)
                }
            }
        }
        adapter!!.notifyDataSetChanged()
        this.checkNumberOfResults()
    }

    private fun setListEventListeners() {
        callListView!!.setOnTouchListener { v, event ->
            (activity as MainActivity).closeKeyboard()
            false
        }
    }

    private fun readCallsFromDatabase() {
        Log.i(TAG, "readCallsFromDatabase")
        val startTime = Calendar.getInstance()

        callList = ArrayList()
        callListOriginal = ArrayList()
        contactsCallList = ArrayList()
        nonContactsCallList = ArrayList()
        val dbHelper = DbHelper.getInstance(activity!!)
        val database = dbHelper.readableDatabase
        val projection = arrayOf(DataContract.CallTable.CALL_ID, DataContract.CallTable.NUMBER, DataContract.CallTable.START_TIME, DataContract.CallTable.END_TIME, DataContract.CallTable.INCOMING_OUTGOING, DataContract.CallTable.ANSWERED_MISSED)

        //specify read order based on number
        val sortOrder = DataContract.CallTable.START_TIME + " DESC"

        //fetch the data from the database as specified
        val cursor = database.query(DataContract.CallTable.TABLE_NAME, projection, null, null, null, null, sortOrder)

        if (cursor.moveToFirst()) {
            do {
                val existingCall = CallItem(cursor.getInt(cursor.getColumnIndexOrThrow(DataContract.CallTable.CALL_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.CallTable.NUMBER)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.CallTable.START_TIME)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.CallTable.END_TIME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DataContract.CallTable.INCOMING_OUTGOING)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DataContract.CallTable.ANSWERED_MISSED)) ?: "")

                //Need to do this every time, in case the user changes a contact name
                existingCall.contactName = (activity as MainActivity).getContactName(existingCall.number.toString())

                if (existingCall.contactName != null) {
                    //Then this call was from a contact
                    contactsCallList!!.add(existingCall)
                } else {
                    //Then this call was not from a contact
                    nonContactsCallList!!.add(existingCall)
                }
                callList!!.add(existingCall)
                callListOriginal!!.add(existingCall)
            } while (cursor.moveToNext())
        }
        cursor.close()

        val endTime = Calendar.getInstance()
        Log.i(TAG, "Time: " + (endTime.timeInMillis - startTime.timeInMillis))
        //        Log.i(TAG, "callList size: "+callList.size());
        //        Log.i(TAG, "contacts size: "+contactsCallList.size());
        //        Log.i(TAG, "non-contacts size: "+ nonContactsCallList.size())
    }

    private fun checkNumberOfResults() {
        if (callList!!.size == 0) {
            noResults!!.visibility = View.VISIBLE
            callListView!!.visibility = View.GONE
            numberResults!!.visibility = View.GONE
        } else {
            noResults!!.visibility = View.GONE
            callListView!!.visibility = View.VISIBLE
            numberResults!!.visibility = View.VISIBLE
            this.showNumberOfResults()
        }
    }

    private fun showNumberOfResults() {
        val numResults = callList!!.size.toString() + " " + resources.getString(R.string.results)
        numberResults!!.text = numResults
    }

    fun refreshList() {
        readCallsFromDatabase()
        adapter = HistoryListAdapter(activity!!, callList!!)
        callListView!!.adapter = adapter
        this.checkNumberOfResults()
    }
}
