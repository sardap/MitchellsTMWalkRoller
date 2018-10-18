package com.example.pfsar.rollwalker

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.*
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
import com.google.firebase.database.FirebaseDatabase
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlin.math.absoluteValue


class Main : AppCompatActivity(), OnMapReadyCallback {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        getLocation()
        mFragmentManager = supportFragmentManager

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        auth = FirebaseAuth.getInstance()
        signIn()
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

                    val myRef = database.getReference(mUser!!.uid)

                    myRef.addListenerForSingleValueEvent(
                        object : ValueEventListener {
                            override fun onCancelled(p0: DatabaseError) {
                            }
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (!snapshot.hasChild("rolls")) {
                                    myRef.setValue("rolls")
                                }
                            }
                    })

                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                }
            }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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

        // Add a marker in Sydney and move the camera
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

    private fun replaceFragment(fragment: Fragment) {
        mFragmentStack = Stack()
        val transaction = mFragmentManager.beginTransaction()
        transaction.replace(R.id.main_frame, fragment)
        mFragmentStack.push(fragment)
        transaction.commitAllowingStateLoss()
    }

    private fun getLocation() {

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?

        val locationListener = object : LocationListener
        {
            override fun onLocationChanged(location: Location?) {

                if(mLastLocation == null)
                {
                    mLastLocation = location
                }

                val latitude = location!!.latitude
                val longitude = location!!.longitude

                Log.i(Main.TAG, "Latitute: $latitude ; Longitute: $longitude")

                mDistanceTraveledSinceRoll += mLastLocation!!.distanceTo(location)

                tryRoll()

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
        } catch (ex:SecurityException) {
            Toast.makeText(applicationContext, "Isuffent permsions!", Toast.LENGTH_SHORT).show()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST_ACCESS_FINE_LOCATION)
            return
        }
        locationManager!!.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener)
    }

    private fun IntRange.random() = Random().nextInt((endInclusive + 1) - start) +  start

    private fun tryRoll()
    {
        Log.i(TAG, "Travaled $mDistanceTraveledSinceRoll")

        val progressBar = findViewById<ProgressBar>(R.id.progress_to_next_roll)
        val percentDone = Math.round(mDistanceTraveledSinceRoll / DISTANCE_BETWEEN_ROLLS * 100).toInt()
        progressBar.progress = percentDone

        while(mDistanceTraveledSinceRoll > DISTANCE_BETWEEN_ROLLS)
        {
            mDistanceTraveledSinceRoll -= DISTANCE_BETWEEN_ROLLS

            mLastRoll = (MIN_ROLL .. MAX_ROLL).random()

            Log.i(TAG, "Rolled $mLastRoll")

            mRollsToPush.push(mLastRoll)

            if(mUser != null)
            {
                val database = FirebaseDatabase.getInstance()

                val myRef = database.getReference(mUser!!.uid)

                while (!mRollsToPush.empty())
                {
                    myRef.child("rolls").push().setValue(mRollsToPush.pop())
                }

                val text = findViewById<TextView>(R.id.rollResult)
                text.text = mLastRoll.toString()
            }

            if(mLastRoll == MAX_ROLL)
            {
                Toast.makeText(this, getString(R.string.rolled_max_contents, mLastRoll),  Toast.LENGTH_LONG).show()
            }

            progressBar.progress = 0
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 100
        private const val TAG = "ROLL_WALKER"
        private const val DISTANCE_BETWEEN_ROLLS = 1000
        private const val MIN_ROLL = 1
        private const val MAX_ROLL = 10000
        private const val RC_SIGN_IN = 9001
    }
}
