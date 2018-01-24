package com.prentice.ethan.shopifymobiledevchallenge

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.support.v4.content.ContextCompat
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.prentice.ethan.shopifymobiledevchallenge.Utilities.Companion.toDP
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference


/**
 * Created by Ethan on 2017-12-31.
 */

class Product(val context: Context, jsonObj: JsonObject) {

    var scale = context.resources.displayMetrics.density

    val productJson: JsonObject = jsonObj
    val productID = jsonObj["id"] as Long
    val productName = jsonObj["title"] as String
    val productDesc = jsonObj["body_html"] as String
    val productTags = jsonObj["tags"] as String

    var imageDownloaded = false


    lateinit var productImage: Bitmap

    val layout = RelativeLayout(context)

    private val productImageView = ImageView(context)
    private val productTitleLabel = TextView(context)
    private val productDescLabel = TextView(context)

    init {
        layout.setBackgroundResource(R.drawable.product_background)
        layout.isClickable = true


        // OnClick add effect and send product data to the MainActivity
        layout.setOnClickListener {
            val scaleFactor = 0.9f
            val alphaFactor = 0.8f

            layout.alpha *= alphaFactor
            layout.scaleX *= scaleFactor
            layout.scaleY *= scaleFactor

            val productID =  productJson["id"] as Long
            val intent = Intent("com.prentice.ethan.shopifymobiledevchallenge.PRODUCT_CLICKED")
            intent.putExtra("product_id", productID)
            intent.putExtra("product_json", productJson.toJsonString())
            intent.putExtra("product_image", bitmapToByteArray(productImage))
            context.sendBroadcast(intent)
        }


        addImageToLayout()
        addTitleToLayout()
        addDescToLayout()
    }

    fun resetButtonAnim() {
        val scaleFactor = 0.9f
        val alphaFactor = 0.8f

        layout.alpha /= alphaFactor
        layout.scaleX /= scaleFactor
        layout.scaleY /= scaleFactor
    }

    private fun addImageToLayout() {
        val layoutParams = RelativeLayout.LayoutParams(
                (toDP(75, scale)), (toDP(75, scale))
        )
        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL)
        layoutParams.setMargins(toDP(19, scale), 0, 0, 0)

        productImageView.id = View.generateViewId()
        productImageView.layoutParams = layoutParams
        layout.addView(productImageView)

        val imageUrl = (productJson["images"] as JsonArray<JsonObject>)[0]["src"] as String
        GetProductImageTask(WeakReference(context)).execute(imageUrl)
    }

    private fun addTitleToLayout() {
        val layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        layoutParams.addRule(RelativeLayout.ALIGN_TOP, productImageView.id)
        layoutParams.addRule(RelativeLayout.END_OF, productImageView.id)
        layoutParams.setMargins(toDP(9, scale), toDP(12, scale), 0, 0)

        productTitleLabel.id = View.generateViewId()
        productTitleLabel.text = productJson["title"] as String
        productTitleLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        productTitleLabel.setTextColor(ContextCompat.getColor(context, R.color.colorSplashAccent))
        productTitleLabel.layoutParams = layoutParams
        layout.addView(productTitleLabel)
    }

    private fun addDescToLayout() {
        val layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        layoutParams.addRule(RelativeLayout.BELOW, productTitleLabel.id)
        layoutParams.addRule(RelativeLayout.END_OF, productImageView.id)
        layoutParams.setMargins(toDP(15, scale), 0, 0, 0)

        productDescLabel.id = View.generateViewId()
        productDescLabel.text = productJson["body_html"] as String
        productDescLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        productDescLabel.setTextColor(ContextCompat.getColor(context, R.color.colorSplashAccent))
        productDescLabel.layoutParams = layoutParams
        layout.addView(productDescLabel)
    }

    fun imageDownloaded(image: Bitmap?) {
        if (image != null) {
            productImage = image
            productImageView.setImageBitmap(productImage)
        }
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val bStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bStream)
        return bStream.toByteArray()
    }


    // TODO: Listen for when device is connected to internet. Update to valid image when device reconnects.
    inner internal class GetProductImageTask(private val contextRef: WeakReference<Context>) : AsyncTask<String, String?, Bitmap?>() {
        override fun doInBackground(vararg urls: String): Bitmap? {
            val url = urls[0]
            val httpClient = OkHttpClient()
            try {
                val request = Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()
                val bytes = response.body()?.byteStream()
                return BitmapFactory.decodeStream(bytes)
            } catch (e: Exception) {
                Log.e("Exception", e.toString())
                return null
            }
        }
        override fun onPostExecute(result: Bitmap?) {
            super.onPostExecute(result)
            imageDownloaded = true
            if (result != null) {
                imageDownloaded(result)
            } else {
                productImage = getDefaultImage(context)
                productImageView.setImageBitmap(productImage)
            }
            val productID = productJson["id"] as Long
            val intent = Intent("com.prentice.ethan.shopifymobiledevchallenge.IMAGE_DOWNLOADED")
            intent.putExtra("product_id", productID)
            val context = contextRef.get()
            context?.sendBroadcast(intent)
        }
    }

    companion object {
        fun getDefaultImage(context: Context): Bitmap {
            return BitmapFactory.decodeResource(context.resources, R.drawable.image_error)
        }
    }


}