package se.apothictech.euthertime

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import se.apothictech.euthertime.alarm.AlarmKind
import se.apothictech.euthertime.alarm.AlarmScheduler
import se.apothictech.euthertime.alarm.AlarmStore
import se.apothictech.euthertime.alarm.ScheduledAlarm
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

private val Void = Color(0xFF030806)
private val Panel = Color(0xFF07110D)
private val Toxic = Color(0xFF74FF63)
private val ToxicDim = Color(0xFF2C8F3D)
private val Amber = Color(0xFFFFB000)
private val Magenta = Color(0xFFFF3AA7)
private val Ice = Color(0xFFB9FFE8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { EutherTimeApp() }
    }
}

private enum class TimeTab(val title: String, val sigil: String) {
    CLOCK("CLOCK", "◷"),
    ALARMS("ALARMS", "△"),
    TIMER("TIMER", "⌛"),
    EGG("EGG", "◉"),
    CHRONO("CHRONO", "◇"),
}

@Composable
private fun EutherTimeApp() {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(TimeTab.CLOCK) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var revision by remember { mutableIntStateOf(0) }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) Toast.makeText(context, "Notification permission is needed for full-screen alarms", Toast.LENGTH_LONG).show()
    }

    LaunchedEffect(Unit) {
        AlarmStore.removeExpired(context)
        while (true) {
            now = System.currentTimeMillis()
            delay(250)
        }
    }

    fun arm(triggerAt: Long, label: String, kind: AlarmKind) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        runCatching { AlarmScheduler.create(context, triggerAt, label, kind) }
            .onSuccess {
                revision++
                Toast.makeText(context, "$label armed", Toast.LENGTH_SHORT).show()
            }
            .onFailure {
                Toast.makeText(context, "Could not arm signal: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    val alarms = remember(revision, now / 1_000L) {
        AlarmStore.all(context).filter { it.triggerAtMillis >= now }
    }

    val scheme = darkColorScheme(
        primary = Toxic,
        onPrimary = Void,
        secondary = Amber,
        tertiary = Magenta,
        background = Void,
        surface = Panel,
        onBackground = Ice,
        onSurface = Ice,
    )

    MaterialTheme(colorScheme = scheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = Void) {
            CyberBackground {
                Scaffold(
                    containerColor = Color.Transparent,
                    bottomBar = {
                        TimeNavigation(selectedTab) { selectedTab = it }
                    },
                    modifier = Modifier.safeDrawingPadding(),
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 18.dp),
                    ) {
                        SystemHeader(now, alarms.firstOrNull())
                        Box(modifier = Modifier.weight(1f)) {
                            when (selectedTab) {
                                TimeTab.CLOCK -> ClockScreen(now, alarms.firstOrNull())
                                TimeTab.ALARMS -> AlarmScreen(
                                    alarms = alarms.filter { it.kind == AlarmKind.ALARM },
                                    onArm = ::arm,
                                    onCancel = {
                                        AlarmScheduler.cancel(context, it.id)
                                        revision++
                                    },
                                )
                                TimeTab.TIMER -> TimerScreen(
                                    now = now,
                                    alarms = alarms.filter { it.kind == AlarmKind.TIMER },
                                    onArm = ::arm,
                                    onCancel = {
                                        AlarmScheduler.cancel(context, it.id)
                                        revision++
                                    },
                                )
                                TimeTab.EGG -> EggScreen(
                                    now = now,
                                    alarms = alarms.filter { it.kind == AlarmKind.EGG },
                                    onArm = ::arm,
                                    onCancel = {
                                        AlarmScheduler.cancel(context, it.id)
                                        revision++
                                    },
                                )
                                TimeTab.CHRONO -> ChronoScreen(now)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CyberBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0D2518), Void, Color.Black),
                    radius = 1_300f,
                ),
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            var y = 0f
            while (y < size.height) {
                drawLine(Color.White.copy(alpha = 0.018f), Offset(0f, y), Offset(size.width, y), 1f)
                y += 7f
            }
            drawCircle(Toxic.copy(alpha = 0.08f), radius = size.minDimension * 0.43f, style = Stroke(1.5f))
            drawCircle(Toxic.copy(alpha = 0.035f), radius = size.minDimension * 0.36f, style = Stroke(1f))
        }
        content()
    }
}

@Composable
private fun SystemHeader(now: Long, next: ScheduledAlarm?) {
    val batterylessStatus = if (next == null) "NO SIGNALS ARMED" else "NEXT // ${formatClock(next.triggerAtMillis)}"
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "EUTHER//TIME",
                color = Toxic,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
            Text(
                "CHRONOMETRIC SYSTEM 0.1",
                color = Toxic.copy(alpha = 0.48f),
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 1.sp,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                color = Ice,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
            )
            Text(batterylessStatus, color = Amber, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
        }
    }
    HorizontalDivider(color = Toxic.copy(alpha = 0.2f))
}

@Composable
private fun TimeNavigation(selected: TimeTab, onSelected: (TimeTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xF2030806))
            .border(1.dp, Toxic.copy(alpha = 0.22f))
            .padding(horizontal = 4.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        TimeTab.entries.forEach { tab ->
            val active = tab == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(if (active) Toxic.copy(alpha = 0.12f) else Color.Transparent, RoundedCornerShape(5.dp))
                    .clickable { onSelected(tab) }
                    .padding(vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(tab.sigil, color = if (active) Toxic else Ice.copy(alpha = 0.45f), fontSize = 18.sp)
                Text(
                    tab.title,
                    color = if (active) Toxic else Ice.copy(alpha = 0.45f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun ClockScreen(now: Long, next: ScheduledAlarm?) {
    val moment = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())
    val locale = LocalConfiguration.current.locales[0]
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("LOCAL NODE", color = Amber, fontFamily = FontFamily.Monospace, letterSpacing = 4.sp, fontSize = 11.sp)
        Text(
            moment.format(DateTimeFormatter.ofPattern("HH:mm")),
            color = Toxic,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraLight,
            fontSize = 82.sp,
            letterSpacing = (-5).sp,
        )
        Text(
            moment.format(DateTimeFormatter.ofPattern("ss.S")),
            color = Toxic.copy(alpha = 0.5f),
            fontFamily = FontFamily.Monospace,
            fontSize = 22.sp,
        )
        Text(
            moment.dayOfWeek.getDisplayName(TextStyle.FULL, locale).uppercase(locale),
            color = Ice,
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp,
            letterSpacing = 3.sp,
            modifier = Modifier.padding(top = 22.dp),
        )
        Text(
            moment.format(DateTimeFormatter.ofPattern("yyyy.MM.dd // VV")),
            color = Ice.copy(alpha = 0.48f),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(36.dp))
        CyberPanel(title = "UPCOMING TRANSMISSION", accent = if (next == null) ToxicDim else Amber) {
            if (next == null) {
                Text("SYSTEM QUIET", color = Ice.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace)
            } else {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(next.label.uppercase(), color = Ice, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text(next.kind.name, color = Amber, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    }
                    Text(formatClock(next.triggerAtMillis), color = Toxic, fontFamily = FontFamily.Monospace, fontSize = 25.sp)
                }
            }
        }
    }
}

@Composable
private fun AlarmScreen(
    alarms: List<ScheduledAlarm>,
    onArm: (Long, String, AlarmKind) -> Unit,
    onCancel: (ScheduledAlarm) -> Unit,
) {
    val context = LocalContext.current
    val current = LocalDateTime.now()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
        SectionTitle("WAKE PROTOCOLS", "Exact local alarms")
        PrimaryProtocolButton("+ ARM NEW WAKE SIGNAL") {
            TimePickerDialog(context, { _, hour, minute ->
                var target = LocalDateTime.now().withHour(hour).withMinute(minute).withSecond(0).withNano(0)
                if (!target.isAfter(LocalDateTime.now())) target = target.plusDays(1)
                onArm(target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), "Wake protocol", AlarmKind.ALARM)
            }, current.hour, current.minute, true).show()
        }
        Spacer(Modifier.height(18.dp))
        if (alarms.isEmpty()) EmptyState("NO WAKE SIGNALS ARMED")
        alarms.forEach { alarm -> ScheduledCard(alarm, System.currentTimeMillis(), onCancel) }
        InfoStrip("Alarm signals use Android's exact alarm clock channel and survive app closure.")
    }
}

@Composable
private fun TimerScreen(
    now: Long,
    alarms: List<ScheduledAlarm>,
    onArm: (Long, String, AlarmKind) -> Unit,
    onCancel: (ScheduledAlarm) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
        SectionTitle("COUNTDOWN MATRIX", "Fast deterministic timers")
        Text("QUICK PROTOCOLS", color = Amber, fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 2.sp)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(1, 3, 5, 10, 20, 45).forEach { minutes ->
                SmallProtocolButton("$minutes MIN") {
                    onArm(now + minutes * 60_000L, "$minutes minute timer", AlarmKind.TIMER)
                }
            }
        }
        alarms.forEach { alarm -> ScheduledCard(alarm, now, onCancel) }
        if (alarms.isEmpty()) EmptyState("NO COUNTDOWNS ACTIVE")
    }
}

private enum class EggState(val title: String, val seconds: Int, val detail: String) {
    SOFT("SOFT", 330, "Liquid gold // delicate white"),
    JAMMY("JAMMY", 390, "Custard core // ideal ramen state"),
    MEDIUM("MEDIUM", 480, "Set edge // soft center"),
    HARD("HARD", 630, "Fully set // maximum stability"),
}

private enum class EggSize(val adjustment: Int) {
    M(-20), L(0), XL(35),
}

@Composable
private fun EggScreen(
    now: Long,
    alarms: List<ScheduledAlarm>,
    onArm: (Long, String, AlarmKind) -> Unit,
    onCancel: (ScheduledAlarm) -> Unit,
) {
    var selected by remember { mutableStateOf(EggState.JAMMY) }
    var size by remember { mutableStateOf(EggSize.L) }
    var chilled by remember { mutableStateOf(true) }
    val seconds = selected.seconds + size.adjustment + if (chilled) 20 else 0

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
        SectionTitle("EGG PROTOCOL", "Precision ova transformation")
        CyberPanel(title = "TARGET CONSISTENCY", accent = Amber) {
            EggDiagram(selected)
            EggState.entries.forEach { state ->
                ChoiceRow(state.title, state.detail, selected == state) { selected = state }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EggSize.entries.forEach { option ->
                ChoiceChip(option.name, size == option, Modifier.weight(1f)) { size = option }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip("FRIDGE", chilled, Modifier.weight(1f)) { chilled = true }
            ChoiceChip("ROOM TEMP", !chilled, Modifier.weight(1f)) { chilled = false }
        }
        Text(
            "Start when the water reaches a steady boil.",
            color = Ice.copy(alpha = 0.55f),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        PrimaryProtocolButton("INITIATE // ${formatDuration(seconds * 1_000L)}", accent = Amber) {
            onArm(now + seconds * 1_000L, "${selected.title.lowercase().replaceFirstChar { it.uppercase() }} egg", AlarmKind.EGG)
        }
        alarms.forEach { alarm -> ScheduledCard(alarm, now, onCancel, Amber) }
    }
}

@Composable
private fun EggDiagram(state: EggState) {
    val yolk = when (state) {
        EggState.SOFT -> Color(0xFFFFC400)
        EggState.JAMMY -> Color(0xFFFFA000)
        EggState.MEDIUM -> Color(0xFFF28C00)
        EggState.HARD -> Color(0xFFE77A00)
    }
    Box(modifier = Modifier.fillMaxWidth().height(126.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(105.dp)) {
            drawOval(Ice.copy(alpha = 0.88f))
            drawCircle(yolk, radius = size.minDimension * 0.27f)
            drawCircle(Color.White.copy(alpha = 0.3f), radius = size.minDimension * 0.1f, center = Offset(size.width * 0.43f, size.height * 0.4f))
            drawOval(Toxic.copy(alpha = 0.65f), style = Stroke(width = 2f))
        }
        Text("${state.seconds / 60}:${(state.seconds % 60).toString().padStart(2, '0')}", color = Void, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ChronoScreen(now: Long) {
    var running by remember { mutableStateOf(false) }
    var startedAt by remember { mutableLongStateOf(0L) }
    var accumulated by remember { mutableLongStateOf(0L) }
    var laps by remember { mutableStateOf(emptyList<Long>()) }
    val elapsed = accumulated + if (running) now - startedAt else 0L

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SectionTitle("CHRONOGRAPH", "Monotonic session measurement")
        Spacer(Modifier.height(36.dp))
        Text(
            formatChrono(elapsed),
            color = Toxic,
            fontFamily = FontFamily.Monospace,
            fontSize = 48.sp,
            fontWeight = FontWeight.Light,
        )
        Text("ELAPSED // LOCAL", color = Toxic.copy(alpha = 0.45f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        Spacer(Modifier.height(34.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryProtocolButton(
                if (running) "PAUSE" else "START",
                modifier = Modifier.weight(1f),
            ) {
                if (running) {
                    accumulated += now - startedAt
                    running = false
                } else {
                    startedAt = now
                    running = true
                }
            }
            SmallProtocolButton("LAP", Modifier.weight(1f), enabled = elapsed > 0) { laps = listOf(elapsed) + laps }
            SmallProtocolButton("RESET", Modifier.weight(1f), enabled = elapsed > 0) {
                running = false
                accumulated = 0L
                laps = emptyList()
            }
        }
        laps.forEachIndexed { index, lap ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp)) {
                Text("MARK ${(laps.size - index).toString().padStart(2, '0')}", color = Amber, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                Text(formatChrono(lap), color = Ice, fontFamily = FontFamily.Monospace)
            }
            HorizontalDivider(color = Toxic.copy(alpha = 0.12f))
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Text(title, color = Toxic, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = 2.sp)
        Text("// $subtitle", color = Ice.copy(alpha = 0.48f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
    }
}

@Composable
private fun CyberPanel(title: String, accent: Color = Toxic, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel.copy(alpha = 0.88f)),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.44f)),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, color = accent, fontFamily = FontFamily.Monospace, fontSize = 9.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun ScheduledCard(alarm: ScheduledAlarm, now: Long, onCancel: (ScheduledAlarm) -> Unit, accent: Color = Toxic) {
    CyberPanel(title = alarm.kind.name, accent = accent) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(alarm.label.uppercase(), color = Ice, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Text(
                    if (alarm.kind == AlarmKind.ALARM) formatDateTime(alarm.triggerAtMillis) else "T-${formatDuration(alarm.triggerAtMillis - now)}",
                    color = accent,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                )
            }
            OutlinedButton(
                onClick = { onCancel(alarm) },
                border = BorderStroke(1.dp, Magenta.copy(alpha = 0.55f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Magenta),
                shape = RoundedCornerShape(3.dp),
            ) { Text("ABORT", fontFamily = FontFamily.Monospace, fontSize = 10.sp) }
        }
    }
    Spacer(Modifier.height(9.dp))
}

@Composable
private fun ChoiceRow(title: String, detail: String, selected: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) Toxic.copy(alpha = 0.13f) else Color.Transparent,
            contentColor = if (selected) Toxic else Ice.copy(alpha = 0.65f),
        ),
        border = BorderStroke(1.dp, if (selected) Toxic else Toxic.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(3.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(title, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text(detail, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Ice.copy(alpha = 0.45f))
        }
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) Toxic.copy(alpha = 0.16f) else Color.Transparent,
            contentColor = if (selected) Toxic else Ice.copy(alpha = 0.55f),
        ),
        border = BorderStroke(1.dp, if (selected) Toxic else Toxic.copy(alpha = 0.16f)),
        shape = RoundedCornerShape(3.dp),
        modifier = modifier,
    ) { Text(label, fontFamily = FontFamily.Monospace, fontSize = 11.sp) }
}

@Composable
private fun PrimaryProtocolButton(
    label: String,
    modifier: Modifier = Modifier,
    accent: Color = Toxic,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Void),
        shape = RoundedCornerShape(3.dp),
        modifier = modifier.fillMaxWidth().height(52.dp),
    ) { Text(label, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, letterSpacing = 1.sp) }
}

@Composable
private fun SmallProtocolButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Toxic),
        border = BorderStroke(1.dp, Toxic.copy(alpha = if (enabled) 0.5f else 0.12f)),
        shape = RoundedCornerShape(3.dp),
        modifier = modifier,
    ) { Text(label, fontFamily = FontFamily.Monospace, fontSize = 11.sp) }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().border(1.dp, Toxic.copy(alpha = 0.15f)).padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(message, color = Ice.copy(alpha = 0.35f), fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
    }
}

@Composable
private fun InfoStrip(message: String) {
    Text(
        "INFO // $message",
        color = Ice.copy(alpha = 0.4f),
        fontFamily = FontFamily.Monospace,
        fontSize = 9.sp,
        modifier = Modifier.padding(vertical = 16.dp),
    )
}

private fun formatClock(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))

private fun formatDateTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE HH:mm"))

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0L) / 1_000L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) "%02d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
}

private fun formatChrono(millis: Long): String {
    val minutes = millis / 60_000L
    val seconds = (millis % 60_000L) / 1_000L
    val hundredths = (millis % 1_000L) / 10L
    return "%02d:%02d.%02d".format(minutes, seconds, hundredths)
}
