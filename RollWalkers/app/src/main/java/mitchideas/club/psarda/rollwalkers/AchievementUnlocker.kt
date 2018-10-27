package mitchideas.club.psarda.rollwalkers

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.games.Games
import java.util.*

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

	fun checkCombo(main: Main){
		val curCombo = mData.comboNum

		when {
			curCombo > 50 -> unlock(main, R.string.achievement_combo_maximums)
			curCombo > 15 -> unlock(main, R.string.achievement_combo_major)
			curCombo > 5 -> unlock(main, R.string.achievement_combo_lesser)
		}
	}

	private fun unlock(main: Main, id: Int){
		Games.getAchievementsClient(main, GoogleSignIn.getLastSignedInAccount(main)!!)
			.unlockImmediate(main.getString(id))
	}

	private fun checkRollStackAchievements(main: Main){
		val rollStackCopy =  mData.animeRollStack.toArray()

        if( rollStackCopy.isNotEmpty()){
			var i = 0
			while(i < 5 && i < rollStackCopy.size){
				if(rollStackCopy[i] == mData.maxRoll){
					unlock(main, R.string.achievement_stared_victory_in_the_face_and_walked_straight_past)
				}
				i++
			}

			if(rollStackCopy.first() == 80085){
				unlock(main, R.string.achievement_roll_80085)
			}

			val calendar = Calendar.getInstance()
			val day = calendar.get(Calendar.DAY_OF_WEEK)

			if(day == Calendar.TUESDAY && rollStackCopy.first() == 45){
				unlock(main, R.string.achievement_on_a_tuesday_roll_45)
			}

		}
	}

	private fun distanceAchievements(main: Main){
		var totalDistanceTravled = 0.0

		for(roll in mData.rollData){
			totalDistanceTravled += roll.distance
		}

		when {
			totalDistanceTravled > 1 -> unlock(main, R.string.achievement_baby_steps)
		}

	}

	private fun checkShakeAchievements(main: Main){
		var totalShakes = 0.0

		for(roll in mData.rollData){
			totalShakes += roll.shakes
		}

		when {
			totalShakes > 7000 -> unlock(main, R.string.achievement_shakeius_maximums)
			totalShakes > 6000 -> unlock(main, R.string.achievement_shakeius_consul)
			totalShakes > 5000 -> unlock(main, R.string.achievement_shakeius_senator)
			totalShakes > 4000 -> unlock(main, R.string.achievement_shakeius_quaestor)
			totalShakes > 3000 -> unlock(main, R.string.achievement_shakeius_military_tribune)
			totalShakes > 2000 -> unlock(main, R.string.achievement_why_walk_when_you_can_shake)
		}
	}

}