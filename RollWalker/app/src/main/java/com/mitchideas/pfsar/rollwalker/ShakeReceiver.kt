package com.mitchideas.pfsar.rollwalker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ShakeReceiver: BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		if (intent.action.equals("shake.detector")) {
			Log.d(Main.TAG, "BACKGROUND EVENET: SHOOCK:")
		}

	}
}