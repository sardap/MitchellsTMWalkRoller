package com.example.pfsar.rollwalker

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent.getActivity
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
import android.widget.Chronometer
import android.widget.Toast

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.security.AccessController.getContext
import java.util.*
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.*
import org.w3c.dom.Text
import kotlin.collections.ArrayList
import kotlin.math.absoluteValue


class Main : AppCompatActivity(), OnMapReadyCallback {

    private class ViewHolder(activity: Activity) {

        val rollResult: TextView = activity.findViewById(R.id.rollResult)
        val rollCombo: TextView = activity.findViewById(R.id.roll_combo)
        val animeRollResult: TextView = activity.findViewById(R.id.roll_animation_result)
        val porgressBar: ProgressBar = activity.findViewById(R.id.progress_to_next_roll)
        val rollTarget: TextView = activity.findViewById(R.id.roll_target)
    }

    private lateinit var mMap: GoogleMap
    private lateinit var mFragmentStack: Stack<Fragment>
    private lateinit var mFragmentManager: FragmentManager
    private var mLastLocation: Location? = null
    private var mDistanceTraveledSinceRoll = 0e0
    private var mLastRoll = 0
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private var mUser: FirebaseUser? = null
    private val mRollsToPush: Stack<Int> = Stack()
    private var mMaxRoll: Int = 0
    private var mComboNum: Int = 0
    private var mMaxCombo: Int = 0
    private var mRef: DatabaseReference? = null
    private val mLoaded = hashMapOf<String, Boolean>(ROLL_CHILD to false, LEVEL_CHILD to false, COMBO_CHILD to false, PROGRESS_CHILD to false)
    private var mAnimeRollStack = Stack<Int>()
    private lateinit var mViewHolder: ViewHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        getLocation()
        mFragmentManager = supportFragmentManager
        mViewHolder = ViewHolder(this)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        auth = FirebaseAuth.getInstance()
        signIn()
        initlise()
    }

    override fun onDestroy() {
        super.onDestroy()
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


    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setMyLocationEnabled(true);

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
        return when (item.itemId) {
            R.id.menu_switch_to_main -> {
                replaceFragment(mFragmentManager.findFragmentById(R.id.map) as Fragment)
                true
            }
            R.id.menu_see_last_rolls -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()

        mInit += System.currentTimeMillis()
    }

    var mInit : Long = 0


    private fun initlise() {
        mViewHolder.rollCombo.text = getString(R.string.combo_title, 0)
        mViewHolder.rollResult.text = getString(R.string.roll_result_defualt, 0)

        val handler = Handler()

        val updater = object : Runnable {

            private var mTime = 0L
            private var mNextAnimeUpdate = 0L
            private var mAnimeNextIncremnet = 0.1

            override fun run()
            {
                val now = System.currentTimeMillis();

                if (!mAnimeRollStack.empty()) {
                    if (now > mNextAnimeUpdate) {

                        val next = mAnimeRollStack.pop().toString()
                        mViewHolder.animeRollResult.text = next
                        mViewHolder.rollResult.text = next

                        mNextAnimeUpdate = now + mAnimeNextIncremnet.toLong()


                        if(mAnimeRollStack.size > 15)
                        {
                            mAnimeNextIncremnet += 20
                        }
                        else if(mAnimeNextIncremnet > 5)
                        {
                            mAnimeNextIncremnet += 40
                        }
                        else
                        {
                            mAnimeNextIncremnet += 100
                        }

                        Log.w(TAG, "Now:$now NextTime:$mAnimeNextIncremnet StackSize:${mAnimeRollStack.size} NextVaule:$next")
                    }
                }
                else
                {
                    mAnimeNextIncremnet = 0.0
                }

                handler.postDelayed(this, 30)
            }
        }

        handler.post(updater)
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

                    mRef!!.addListenerForSingleValueEvent(
                        object : ValueEventListener {
                            override fun onCancelled(p0: DatabaseError) {
                            }

                            override fun onDataChange(snapshot: DataSnapshot) {
                                mLoaded[ROLL_CHILD] = true

                                if (!snapshot.hasChild(ROLL_CHILD)) {
                                    val vaule = ArrayList<Int>()
                                    mRef!!.child(ROLL_CHILD).setValue(vaule)
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
                                    result.toString().toInt()
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

                                mMaxCombo = if (!snapshot.hasChild(COMBO_CHILD)) {
                                    mRef!!.child(COMBO_CHILD).setValue(0)
                                    0
                                } else {
                                    val result = snapshot.child(COMBO_CHILD).value
                                    result.toString().toInt()
                                }
                            }
                        })

                    mRef!!.addListenerForSingleValueEvent(
                        object : ValueEventListener {
                            override fun onCancelled(p0: DatabaseError) {
                            }

                            override fun onDataChange(snapshot: DataSnapshot) {
                                mLoaded[PROGRESS_CHILD] = true

                                mDistanceTraveledSinceRoll = if (!snapshot.hasChild(PROGRESS_CHILD)) {
                                    mRef!!.child(PROGRESS_CHILD).setValue(0)
                                    0.0
                                } else {
                                    val result = snapshot.child(PROGRESS_CHILD).value
                                    result.toString().toDouble()
                                }
                            }
                        })


                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                }
            }
    }

    private fun replaceFragment(fragment: Fragment) {
        mFragmentStack = Stack()
        val transaction = mFragmentManager.beginTransaction()
        transaction.replace(R.id.main_frame, fragment)
        mFragmentStack.push(fragment)
        transaction.commitAllowingStateLoss()
    }

    private fun getLocation() {

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location?) {

                if (mLastLocation == null) {
                    mLastLocation = location
                }

                val latitude = location!!.latitude
                val longitude = location!!.longitude

                Log.i(Main.TAG, "Latitute: $latitude ; Longitute: $longitude")

                mDistanceTraveledSinceRoll += mLastLocation!!.distanceTo(location)

                if(mLoaded.all { it.value })
                {
                    tryRoll()
                    mRef!!.child(PROGRESS_CHILD).setValue(mDistanceTraveledSinceRoll)
                }

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

    private fun updateProgressBar() {
        val percentDone = Math.round(mDistanceTraveledSinceRoll / DISTANCE_BETWEEN_ROLLS * 100).toInt()
        mViewHolder.porgressBar.progress = percentDone
    }

    private fun SetTarget(value: Int) {
        mMaxRoll = value
        mViewHolder.rollTarget.text = getString(R.string.target_title, mMaxRoll)
    }

    private fun updateCombo(nextRoll: Int) {

        if (nextRoll > mLastRoll) {
            mComboNum++

            if(mComboNum > mMaxCombo)
            {
                mRef!!.child(COMBO_CHILD).setValue(mComboNum)

                Toast.makeText(this, getString(R.string.combo_broken_message, mMaxCombo, mComboNum), Toast.LENGTH_LONG).show()

                mMaxCombo = mComboNum
            }

        } else {
            mComboNum = 1
        }

        mViewHolder.rollCombo.text = getString(R.string.combo_title, mComboNum)
    }

    private fun addRollToDatabase() {
        while (!mRollsToPush.empty()) {
            mRef!!.child(ROLL_CHILD).push().setValue(mRollsToPush.pop())
        }
    }

    private fun sucessfullRoll() {
        Toast.makeText(this, getString(R.string.rolled_max_contents, mLastRoll), Toast.LENGTH_LONG).show()

        SetTarget(mMaxRoll * 10)

        mRef!!.child(LEVEL_CHILD).setValue(mMaxRoll)
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

    private fun tryRoll() {
        Log.i(TAG, "Travaled $mDistanceTraveledSinceRoll")

        if (mDistanceTraveledSinceRoll > DISTANCE_BETWEEN_ROLLS) {
            roll()
        }

        updateProgressBar()

    }

    private fun IntRange.random() = Random().nextInt((endInclusive + 1) - start) + start

    private fun roll()
    {
        vibrate()

        mDistanceTraveledSinceRoll = 0.0

        val nextRoll = startDiceRollAnimation()

        updateCombo(nextRoll)

        mLastRoll = nextRoll

        Log.i(TAG, "Rolled $mLastRoll")

        mRollsToPush.push(mLastRoll)

        addRollToDatabase()

        if(mLastRoll == mMaxRoll)
        {
            sucessfullRoll()
        }
    }

    private fun startDiceRollAnimation() : Int
    {
        val tempList = ArrayList<Int>()

        for(i in 0 until ANIMATION_COUNT - 1)
        {
            tempList.add(((MIN_ROLL .. mMaxRoll).random()))
            mAnimeRollStack.push(tempList.last())
        }

        return tempList[0]
    }

    companion object {
        private const val PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 100
        private const val TAG = "ROLL_WALKER"
        private const val DISTANCE_BETWEEN_ROLLS = 2
        private const val MIN_ROLL = 1
        private const val START_MAX_ROLL = 10
        private const val ANIMATION_COUNT = 25
        private const val RC_SIGN_IN = 9001
        private const val ROLL_CHILD = "rolls"
        private const val LEVEL_CHILD = "level"
        private const val COMBO_CHILD = "combo"
        private const val PROGRESS_CHILD = "progress"
    }
}
