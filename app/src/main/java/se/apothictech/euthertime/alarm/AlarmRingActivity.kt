package se.apothictech.euthertime.alarm

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AlarmRingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val id = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        val alarm = AlarmStore.get(this, id)
        val label = alarm?.label ?: intent.getStringExtra(AlarmScheduler.EXTRA_LABEL) ?: "EutherTime"
        val kind = alarm?.kind ?: runCatching {
            AlarmKind.valueOf(intent.getStringExtra(AlarmScheduler.EXTRA_KIND).orEmpty())
        }.getOrDefault(AlarmKind.ALARM)
        val hasWakeSetCompanions = alarm?.let { AlarmScheduler.hasWakeSetCompanions(this, it) } == true

        setContent {
            RingScreen(
                label = label,
                kind = kind,
                stageRole = alarm?.stageRole ?: WakeStageRole.PRIMARY,
                isWakeSetStage = alarm?.wakeSetId != null,
                onDismiss = { complete(id, false) },
                onSnooze = { complete(id, true) },
                onDismissSet = { dismissWakeSet(id) },
                hasWakeSetCompanions = hasWakeSetCompanions,
            )
        }
    }

    private fun complete(id: Int, snooze: Boolean) {
        sendBroadcast(
            Intent(this, AlarmActionReceiver::class.java)
                .setAction(if (snooze) AlarmActionReceiver.ACTION_SNOOZE else AlarmActionReceiver.ACTION_DISMISS)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID, id),
        )
        setResult(Activity.RESULT_OK)
        finishAndRemoveTask()
    }

    private fun dismissWakeSet(id: Int) {
        sendBroadcast(
            Intent(this, AlarmActionReceiver::class.java)
                .setAction(AlarmActionReceiver.ACTION_CANCEL_WAKE_SET)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID, id),
        )
        setResult(Activity.RESULT_OK)
        finishAndRemoveTask()
    }
}

@Composable
private fun RingScreen(
    label: String,
    kind: AlarmKind,
    stageRole: WakeStageRole,
    isWakeSetStage: Boolean,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
    onDismissSet: () -> Unit,
    hasWakeSetCompanions: Boolean,
) {
    val green = Color(0xFF74FF63)
    val amber = Color(0xFFFFB000)
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF030806)).padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = when (kind) {
                    AlarmKind.EGG -> "EGG PROTOCOL"
                    AlarmKind.TIMER -> "COUNTDOWN COMPLETE"
                    AlarmKind.ALARM -> if (isWakeSetStage) "${stageRole.name} WAKE STAGE" else "WAKE PROTOCOL"
                },
                color = amber,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
            )
            Text(
                text = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
                color = green,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Light,
                fontSize = 86.sp,
                modifier = Modifier.padding(vertical = 24.dp),
            )
            Text(
                text = label.uppercase(),
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (kind == AlarmKind.EGG) "EXTRACTION REQUIRED" else "SIGNAL REQUIRES RESPONSE",
                color = green.copy(alpha = 0.7f),
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 8.dp, bottom = 40.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onSnooze,
                    colors = ButtonDefaults.buttonColors(containerColor = amber.copy(alpha = 0.22f), contentColor = amber),
                    modifier = Modifier.weight(1f),
                ) { Text("SNOOZE 05", fontFamily = FontFamily.Monospace) }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = green, contentColor = Color.Black),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        if (hasWakeSetCompanions) "NEXT SIGNAL" else "DISMISS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (hasWakeSetCompanions) {
                Button(
                    onClick = onDismissSet,
                    colors = ButtonDefaults.buttonColors(containerColor = green, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                ) {
                    Text("I'M UP · DISMISS SET", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
