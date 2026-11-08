package io.github.mirhamedjz.ehligible.core.model

import kotlinx.serialization.Serializable

/** The home-screen widget's colours, chosen in Settings. */
@Serializable
enum class WidgetStyle {
    /** Light when the phone is in light mode, dark in dark mode. */
    SYSTEM,
    LIGHT,
    DARK,

    /** Red, with a white leaf, like the flag. */
    MAPLE,
    ;

    /** The colours to draw with; [night] is whether the phone is in dark mode (it only matters for [SYSTEM]). */
    fun colors(night: Boolean): WidgetColors = when (this) {
        SYSTEM -> if (night) WidgetColors.Dark else WidgetColors.Light
        LIGHT -> WidgetColors.Light
        DARK -> WidgetColors.Dark
        MAPLE -> WidgetColors.Maple
    }
}

/**
 * One widget look, as ARGB values. The Android widget and the Settings preview draw with these; the iOS widget
 * has the same values in Swift (`Palette` in `iosApp/EhligibleWidget/CountdownWidget.swift`).
 */
data class WidgetColors(
    val background: Long,
    /** Labels and the date line. */
    val ink: Long,
    /** The big number of days. */
    val number: Long,
    /** The status line ("In Canada", "2 days need your review"). */
    val accent: Long,
    /** The filled part of the leaf, the ring and the progress bar. */
    val fill: Long,
    /** The leaf's empty part. */
    val leafEmpty: Long,
    /** Behind the ring and the progress bar. */
    val track: Long,
    /** The ring once all the days are in. */
    val done: Long = GOLD,
) {
    companion object {
        private const val GOLD = 0xFFF4A259
        private const val RED = 0xFFD62828

        val Light = WidgetColors(
            background = 0xFFFFF8F5,
            ink = 0xFF231917,
            number = RED,
            accent = 0xFF2F5DA8,
            fill = RED,
            leafEmpty = 0xFFF6DCD8,
            track = 0xFFF5DDDA,
        )

        val Dark = WidgetColors(
            background = 0xFF1A1112,
            ink = 0xFFF1DEDC,
            number = 0xFFFF8A80,
            accent = 0xFFADC6FF,
            fill = RED,
            leafEmpty = 0xFF3D2A2B,
            track = 0xFF534341,
        )

        val Maple = WidgetColors(
            background = 0xFFA4161A,
            ink = 0xFFFFFFFF,
            number = 0xFFFFFFFF,
            accent = 0xFFFFE0B2,
            fill = 0xFFFFFFFF,
            leafEmpty = 0xFFC8474B,
            track = 0xFFC23B40,
        )
    }
}

/** Replaces the alpha of an ARGB colour with [percent] (0 = invisible, 100 = solid). */
fun Long.withOpacity(percent: Int): Long {
    val alpha = (percent.coerceIn(0, 100) * 255 + 50) / 100
    return (this and 0xFFFFFF) or (alpha.toLong() shl 24)
}
