package io.github.mirhamedjz.ehligible.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider as GlanceColor
import io.github.mirhamedjz.ehligible.EhligibleApp
import io.github.mirhamedjz.ehligible.MainActivity
import io.github.mirhamedjz.ehligible.app.platform.countryName
import io.github.mirhamedjz.ehligible.app.platform.flagEmoji
import io.github.mirhamedjz.ehligible.app.platform.formatDate
import io.github.mirhamedjz.ehligible.app.platform.formatNumber
import io.github.mirhamedjz.ehligible.app.resources.Res
import io.github.mirhamedjz.ehligible.app.resources.home_apply_from
import io.github.mirhamedjz.ehligible.app.resources.home_counted
import io.github.mirhamedjz.ehligible.app.resources.home_days_to_go
import io.github.mirhamedjz.ehligible.app.resources.home_eligible_title
import io.github.mirhamedjz.ehligible.app.resources.home_review_banner
import io.github.mirhamedjz.ehligible.app.resources.home_safer_date
import io.github.mirhamedjz.ehligible.app.resources.status_abroad
import io.github.mirhamedjz.ehligible.app.resources.status_abroad_unknown
import io.github.mirhamedjz.ehligible.app.resources.status_in_canada
import io.github.mirhamedjz.ehligible.app.resources.status_near_border
import io.github.mirhamedjz.ehligible.app.resources.status_paused
import io.github.mirhamedjz.ehligible.app.resources.status_unknown
import io.github.mirhamedjz.ehligible.app.resources.widget_setup
import io.github.mirhamedjz.ehligible.core.model.AppData
import io.github.mirhamedjz.ehligible.core.model.CountdownTarget
import io.github.mirhamedjz.ehligible.core.model.CurrentStatus
import io.github.mirhamedjz.ehligible.core.model.Dashboard
import io.github.mirhamedjz.ehligible.core.model.Settings
import io.github.mirhamedjz.ehligible.core.model.WidgetColors
import io.github.mirhamedjz.ehligible.core.model.dashboard
import io.github.mirhamedjz.ehligible.core.model.withOpacity
import io.github.mirhamedjz.ehligible.core.rules.CitizenshipRules
import kotlin.time.Clock
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

/** Everything the widget shows, prepared before composition (Glance can't call suspend functions while composing). */
private data class WidgetState(
    val number: String?,
    val numberLabel: String,
    val dateLine: String?,
    val statusLine: String?,
    val countedLine: String?,
    val reviewLine: String?,
    val progress: Float,
    val leaf: Bitmap,
    val eligible: Boolean,
    val look: WidgetLook,
)

/**
 * The colours from Settings. Glance picks the day or night value itself when the phone switches modes; only the
 * leaf bitmap is drawn for the mode at refresh time.
 */
private class WidgetLook(private val day: WidgetColors, private val night: WidgetColors, private val opacity: Int) {
    fun color(pick: (WidgetColors) -> Long) = ColorProvider(Color(pick(day)), Color(pick(night)))

    val background = color { it.background.withOpacity(opacity) }
}

class CountdownWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = (context.applicationContext as EhligibleApp).graph.repository
        val initial = widgetState(context, repository.current())
        provideContent {
            // Glance keeps a session open for a while after drawing, and `updateAll` during it only recomposes:
            // follow the saved data so a change made in the app shows right away, not after the next refresh.
            val state by produceState(initial) {
                repository.data.drop(1).collectLatest { value = widgetState(context, it) }
            }
            Content(state)
        }
    }

    private suspend fun widgetState(context: Context, data: AppData): WidgetState {
        val now = Clock.System.now()
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        return widgetState(context, data.dashboard(today, now), data.settings)
    }

    private suspend fun widgetState(context: Context, dashboard: Dashboard?, settings: Settings): WidgetState {
        val night =
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        val look = WidgetLook(
            day = settings.widgetStyle.colors(night = false),
            night = settings.widgetStyle.colors(night = true),
            opacity = settings.widgetOpacity,
        )
        val colors = settings.widgetStyle.colors(night)
        val progress = dashboard?.progress ?: 0f
        val leaf = LeafBitmap.render(
            sizePx = 240,
            progress = progress,
            filled = colors.fill.toInt(),
            empty = colors.leafEmpty.toInt(),
            ring = (if (progress >= 1f) colors.done else colors.fill).toInt(),
            ringTrack = colors.track.toInt(),
        )
        if (dashboard == null) {
            return WidgetState(
                null,
                getString(
                    Res.string.widget_setup,
                ),
                null, null, null, null, 0f, leaf, eligible = false, look = look,
            )
        }
        val left = dashboard.daysLeft
        val eligible = dashboard.showsCelebration
        val dateLine = dashboard.countdownDate?.let {
            if (dashboard.target ==
                CountdownTarget.SAFER
            ) {
                getString(Res.string.home_safer_date, formatDate(it))
            } else {
                getString(Res.string.home_apply_from, formatDate(it))
            }
        }
        val statusLine = when (val status = dashboard.currentStatus) {
            CurrentStatus.InCanada -> "🇨🇦 " + getString(Res.string.status_in_canada)

            is CurrentStatus.Abroad -> {
                val where =
                    status.country?.let { getString(Res.string.status_abroad, countryName(it)) }
                        ?: getString(Res.string.status_abroad_unknown)
                "${status.country?.let(::flagEmoji)?.ifBlank {
                    null
                } ?: "✈️"} $where · ${getString(Res.string.status_paused)}"
            }

            CurrentStatus.NearBorder -> "🧭 " + getString(Res.string.status_near_border)

            CurrentStatus.Unknown -> "📍 " + getString(Res.string.status_unknown)
        }
        val review = dashboard.reviewDays.size.takeIf { it > 0 }?.let {
            "⚠️ " +
                getPluralString(Res.plurals.home_review_banner, it, it)
        }
        return WidgetState(
            number = if (eligible) "🎉" else left?.let(::formatNumber),
            numberLabel = if (eligible) {
                getString(Res.string.home_eligible_title)
            } else {
                left?.let {
                    getPluralString(Res.plurals.home_days_to_go, it)
                }.orEmpty()
            },
            dateLine = if (eligible) null else dateLine,
            statusLine = statusLine,
            countedLine = getString(
                Res.string.home_counted,
                formatNumber(dashboard.creditedDays.coerceAtMost(CitizenshipRules.REQUIRED_DAYS)),
                formatNumber(CitizenshipRules.REQUIRED_DAYS),
            ),
            reviewLine = review,
            progress = progress,
            leaf = leaf,
            eligible = eligible,
            look = look,
        )
    }

    @Composable
    private fun Content(state: WidgetState) {
        val size = LocalSize.current
        Box(
            GlanceModifier.fillMaxSize().cornerRadius(
                24.dp,
            ).background(state.look.background).clickable(actionStartActivity<MainActivity>()).padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                size.width < WIDE.width -> Small(state)
                size.height < LARGE.height -> Wide(state)
                else -> Large(state)
            }
        }
    }

    @Composable
    private fun Small(state: WidgetState) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
            Image(ImageProvider(state.leaf), contentDescription = null, modifier = GlanceModifier.size(76.dp))
            Number(state, 32)
            Text(state.numberLabel, style = label(state, 13), maxLines = 2)
        }
    }

    @Composable
    private fun Wide(state: WidgetState) {
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Image(ImageProvider(state.leaf), contentDescription = null, modifier = GlanceModifier.size(84.dp))
            Spacer(GlanceModifier.width(12.dp))
            Column {
                Number(state, 34)
                Text(state.numberLabel, style = label(state, 13), maxLines = 1)
                state.dateLine?.let { Text(it, style = label(state, 12), maxLines = 1) }
                (state.reviewLine ?: state.statusLine)?.let {
                    Text(it, style = label(state, 11, state.look.color(WidgetColors::accent)), maxLines = 1)
                }
            }
        }
    }

    @Composable
    private fun Large(state: WidgetState) {
        Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Wide(state)
            if (state.number != null && !state.eligible) {
                Spacer(GlanceModifier.height(8.dp))
                LinearProgressIndicator(
                    progress = state.progress,
                    modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                    color = state.look.color(WidgetColors::fill),
                    backgroundColor = state.look.color(WidgetColors::track),
                )
                state.countedLine?.let {
                    Text(it, style = label(state, 11), modifier = GlanceModifier.padding(top = 4.dp))
                }
                if (state.reviewLine != null) state.statusLine?.let { Text(it, style = label(state, 11), maxLines = 1) }
            }
        }
    }

    @Composable
    private fun Number(state: WidgetState, sizeSp: Int) {
        state.number?.let {
            Text(
                it,
                style = TextStyle(
                    color = state.look.color(WidgetColors::number),
                    fontSize = sizeSp.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }

    private fun label(state: WidgetState, sizeSp: Int, color: GlanceColor = state.look.color(WidgetColors::ink)) =
        TextStyle(color = color, fontSize = sizeSp.sp, textAlign = TextAlign.Start)

    companion object {
        private val SMALL = DpSize(110.dp, 110.dp)
        private val WIDE = DpSize(250.dp, 110.dp)
        private val LARGE = DpSize(250.dp, 190.dp)

        suspend fun refreshAll(context: Context) {
            CountdownWidget().updateAll(context)
        }

        fun scheduleMidnightRefresh(context: Context) = MidnightRefreshWorker.schedule(context)
    }
}

class CountdownWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CountdownWidget()
}
