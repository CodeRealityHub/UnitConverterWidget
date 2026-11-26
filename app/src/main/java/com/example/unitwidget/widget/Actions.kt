
package com.example.unitwidget.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState

private val K = WidgetState

class WidgetAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val action =
            parameters[ActionParameters.Key<String>("action")] ?: return

        updateAppWidgetState(context, glanceId) { p ->

            when (action) {

                "home" -> {
                    p[K.screen] = "home"
                }

                "picker" -> {
                    p[K.screen] = "picker"
                }

                "quick" -> {
                    p[K.screen] = "quick"
                }

                "appearance" -> {
                    p[K.screen] = "appearance"
                }

                "convert" -> {
                    p[K.category] =
                        parameters[ActionParameters.Key<String>("category")]
                            ?: "length"

                    p[K.input] =
                        parameters[ActionParameters.Key<String>("input")]
                            ?: "1"

                    p[K.fromIndex] = 0
                    p[K.screen] = "convert"
                }

                "system" -> {
                    p[K.system] =
                        if ((p[K.system] ?: "INDIA") == "INDIA")
                            "WORLD"
                        else
                            "INDIA"
                }

                "mode" -> {
                    val old = p[K.appearance] ?: "AUTO"

                    p[K.appearance] = when (old) {
                        "AUTO" -> "LIGHT"
                        "LIGHT" -> "DARK"
                        else -> "AUTO"
                    }
                }

                "accent" -> {
                    val old = p[K.accent] ?: "0"

                    p[K.accent] =
                        ((old.toIntOrNull() ?: 0) + 1)
                            .rem(7)
                            .toString()
                }

                "cycle" -> {
                    p[K.fromIndex] = (p[K.fromIndex] ?: 0) + 1
                }

                "clear" -> {
                    p[K.input] = "0"
                }

                "back" -> {
                    p[K.input] =
                        (p[K.input] ?: "0")
                            .dropLast(1)
                            .ifEmpty { "0" }
                }

                "sign" -> {
                    val old = p[K.input] ?: "0"

                    p[K.input] =
                        if (old.startsWith("-"))
                            old.drop(1)
                        else
                            "-$old"
                }

                "dot" -> {
                    if (!(p[K.input] ?: "").contains(".")) {
                        p[K.input] = (p[K.input] ?: "0") + "."
                    }
                }

                else -> {
                    if (action.startsWith("digit:")) {
                        val digit = action.substringAfter(":")
                        val old = p[K.input] ?: "0"

                        p[K.input] =
                            if (old == "0")
                                digit
                            else
                                (old + digit).take(14)
                    }
                }
            }

            p
        }

        UnitConverterWidget().update(context, glanceId)
    }
}
