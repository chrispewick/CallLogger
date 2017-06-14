package com.pewick.calllogger.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.telecom.Call;
import android.util.Log;

import com.pewick.calllogger.R;

import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;

/**
 * Created by Chris on 5/17/2017.
 */
public class CallItem implements ILoggerListItem, Comparable<CallItem>, Parcelable {

    private final String TAG = getClass().getSimpleName();

    private int callId;
    private long number;
    private long startTime;
    private long endTime;
    private String inOut;
    private String ansMiss;
    private String contactName;
    private String duration;

    public CallItem(int id, long num, long start, long end, String inOut, String ansMiss){
        this.callId = id;
        this.number = num;
        this.startTime = start;
        this.endTime = end;
        this.inOut = inOut;
        this.ansMiss = ansMiss;
        this.setDuration();
    }

    private void setDuration(){
        if(endTime == 0){
            //Then the call was a missed call, no duration
            this.duration = "";
        } else{
            long time = endTime - startTime;
            Log.i(TAG, "Time: "+endTime);

            long second = (time / 1000) % 60;
            long minute = (time / (1000 * 60)) % 60;
            long hour = (time / (1000 * 60 * 60)) % 24;

            if(hour != 0){
                this.duration  = String.format("%dh %dm %ds", hour, minute, second);
            } else if(minute != 0){
                this.duration  = String.format("%dm %ds", minute, second);
            } else{
                this.duration  = String.format("%ds", second);
            }


            Log.i(TAG, "Duration: "+duration);
        }
    }

    public String getDuration(){
        return this.duration;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getDisplayText(){
        if(contactName == null){
            return getFormattedNumber();
        } else{
            return contactName;
        }
    }

    public String getFormattedDateTime(){
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startTime);

        return android.text.format.DateFormat.format("MM/dd/yyyy h:mm a", cal).toString();
    }

    public String getFormattedNumber(){
        String temp = ""+this.number;
        if(temp.length()==10) {
            return String.format("(%s) %s-%s",
                    temp.substring(0, 3),
                    temp.substring(3, 6),
                    temp.substring(6, 10));
        }else {
            return temp;
        }
    }

    public int getCallType(){
        if(this.inOut.equalsIgnoreCase("outgoing")){
            return 1;
        } else if(this.ansMiss.equalsIgnoreCase("answered")){
            return 2;
        } else if (this.ansMiss.equalsIgnoreCase("missed")){
            return 3;
        } else {
            return -1;
        }
    }

    public int getCallId() {
        return callId;
    }

    public long getNumber() {
        return number;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public String getInOut() {
        return inOut;
    }

    public String getAnsMiss() {
        return ansMiss;
    }

    @Override
    public int compareTo(CallItem item){
        if(this.startTime < item.getStartTime()){
            return 1;
        } else if(this.startTime > item.getStartTime()){
            return -1;
        }
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.callId);
        out.writeLongArray(new long[]{
                this.number,
                this.startTime,
                this.endTime
        });
        out.writeStringArray(new String[] {
            this.inOut,
            this.ansMiss,
            this.contactName
        });
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public CallItem(Parcel in){
        this.callId = in.readInt();

        long[] longVals = new long[3];
        in.readLongArray(longVals);
        this.number = longVals[0];
        this.startTime = longVals[1];
        this.endTime = longVals[2];

        String[] stringVals = new String[3];
        in.readStringArray(stringVals);
        this.inOut = stringVals[0];
        this.ansMiss = stringVals[1];
        this.contactName = stringVals[2];
    }

    public static final Parcelable.Creator<CallItem> CREATOR
            = new Parcelable.Creator<CallItem>() {

        // This simply calls the Parcel constructor
        @Override
        public CallItem createFromParcel(Parcel in) {
            return new CallItem(in);
        }

        @Override
        public CallItem[] newArray(int size) {
            return new CallItem[size];
        }
    };
}
