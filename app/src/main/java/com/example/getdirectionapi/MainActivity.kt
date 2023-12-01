package com.example.getdirectionapi

import android.content.Intent
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import okhttp3.ResponseBody
import org.json.JSONException
import java.io.IOException

class MainActivity : AppCompatActivity(){


    private lateinit var originEdt: EditText
    private lateinit var destinationEdt: EditText
    private lateinit var submitBtn: Button



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        originEdt = findViewById(R.id.originEdt)
        destinationEdt = findViewById(R.id.destinationEdt)

        submitBtn = findViewById(R.id.submitBtn)
        submitBtn.setOnClickListener {
            val origin = originEdt.text.toString()
            val destination = destinationEdt.text.toString()

            // Create an Intent to start the MapActivity
            val intent = Intent(this@MainActivity, MapsActivity::class.java)
            // Pass data to MapActivity using extras
            intent.putExtra("origin", origin)
            intent.putExtra("destination", destination)
            // Start the MapActivity
            startActivity(intent)
        }
    }

}