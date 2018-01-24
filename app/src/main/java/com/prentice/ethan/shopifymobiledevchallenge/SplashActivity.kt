package com.prentice.ethan.shopifymobiledevchallenge

import android.content.Intent
import android.content.IntentFilter
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.prentice.ethan.shopifymobiledevchallenge.MainActivity.Companion.updateProducts
import kotlinx.android.synthetic.main.activity_splash.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.lang.Thread.sleep
import java.util.*
import kotlin.concurrent.thread


class SplashActivity : AppCompatActivity() {

    private val productsURL = "https://shopicruit.myshopify.com/admin/products.json?page=1&access_token=c32313df0d0ef512ca64d5b336a0d7c6"

    private lateinit var iReceiver: MainActivity.Companion.ImageDownloadedReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val scale = resources.displayMetrics.density
        iReceiver = MainActivity.Companion.ImageDownloadedReceiver(findViewById(R.id.productsList), scale)

        // Set up splash's animations
        val animFadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_expand_in)
        val animFadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)

        animFadeIn.fillAfter = true
        animFadeOut.fillAfter = true

        val timer = Timer()
        val runFadeOut = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    splashObjects.startAnimation(animFadeOut)
                }
            }
        }

        animFadeIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(anim: Animation?) {}
            override fun onAnimationStart(anim: Animation?) {}

            override fun onAnimationEnd(anim: Animation?) {
                timer.schedule(runFadeOut, 2000)
            }
        })

        animFadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(anim: Animation?) {}
            override fun onAnimationStart(anim: Animation?) {}

            override fun onAnimationEnd(anim: Animation?) {
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        })

        // Check internet connection and notify user if no connection
        // When user has connection, start downloading product data and start animation
        splashObjects.visibility = View.INVISIBLE
        thread {
            while (!Utilities.hasInternet()) {
                sleep(500)
            }
            runOnUiThread {
                removeNetworkError()
                splashObjects.startAnimation(animFadeIn)
            }
            thread {
                retrieveProducts(productsURL)
            }
        }

    }

    override fun onStart() {
        super.onStart()
        val iFilter = IntentFilter()
        iFilter.addAction("com.prentice.ethan.shopifymobiledevchallenge.IMAGE_DOWNLOADED")
        registerReceiver(iReceiver, iFilter)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(iReceiver)
    }

    // Hide network error message from screen
    private fun removeNetworkError() {
        splashObjects.visibility = View.VISIBLE
        networkErrorFrame.visibility = View.INVISIBLE
    }


    private fun retrieveProducts(url: String) {
        val httpClient = OkHttpClient()
        var result: String? = null

        // Access Shopify API, download product JSON
        try {
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()
            println("Response: $response")
            result = response.body()?.string()

        } catch (e: Exception) {
            Log.e("Exception", e.toString())
        }

        if (result != null) {
            //Send productJson to MainActivity's companion object to be updated
            val productsJson = Utilities.parseJson(result)
            updateProducts(this, productsJson["products"] as JsonArray<JsonObject>)

        } else {
            Log.d("SplashActivity", "There was an error downloading products.  Result is null.")
        }
    }

}
