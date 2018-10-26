package com.mitchideas.pfsar.rollwalker

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.games.Games

class AchievementUnlocker {

	private val mData = Data.instance()

	fun Check(main: Main){
		checkRollStackAchievements(main)
	}

	private fun unlock(main: Main, id: Int){
		Games.getAchievementsClient(main, GoogleSignIn.getLastSignedInAccount(main)!!)
			.unlock(main.getString(id))
	}

	private fun checkRollStackAchievements(main: Main){
		val rollStackCopy =  mData.animeRollStack.toArray()


		for(i in rollStackCopy.size - 5 until rollStackCopy.size){
			if(rollStackCopy[i] == mData.maxRoll){
				unlock(main, R.string.ach_stared_victory)
			}
		}
	}

}