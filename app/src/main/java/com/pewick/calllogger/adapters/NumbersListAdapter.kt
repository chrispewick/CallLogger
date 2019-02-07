package com.pewick.calllogger.adapters

import android.app.Activity
import android.graphics.Bitmap

import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

import com.pewick.calllogger.R
import com.pewick.calllogger.models.NumberItem

import java.util.ArrayList

/**
 * Custom ArrayAdapter for the phone numbers and contacts list found in the NumbersFragment.
 */
class NumbersListAdapter(private val activity: Activity, private val data: ArrayList<NumberItem>) : ArrayAdapter<NumberItem>(activity, R.layout.fragment_numbers_list, data) {
    private val TAG = javaClass.simpleName

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = this.data[position]
        var cellView = convertView
        val vh: NumberVH

        if (convertView == null) {
            val inflater = activity.layoutInflater
            cellView = inflater.inflate(R.layout.list_item_number, null)

            vh = NumberVH()
            vh.number = cellView!!.findViewById<View>(R.id.contact_number) as TextView
            vh.icon = cellView.findViewById<View>(R.id.contact_icon) as ImageView

            cellView.tag = vh
        } else {
            vh = cellView!!.tag as NumberVH
        }

        vh.number!!.text = item.displayText

        val image = item.contactImage
        if (image != null) {
            vh.icon!!.setImageBitmap(image)
            //            vh.icon.setColorFilter(ContextCompat.getColor(activity,R.color.transparent));
        } else {
            vh.icon!!.setImageResource(R.drawable.contact_phone_icon)
            //            vh.icon.setColorFilter(ContextCompat.getColor(activity,R.color.black_75_percent));
        }

        return cellView
    }

    private class NumberVH {
        internal var number: TextView? = null
        internal var icon: ImageView? = null
    }
}
