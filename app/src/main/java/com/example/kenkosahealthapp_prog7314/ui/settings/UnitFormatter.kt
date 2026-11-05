package com.example.kenkosahealthapp_prog7314.ui.settings

import android.content.Context
import com.example.kenkosahealthapp_prog7314.data.model.UnitConversions
import java.util.Locale

/**
 * Turns stored metric values (cm, kg, ml, g) into the unit system the user chose in Settings
 * ("Metric", "Imperial" or "UK"). The choice is cached in SharedPreferences, like ThemeManager does,
 * so every screen can read it instantly without waiting for Firestore.
 */
object UnitFormatter {

    private const val PREFS = "kenkosa_prefs"
    private const val KEY = "unit_system"

    private const val ML_PER_FL_OZ = 29.5735
    private const val G_PER_OZ = 28.349523

    fun save(context: Context, system: String) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, system).apply()

    fun current(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "Metric") ?: "Metric"

    // ---- pure helpers (no Android, easy to unit test) ----

    fun kgToLb(kg: Double): Double = UnitConversions.kgToLb(kg)
    fun mlToFlOz(ml: Double): Double = ml / ML_PER_FL_OZ
    fun gToOz(g: Double): Double = g / G_PER_OZ

    /** 70 kg -> 11 st 0.3 lb */
    fun stonesAndPounds(kg: Double): Pair<Int, Double> {
        val totalLb = kgToLb(kg)
        val stones = (totalLb / 14).toInt()
        return stones to (totalLb - stones * 14)
    }

    // ---- display strings ----

    fun weight(context: Context, kg: Double): String = when (current(context)) {
        "Imperial" -> String.format(Locale.getDefault(), "%.1f lb", kgToLb(kg))
        "UK" -> stonesAndPounds(kg).let { (st, lb) -> String.format(Locale.getDefault(), "%d st %.1f lb", st, lb) }
        else -> String.format(Locale.getDefault(), "%.1f kg", kg)
    }

    fun height(context: Context, cm: Double): String =
        if (current(context) == "Metric") {
            String.format(Locale.getDefault(), "%.0f cm", cm)
        } else {
            UnitConversions.cmToFtIn(cm).let { (ft, inch) ->
                String.format(Locale.getDefault(), "%d ft %.0f in", ft, inch)
            }
        }

    /** Water volumes. UK uses metric litres/ml, so only Imperial switches to fluid ounces. */
    fun volume(context: Context, ml: Double): String =
        if (current(context) == "Imperial") String.format(Locale.getDefault(), "%.1f fl oz", mlToFlOz(ml))
        else String.format(Locale.getDefault(), "%.0f ml", ml)

    /** Food masses (macros). */
    fun mass(context: Context, g: Double): String =
        if (current(context) == "Imperial") String.format(Locale.getDefault(), "%.1f oz", gToOz(g))
        else String.format(Locale.getDefault(), "%.0f g", g)
}