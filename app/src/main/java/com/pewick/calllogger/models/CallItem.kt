package com.pewick.calllogger.models

import android.os.Parcel
import android.os.Parcelable

import java.util.Calendar

/**
 * Defines the model for a call event.
 */
class CallItem : Comparable<CallItem>, Parcelable {
    private val TAG = javaClass.simpleName

    var callId: Int = 0
        private set
    var number: Long = 0
        private set
    var startTime: Long = 0
        private set
    var endTime: Long = 0
        private set
    var inOut: String? = null
        private set
    var ansMiss: String? = null
        private set
    var contactName: String? = null
    var duration: String? = null
        private set

    val displayText: String
        get() = if (contactName == null) {
            formattedNumber
        } else {
            contactName!!
        }

    val formattedDateTime: String
        get() {
            val cal = Calendar.getInstance()
            cal.timeInMillis = startTime

            return android.text.format.DateFormat.format("MM/dd/yyyy h:mm a", cal).toString()
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

    val callType: Int
        get() = if (this.inOut!!.equals("outgoing", ignoreCase = true)) {
            1
        } else if (this.ansMiss!!.equals("answered", ignoreCase = true)) {
            2
        } else if (this.ansMiss!!.equals("missed", ignoreCase = true)) {
            3
        } else {
            -1
        }

    constructor(id: Int, num: Long, start: Long, end: Long, inOut: String, ansMiss: String) {
        this.callId = id
        this.number = num
        this.startTime = start
        this.endTime = end
        this.inOut = inOut
        this.ansMiss = ansMiss
        this.setDuration()
    }

    private fun setDuration() {
        if (endTime == 0L) {
            //Then the call was a missed call, no duration
            this.duration = ""
        } else {
            val time = endTime - startTime
            val second = time / 1000 % 60
            val minute = time / (1000 * 60) % 60
            val hour = time / (1000 * 60 * 60) % 24

            if (hour != 0L) {
                this.duration = String.format("%dh %dm %ds", hour, minute, second)
            } else if (minute != 0L) {
                this.duration = String.format("%dm %ds", minute, second)
            } else {
                this.duration = String.format("%ds", second)
            }
        }
    }

    override fun compareTo(item: CallItem): Int {
        if (this.startTime < item.startTime) {
            return 1
        } else if (this.startTime > item.startTime) {
            return -1
        }
        return 0
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeInt(this.callId)
        out.writeLongArray(longArrayOf(this.number, this.startTime, this.endTime))
        out.writeStringArray(arrayOf<String>(this.inOut!!, this.ansMiss!!, this.contactName!!))
    }

    override fun describeContents(): Int {
        return 0
    }

    constructor(`in`: Parcel) {
        this.callId = `in`.readInt()

        val longVals = LongArray(3)
        `in`.readLongArray(longVals)
        this.number = longVals[0]
        this.startTime = longVals[1]
        this.endTime = longVals[2]

        val stringVals = arrayOfNulls<String>(3)
        `in`.readStringArray(stringVals)
        this.inOut = stringVals[0]
        this.ansMiss = stringVals[1]
        this.contactName = stringVals[2]
    }

    companion object {

        @JvmField val CREATOR: Parcelable.Creator<CallItem> = object : Parcelable.Creator<CallItem> {

            // This simply calls the Parcel constructor
            override fun createFromParcel(`in`: Parcel): CallItem {
                return CallItem(`in`)
            }

            override fun newArray(size: Int): Array<CallItem?> {
                return arrayOfNulls(size)
            }
        }
    }
}
