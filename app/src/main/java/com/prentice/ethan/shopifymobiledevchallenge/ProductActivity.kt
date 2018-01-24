package com.prentice.ethan.shopifymobiledevchallenge

import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import com.beust.klaxon.JsonObject
import kotlinx.android.synthetic.main.activity_product.*
import com.beust.klaxon.JsonArray
import com.prentice.ethan.shopifymobiledevchallenge.Utilities.Companion.parseJson


class ProductActivity : AppCompatActivity() {

    private var scale = 1f

    private lateinit var productImageView: ImageView

    private var productID = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product)

        scale = resources.displayMetrics.density

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        productImageView = ImageView(this)

        productID = intent.getLongExtra("product_id", 0)

        val productJson = parseJson(intent.getStringExtra("product_json"))
        val variants = productJson["variants"] as JsonArray<JsonObject>

        idText.text = productID.toString()
        typeText.text = productJson["product_type"] as String

        val options = arrayOf(ArrayList<String?>(), ArrayList(), ArrayList())

        // Get all options
        for (variant in variants) {
            (1..4).forEach {
                val option = variant["option$it"] as String?
                if (option != null && option !in options[it-1]) {
                    options[it-1].add(option)
                }
            }
        }
        // If list is empty, add a null entry
        options.filter { it.size == 0 }.forEach { it.add(null) }

        // Organize variants by option configuration
        val variantsByOptions = HashMap<ArrayList<String?>, JsonObject>()
        for (o1 in options[0]) {
            for (o2 in options[1]) {
                for (o3 in options[2]) {
                    val variant = variants.filter {
                        it["option1"] == o1 &&
                        it["option2"] == o2 &&
                        it["option3"] == o3
                    }[0]
                    variantsByOptions[arrayListOf(o1, o2, o3)] = variant
                }
            }
        }

        // Style spinners, add entries and listeners to them
        val optionSpinnerIDs = arrayOf(option1, option2, option3)
        for (i in 0..2) {
            if (null in options[i]) {
                productDetailedLayout.removeView(optionSpinnerIDs[i])
            } else {
                val optionsSpinner = optionSpinnerIDs[i]
                val adapter = ArrayAdapter<String>(this, R.layout.spinner_style, options[i])
                optionsSpinner.adapter = adapter

                optionsSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(p0: AdapterView<*>?) { }

                    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                        updateConfig(variantsByOptions)
                    }
                }
            }
        }

        // Add the image to the layout
        val imageByteArray = intent.getByteArrayExtra("product_image")
        val image = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.size)

        productImage.setImageBitmap(image)
        productTitle.text = productJson["title"] as String
        productDesc.text = productJson["body_html"] as String

        println("Product ID: $productID")
    }

    override fun onSupportNavigateUp(): Boolean{
        resetProductPressAnim()
        finish()
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
        resetProductPressAnim()
        finish()
    }

    private fun resetProductPressAnim() {
        MainActivity.productList.filter {
            it.productID == productID
        }.forEach {
            it.resetButtonAnim()
        }
    }

    fun updateConfig(variants: HashMap<ArrayList<String?>, JsonObject>) {
        val o1: String? = option1.selectedItem?.toString()
        val o2: String? = option2.selectedItem?.toString()
        val o3: String? = option3.selectedItem?.toString()

        val variant = variants[arrayListOf(o1, o2, o3)]!!

        currentPrice.text = "$" + variant["price"] as String
        varIDText.text = variant["id"].toString()
        inventoryText.text = variant["inventory_quantity"].toString()
    }


}
