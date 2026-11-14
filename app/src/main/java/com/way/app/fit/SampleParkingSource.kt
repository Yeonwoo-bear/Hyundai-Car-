package com.way.app.fit

import android.content.Context
import org.json.JSONArray

/** assets/parking_sample.json 로더. 오프라인/API 실패 시 폴백용. */
object SampleParkingSource {
    private const val FILE = "parking_sample.json"

    fun load(context: Context): List<ParkingLot> {
        val text = context.assets.open(FILE).bufferedReader(Charsets.UTF_8).use { it.readText() }
        return parse(text)
    }

    fun parse(json: String): List<ParkingLot> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            ParkingLot(
                name = o.getString("name"),
                lat = o.getDouble("lat"),
                lon = o.getDouble("lon"),
                totalSpaces = o.getInt("totalSpaces"),
                currentParked = o.getInt("currentParked"),
                baseFee = o.getInt("baseFee")
            )
        }
    }
}
