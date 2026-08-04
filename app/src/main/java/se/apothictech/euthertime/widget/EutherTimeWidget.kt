package se.apothictech.euthertime.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.Button
import androidx.glance.action.ActionParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import se.apothictech.euthertime.alarm.AlarmScheduler
import se.apothictech.euthertime.alarm.AlarmStore
import java.text.DateFormat
import java.util.Date

class EutherTimeWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val next = AlarmStore.all(context).minByOrNull { it.triggerAtMillis }
        provideContent { WidgetContent(next?.label, next?.triggerAtMillis, next?.wakeSetId != null) }
    }
}

@Composable
private fun WidgetContent(label: String?, triggerAtMillis: Long?, isWakeSet: Boolean) {
    Column(
        modifier = GlanceModifier.fillMaxSize()
            .background(ColorProvider(Color(0xFF030806)))
            .padding(14.dp),
    ) {
        Text(
            "EUTHERTIME // NEXT SIGNAL",
            style = TextStyle(color = ColorProvider(Color(0xFFFFB000)), fontWeight = FontWeight.Bold),
        )
        Spacer(GlanceModifier.height(8.dp))
        if (label == null || triggerAtMillis == null) {
            Text("SYSTEM QUIET", style = TextStyle(color = ColorProvider(Color(0xFFB9FFE8))))
        } else {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Text(
                    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(triggerAtMillis)),
                    style = TextStyle(color = ColorProvider(Color(0xFF74FF63)), fontWeight = FontWeight.Bold),
                )
                Spacer(GlanceModifier.width(12.dp))
                Text(if (isWakeSet) "MORNING LINK" else "WAKE SIGNAL", style = TextStyle(color = ColorProvider(Color(0xFFFFB000))))
            }
            Text(label.uppercase(), style = TextStyle(color = ColorProvider(Color(0xFFB9FFE8))))
            Spacer(GlanceModifier.height(8.dp))
            Button("SKIP NEXT", onClick = actionRunCallback<SkipNextWidgetAction>())
        }
    }
}

class SkipNextWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        AlarmStore.all(context).minByOrNull { it.triggerAtMillis }?.let { AlarmScheduler.dismissOccurrence(context, it.id) }
        EutherTimeWidget().updateAll(context)
    }
}

class EutherTimeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EutherTimeWidget()
}

object AlarmWidgetUpdater {
    fun update(context: Context) {
        CoroutineScope(Dispatchers.Default).launch { EutherTimeWidget().updateAll(context) }
    }
}
