package com.prentice.ethan.shopifymobiledevchallenge

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.beust.klaxon.JsonObject
import android.view.Menu
import android.app.SearchManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.LinearLayout
import com.beust.klaxon.JsonArray
import kotlinx.android.synthetic.main.activity_main.*
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.widget.SearchView


class MainActivity : AppCompatActivity(){

    private lateinit var iReceiver: ImageDownloadedReceiver
    private lateinit var clickReceiver: ProductClickedListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Handle any searches send to the activity
        handleIntent(intent)

        // Show the products downloaded during the SplashActivity
        val scale = resources.displayMetrics.density
        val productHeight = (100 * scale + 0.5f).toInt()
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, productHeight)
        productsNotOnView.forEach {
            it.layout.layoutParams = params
            productsList?.addView(it.layout, params)
        }

        iReceiver = ImageDownloadedReceiver(productsList, scale)
        clickReceiver = ProductClickedListener()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.options_menu, menu)

        // Associate searchable configuration with the SearchView
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menu.findItem(R.id.search).actionView as SearchView
        searchView.setIconifiedByDefault(false)
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))

        // When search view is opened show search results
        // When search view is closed show all products
        searchView.addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
            override fun onViewDetachedFromWindow(view: View) {
                removeAllProductViews()
                addProductViews(productList)
            }
            override fun onViewAttachedToWindow(view: View) {
                val query = searchView.query.toString()
                if (query.trim() != "") {
                    val searchProducts = getSearchedProducts(query)
                    removeAllProductViews()
                    addProductViews(searchProducts)
                }
            }
        })

        return true
    }


    override fun onStart() {
        super.onStart()
        val iFilter = IntentFilter()
        iFilter.addAction("com.prentice.ethan.shopifymobiledevchallenge.IMAGE_DOWNLOADED")
        registerReceiver(iReceiver, iFilter)

        val clickFilter = IntentFilter()
        clickFilter.addAction("com.prentice.ethan.shopifymobiledevchallenge.PRODUCT_CLICKED")
        registerReceiver(clickReceiver, clickFilter)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(clickReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(iReceiver)
    }

    override fun onBackPressed() {
        // Instead of sending user back to splash screen, send them to launcher
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(startMain)
    }

    override fun onNewIntent(intent: Intent) {
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        // Handle search intents
        if (Intent.ACTION_SEARCH == intent.action) {
            query = intent.getStringExtra(SearchManager.QUERY)
            searchInProgress = true

            // Show searched products
            val searchProducts = getSearchedProducts(query)
            removeAllProductViews()
            addProductViews(searchProducts)
        }
    }

    private fun getSearchedProducts(query: String): ArrayList<Product> {
        val productsInSearch = ArrayList<Product>()
        for (product in productList) {
            val searchableWords = product.productDesc.split(" ") +
                                  product.productName.split(" ") +
                                  product.productTags.split(", ")

            // Check if search query matches the ID or the start of any words in the title, tags or desc
            if (query == product.productID.toString()) {
                productsInSearch.add(product)
            } else {
                searchableWords.filter {
                    queryMatchesWord(query, it)
                }.forEach {
                    productsInSearch.add(product)
                }
            }
        }
        return productsInSearch
    }


    private fun removeAllProductViews() {
        productsList.removeAllViews()
    }

    private fun addProductViews(products: ArrayList<Product>) {
        val scale = resources.displayMetrics.density
        val productHeight = (100 * scale + 0.5f).toInt()
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, productHeight)
        products.forEach {
            if (it.layout.parent == null) {
                if (it.imageDownloaded) {
                    productsList.addView(it.layout, params)
                }
            }
        }
    }

    // When a products is clicked send the product info to the ProductActivity
    internal inner class ProductClickedListener : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val productID = intent.getLongExtra("product_id", 0)
            val productJSON = intent.getStringExtra("product_json")
            val productImage = intent.getByteArrayExtra("product_image")

            val newActivity = Intent(context, ProductActivity::class.java)
            newActivity.putExtra("product_id", productID)
            newActivity.putExtra("product_json", productJSON)
            newActivity.putExtra("product_image", productImage)
            startActivity(newActivity)
        }
    }

    companion object {

        // Query and searchInProgress are static so they can be accessed as images are received and shown in search results
        var query = ""
        var searchInProgress = false
        val productList = ArrayList<Product>()
        private val productsNotOnView = ArrayList<Product>()

        fun updateProducts(context: Context, products: JsonArray<JsonObject>) {
            products.forEach {
                println("Title: ${it["title"]}")
                println("Description: ${it["body_html"]}")
                productList.add(Product(context, it))
            }
        }

        private fun queryMatchesWord(query: String, wordToMatch: String): Boolean {
            return try {
                query.toLowerCase() == wordToMatch.substring(0, query.length).toLowerCase()
            } catch(e: StringIndexOutOfBoundsException) {
                false
            }
        }

        internal class ImageDownloadedReceiver(private var productsList: LinearLayout?, scale:Float) : BroadcastReceiver() {

            private val productHeight = (100 * scale + 0.5f).toInt()

            override fun onReceive(context: Context?, intent: Intent) {
                val productID = intent.getLongExtra("product_id", 0)

                productList.forEach {
                    if (it.productJson["id"] == productID) {
                        println("Product with id $productID has been added to view")
                        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, productHeight)

                        if (productsList != null) {
                            if (it.layout.parent == null) {
                                // If a search is in progress and product matches results update results to show it
                                if (searchInProgress) {
                                    val searchableWords = it.productDesc.split(" ") +
                                                          it.productName.split(" ") +
                                                          it.productTags.split(", ")

                                    if (query == it.productID.toString()) {
                                        productsList?.addView(it.layout, params)
                                    } else {
                                        for (word in searchableWords) {
                                             if (queryMatchesWord(query, word)) {
                                                 productsList?.addView(it.layout, params)
                                             }
                                        }
                                    }
                                } else {
                                    productsList?.addView(it.layout, params)
                                }
                            }
                        } else {
                            productsNotOnView.add(it)
                        }
                    }
                }
            }
        }
    }
}
