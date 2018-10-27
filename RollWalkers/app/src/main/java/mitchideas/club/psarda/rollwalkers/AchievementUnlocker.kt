package mitchideas.club.psarda.rollwalkers

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.games.Games

class AchievementUnlocker {

	private val mData = Data.instance()

	fun CheckRoll(main: Main){
		if(mData.maxRoll > Main.START_MAX_ROLL){
			checkRollStackAchievements(main)
		}
	}

	fun checkMoved(main: Main){
		distanceAchievements(main)
	}

	fun checkShake(main: Main){
		checkShakeAchievements(main)
	}

	private fun unlock(main: Main, id: Int){
		Games.getAchievementsClient(main, GoogleSignIn.getLastSignedInAccount(main)!!)
			.unlockImmediate(main.getString(id))
	}

	private fun checkRollStackAchievements(main: Main){
		val rollStackCopy =  mData.animeRollStack.toArray()


		for(i in rollStackCopy.size - 5 until rollStackCopy.size){
			if(rollStackCopy[i] == mData.maxRoll){
				unlock(main, R.string.achievement_stared_vic_1)
			}
		}
	}

	private fun distanceAchievements(main: Main){
		var totalDistanceTravled = 0.0

		for(roll in mData.rollData){
			totalDistanceTravled += roll.distance
		}
	}

	private fun checkShakeAchievements(main: Main){
		var totalShakes = 0.0

		for(roll in mData.rollData){
			totalShakes += roll.shakes
		}

		when {
			totalShakes > 16000 -> unlock(main, R.string.achievement_shakeius_quaestor)
			totalShakes > 8000 -> unlock(main, R.string.achievement_shakeius_military_tribune)
			totalShakes > 2000 -> unlock(main, R.string.achievement_why_walk_when_you_can_shake)
		}
	}

}