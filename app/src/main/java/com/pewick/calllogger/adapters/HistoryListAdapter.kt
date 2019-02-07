package com.pewick.calllogger.adapters

import android.app.Activity
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

import com.pewick.calllogger.R
import com.pewick.calllogger.models.CallItem

import java.util.ArrayList

/**
 * Custom ArrayAdapter for the call history list found in the HistoryFragment.
 */
class HistoryListAdapter(private val activity: Activity, private val data: ArrayList<CallItem>) : ArrayAdapter<CallItem>(activity, R.layout.fragment_numbers_list, data) {

    override fun getViewTypeCount(): Int {
        return 1
    }


    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = this.data[position]

        var cellView = convertView
        val vh: CallVH

        if (convertView == null) {
            val inflater = activity.layoutInflater
            cellView = inflater.inflate(R.layout.list_item_call, null)

            vh = CallVH()
            vh.number = cellView!!.findViewById<View>(R.id.contact_number) as TextView
            vh.dateTime = cellView.findViewById<View>(R.id.date_time) as TextView
            vh.callDuration = cellView.findViewById<View>(R.id.call_duration) as TextView
            vh.callTypeIcon = cellView.findViewById<View>(R.id.call_type_icon) as ImageView

            cellView.tag = vh
        } else {
            vh = cellView!!.tag as CallVH
        }

        vh.number!!.text = item.displayText
        vh.dateTime!!.text = item.formattedDateTime
        vh.callDuration!!.text = item.duration
        when (item.callType) {
            1 -> {
                //outgoing
                vh.callTypeIcon!!.setImageResource(R.drawable.outgoing_made_icon)
                vh.callTypeIcon!!.setColorFilter(ContextCompat.getColor(activity, R.color.blue))
            }
            2 -> {
                //answered
                vh.callTypeIcon!!.setImageResource(R.drawable.incoming_answered_icon)
                vh.callTypeIcon!!.setColorFilter(ContextCompat.getColor(activity, R.color.green))
            }
            3 -> {
                //missed
                vh.callTypeIcon!!.setImageResource(R.drawable.incoming_missed_icon)
                vh.callTypeIcon!!.setColorFilter(ContextCompat.getColor(activity, R.color.red))
            }
        }
        return cellView
    }

    private class CallVH {
        internal var number: TextView? = null
        internal var dateTime: TextView? = null
        internal var callDuration: TextView? = null
        internal var callTypeIcon: ImageView? = null
    }
}
