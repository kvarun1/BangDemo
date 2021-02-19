package com.mbinfo.bangdemo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.*
import android.location.LocationListener
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Transformations.map
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.*

class MainActivity : FragmentActivity(),OnMapReadyCallback, LocationListener,
GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private var mMap: GoogleMap? = null
    internal lateinit var mLastLocation: Location
    internal lateinit var mLocationResult: LocationRequest
    internal lateinit var mLocationCallback: LocationCallback
    internal var mCurrLocationMarker: Marker? = null
    internal var mGoogleApiClient: GoogleApiClient? = null
    internal lateinit var mLocationRequest: LocationRequest
    internal var mFusedLocationClient: FusedLocationProviderClient? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.myMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
        checkLocation()
    }

    private fun checkLocation() {
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showAlertLocation()
        }
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getLocationUpdates()
    }

    private fun showAlertLocation() {
        val dialog = AlertDialog.Builder(this)
        dialog.setMessage("Your location settings is set to Off, Please enable location to use this application")
        dialog.setPositiveButton("Settings") { _, _ ->
            val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(myIntent)
        }
        dialog.setNegativeButton("Cancel") { _, _ ->
            finish()
        }
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun getLocationUpdates() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mLocationRequest = LocationRequest()
        mLocationRequest.interval = 50000
        mLocationRequest.fastestInterval = 50000
        mLocationRequest.smallestDisplacement = 170f //170 m = 0.1 mile
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY //according to your app
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                if (locationResult.locations.isNotEmpty()) {
                    /*val location = locationResult.lastLocation
                    Log.e("location", location.toString())*/
                    val addresses: List<Address>?
                    val geoCoder = Geocoder(applicationContext, Locale.getDefault())
                    addresses = geoCoder.getFromLocation(
                        locationResult.lastLocation.latitude,
                        locationResult.lastLocation.longitude,
                        1
                    )
                    if (addresses != null && addresses.isNotEmpty()) {
                        val address: String = addresses[0].getAddressLine(0)
                        val city: String = addresses[0].locality
                        val state: String = addresses[0].adminArea
                        val country: String = addresses[0].countryName
                        val postalCode: String = addresses[0].postalCode
                        val knownName: String = addresses[0].featureName
                        Log.v("location", "$address $city $state $postalCode $country $knownName")
                    }
                }

            }
        }
    }


    override fun onMapReady(googleMap: GoogleMap?) {
        mMap = googleMap

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                buildGoogleApiClient()
                mMap!!.isMyLocationEnabled = true
            }
        } else {
            buildGoogleApiClient()
            mMap!!.isMyLocationEnabled = true
        }
    }

    private fun buildGoogleApiClient() {
        mGoogleApiClient = GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API).build()
        mGoogleApiClient!!.connect()

    }

    override fun onConnected(bundle: Bundle?) {

        mLocationRequest = LocationRequest()
        mLocationRequest.interval = 1000
        mLocationRequest.fastestInterval = 1000
        mLocationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            mFusedLocationClient?.requestLocationUpdates(
                mLocationRequest,
                mLocationCallback,
                Looper.myLooper()
            )
        }


    }

    override fun onConnectionSuspended(p0: Int) {

    }

    override fun onLocationChanged(location: Location) {
        mLastLocation = location
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker!!.remove()
        }
        //Place current location marker
        val msg = "Updated Location: " +
                java.lang.Double.toString(location.latitude) + "," +
                java.lang.Double.toString(location.longitude)
        val latLng = LatLng(location.latitude, location.longitude)
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

        val markerOptions = MarkerOptions()
        markerOptions.position(latLng)
        markerOptions.title("Current Position")
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        mCurrLocationMarker = mMap!!.addMarker(markerOptions)

        //move map camera
        mMap!!.moveCamera(CameraUpdateFactory.newLatLng(latLng))
        mMap!!.animateCamera(CameraUpdateFactory.zoomTo(11f))

        //stop location updates
        if (mGoogleApiClient != null) {
            mFusedLocationClient?.removeLocationUpdates(mLocationCallback)
        }
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        
    }
}


