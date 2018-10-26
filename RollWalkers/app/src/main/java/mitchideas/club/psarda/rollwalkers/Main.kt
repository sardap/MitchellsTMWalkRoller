package mitchideas.club.psarda.rollwalkers

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import android.content.Intent
import android.support.v7.app.AlertDialog
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.games.Games
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider


class Main : AppCompatActivity() {

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val RC_ACHIEVEMENT_UI = 9003

        private const val TAG = "ROLL_WALKER"

    }

    private lateinit var auth: FirebaseAuth
    private var mUser: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
    }


    override fun onResume() {
        super.onResume()
        signInSilently()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result.isSuccess) {
                // The signed in account is stored in the result.
                val signedInAccount = result.signInAccount
            } else {
                var message = result.status.statusMessage
                if (message == null || message.isEmpty()) {
                    message = getString(R.string.signin_other_error)
                }
                AlertDialog.Builder(this).setMessage(message)
                    .setNeutralButton(android.R.string.ok, null).show()
            }
        }
    }

    private fun getGoogleSignInOptions() : GoogleSignInOptions{
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }

    private fun signInSilently() {
        val signInClient = GoogleSignIn.getClient(this, getGoogleSignInOptions())
        signInClient.silentSignIn().addOnCompleteListener(this
        ) { task ->
            if (task.isSuccessful) {
                // The signed in account is stored in the task's result.
                val signedInAccount = task.result
                testUnlock()
                firebaseAuthWithGoogle(signedInAccount!!)

            } else {
                val expect = task.exception as com.google.android.gms.common.api.ApiException

                Log.d(TAG, expect.statusCode.toString())
                startSignInIntent()
            }
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.id!!)

        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    mUser = auth.currentUser

                    /*
                    val database = FirebaseDatabase.getInstance()

                    mRef = database.getReference(mUser!!.uid)

                    readDataFromFirebase()
                    */
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                }
            }
    }


    private fun startSignInIntent() {
        val signInClient = GoogleSignIn.getClient(this, getGoogleSignInOptions())
        val intent = signInClient.signInIntent
        startActivityForResult(intent, RC_SIGN_IN)
    }


    private fun isSignedIn(): Boolean {
        return GoogleSignIn.getLastSignedInAccount(this) != null
    }

    private fun testUnlock(){
        Games.getAchievementsClient(this, GoogleSignIn.getLastSignedInAccount(this)!!)
            .unlock(getString(R.string.achievement_CgkIlvefg7odEAIQAQ))
    }

}
