package com.example.getdirectionapi

import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.getdirectionapi.databinding.ActivityMapsBinding
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    private lateinit var origin : String
    private lateinit var destination : String
    private lateinit var geocoder: Geocoder


    private lateinit var polyline: Polyline
    private lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        geocoder = Geocoder(this)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)




        val origin = intent.getStringExtra("origin")
        val destination = intent.getStringExtra("destination")

        // Now you have the values of origin and destination, use them as needed
        Log.d("MapsActivity", "Origin: $origin, Destination: $destination")

        requestDirections(origin, destination)

    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

    }



         //Start location updates using FusedLocationProviderClient
//         val locationProviderClient = LocationServices.getFusedLocationProviderClient(this)
//         locationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())





    private fun requestDirections(origin: String?, destination: String?) {
        val client = OkHttpClient()

        val startPointLatLng = getLatLngFromAddress(origin!!)
        val endPointLatLng = getLatLngFromAddress(destination!!)

        Log.d("--------", "requestDirections: ${startPointLatLng}  ${endPointLatLng}")

        if (startPointLatLng != null && endPointLatLng != null) {
            val (startPointLat, startPointLng) = startPointLatLng
            val (endPointLat, endPointLng) = endPointLatLng

            val url =
                "https://trueway-directions2.p.rapidapi.com/FindDrivingRoute?stops=$startPointLat,$startPointLng%3B$endPointLat,$endPointLng"
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("X-RapidAPI-Key", "b75b1af2dbmshcf32852a614c58cp1280cajsne0e57708a417")
                .addHeader("X-RapidAPI-Host", "trueway-directions2.p.rapidapi.com")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Toast.makeText(this@MapsActivity, "Getting error", Toast.LENGTH_SHORT).show()
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val jsonData = response.body?.string()
                        Log.d("-----------", "onResponse: ${jsonData}")

                        val directionsData = parseDirections(jsonData)
                        val points = directionsData.first
                        val distance = directionsData.second
                        val duration = directionsData.third

                        val distanceInMeters =distance .toDouble()

                        Log.d("-----------", "onResponse: ${points}, Distance: $distance")

                        val distanceInKm = distanceInMeters/1000.0
                        runOnUiThread {
                            drawPolyline(points)
                            if (distance.isNotEmpty()) {
                                // Update the TextView with the distance
                                val distanceTxt = findViewById<TextView>(R.id.distanceTxt)
                                distanceTxt.text = "Distance: $distanceInKm km  Duration : ${duration}"
                                Toast.makeText(this@MapsActivity, "Distance: $distance", Toast.LENGTH_LONG).show()
                            }

                            if (distance.isNotEmpty()) {
                                Toast.makeText(this@MapsActivity, "Distance: $distance", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            })
            Log.d("--------", "requestDirections: $url")
        }
    }

    private fun parseDirections(jsonData: String?): Triple<List<LatLng>, String, String> {
        val points = ArrayList<LatLng>()
        var distance = ""
        val duration = ""

        try {
            if (jsonData.isNullOrBlank()) {
                Log.e("Parse Directions", "JSON data is null or blank")
                return Triple(points, distance, duration)
            }

            val jsonObject = JSONObject(jsonData)
            val route = jsonObject.optJSONObject("route")

            if (route == null) {
                Log.e("Parse Directions", "No route found in JSON data")
                return Triple(points, distance, duration)
            }

            val geometry = route.optJSONObject("geometry")

            if (geometry == null) {
                Log.e("Parse Directions", "No geometry found in JSON data")
                return Triple(points, distance, duration)
            }

            val coordinates = geometry.optJSONArray("coordinates")

            if (coordinates == null || coordinates.length() == 0) {
                Log.e("Parse Directions", "No coordinates found in JSON data")
                return Triple(points, distance, duration)
            }

            for (i in 0 until coordinates.length()) {
                val coordinate = coordinates.optJSONArray(i)

                if (coordinate != null && coordinate.length() == 2) {
                    val lat = coordinate.getDouble(0)
                    val lng = coordinate.getDouble(1)
                    points.add(LatLng(lat, lng))
                }
            }
            distance = route.optString("distance", "")

        } catch (e: JSONException) {
            e.printStackTrace()
            Log.e("Parse Directions", "Error parsing JSON data: ${e.message}")
        }

        return  return Triple(points, distance, duration)
    }

    fun getLatLngFromAddress(address: String): Pair<Double, Double>? {
        try {
            val addresses: List<Address> = geocoder.getFromLocationName(address, 1)!!
            if (addresses.isNotEmpty()) {
                val latitude = addresses[0].latitude
                val longitude = addresses[0].longitude
                return Pair(latitude, longitude)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun drawPolyline(points: List<LatLng>?) {
        if (points != null && points.isNotEmpty()) {
            val polylineOptions = PolylineOptions()
                .addAll(points)
                .color(Color.BLUE)
                .width(7f)

            map.addPolyline(polylineOptions)

            val builder = LatLngBounds.Builder()
            for (point in points) {
                builder.include(point)
            }
            val bounds = builder.build()

            addMarker(
                points.first(),
                "origin",
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            )
            addMarker(
                points.last(),
                "destination",
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            )

            val padding = 50
            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)

            map.animateCamera(cameraUpdate)

            try {
                MapsInitializer.initialize(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            Log.e("Draw Polyline", "No valid points to draw")
        }
    }

    private fun addMarker(latLng: LatLng, title: String, icon: BitmapDescriptor) {
        val markerOptions = MarkerOptions()
            .position(latLng)
            .title(title)
            .icon(icon)

        map.addMarker(markerOptions)
    }
}

//    private fun requestDirections(origin: String?, destination: String?) {
//        val client = OkHttpClient()
//
////        val origin = originEdt.text.toString()
////        val destination = destinationEdt.text.toString()
//
//
//        val startPointLatLng = getLatLngFromAddress(origin!!)
//        val endPointLatLng = getLatLngFromAddress(destination!!)
//
//        Log.d("--------", "requestDirections: ${startPointLatLng}  ${endPointLatLng}")
//
//        if (startPointLatLng != null && endPointLatLng != null) {
//            // Split latitude and longitude values
//            val (startPointLat, startPointLng) = startPointLatLng
//            val (endPointLat, endPointLng) = endPointLatLng
//
//            // Create the URL with separately defined latitudes and longitudes
//            val url =
//                "https://trueway-directions2.p.rapidapi.com/FindDrivingRoute?stops=$startPointLat,$startPointLng%3B$endPointLat,$endPointLng"
//            val request = Request.Builder()
//                .url(url)
//                .get()
//                .addHeader("X-RapidAPI-Key", "b75b1af2dbmshcf32852a614c58cp1280cajsne0e57708a417")
//                .addHeader("X-RapidAPI-Host", "trueway-directions2.p.rapidapi.com")
//                .build()
//            client.newCall(request).enqueue(object : Callback {
//                override fun onFailure(call: Call, e: IOException) {
//                    Toast.makeText(this@MapsActivity, "getting error", Toast.LENGTH_SHORT).show()
//                }
//
//                override fun onResponse(call: Call, response: Response) {
//                    if (response.isSuccessful) {
//                        val jsonData = response.body?.string()
//                        Log.d("-----------", "onResponse: ${jsonData}")
//                        val points = parseDirections(jsonData)
//                        Log.d("-----------", "onResponse: ${points}")
//                        runOnUiThread {
//                            drawPolyline(points)
//                        }
//                    }
//                }
//            })
//            // Rest of your code...
//            Log.d("--------", "requestDirections: $url")
//        }
//
//    }
//
//    private fun parseDirections(jsonData: String?): List<LatLng> {
//        val points = ArrayList<LatLng>()
//
//        try {
//            if (jsonData.isNullOrBlank()) {
//                Log.e("Parse Directions", "JSON data is null or blank")
//                return points
//            }
//
//            val jsonObject = JSONObject(jsonData)
//            val route = jsonObject.optJSONObject("route")
//
//            if (route == null) {
//                Log.e("Parse Directions", "No route found in JSON data")
//                return points
//            }
//
//            val geometry = route.optJSONObject("geometry")
//
//            if (geometry == null) {
//                Log.e("Parse Directions", "No geometry found in JSON data")
//                return points
//            }
//
//            val coordinates = geometry.optJSONArray("coordinates")
//
//            if (coordinates == null || coordinates.length() == 0) {
//                Log.e("Parse Directions", "No coordinates found in JSON data")
//                return points
//            }
//
//            for (i in 0 until coordinates.length()) {
//                val coordinate = coordinates.optJSONArray(i)
//
//                if (coordinate != null && coordinate.length() == 2) {
//                    val lat = coordinate.getDouble(0)
//                    val lng = coordinate.getDouble(1)
//                    points.add(LatLng(lat, lng))
//                }
//            }
//        } catch (e: JSONException) {
//            e.printStackTrace()
//            Log.e("Parse Directions", "Error parsing JSON data: ${e.message}")
//        }
//
//        return points
//    }
//
//    fun getLatLngFromAddress(address: String): Pair<Double, Double>? {
//        try {
//            val addresses: List<Address> = geocoder.getFromLocationName(address, 1)!!
//            if (addresses.isNotEmpty()) {
//                val latitude = addresses[0].latitude
//                val longitude = addresses[0].longitude
//                return Pair(latitude, longitude)
//            }
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
//        return null
//    }
//
//    private fun drawPolyline(points: List<LatLng>?) {
//        if (points != null && points.isNotEmpty()) {
//            val polylineOptions = PolylineOptions()
//                .addAll(points)
//                .color(Color.BLUE)
//                .width(5f)
//
//            map.addPolyline(polylineOptions)
//
//            // Create LatLngBounds only if there are included points
//            val builder = LatLngBounds.Builder()
//            for (point in points) {
//                builder.include(point)
//            }
//            val bounds = builder.build()
//
//            addMarker(
//                points.first(),
//                "Origin",
//                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
//            )
//            addMarker(
//                points.last(),
//                "Destination",
//                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
//            )
//
//            val padding = 50 // Adjust this padding as needed
//            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
//
//            map.animateCamera(cameraUpdate)
//
//            // Enable Google Maps features
//            try {
//                MapsInitializer.initialize(this)
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        } else {
//            // Handle the case where points are null or empty
//            Log.e("Draw Polyline", "No valid points to draw")
//        }
//    }
//
//    private fun addMarker(latLng: LatLng, title: String, icon: BitmapDescriptor) {
//        val markerOptions = MarkerOptions()
//            .position(latLng)
//            .title(title)
//            .icon(icon)
//
//        map.addMarker(markerOptions)
//    }
//}