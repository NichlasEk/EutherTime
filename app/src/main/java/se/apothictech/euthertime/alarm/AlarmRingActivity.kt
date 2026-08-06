package se.apothictech.euthertime.alarm

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AlarmRingActivity : ComponentActivity(), NfcAdapter.ReaderCallback {
    private var alarmId = -1
    private var nfcReleaseRequired = false
    private var nfcStatus by mutableStateOf("SCAN ENROLLED TAG TO RELEASE")
    private val nfcAdapter by lazy { NfcAdapter.getDefaultAdapter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val id = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        alarmId = id
        val alarm = AlarmStore.get(this, id)
        val label = alarm?.label ?: intent.getStringExtra(AlarmScheduler.EXTRA_LABEL) ?: "EutherTime"
        val kind = alarm?.kind ?: runCatching {
            AlarmKind.valueOf(intent.getStringExtra(AlarmScheduler.EXTRA_KIND).orEmpty())
        }.getOrDefault(AlarmKind.ALARM)
        val hasWakeSetCompanions = alarm?.let { AlarmScheduler.hasWakeSetCompanions(this, it) } == true
        nfcReleaseRequired = alarm?.nfcChallengeEnabled == true && NfcTagStore.isEnrolled(this)

        setContent {
            RingScreen(
                label = label,
                kind = kind,
                stageRole = alarm?.stageRole ?: WakeStageRole.PRIMARY,
                isWakeSetStage = alarm?.wakeSetId != null,
                onDismiss = {
                    if (nfcReleaseRequired && !hasWakeSetCompanions) {
                        nfcStatus = "NFC READER READY // PRESENT TAG"
                    } else {
                        complete(id, false)
                    }
                },
                onSnooze = { complete(id, true) },
                onDismissSet = {
                    if (nfcReleaseRequired) {
                        nfcStatus = "NFC READER READY // PRESENT TAG"
                    } else {
                        dismissWakeSet(id)
                    }
                },
                hasWakeSetCompanions = hasWakeSetCompanions,
                nfcReleaseRequired = nfcReleaseRequired,
                nfcStatus = nfcStatus,
                onEmergencyRelease = {
                    if (hasWakeSetCompanions) dismissWakeSet(id) else complete(id, false)
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (!nfcReleaseRequired) return
        when {
            nfcAdapter == null -> nfcStatus = "NO NFC READER // EMERGENCY RELEASE AVAILABLE"
            nfcAdapter?.isEnabled != true -> nfcStatus = "NFC DISABLED // EMERGENCY RELEASE AVAILABLE"
            else -> nfcAdapter?.enableReaderMode(this, this, NfcTagEnrollmentActivity.READER_FLAGS, null)
        }
    }

    override fun onPause() {
        nfcAdapter?.disableReaderMode(this)
        super.onPause()
    }

    override fun onTagDiscovered(tag: Tag) {
        val accepted = NfcTagStore.matches(this, tag.id)
        runOnUiThread {
            if (!accepted) {
                nfcStatus = "TAG REJECTED // TRY ENROLLED TAG"
                return@runOnUiThread
            }
            nfcStatus = "TAG ACCEPTED // MORNING LINK RELEASED"
            val alarm = AlarmStore.get(this, alarmId)
            if (alarm?.let { AlarmScheduler.hasWakeSetCompanions(this, it) } == true) {
                dismissWakeSet(alarmId)
            } else {
                complete(alarmId, false)
            }
        }
    }

    private fun complete(id: Int, snooze: Boolean) {
        ActiveAlarmStore.clear(this, id)
        sendBroadcast(
            Intent(this, AlarmActionReceiver::class.java)
                .setAction(if (snooze) AlarmActionReceiver.ACTION_SNOOZE else AlarmActionReceiver.ACTION_DISMISS)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID, id),
        )
        setResult(Activity.RESULT_OK)
        finishAndRemoveTask()
    }

    private fun dismissWakeSet(id: Int) {
        ActiveAlarmStore.clear(this, id)
        sendBroadcast(
            Intent(this, AlarmActionReceiver::class.java)
                .setAction(AlarmActionReceiver.ACTION_CLEAR_WAKE_SET_WITH_GUARD)
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
    nfcReleaseRequired: Boolean,
    nfcStatus: String,
    onEmergencyRelease: () -> Unit,
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
                        when {
                            hasWakeSetCompanions -> "NEXT SIGNAL"
                            nfcReleaseRequired -> "SCAN NFC"
                            else -> "DISMISS"
                        },
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
                    Text(
                        if (nfcReleaseRequired) "I'M UP · SCAN NFC" else "I'M UP · DISMISS SET",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (nfcReleaseRequired) {
                NfcEmergencyRelease(
                    status = nfcStatus,
                    onEmergencyRelease = onEmergencyRelease,
                )
            }
        }
    }
}

@Composable
private fun NfcEmergencyRelease(
    status: String,
    onEmergencyRelease: () -> Unit,
) {
    var deadline by rememberSaveable { mutableLongStateOf(0L) }
    var now by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(deadline) {
        while (deadline > 0L && now < deadline) {
            now = System.currentTimeMillis()
            delay(250L)
        }
    }
    val remainingSeconds = if (deadline == 0L) 30L else ((deadline - now).coerceAtLeast(0L) + 999L) / 1_000L

    Text(
        status,
        color = Color(0xFFFF3AA7),
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
    Button(
        onClick = {
            when {
                deadline == 0L -> {
                    now = System.currentTimeMillis()
                    deadline = now + 30_000L
                }
                now >= deadline -> onEmergencyRelease()
            }
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFF3AA7).copy(alpha = 0.18f),
            contentColor = Color(0xFFFF3AA7),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            when {
                deadline == 0L -> "LOST TAG? START 30s RELEASE"
                now < deadline -> "EMERGENCY RELEASE IN ${remainingSeconds}s"
                else -> "CONFIRM EMERGENCY RELEASE"
            },
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
        )
    }
}
