package app.tarkeez

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DURATIONS = listOf(
    "ساعة" to 1L * 60 * 60 * 1000,
    "4 ساعات" to 4L * 60 * 60 * 1000,
    "24 ساعة" to 24L * 60 * 60 * 1000,
    "3 أيام" to 3L * 24 * 60 * 60 * 1000,
    "7 أيام" to 7L * 24 * 60 * 60 * 1000,
)

@Composable
fun StrictModeScreen() {
    val now = rememberNow()
    val active = BlockRepository.strictUntil > now
    var selected by remember { mutableIntStateOf(2) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "الوضع الصارم",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(32.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(150.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (active) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(64.dp)
                )
            }
        }
        Spacer(Modifier.height(24.dp))

        if (active) {
            Text(
                "مقفل",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "المتبقي " + formatDuration(BlockRepository.strictUntil - now),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Text(
                "ينتهي القفل " + Instant.ofEpochMilli(BlockRepository.strictUntil)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("EEEE d MMMM، HH:mm", java.util.Locale("ar"))),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                "لا يمكن إضعاف إعدادات الحظر حتى ينتهي المؤقت. تحتاج عزيمة أكثر؟ يمكنك تمديد القفل بالأسفل.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                "إعدادات الحظر غير مقفلة",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "أثناء القفل لا يمكن إيقاف القوائم ولا تعديلها ولا حذفها، ولا يمكن إنهاء جلسة التركيز قبل وقتها.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(Modifier.height(32.dp))
        Text(
            "مدة القفل",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DURATIONS.forEachIndexed { index, (label, _) ->
                FilterChip(
                    selected = selected == index,
                    onClick = { selected = index },
                    label = { Text(label) }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { BlockRepository.activateStrict(DURATIONS[selected].second) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(50)
        ) {
            Text(
                if (active) "تمديد القفل بمقدار ${DURATIONS[selected].first}" else "تفعيل القفل",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(Modifier.height(24.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                "تنبيه: الوضع الصارم يقفل التعديلات داخل «تركيز» فقط. إيقاف خدمة إمكانية الوصول أو حذف التطبيق يتجاوزه — فخط الدفاع الأخير هو أنت.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
