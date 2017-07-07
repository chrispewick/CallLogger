package com.pewick.calllogger.adapters;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.pewick.calllogger.R;
import com.pewick.calllogger.database.DataContract;
import com.pewick.calllogger.models.NumberItem;

import org.w3c.dom.Text;

import java.io.InputStream;
import java.util.ArrayList;

/**
 * Custom ArrayAdapter for the phone numbers and contacts list found in the NumbersFragment.
 */
public class NumbersListAdapter extends ArrayAdapter<NumberItem> {
    private final String TAG = getClass().getSimpleName();

    private Activity activity;
    private ArrayList<NumberItem> data;

    public NumbersListAdapter(Activity activity, ArrayList<NumberItem> data){
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
        NumberItem item = this.data.get(position);
        View cellView = convertView;
        NumberVH vh;

        if(convertView == null){
            LayoutInflater inflater = activity.getLayoutInflater();
            cellView = inflater.inflate(R.layout.list_item_number, null);

            vh = new NumberVH();
            vh.number = (TextView) cellView.findViewById(R.id.contact_number);
            vh.icon = (ImageView) cellView.findViewById(R.id.contact_icon);

            cellView.setTag(vh);
        } else{
            vh = (NumberVH) cellView.getTag();
        }

        vh.number.setText(item.getDisplayText());

        Bitmap image = item.getContactImage();
        if(image != null){
            vh.icon.setImageBitmap(image);
//            vh.icon.setColorFilter(ContextCompat.getColor(activity,R.color.transparent));
        } else {
            vh.icon.setImageResource(R.drawable.contact_phone_icon);
//            vh.icon.setColorFilter(ContextCompat.getColor(activity,R.color.black_75_percent));
        }

        return cellView;
    }

    private static class NumberVH {
        TextView number;
        ImageView icon;
    }
}
