package app.tarkeez

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MoreScreen() {
    val context = LocalContext.current
    val hasUsage = remember { UsageReport.hasAccess(context) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "المزيد",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        // Telegram channel
        MoreCard {
            Text("قناتنا على تيليجرام", style = MaterialTheme.typography.titleMedium)
            Text(
                "تابع جديد التطبيقات والتحديثات.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Button(onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TELEGRAM_URL)))
                } catch (e: ActivityNotFoundException) {
                    // No app can open the link.
                }
            }) { Text("فتح القناة") }
        }
        Spacer(Modifier.height(16.dp))

        // Daily report
        MoreCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("التقرير اليومي", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "إشعار في آخر اليوم بوقت استخدام الهاتف وترتيب أكثر التطبيقات.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Switch(
                    checked = BlockRepository.reportEnabled,
                    onCheckedChange = { BlockRepository.setReportEnabled(it) }
                )
            }
            if (BlockRepository.reportEnabled) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "وقت إرسال التقرير",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(20, 21, 22, 23)) { h ->
                        FilterChip(
                            selected = BlockRepository.reportHour == h,
                            onClick = { BlockRepository.setReportHour(h) },
                            label = { Text("${h - 12} م") }
                        )
                    }
                }
                if (!hasUsage) {
                    Text(
                        "يحتاج التقرير إلى صلاحية «الوصول إلى بيانات الاستخدام» — امنحها من تبويب الإحصاءات.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // Reliability tips
        MoreCard {
            Text("حتى يعمل التطبيق بثبات", style = MaterialTheme.typography.titleMedium)
            Text(
                "بعض الهواتف تغلق التطبيقات في الخلفية لتوفير البطارية. استثنِ «تركيز» من توفير البطارية، وفعّل له التشغيل التلقائي والإشعارات.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }) { Text("البطارية") }
                OutlinedButton(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.parse("package:${context.packageName}"))
                    )
                }) { Text("إعدادات التطبيق") }
            }
        }
        Spacer(Modifier.height(16.dp))

        // About
        MoreCard {
            Text("عن التطبيق", style = MaterialTheme.typography.titleMedium)
            Text(
                "«تركيز» يعمل بالكامل على هاتفك، بلا إنترنت وبلا جمع أي بيانات. " +
                    "مبني على المشروع المفتوح AppBlock برخصة MIT.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun MoreCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}
