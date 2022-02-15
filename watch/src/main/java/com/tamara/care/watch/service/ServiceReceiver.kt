package com.tamara.care.watch.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint

class ServiceReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val telephony = context!!.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val number = intent!!.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        Log.w("NUMBER", ">>>>>>>>>>>>> phone $number")
        telephony.listen(object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, incomingNumber: String) {
                super.onCallStateChanged(state, incomingNumber)
                val msg = "incomingNumber : $incomingNumber"
                val toast = Toast.makeText(context, msg, Toast.LENGTH_LONG)
                toast.show()
            }
        }, PhoneStateListener.LISTEN_CALL_STATE)
    }
}