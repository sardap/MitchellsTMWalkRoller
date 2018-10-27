package mitchideas.club.psarda.rollwalkers

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.games.Games
import java.util.*

class AchievementUnlocker {

	companion object {
	    var lastRoll = 0L
	}

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
			curCombo >= 50 -> unlock(main, R.string.achievement_combo_maximums)
			curCombo >= 15 -> unlock(main, R.string.achievement_combo_major)
			curCombo >= 5 -> unlock(main, R.string.achievement_combo_lesser)
		}
	}

	fun clearedData(main: Main){
		unlock(main, R.string.achievement_burn_it_all_to_hell)
	}

	fun checkedOptions(main: Main){
		unlock(main, R.string.achievement_checking_your_options)
	}

	fun disalbedAnimation(main: Main){
		unlock(main, R.string.achievement_you_spat_on_me)
	}

	fun onSucessAchievement(main: Main){

        if(mData.rollData.last().rolls >= mData.maxRoll * 2){
            unlock(main, R.string.achievement_slow_arent_you)
        }

        if(mData.maxRoll > 10){
			// Check avearge combo
			if(mData.rollData.last().rolls == mData.maxRoll){
				unlock(main, R.string.achievement_you_are_average)
			}
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

			val nextRoll = rollStackCopy.first() as Long

			val rollAchievements = hashMapOf(
				80085L to R.string.achievement_roll_80085,
				1L to R.string.achievement_one_and_done,
				AchievementUnlocker.lastRoll + 1L to R.string.achievement_roll_consecutive_numbers,
				mData.maxRoll -1L to R.string.achievement_you_will_always_be_2nd
			)

			for(entry in rollAchievements){
				if(nextRoll == entry.key){
					unlock(main, entry.value)
				}
			}

			val calendar = Calendar.getInstance()
			val day = calendar.get(Calendar.DAY_OF_WEEK)

			if(day == Calendar.TUESDAY && nextRoll == 45L){
				unlock(main, R.string.achievement_on_a_tuesday_roll_45)
			}
		}

		AchievementUnlocker.lastRoll = rollStackCopy.first() as Long
	}

	private fun distanceAchievements(main: Main){
		var totalDistanceTravled = 0.0

		for(roll in mData.rollData){
			totalDistanceTravled += roll.distance
		}

		when {
			totalDistanceTravled > 327 * 1000 -> unlock(main, R.string.achievement_yuri_gagarin)
			totalDistanceTravled > 1 -> unlock(main, R.string.achievement_baby_steps)
		}

	}

	private fun checkShakeAchievements(main: Main){
		var totalShakes = 0.0

		for(roll in mData.rollData){
			totalShakes += roll.shakes
		}

		if(totalShakes > 7000){
			unlock(main, R.string.achievement_shakeius_maximums)
		}

		if(totalShakes > 6000){
			unlock(main, R.string.achievement_shakeius_consul)
		}

		if(totalShakes > 5000){
			unlock(main, R.string.achievement_shakeius_senator)
		}

		if(totalShakes > 4000){
			unlock(main, R.string.achievement_shakeius_quaestor)
		}

		if(totalShakes > 3000){
			unlock(main, R.string.achievement_shakeius_military_tribune)
		}

		if(totalShakes > 2000){
			unlock(main, R.string.achievement_why_walk_when_you_can_shake)
		}
	}

}