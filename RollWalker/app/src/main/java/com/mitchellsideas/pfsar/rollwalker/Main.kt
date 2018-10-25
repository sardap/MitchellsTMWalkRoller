package com.mitchellsideas.pfsar.rollwalker

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.Toast

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import java.util.*
import android.widget.ProgressBar
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.games.Games
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.*
import safety.com.br.android_shake_detector.core.ShakeDetector
import safety.com.br.android_shake_detector.core.ShakeOptions
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class Main : AppCompatActivity(), OnMapReadyCallback {

	private class ViewHolder(activity: Activity) {

        val porgressBar: ProgressBar = activity.findViewById(R.id.progress_to_next_roll)
		val mainLayout: LinearLayout = activity.findViewById(R.id.main_layout)
    }


    private lateinit var mMap: GoogleMap
    private lateinit var mFragmentManager: FragmentManager
    private lateinit var mActiveFragment: Fragment
    private var mLastLocation: Location? = null
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private var mUser: FirebaseUser? = null
    private var mRef: DatabaseReference? = null
    private var mLoaded = hashMapOf(ROLL_CHILD to false, LEVEL_CHILD to false, COMBO_CHILD to false, PROGRESS_CHILD to false)
    private lateinit var mViewHolder: ViewHolder
    private lateinit var mFirebaseAnalytics: FirebaseAnalytics
    private var mActivityVisible: Boolean = true
	private lateinit var mShakeDetector: ShakeDetector
	private var mData = Data.instance()

	override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        replaceFragment(MainFragment())

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        getLocation()
        mFragmentManager = supportFragmentManager
        mViewHolder = ViewHolder(this)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
			//.requestScopes(Games.SCOPE_GAMES_LITE)
			.requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        auth = FirebaseAuth.getInstance()
        signIn()

		setupShakeDectector()

		initlise()
    }

	override fun onPause() {
        super.onPause()
        mActivityVisible = false
    }

    override fun onResume() {
        super.onResume()
        mActivityVisible = true
    }


    override fun onStop() {
        super.onStop()
        mRef!!.child(SETTINGS_CHILD).setValue(mData.settings)
        addRollToDatabase()
    }

	override fun onDestroy() {
		super.onDestroy()
		mShakeDetector.destroy(baseContext)
	}

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e)
                // ...
            }
        }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED )
        {
            mMap.isMyLocationEnabled = true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_ACCESS_FINE_LOCATION) {
            when (grantResults[0]) {
                PackageManager.PERMISSION_GRANTED -> getLocation()
                //PackageManager.PERMISSION_DENIED ->
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
		if(!mLoaded.all { it .value })
		{
			Toast.makeText(applicationContext, "Not ready yet!", Toast.LENGTH_SHORT).show()
			return false
		}

        return when (item.itemId) {
            R.id.menu_switch_to_main -> {
                if(mActiveFragment !is MainFragment)
                {
                    replaceFragment(MainFragment())
                }
                true
            }
            R.id.menu_see_last_rolls -> {
                if(mActiveFragment !is LastRollFragment)
                {
					replaceFragment(LastRollFragment.newInstance(mData.rollData, this))
                }
                true
            }
			R.id.menu_options -> {
				if(mActiveFragment !is optionsFragment)
				{
					replaceFragment(optionsFragment.newInstance(this, this))
				}

				true
			}
            else -> super.onOptionsItemSelected(item)
        }
    }

	fun showNotification(title: String, content: String)
    {

		val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			val channel = NotificationChannel("default", CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
			channel.description = "YOUR_NOTIFICATION_CHANNEL_DISCRIPTION"

			mNotificationManager.createNotificationChannel(channel);
		}

		val mBuilder = NotificationCompat.Builder(applicationContext, "default")
			.setSmallIcon(R.mipmap.ic_launcher) // notification icon
			.setContentTitle(title) // title for notification
			.setContentText(content)// message for notification
			//.setSound(alarmSound) // set alarm sound for notification
			.setAutoCancel(true) // clear notification after click

		val intent = Intent(applicationContext, Main::class.java)
		val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
		mBuilder.setContentIntent(pi)
		mNotificationManager.notify(0, mBuilder.build())
    }

	fun clearDatabase() {
		mLoaded.forEach { t, u -> mLoaded[t] = false }

		mRef!!.child(COMBO_CHILD).setValue(null)
		mRef!!.child(LEVEL_CHILD).setValue(null)
		mRef!!.child(PROGRESS_CHILD).setValue(null)
		mRef!!.child(ROLL_CHILD).setValue(null)
		mRef!!.child(ROLL_CHILD).setValue(null)
		readDataFromFirebase()
		initlise()

		Toast.makeText(applicationContext, getString(R.string.completed_clear), Toast.LENGTH_SHORT).show()
	}

    private fun initlise() {

    }

	private fun isSignedIn(): Boolean {
		return GoogleSignIn.getLastSignedInAccount(this) != null
	}


	private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
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

                    val database = FirebaseDatabase.getInstance()

                    mRef = database.getReference(mUser!!.uid)

                    readDataFromFirebase()



                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                }
            }
    }

    private fun replaceFragment(newFragment: Fragment) {
        val fragmentManager = supportFragmentManager

        val transaction = fragmentManager.beginTransaction()
        transaction.setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit)

        mActiveFragment = newFragment
        transaction.replace(R.id.main_frame, newFragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun getLocation() {

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location?) {
				if (mLastLocation == null) {
					mLastLocation = location
				}

				val latitude = location!!.latitude
				val longitude = location.longitude

				Log.i(Main.TAG, "Latitute: $latitude ; Longitute: $longitude")

				val distance = mLastLocation!!.distanceTo(location)

				if (mLoaded.all { it.value }) {
					mData.rollData.last().distance += distance
				}

				moved(distance.toDouble())

                mLastLocation = location
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            }

            override fun onProviderEnabled(provider: String?) {
            }

            override fun onProviderDisabled(provider: String?) {
            }

        }

        try {
            locationManager!!.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener)
        } catch (ex: SecurityException) {
            Toast.makeText(applicationContext, "Isuffent permsions!", Toast.LENGTH_SHORT).show()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST_ACCESS_FINE_LOCATION
            )
            return
        }

        locationManager!!.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener)
    }

    private fun updateProgressBar()
    {
        val percentDone = Math.round(mData.distanceTraveledSinceRoll / DISTANCE_BETWEEN_ROLLS * 100).toInt()
        mViewHolder.porgressBar.progress = percentDone
    }

    private fun SetTarget(value: Long)
    {
		mData.maxRoll = value

        if(mActiveFragment is MainFragment)
        {
            (mActiveFragment as MainFragment).updateTargetText(mData.maxRoll)
        }
    }

    private fun updateCombo(nextRoll: Long)
    {

        if (nextRoll > mData.lastRoll) {
			mData.comboNum++

            if(mData.comboNum > mData.maxCombo)
            {
                mRef!!.child(COMBO_CHILD).setValue(mData.comboNum)

				mData.maxCombo = mData.comboNum
            }

        } else {
			mData.comboNum = 1
        }

        if(mData.comboNum > mData.rollData.last().bestCombo)
        {
			mData.rollData.last().bestCombo = mData.comboNum
        }

        if(mActiveFragment is MainFragment)
        {
            (mActiveFragment as MainFragment).updateComboText(mData.comboNum)
        }
    }

    private fun addRollToDatabase()
    {
        mRef!!.child(ROLL_CHILD).setValue(mData.rollData)
		mRef!!.child(CURRENT_COMBO_CHILD).setValue(mData.comboNum)
    }

    private fun sucessfullRoll() {
		if(mActiveFragment !is MainFragment)
        	Toast.makeText(this, getString(R.string.rolled_max_contents, mData.lastRoll), Toast.LENGTH_LONG).show()

		if(!mActivityVisible)
			showNotification(getString(R.string.notifaction_sucess_title), getString(R.string.notifaction_sucess_content, mData.maxRoll))

		mData.comboNum = 0

        SetTarget(mData.maxRoll * 10)

        mRef!!.child(LEVEL_CHILD).setValue(mData.maxRoll)

		mData.rollData.add(RollData(mData.maxRoll, 0.0, 0,0, 0))

    }

    private fun vibrate()
    {
        val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        // Vibrate for 500 milliseconds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26
            v.vibrate(500);
        }
    }

	private fun moved(distance: Double){
		mData.distanceTraveledSinceRoll += distance

		if(mLoaded.all { it.value })
		{
			tryRoll()
			mRef!!.child(PROGRESS_CHILD).setValue(mData.distanceTraveledSinceRoll)
		}
	}

	private fun tryRoll() {
        Log.i(TAG, "Travaled $mData.distanceTraveledSinceRoll")

        if (mData.distanceTraveledSinceRoll > DISTANCE_BETWEEN_ROLLS) {
			Log.i(TAG, "ROLLING")
			roll()
			AchievementUnlocker().Check(this)
        }

        updateProgressBar()

    }

    private fun roll()
    {
		mData.rollData.last().rolls++

        if(mData.settings.vibrateOnRoll)
            vibrate()

		mData.distanceTraveledSinceRoll = 0.0

        val nextRoll = genrateRollStack()

        updateCombo(nextRoll)

		mData.lastRoll = nextRoll

        addRollToDatabase()

		if(mActiveFragment is MainFragment){
			(mActiveFragment as MainFragment).updateRollResult()
		}
		else if(mData.settings.notifcationEveryRoll){
			val title = getString(R.string.notifaction_roll_title)

			val notContent = if(mData.lastRoll == mData.maxRoll){
				getString(R.string.notifaction_roll_sucess, nextRoll)
			}else{
				getString(R.string.notifaction_roll_content, nextRoll.toString(), mData.comboNum)
			}

			showNotification(title, notContent)
		}

		if(mData.lastRoll == mData.maxRoll)
        {
            sucessfullRoll()
        }
    }

    private fun genrateRollStack() : Long
    {
		mData.animeRollStack.clear()

		val tempList = ArrayList<Long>()

		mData.animeRollStack.clear()

        for(i in 0 until ANIMATION_COUNT)
        {
            val randomNumber = JavaUtils.nextLong (MIN_ROLL, mData.maxRoll + 1, RANDOM)
            tempList.add(randomNumber)
			mData.animeRollStack.push(tempList.last())
        }

        return tempList[0]
    }

	private fun readDataFromFirebase() {
		mRef!!.addListenerForSingleValueEvent(
			object : ValueEventListener {
				override fun onCancelled(p0: DatabaseError) {
				}

				override fun onDataChange(snapshot: DataSnapshot) {
					mLoaded[ROLL_CHILD] = true

					mData.rollData = if (!snapshot.hasChild(ROLL_CHILD)) {
						val vaule = ArrayList<RollData>()
						vaule.add(RollData(START_MAX_ROLL, 0.0, 0,0, 0))
						mRef!!.child(ROLL_CHILD).setValue(vaule)
						vaule
					}
					else
					{
						val value = snapshot.child(ROLL_CHILD).value as ArrayList<HashMap<String, Any>>

						val result = ArrayList<RollData>()

						for(entry in value)
						{
							result.add(
								RollData(
									entry["target"].toString().toLong(),
									entry["distance"].toString().toDouble(),
									entry["rolls"].toString().toLong(),
									entry["bestCombo"].toString().toLong(),
									entry["shakes"].toString().toLong()
								)
							)
						}

						result
					}
				}
			})

		mRef!!.addListenerForSingleValueEvent(
			object : ValueEventListener {
				override fun onCancelled(p0: DatabaseError) {
				}

				override fun onDataChange(snapshot: DataSnapshot) {
					mLoaded[LEVEL_CHILD] = true

					val target = if (!snapshot.hasChild(LEVEL_CHILD)) {
						mRef!!.child(LEVEL_CHILD).setValue(START_MAX_ROLL)
						START_MAX_ROLL
					} else {
						val result = snapshot.child(LEVEL_CHILD).value
						result.toString().toLong()
					}

					SetTarget(target)
				}
			})

		mRef!!.addListenerForSingleValueEvent(
			object : ValueEventListener {
				override fun onCancelled(p0: DatabaseError) {
				}

				override fun onDataChange(snapshot: DataSnapshot) {
					mLoaded[COMBO_CHILD] = true

					mData.maxCombo = if (!snapshot.hasChild(COMBO_CHILD)) {
						mRef!!.child(COMBO_CHILD).setValue(0)
						0
					} else {
						val result = snapshot.child(COMBO_CHILD).value
						result.toString().toLong()
					}
				}
			})

		mRef!!.addListenerForSingleValueEvent(
			object : ValueEventListener {
				override fun onCancelled(p0: DatabaseError) {
				}

				override fun onDataChange(snapshot: DataSnapshot) {
					mLoaded[PROGRESS_CHILD] = true

					mData.distanceTraveledSinceRoll = if (!snapshot.hasChild(PROGRESS_CHILD)) {
						mRef!!.child(PROGRESS_CHILD).setValue(0.0)
						0.0
					} else {
						val result = snapshot.child(PROGRESS_CHILD).value
						result.toString().toDouble()
					}
				}
			})


		mRef!!.addListenerForSingleValueEvent(
			object : ValueEventListener {
				override fun onCancelled(p0: DatabaseError) {
				}

				override fun onDataChange(snapshot: DataSnapshot) {
					mData.settings = if (!snapshot.hasChild(SETTINGS_CHILD)) {
						val result = Settings()
						mRef!!.child(SETTINGS_CHILD).setValue(result)
						result
					} else {
						FirebaseUtilsJava.deserialize(snapshot.child(SETTINGS_CHILD), Settings::class.java)
					}
				}
			})
	}

	private fun setupShakeDectector() {
		val options = ShakeOptions()
			.background(true)
			.interval(1000)
			.shakeCount(1)
			.sensibility(3f)

		mShakeDetector = ShakeDetector(options).start(this) {
			Log.d(TAG, "SHAKEN")

			mData.rollData.last().shakes++

			mViewHolder.mainLayout.clearAnimation()
			val animShake = AnimationUtils.loadAnimation(this, R.anim.shake)

			animShake.setAnimationListener(object : Animation.AnimationListener {
				override fun onAnimationStart(animation: Animation?) {
				}

				override fun onAnimationEnd(animation: Animation?) {
				}

				override fun onAnimationRepeat(animation: Animation?) {
				}
			})
			mViewHolder.mainLayout.startAnimation(animShake)

			moved(SHAKE_DISTANCE)
		}

		mShakeDetector = ShakeDetector(options).start(this)
	}

    companion object {
        val RANDOM = Random()

        const val TAG = "ROLL_WALKER"
		const val ROLL_CHILD = "rolls"
		const val CURRENT_COMBO_CHILD = "curCombo"
        const val LEVEL_CHILD = "level"
        const val COMBO_CHILD = "combo"
		const val PROGRESS_CHILD = "progress"
		const val SETTINGS_CHILD = "settings"
        const val DISTANCE_BETWEEN_ROLLS = 1
        const val ANIMATION_COUNT = 50
        const val CHANNEL_NAME = "ROLL_WALKER"
		const val SHAKE_DISTANCE = 0.256

        private const val MIN_ROLL = 1L
        private const val START_MAX_ROLL = 10L
        private const val RC_SIGN_IN = 9001
        private const val PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 100
    }


}
