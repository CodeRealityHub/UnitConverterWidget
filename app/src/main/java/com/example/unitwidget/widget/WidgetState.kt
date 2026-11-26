
package com.example.unitwidget.widget

import androidx.datastore.preferences.core.*

object WidgetState {

    val screen = stringPreferencesKey("screen")

    val category = stringPreferencesKey("category")

    val input = stringPreferencesKey("input")

    val fromIndex = intPreferencesKey("from_index")

    val system = stringPreferencesKey("system")

    val appearance = stringPreferencesKey("appearance")

    val accent = stringPreferencesKey("accent")

    val updatedAt = longPreferencesKey("rates_updated")

    val rateJson = stringPreferencesKey("rate_json")
}
