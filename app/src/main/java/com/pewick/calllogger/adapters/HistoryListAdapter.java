package com.pewick.calllogger.adapters;

import android.app.Activity;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.pewick.calllogger.R;
import com.pewick.calllogger.models.CallItem;
import com.pewick.calllogger.models.NumberItem;

import java.util.ArrayList;

/**
 * Created by Chris on 5/17/2017.
 */
public class HistoryListAdapter extends ArrayAdapter<CallItem> {

    private Activity activity;
    private ArrayList<CallItem> data;

    public HistoryListAdapter(Activity activity, ArrayList<CallItem> data){
        super(activity, R.layout.fragment_numbers_list, data);
        this.data = data;
        this.activity = activity;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        CallItem item = this.data.get(position);

        View cellView = convertView;
        CallVH vh;

        if(convertView == null){
            LayoutInflater inflater = activity.getLayoutInflater();
            cellView = inflater.inflate(R.layout.list_item_call, null);

            vh = new CallVH();
            vh.number = (TextView) cellView.findViewById(R.id.contact_number);
            vh.dateTime = (TextView) cellView.findViewById(R.id.date_time);
            vh.callDuration = (TextView) cellView.findViewById(R.id.call_duration);
            vh.callTypeIcon = (ImageView) cellView.findViewById(R.id.call_type_icon);

            cellView.setTag(vh);
        } else{
            vh = (CallVH) cellView.getTag();
        }

        vh.number.setText(item.getDisplayText());
        vh.dateTime.setText(item.getFormattedDateTime());
        vh.callDuration.setText(item.getDuration());
        switch(item.getCallType()){
            case 1:
                //outgoing
                vh.callTypeIcon.setImageResource(R.drawable.outgoing_made_icon);
                vh.callTypeIcon.setColorFilter(ContextCompat.getColor(activity,R.color.blue));
                break;
            case 2:
                //answered
                vh.callTypeIcon.setImageResource(R.drawable.incoming_answered_icon);
                vh.callTypeIcon.setColorFilter(ContextCompat.getColor(activity,R.color.green));
                break;
            case 3:
                //missed
                vh.callTypeIcon.setImageResource(R.drawable.incoming_missed_icon);
                vh.callTypeIcon.setColorFilter(ContextCompat.getColor(activity,R.color.red));
                break;
        }
        return cellView;
    }

    private static class CallVH {
        TextView number;
        TextView dateTime;
        TextView callDuration;
        ImageView callTypeIcon;
    }
}
