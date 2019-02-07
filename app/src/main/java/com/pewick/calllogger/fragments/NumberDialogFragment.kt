package com.pewick.calllogger.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import com.pewick.calllogger.R
import com.pewick.calllogger.activity.MainActivity
import com.pewick.calllogger.database.DataContract
import com.pewick.calllogger.database.DbHelper
import com.pewick.calllogger.models.CallItem
import com.pewick.calllogger.models.NumberItem

/**
 * Custom DialogFragment used to display additional information about a number or contact from the
 * NumbersFragment. Opened by clicking a NumberItem in the numbers list, the dialog contains all
 * relevant information about that number.
 */
class NumberDialogFragment : DialogFragment() {
    private val TAG = javaClass.simpleName

    private var dialog: AlertDialog? = null
    private var deleteAlertDialog: AlertDialog? = null

    private var numberItem: NumberItem? = null

    private var notes: EditText? = null
    private var titleContact: TextView? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog? {
        val builder = AlertDialog.Builder(activity,
                R.style.NewCustomDialog)
        val inflater = activity.layoutInflater
        val view = inflater.inflate(R.layout.fragment_number_dialog,
                null)

        val args = arguments
        this.numberItem = args.get("number_item") as NumberItem

        view.requestFocus()
        builder.setTitle(null).setView(view)
        dialog = builder.create()
        return dialog
    }

    override fun onStart() {
        super.onStart()
        getDialog().window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)

        this.configureDialogTextContent()
        this.configureCallButton()
        this.configureDoneButton()
        this.configureDeleteButton()
    }

    private fun configureDialogTextContent() {
        titleContact = dialog!!.findViewById<View>(R.id.title_contact_name) as TextView
        if (numberItem!!.contactName != null) {
            titleContact!!.text = numberItem!!.contactName
        } else {
            titleContact!!.visibility = View.GONE
        }

        val titleNumber = dialog!!.findViewById<View>(R.id.title_number) as TextView
        titleNumber.text = numberItem!!.formattedNumber

        val mostRecentDate = dialog!!.findViewById<View>(R.id.recent_date_time) as TextView
        val mostRecentIcon = dialog!!.findViewById<View>(R.id.recent_call_type_icon) as ImageView
        val callItem = this.readMostRecentCallFromDatabase()
        if (callItem != null) {
            mostRecentDate.text = callItem.formattedDateTime
            when (callItem.callType) {
                1 -> {
                    //outgoing
                    mostRecentIcon.setImageResource(R.drawable.outgoing_made_icon)
                    mostRecentIcon.setColorFilter(ContextCompat.getColor(activity, R.color.blue))
                }
                2 -> {
                    //answered
                    mostRecentIcon.setImageResource(R.drawable.incoming_answered_icon)
                    mostRecentIcon.setColorFilter(ContextCompat.getColor(activity, R.color.green))
                }
                3 -> {
                    //missed
                    mostRecentIcon.setImageResource(R.drawable.incoming_missed_icon)
                    mostRecentIcon.setColorFilter(ContextCompat.getColor(activity, R.color.red))
                }
            }
        }

        val incomingTotal = dialog!!.findViewById<View>(R.id.total_incoming) as TextView
        val outgoingTotal = dialog!!.findViewById<View>(R.id.total_outgoing) as TextView
        val incomingTotalStr = (resources.getString(R.string.total_in)
                + " "
                + (numberItem!!.answeredCount + numberItem!!.missedCount))
        val outgoingTotalStr = (resources.getString(R.string.total_out)
                + " "
                + numberItem!!.outgoingCount)
        incomingTotal.text = incomingTotalStr
        outgoingTotal.text = outgoingTotalStr

        notes = dialog!!.findViewById<View>(R.id.notes_field) as EditText
        notes!!.setText(this.readNotesFormDatabase())
        notes!!.setSelection(notes!!.text.length)
    }

    private fun readMostRecentCallFromDatabase(): CallItem? {
        val dbHelper = DbHelper.getInstance(activity)
        val database = dbHelper.readableDatabase
        val projection = arrayOf(DataContract.CallTable.CALL_ID, DataContract.CallTable.NUMBER, DataContract.CallTable.START_TIME, DataContract.CallTable.END_TIME, DataContract.CallTable.INCOMING_OUTGOING, DataContract.CallTable.ANSWERED_MISSED)

        val whereArgs = String.format("%s = %s",
                DataContract.CallTable.CALL_ID,
                numberItem!!.mostRecentCallId)
        val cursor = database.query(DataContract.CallTable.TABLE_NAME,
                projection,
                whereArgs, null, null, null, null)

        var callItem: CallItem? = null
        if (cursor.moveToFirst()) {
            callItem = CallItem(cursor.getInt(cursor.getColumnIndexOrThrow(DataContract.CallTable.CALL_ID)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.CallTable.NUMBER)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.CallTable.START_TIME)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(DataContract.CallTable.END_TIME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DataContract.CallTable.INCOMING_OUTGOING)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DataContract.CallTable.ANSWERED_MISSED)) ?: "")
        } else {
            Log.i(TAG, "Most recent was null!") //This could happen if I decide to allow users to delete calls from app
        }
        cursor.close()
        database.close()
        dbHelper.close()
        return callItem
    }

    private fun readNotesFormDatabase(): String {
        //Needed b/c: when user edits the notes, then closes the dialog, the fragment below is still
        //holding the NumberItem with the old value. Since NOTES is the only value that can be
        //changed, I will simply read the notes from the database each time.

        var notes = ""

        val dbHelper = DbHelper.getInstance(activity)
        val database = dbHelper.readableDatabase
        val projection = arrayOf(DataContract.NumbersTable.NOTES)

        val whereArgs = String.format("%s = %s",
                DataContract.NumbersTable.NUMBER,
                numberItem!!.number)
        val cursor = database.query(DataContract.NumbersTable.TABLE_NAME,
                projection,
                whereArgs, null, null, null, null)
        if (cursor.moveToFirst()) {
            notes = cursor.getString(cursor.getColumnIndexOrThrow(DataContract.NumbersTable.NOTES)) ?: ""
        }
        cursor.close()
        return notes
    }

    private fun configureDeleteButton() {
        val deleteButton = dialog!!.findViewById<View>(R.id.delete_button) as ImageView
        deleteButton.setOnClickListener { showDeleteConfirmationDialog() }
    }

    private fun showDeleteConfirmationDialog() {
        val builder = AlertDialog.Builder(activity,
                R.style.AlertDialogTheme)
        val inflater = activity.layoutInflater
        val alertDialogView = inflater.inflate(R.layout.delete_alert_dialog, null)
        builder.setView(alertDialogView)

        val deleteButton = alertDialogView.findViewById<View>(R.id.confirm_delete) as TextView
        deleteButton.setOnClickListener { view ->
            Toast.makeText(view.context, "Number Deleted", Toast.LENGTH_SHORT).show()
            //Delete the number, and its calls, from the database
            deleteNumberFromDatabase(java.lang.Long.toString(numberItem!!.number))
            //Now refresh the lists
            (activity as MainActivity).refreshLists()

            deleteAlertDialog!!.dismiss()
            dialog!!.dismiss()
        }

        val cancelButton = alertDialogView.findViewById<View>(R.id.cancel_delete) as TextView
        cancelButton.setOnClickListener { deleteAlertDialog!!.dismiss() }

        deleteAlertDialog = builder.create()
        deleteAlertDialog!!.setCanceledOnTouchOutside(true)
        deleteAlertDialog!!.show()
    }

    private fun deleteNumberFromDatabase(number: String) {
        val dbHelper = DbHelper.getInstance(activity)
        val database = dbHelper.writableDatabase
        //First, delete all call entries in the CallTable
        database.delete(DataContract.CallTable.TABLE_NAME,
                DataContract.CallTable.NUMBER + "=" + number, null)

        //Then delete the number from the NumberTable
        database.delete(DataContract.NumbersTable.TABLE_NAME,
                DataContract.NumbersTable.NUMBER + "=" + number, null)
    }

    private fun configureDoneButton() {
        val doneButton = dialog!!.findViewById<View>(R.id.done_button) as LinearLayout
        doneButton.setOnClickListener {
            val dbHelper = DbHelper.getInstance(activity)
            val database = dbHelper.readableDatabase
            val cv = ContentValues()
            cv.put(DataContract.NumbersTable.NOTES, notes!!.text.toString())
            database.update(DataContract.NumbersTable.TABLE_NAME,
                    cv,
                    DataContract.NumbersTable.NUMBER + "= ?",
                    arrayOf(java.lang.Long.toString(numberItem!!.number)))
            database.close()
            dbHelper.close()
            dialog!!.dismiss()
        }
    }

    private fun configureCallButton() {
        val callButton = dialog!!.findViewById<View>(R.id.call_button) as LinearLayout
        callButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:" + numberItem!!.number)

            try {
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
