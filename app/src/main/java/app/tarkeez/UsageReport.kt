package app.tarkeez

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.time.LocalDate
import java.time.ZoneId

data class AppTime(val label: String, val ms: Long)
data class DayReport(val totalMs: Long, val top: List<AppTime>)

object UsageReport {

    fun hasAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Screen time so far today and the most used apps, or null without usage access. */
    fun today(context: Context, topN: Int = 5): DayReport? {
        if (!hasAccess(context)) return null
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val stats = usm.queryAndAggregateUsageStats(start, System.currentTimeMillis()).values
        val pm = context.packageManager
        val total = stats.sumOf { it.totalTimeInForeground }
        val top = stats
            .filter { it.totalTimeInForeground >= 60_000 && it.packageName != context.packageName }
            .sortedByDescending { it.totalTimeInForeground }
            .take(topN)
            .mapNotNull {
                try {
                    val info = pm.getApplicationInfo(it.packageName, 0)
                    AppTime(pm.getApplicationLabel(info).toString(), it.totalTimeInForeground)
                } catch (e: Exception) {
                    null
                }
            }
        return DayReport(total, top)
    }
}
