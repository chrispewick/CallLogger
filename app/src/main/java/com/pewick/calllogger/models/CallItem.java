package com.pewick.calllogger.models;

import android.telecom.Call;

import com.pewick.calllogger.R;

import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;

/**
 * Created by Chris on 5/17/2017.
 */
public class CallItem implements ILoggerListItem, Comparable<CallItem> {

    private final String TAG = getClass().getSimpleName();

    private int callId;
    private long number;
    private long startTime;
    private long endTime;
    private String inOut;
    private String ansMiss;
    private String contactName;

    public CallItem(int id, long num, long start, long end, String inOut, String ansMiss){
        this.callId = id;
        this.number = num;
        this.startTime = start;
        this.endTime = end;
        this.inOut = inOut;
        this.ansMiss = ansMiss;
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

}
