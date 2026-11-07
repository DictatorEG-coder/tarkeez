package app.tarkeez

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private data class Template(
    val name: String,
    val emoji: String,
    val days: Set<Int>,
    val allDay: Boolean,
    val start: Int,
    val end: Int,
    val tagline: String,
)

private val TEMPLATES = listOf(
    Template("تركيز العمل", "🎯", (1..5).toSet(), false, 9 * 60, 17 * 60, "اجعل ساعات عملك أكثر إنتاجًا."),
    Template("وقت المذاكرة", "📚", (1..5).toSet(), false, 16 * 60, 21 * 60, "ذاكر بلا مشتّتات."),
    Template("قبل النوم", "🌙", (1..7).toSet(), false, 20 * 60, 23 * 60 + 59, "نم أسرع وأهدأ."),
    Template("يوم بلا هاتف", "🧘", setOf(7), true, 0, 0, "امنح نفسك يومًا بعيدًا عن الشاشة."),
)

@Composable
fun BlockingScreen(onEditSchedule: (Schedule) -> Unit) {
    val context = LocalContext.current
    var serviceEnabled by remember { mutableStateOf(isServiceEnabled(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) serviceEnabled = isServiceEnabled(context)
        })
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "تركيز",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            if (BlockRepository.blockedToday > 0) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Block,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${BlockRepository.blockedToday} اليوم",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        if (!serviceEnabled) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "الحظر متوقف",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        "فعّل خدمة «تركيز» من إعدادات إمكانية الوصول حتى يعمل الحظر.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Button(onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }) {
                        Text("فتح إعدادات إمكانية الوصول")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        QuickBlockCard()
        Spacer(Modifier.height(16.dp))
        ContentBlocksCard()
        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "القوائم",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { onEditSchedule(Schedule(name = "")) }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("إضافة")
            }
        }

        if (BlockRepository.schedules.isEmpty()) {
            Text(
                "لا توجد قوائم بعد. أضف واحدة أو ابدأ من قالب جاهز بالأسفل.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        BlockRepository.schedules.forEach { schedule ->
            ScheduleCard(schedule, onClick = { onEditSchedule(schedule) })
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "قوالب جاهزة",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            "اضغط على قالب لتعدّله على ذوقك.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(TEMPLATES) { t ->
                Card(
                    onClick = {
                        onEditSchedule(
                            Schedule(
                                name = t.name,
                                emoji = t.emoji,
                                days = t.days,
                                allDay = t.allDay,
                                startMinute = t.start,
                                endMinute = t.end,
                            )
                        )
                    },
                    modifier = Modifier.width(170.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(t.emoji, fontSize = 36.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(t.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (t.allDay) "طوال اليوم" else
                                "${Schedule.formatMinute(t.start)} – ${Schedule.formatMinute(t.end)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            t.tagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun QuickBlockCard() {
    val now = rememberNow()
    val active = BlockRepository.quickBlockUntil > now
    var selectedMinutes by remember { mutableIntStateOf(30) }
    var custom by remember { mutableStateOf(false) }
    var customText by remember { mutableStateOf("") }
    var typeId by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf<String?>(null) }
    val usable = BlockRepository.schedules.filter { it.apps.isNotEmpty() || it.sites.isNotEmpty() }
    val minutes = if (custom) customText.toIntOrNull() else selectedMinutes
    val minutesValid = minutes != null && minutes in 1..720

    LaunchedEffect(active) { if (!active) BlockRepository.finishExpiredSession() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "جلسة تركيز",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (active) {
                Text(
                    "متبقٍ " + formatDuration(BlockRepository.quickBlockUntil - now),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    "يتم الآن حظر تطبيقات ومواقع الجلسة. سيصلك إشعار عند انتهاء الحظر.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = {
                    note = if (BlockRepository.stopQuickBlock()) null
                    else "الوضع الصارم مفعّل — لا يمكن إيقاف الجلسة قبل انتهائها."
                }) {
                    Text("إيقاف")
                }
            } else {
                Text(
                    "اختر نوع التركيز والمدة، ثم ابدأ.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    "نوع التركيز",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = typeId == null,
                            onClick = { typeId = null },
                            label = { Text("الكل") }
                        )
                    }
                    items(usable, key = { it.id }) { sch ->
                        FilterChip(
                            selected = typeId == sch.id,
                            onClick = { typeId = sch.id },
                            label = { Text("${sch.emoji} ${sch.name}") }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "المدة",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(15, 30, 60, 120)) { min ->
                        FilterChip(
                            selected = !custom && selectedMinutes == min,
                            onClick = { custom = false; selectedMinutes = min },
                            label = { Text(if (min < 60) "${min} د" else "${min / 60} س") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = custom,
                            onClick = { custom = true },
                            label = { Text("مخصصة") }
                        )
                    }
                }
                if (custom) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { customText = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("عدد الدقائق (1 – 720)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        BlockRepository.startQuickBlock((minutes ?: 30) * 60_000L, typeId)
                        note = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(50),
                    enabled = usable.isNotEmpty() && minutesValid
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("ابدأ التركيز", style = MaterialTheme.typography.titleMedium)
                }
                if (usable.isEmpty()) {
                    Text(
                        "أضف تطبيقات أو مواقع إلى قائمة أولاً — الجلسة تحظر ما في القوائم.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            note?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ContentBlocksCard() {
    var note by remember { mutableStateOf<String?>(null) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "حظر المحتوى",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "مناطق محظورة داخل تطبيقات تسمح بها عادةً. تعمل دائمًا ليلًا ونهارًا.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
            ContentBlockRow(
                title = "يوتيوب شورتس",
                subtitle = "مشغّل الشورتس في التطبيق، وyoutube.com/shorts في المتصفح",
                checked = BlockRepository.blockYtShorts,
                onChange = { checked ->
                    note = if (BlockRepository.setBlockYtShorts(checked)) null
                    else "الوضع الصارم مفعّل — لا يمكن إيقاف حظر المحتوى."
                }
            )
            ContentBlockRow(
                title = "يوتيوب ميوزك",
                subtitle = "تطبيق يوتيوب ميوزك، وmusic.youtube.com في المتصفح",
                checked = BlockRepository.blockYtMusic,
                onChange = { checked ->
                    note = if (BlockRepository.setBlockYtMusic(checked)) null
                    else "الوضع الصارم مفعّل — لا يمكن إيقاف حظر المحتوى."
                }
            )
            note?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ContentBlockRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ScheduleCard(schedule: Schedule, onClick: () -> Unit) {
    var note by remember { mutableStateOf<String?>(null) }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (schedule.isActiveNow())
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(schedule.emoji, fontSize = 28.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(schedule.name, style = MaterialTheme.typography.titleMedium)
                        if (schedule.isActiveNow()) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    "نشطة",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        "${schedule.daysSummary()}، ${schedule.timeSummary()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${schedule.apps.size} تطبيق · ${schedule.sites.size} موقع",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = schedule.enabled,
                    onCheckedChange = { checked ->
                        note = if (BlockRepository.setScheduleEnabled(schedule.id, checked)) null
                        else "الوضع الصارم مفعّل — لا يمكن إيقاف القوائم."
                    }
                )
            }
            note?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
