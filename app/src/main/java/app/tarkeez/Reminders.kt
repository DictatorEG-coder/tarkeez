package app.tarkeez

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.time.LocalDateTime
import java.time.ZoneId

/** Alarms and notifications: end-of-session message and the daily usage report. */
object Reminders {

    private const val CH_SESSION = "session_end"
    private const val CH_REPORT = "daily_report"
    private const val RC_SESSION = 1001
    private const val RC_REPORT = 1002
    private const val ID_SESSION = 2001
    private const val ID_REPORT = 2002

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_SESSION, "انتهاء الحظر", NotificationManager.IMPORTANCE_HIGH)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_REPORT, "التقرير اليومي", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    private fun pending(context: Context, code: Int, cls: Class<*>): PendingIntent =
        PendingIntent.getBroadcast(
            context, code, Intent(context, cls),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun setAlarm(context: Context, atMs: Long, code: Int, cls: Class<*>, exact: Boolean) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pending(context, code, cls)
        try {
            if (exact && (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms())) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi)
        }
    }

    fun scheduleSessionEnd(context: Context, atMs: Long) =
        setAlarm(context, atMs, RC_SESSION, SessionEndReceiver::class.java, exact = true)

    fun cancelSessionEnd(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(context, RC_SESSION, SessionEndReceiver::class.java))
    }

    fun scheduleDailyReport(context: Context) {
        val zone = ZoneId.systemDefault()
        var next = LocalDateTime.now().withHour(BlockRepository.reportHour).withMinute(0).withSecond(0).withNano(0)
        if (!next.isAfter(LocalDateTime.now())) next = next.plusDays(1)
        setAlarm(context, next.atZone(zone).toInstant().toEpochMilli(), RC_REPORT,
            DailyReportReceiver::class.java, exact = false)
    }

    fun cancelDailyReport(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(context, RC_REPORT, DailyReportReceiver::class.java))
    }

    private fun notify(context: Context, channel: String, id: Int, title: String, text: String, big: String?) {
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return
        ensureChannels(context)
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val b = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_tarkeez)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        if (big != null) b.setStyle(NotificationCompat.BigTextStyle().bigText(big))
        try {
            nm.notify(id, b.build())
        } catch (e: SecurityException) {
            // Notification permission was revoked meanwhile; nothing to do.
        }
    }

    fun notifySessionEnded(context: Context) {
        val focused = BlockRepository.sessions.lastOrNull()
        val text = if (focused != null) {
            "أنجزت ${formatDuration(focused.achievedMs(0L))} من التركيز. أحسنت."
        } else {
            "يمكنك الآن العودة إلى تطبيقاتك."
        }
        notify(context, CH_SESSION, ID_SESSION, "انتهى الحظر", text, null)
    }

    fun notifyDailyReport(context: Context) {
        val report = UsageReport.today(context)
        if (report == null) {
            notify(
                context, CH_REPORT, ID_REPORT, "تقرير اليوم",
                "فعّل «الوصول إلى بيانات الاستخدام» من التطبيق لتصلك تقارير الاستخدام.", null
            )
            return
        }
        val focus = BlockRepository.focusMsToday()
        val head = "استخدمت الهاتف اليوم ${formatDuration(report.totalMs)}"
        val lines = StringBuilder(head)
        if (report.top.isNotEmpty()) {
            lines.append("\n\nالأكثر استخدامًا:")
            report.top.forEachIndexed { i, a -> lines.append("\n${i + 1}. ${a.label} — ${formatDuration(a.ms)}") }
        }
        lines.append("\n\nتركيزك اليوم: ${formatDuration(focus)}")
        lines.append("\nمحاولات تم منعها: ${BlockRepository.blockedToday}")
        val short = report.top.firstOrNull()?.let { "$head · الأكثر: ${it.label}" } ?: head
        notify(context, CH_REPORT, ID_REPORT, "تقرير اليوم", short, lines.toString())
    }
}

class SessionEndReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        BlockRepository.init(context)
        BlockRepository.finishExpiredSession()
        // Announce only a session that really ran out just now (not a stale alarm of an
        // extended or stopped session). The app may already have logged it, so check the log.
        val last = BlockRepository.sessions.lastOrNull()
        if (!BlockRepository.isQuickBlocking && last != null && last.completed &&
            System.currentTimeMillis() - last.endMs < 5 * 60_000L
        ) {
            Reminders.notifySessionEnded(context)
        }
    }
}

class DailyReportReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        BlockRepository.init(context)
        if (BlockRepository.reportEnabled) {
            Reminders.notifyDailyReport(context)
            Reminders.scheduleDailyReport(context)
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        BlockRepository.init(context)
        if (BlockRepository.reportEnabled) Reminders.scheduleDailyReport(context)
        val until = BlockRepository.quickBlockUntil
        if (until > System.currentTimeMillis()) Reminders.scheduleSessionEnd(context, until)
    }
}
