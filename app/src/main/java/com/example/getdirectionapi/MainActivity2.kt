package com.example.getdirectionapi

import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.AsyncHttpClient.log
import com.loopj.android.http.AsyncHttpResponseHandler
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.http2.Header
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class MainActivity2 : AppCompatActivity() {

    private lateinit var originAutoCompleteTextView: AutoCompleteTextView
    private lateinit var destinationAutoCompleteTextView: AutoCompleteTextView
    private lateinit var submitButton: Button
    private lateinit var autoSuggestAdapter: AutoSuggestAdapter

    private var selectedOrigin: String = ""
    private var selectedDestination: String = ""

    // MapTiler Geocoding API key
    private val mapTilerApiKey = "R7b5ucof5xtiFEH9f5il"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)


        originAutoCompleteTextView = findViewById(R.id.originAutoCompleteTextView);
        destinationAutoCompleteTextView = findViewById(R.id.destinationAutoCompleteTextView);
        submitButton = findViewById(R.id.submitButton);

        autoSuggestAdapter = AutoSuggestAdapter(this, android.R.layout.simple_dropdown_item_1line)

        Log.d("*******", "onCreate: ${originAutoCompleteTextView} ")
        Log.d("*******", "onCreate: ${destinationAutoCompleteTextView} ")
        // Set up adapters for autocomplete suggestions
        setupAutoCompleteAdapter(originAutoCompleteTextView, autoSuggestAdapter)
        setupAutoCompleteAdapter(destinationAutoCompleteTextView, autoSuggestAdapter)

        submitButton.setOnClickListener {
            val origin = originAutoCompleteTextView.text.toString()
            val destination = destinationAutoCompleteTextView.text.toString()

            Log.d("*******", "onCreate: ${origin} ")
            Log.d("*******", "onCreate: ${destination} ")


            // Further actions with origin and destination values
            Toast.makeText(this@MainActivity2, "Origin: $origin\nDestination: $destination", Toast.LENGTH_SHORT).show()
        }
    }


    private fun setupAutoCompleteAdapter(autoCompleteTextView: AutoCompleteTextView, adapter: AutoSuggestAdapter) {
        autoCompleteTextView.setAdapter(adapter)

        autoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
            // Retrieve the selected name
            val selectedName = adapter.getObject(position)
            Log.d("-------", "onCreate: ${position}")

            // Set the selected name based on the AutoCompleteTextView
            when (autoCompleteTextView.id) {
                R.id.originAutoCompleteTextView -> selectedOrigin = selectedName
                R.id.destinationAutoCompleteTextView -> selectedDestination = selectedName
            }
        }
        Log.d("-------", "onCreate: ${selectedOrigin}")
        Log.d("-------", "onCreate: ${selectedDestination}")

        autoCompleteTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s != null && s.length >= 3) {
                    performAutoCompleteQuery(s.toString(), adapter)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    private fun performAutoCompleteQuery(query: String, adapter: AutoSuggestAdapter) {
        val url =
            "https://api.maptiler.com/geocoding/$query.json?autocomplete=true&fuzzyMatch=true&limit=5&key=$mapTilerApiKey"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseJson = JSONObject(response.body!!.string())
                    Log.d("API Response", responseJson.toString())

                    val featuresArray = responseJson.optJSONArray("features")
                    val suggestions = mutableListOf<String>()

                    if (featuresArray != null) {
                        for (i in 0 until featuresArray.length()) {
                            try {
                                val feature = featuresArray.getJSONObject(i)
                                val properties = feature.optJSONObject("properties")
                                val placeName = properties?.optString("place_name")
                                val placeTypeArray = properties?.optJSONArray("place_type")
                                val relevance = properties?.optInt("relevance", 0)

                                if (relevance == 1 && placeName != null) {
                                    if (placeTypeArray != null && placeTypeArray.length() > 0) {
                                        val placeType = placeTypeArray.getString(0)
                                        if (placeType == "place" || placeType == "address") {
                                            suggestions.add(placeName)
                                        }
                                    }
                                }

                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }
                    }

                    runOnUiThread { updateAutoCompleteSuggestions(suggestions, adapter) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(applicationContext, "API call failed", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun updateAutoCompleteSuggestions(suggestions: List<String>, adapter: AutoSuggestAdapter) {
        adapter.setData(suggestions)
        adapter.notifyDataSetChanged()
        Log.d("==================", "updateAutoCompleteSuggestions: ${suggestions}")
    }
}





