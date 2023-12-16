    package com.example.getdirectionapi


    import kotlin.math.*
    import android.Manifest
    import android.content.Intent
    import android.content.IntentSender
    import android.content.pm.PackageManager
    import android.graphics.Color
    import android.location.Location
    import androidx.appcompat.app.AppCompatActivity
    import android.os.Bundle
    import android.util.Log
    import android.widget.Button
    import androidx.core.app.ActivityCompat
    import androidx.core.content.ContextCompat

    import com.google.android.gms.maps.CameraUpdateFactory
    import com.google.android.gms.maps.GoogleMap
    import com.google.android.gms.maps.OnMapReadyCallback
    import com.google.android.gms.maps.SupportMapFragment
    import com.google.android.gms.maps.model.LatLng
    import com.google.android.gms.maps.model.MarkerOptions
    import com.example.getdirectionapi.databinding.ActivityMapTesterBinding
    import com.example.getdirectionapi.mapTester.Companion.REQUEST_LOCATION_PERMISSION
    import com.google.android.gms.common.api.ResolvableApiException
    import com.google.android.gms.location.FusedLocationProviderClient
    import com.google.android.gms.location.LocationCallback
    import com.google.android.gms.location.LocationRequest
    import com.google.android.gms.location.LocationResult
    import com.google.android.gms.location.LocationServices
    import com.google.android.gms.location.LocationSettingsRequest
    import com.google.android.gms.location.LocationSettingsResponse
    import com.google.android.gms.location.SettingsClient
    import com.google.android.gms.maps.model.Polyline
    import com.google.android.gms.maps.model.PolylineOptions
    import com.google.android.gms.tasks.Task
    import com.google.firebase.database.FirebaseDatabase
    import java.util.Timer
    import java.util.TimerTask

    class mapTester : AppCompatActivity(), OnMapReadyCallback {

        private lateinit var mMap: GoogleMap
        private lateinit var polyline: Polyline
        private lateinit var binding: ActivityMapTesterBinding
        private lateinit var fusedLocationClient: FusedLocationProviderClient
        private lateinit var locationCallback: LocationCallback
        private lateinit var locationRequest: LocationRequest

        private var isTracking = false

        private var lastLogTimeMillis: Long = 0
        private var totalDistance: Double = 0.0


        private val database = FirebaseDatabase.getInstance()
        private val locationsReference = database.getReference("locations")
        companion object {
            const val REQUEST_LOCATION_PERMISSION = 1
            const val REQUEST_CHECK_SETTINGS = 2
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            binding = ActivityMapTesterBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map1) as SupportMapFragment
            mapFragment.getMapAsync(this)


            val startButton: Button = findViewById(R.id.startTrack)
            startButton.setOnClickListener { checkLocationPermission() }


            val stopButton: Button = findViewById(R.id.endTrack)
            stopButton.setOnClickListener { stopTracking() }

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

            // Initialize location request
            locationRequest = LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                interval = 1000 // Update location every 1 second
            }
        }

        private fun checkLocationPermission() {
            // Check if the app has location permission
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request location permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_LOCATION_PERMISSION
                )
            } else {
                // Permission already granted, check location settings
                checkLocationSettings()
            }
        }

        private fun checkLocationSettings() {
            // Check if location settings are enabled
            val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)

            val client: SettingsClient = LocationServices.getSettingsClient(this)
            val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

            task.addOnSuccessListener {
                // All location settings are satisfied, proceed with location updates
                startTracking()
            }

            task.addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    // Location settings are not satisfied, show a dialog to the user
                    try {
                        exception.startResolutionForResult(
                            this,
                            REQUEST_CHECK_SETTINGS
                        )
                    } catch (sendEx: IntentSender.SendIntentException) {
                        // Ignore the error.
                    }
                }
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            when (requestCode) {
                REQUEST_CHECK_SETTINGS -> {
                    if (resultCode == RESULT_OK) {
                        // User enabled location settings, start tracking
                        startTracking()
                    } else {
                    }
                }
            }
        }

        private fun startTracking() {
            if (!isTracking) {
                isTracking = true

                // Set up location updates
                locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult?) {
                        locationResult?.let {
                            for (location in it.locations) {
                                updatePolyline(location)
                            }
                        }
                    }


                }


                // Start location updates using FusedLocationProviderClient
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            }
        }

        private fun stopTracking() {
            if (isTracking) {
                isTracking = false

                // Stop location updates
                fusedLocationClient.removeLocationUpdates(locationCallback)

    //            // Clear the Polyline on the map
    //            polyline.remove()
            }
        }



        private fun updatePolyline(location: Location) {
            val currentTimeMillis = System.currentTimeMillis()

            // Log only if 10 seconds have passed since the last log
            if (currentTimeMillis - lastLogTimeMillis >= 10000) {
                lastLogTimeMillis = currentTimeMillis

                val latLng = LatLng(location.latitude, location.longitude)
                Log.d("---------", "latlon ----: ${latLng}")

                Log.d("Polyline", "Number of points: ${polyline.points.size}")

                for ((index, point) in polyline.points.withIndex()) {
                    Log.d("Polyline", "Point $index: lat/lng: (${point.latitude},${point.longitude})")
                }

                if (polyline.points.size > 1) {
                    val lastLatLng = polyline.points[polyline.points.size - 2]
                    val distance = calculateDistance(lastLatLng, latLng)
                    totalDistance += distance
                    Log.d("Distance", "Total Distance: $totalDistance km")

                    // Save the data to Firebase
                    saveLocationToFirebase(location.latitude, location.longitude, totalDistance, currentTimeMillis)
                }
            }

            // Move the camera to the new location with zoom
            mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(location.latitude, location.longitude),
                    22f
                )
            )
        }
        private fun saveLocationToFirebase(latitude: Double, longitude: Double, distance: Double, timestamp: Long) {
            val locationData = LocationData(latitude, longitude, distance, timestamp)
            locationsReference.child(timestamp.toString()).setValue(locationData)
        }
        override fun onMapReady(googleMap: GoogleMap) {
            mMap = googleMap

            // Create a PolylineOptions object and set its properties
            val polylineOptions = PolylineOptions()
                .width(10f)
                .color(Color.RED)

            // Create the Polyline and add it to the map
            polyline = mMap.addPolyline(polylineOptions)
        }


        override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
        ) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            when (requestCode) {
                REQUEST_LOCATION_PERMISSION -> {
                    if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        // Location permission granted, check location settings
                        checkLocationSettings()
                    } else {
                        // Location permission denied, handle accordingly
                    }
                }
            }
        }
    }

    fun calculateDistance(startLatLng: LatLng, endLatLng: LatLng): Double {
        val radius = 6371 // Earth radius in kilometers

        val startLat = Math.toRadians(startLatLng.latitude)
        val startLon = Math.toRadians(startLatLng.longitude)
        val endLat = Math.toRadians(endLatLng.latitude)
        val endLon = Math.toRadians(endLatLng.longitude)

        val dLat = endLat - startLat
        val dLon = endLon - startLon

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(startLat) * cos(endLat) * sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return radius * c
    }

    data class LocationData(
        val latitude: Double,
        val longitude: Double,
        val distance: Double,
        val timestamp: Long
    )