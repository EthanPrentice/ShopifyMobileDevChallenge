package com.prentice.ethan.shopifymobiledevchallenge

import android.content.Context
import android.net.ConnectivityManager
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser


/**
 * Created by Ethan on 2018-01-02.
 */
class Utilities {
    companion object {

        fun parseJson(jsonString: String): JsonObject {
            val parser = Parser()
            val stringBuilder = StringBuilder(jsonString)
            return parser.parse(stringBuilder) as JsonObject
        }

        // Pings Google's server to check connection
        fun hasInternet(): Boolean{
            try {
                val ping = java.lang.Runtime.getRuntime().exec("ping -c 1 8.8.8.8")
                val value = ping.waitFor()
                return value == 0
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return false
        }

        fun toDP(pixelVal: Int, scale: Float): Int {
            return (pixelVal * scale).toInt()
        }
    }
}