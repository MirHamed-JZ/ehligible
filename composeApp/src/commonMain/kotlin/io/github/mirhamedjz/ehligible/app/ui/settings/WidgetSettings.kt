package io.github.mirhamedjz.ehligible.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mirhamedjz.ehligible.app.platform.formatNumber
import io.github.mirhamedjz.ehligible.app.resources.Res
import io.github.mirhamedjz.ehligible.app.resources.home_counted
import io.github.mirhamedjz.ehligible.app.resources.home_days_to_go
import io.github.mirhamedjz.ehligible.app.resources.home_eligible_title
import io.github.mirhamedjz.ehligible.app.resources.settings_widget_opacity
import io.github.mirhamedjz.ehligible.app.resources.settings_widget_opacity_body
import io.github.mirhamedjz.ehligible.app.resources.settings_widget_opacity_value
import io.github.mirhamedjz.ehligible.app.resources.settings_widget_preview
import io.github.mirhamedjz.ehligible.app.resources.settings_widget_style
import io.github.mirhamedjz.ehligible.app.resources.settings_widget_style_body
import io.github.mirhamedjz.ehligible.app.resources.settings_widget_style_dark
import io.github.mirhamedjz.ehligible.app.resources.settings_widget_style_light
import io.github.mirhamedjz.ehligible.app.resources.settings_widget_style_maple
import io.github.mirhamedjz.ehligible.app.resources.settings_widget_style_system
import io.github.mirhamedjz.ehligible.app.ui.components.LeafColors
import io.github.mirhamedjz.ehligible.app.ui.components.MapleLeafProgress
import io.github.mirhamedjz.ehligible.core.model.Dashboard
import io.github.mirhamedjz.ehligible.core.model.Settings
import io.github.mirhamedjz.ehligible.core.model.WidgetStyle
import io.github.mirhamedjz.ehligible.core.model.withOpacity
import io.github.mirhamedjz.ehligible.core.rules.CitizenshipRules
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** The widget's colours and see-through background, with a preview over a stand-in wallpaper. */
@Composable
internal fun WidgetSettings(
    settings: Settings,
    dashboard: Dashboard,
    opacitySupported: Boolean,
    onStyle: (WidgetStyle) -> Unit,
    onOpacity: (Int) -> Unit,
) {
    // Follows the slider while it moves; saved (and the real widget refreshed) when it's let go.
    var opacity by remember(settings.widgetOpacity) { mutableFloatStateOf(settings.widgetOpacity.toFloat()) }
    val shownOpacity = if (opacitySupported) opacity.roundToInt() else Settings.MAX_WIDGET_OPACITY

    WidgetPreview(settings.widgetStyle, shownOpacity, dashboard)

    Text(
        stringResource(Res.string.settings_widget_style),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp),
    )
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        WidgetStyle.entries.forEachIndexed { index, style ->
            SegmentedButton(
                selected = settings.widgetStyle == style,
                onClick = { onStyle(style) },
                shape = SegmentedButtonDefaults.itemShape(index, WidgetStyle.entries.size),
                icon = {},
            ) { Text(stringResource(style.label), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
    Text(stringResource(Res.string.settings_widget_style_body), style = MaterialTheme.typography.bodySmall)

    if (opacitySupported) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Text(
                stringResource(Res.string.settings_widget_opacity),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(Res.string.settings_widget_opacity_value, opacity.roundToInt()),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Text(stringResource(Res.string.settings_widget_opacity_body), style = MaterialTheme.typography.bodySmall)
        Slider(
            value = opacity,
            // Snapped by hand rather than with `steps`, which would draw 19 tick marks.
            onValueChange = { opacity = (it / OPACITY_STEP).roundToInt() * OPACITY_STEP.toFloat() },
            onValueChangeFinished = { onOpacity(opacity.roundToInt()) },
            valueRange = 0f..Settings.MAX_WIDGET_OPACITY.toFloat(),
        )
    }
}

/** Snaps the slider to 5 % steps: finer than anyone can tell apart on a home screen. */
private const val OPACITY_STEP = 5

private val WidgetStyle.label: StringResource
    get() = when (this) {
        WidgetStyle.SYSTEM -> Res.string.settings_widget_style_system
        WidgetStyle.LIGHT -> Res.string.settings_widget_style_light
        WidgetStyle.DARK -> Res.string.settings_widget_style_dark
        WidgetStyle.MAPLE -> Res.string.settings_widget_style_maple
    }

/** Roughly the wide widget, drawn with the same colours as the real one (`CountdownWidget` on Android). */
@Composable
private fun WidgetPreview(style: WidgetStyle, opacity: Int, dashboard: Dashboard) {
    val colors = style.colors(night = isSystemInDarkTheme())
    val description = stringResource(Res.string.settings_widget_preview)
    val eligible = dashboard.showsCelebration
    val left = dashboard.daysLeft
    Box(
        Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(Brush.linearGradient(WALLPAPER))
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .widthIn(max = 320.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(colors.background.withOpacity(opacity)))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MapleLeafProgress(
                progress = dashboard.progress,
                contentDescription = "",
                modifier = Modifier.size(72.dp),
                animate = false,
                colors = LeafColors(
                    fill = Color(colors.fill),
                    empty = Color(colors.leafEmpty),
                    track = Color(colors.track),
                    done = Color(colors.done),
                ),
            )
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    if (eligible) "🎉" else left?.let(::formatNumber).orEmpty(),
                    color = Color(colors.number),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    when {
                        eligible -> stringResource(Res.string.home_eligible_title)
                        left != null -> pluralStringResource(Res.plurals.home_days_to_go, left)
                        else -> ""
                    },
                    color = Color(colors.ink),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(
                        Res.string.home_counted,
                        formatNumber(dashboard.creditedDays.coerceAtMost(CitizenshipRules.REQUIRED_DAYS)),
                        formatNumber(CitizenshipRules.REQUIRED_DAYS),
                    ),
                    color = Color(colors.accent),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** A stand-in wallpaper, busy enough that a see-through widget shows. */
private val WALLPAPER = listOf(Color(0xFF2F5DA8), Color(0xFF4FA3A5), Color(0xFFF2C57C), Color(0xFFE76F51))
