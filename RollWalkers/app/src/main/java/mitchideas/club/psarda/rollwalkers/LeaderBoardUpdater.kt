package mitchideas.club.psarda.rollwalkers

import android.provider.Settings.Global.getString
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.games.Games

class LeaderBoardUpdater {

    private val mData = Data.instance()

    fun updateTargetLeaderboard(main: Main){
        Games.getLeaderboardsClient(main, GoogleSignIn.getLastSignedInAccount(main)!!)
            .submitScore(main.getString(R.string.leaderboard_highest_target), mData.maxRoll)
    }
}