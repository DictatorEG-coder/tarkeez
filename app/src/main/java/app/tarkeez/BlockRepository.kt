package app.tarkeez

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.time.ZoneId
import java.time.LocalDate

/**
 * Single source of truth for the blocking setup. Backed by SharedPreferences so
 * the accessibility service and the UI (separate lifecycles, same process) always
 * agree, and exposed as Compose snapshot state so the UI recomposes on change.
 */
object BlockRepository {

    private const val PREFS_NAME = "tarkeez"
    private const val KEY_SCHEDULES = "schedules_json"
    private const val KEY_QUICK_UNTIL = "quick_until"
    private const val KEY_STRICT_UNTIL = "strict_until"
    private const val KEY_ATTEMPTS_TOTAL = "attempts_total"
    private const val KEY_ATTEMPTS_DAY = "attempts_day"
    private const val KEY_ATTEMPTS_DAY_COUNT = "attempts_day_count"

    private const val KEY_QUICK_SCHEDULE = "quick_schedule"
    private const val KEY_SESSION_START = "session_start"
    private const val KEY_SESSION_LABEL = "session_label"
    private const val KEY_SESSIONS = "sessions_json"
    private const val KEY_REPORT_ON = "report_on"
    private const val KEY_REPORT_HOUR = "report_hour"
    private const val MAX_SESSIONS = 300

    private const val KEY_BLOCK_YT_SHORTS = "block_yt_shorts"
    private const val KEY_BLOCK_YT_MUSIC = "block_yt_music"

    // v1 storage (flat app/site sets) — migrated into an "Always On" schedule.
    private const val KEY_LEGACY_APPS = "blocked_apps"
    private const val KEY_LEGACY_SITES = "blocked_sites"

    private lateinit var prefs: SharedPreferences
    private lateinit var appContext: Context

    private var quickScheduleId: String? = null
    private var sessionStart = 0L
    private var sessionLabel = ""

    var sessions by mutableStateOf<List<FocusSession>>(emptyList())
        private set
    var reportEnabled by mutableStateOf(true)
        private set
    var reportHour by mutableIntStateOf(22)
        private set

    var schedules by mutableStateOf<List<Schedule>>(emptyList())
        private set
    var quickBlockUntil by mutableLongStateOf(0L)
        private set
    var strictUntil by mutableLongStateOf(0L)
        private set
    var blockedToday by mutableIntStateOf(0)
        private set
    var blockedTotal by mutableIntStateOf(0)
        private set
    var blockYtShorts by mutableStateOf(false)
        private set
    var blockYtMusic by mutableStateOf(false)
        private set

    val isStrict: Boolean get() = System.currentTimeMillis() < strictUntil
    val isQuickBlocking: Boolean get() = System.currentTimeMillis() < quickBlockUntil

    @Synchronized
    fun init(context: Context) {
        if (::prefs.isInitialized) return
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        migrateLegacyLists()
        schedules = loadSchedules()
        quickBlockUntil = prefs.getLong(KEY_QUICK_UNTIL, 0L)
        strictUntil = prefs.getLong(KEY_STRICT_UNTIL, 0L)
        blockedTotal = prefs.getInt(KEY_ATTEMPTS_TOTAL, 0)
        blockedToday = if (prefs.getLong(KEY_ATTEMPTS_DAY, 0L) == LocalDate.now().toEpochDay()) {
            prefs.getInt(KEY_ATTEMPTS_DAY_COUNT, 0)
        } else 0
        blockYtShorts = prefs.getBoolean(KEY_BLOCK_YT_SHORTS, false)
        blockYtMusic = prefs.getBoolean(KEY_BLOCK_YT_MUSIC, false)
        quickScheduleId = prefs.getString(KEY_QUICK_SCHEDULE, null)
        sessionStart = prefs.getLong(KEY_SESSION_START, 0L)
        sessionLabel = prefs.getString(KEY_SESSION_LABEL, "") ?: ""
        sessions = loadSessions()
        reportEnabled = prefs.getBoolean(KEY_REPORT_ON, true)
        reportHour = prefs.getInt(KEY_REPORT_HOUR, 22)
        finishExpiredSession()
    }

    // ---- Daily report settings ----

    fun setReportEnabled(value: Boolean) {
        reportEnabled = value
        prefs.edit().putBoolean(KEY_REPORT_ON, value).apply()
        if (value) Reminders.scheduleDailyReport(appContext) else Reminders.cancelDailyReport(appContext)
    }

    fun setReportHour(hour: Int) {
        reportHour = hour
        prefs.edit().putInt(KEY_REPORT_HOUR, hour).apply()
        if (reportEnabled) Reminders.scheduleDailyReport(appContext)
    }

    // ---- Focus session log ----

    private fun loadSessions(): List<FocusSession> {
        val raw = prefs.getString(KEY_SESSIONS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { FocusSession.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun appendSession(session: FocusSession) {
        val list = (sessions + session).takeLast(MAX_SESSIONS)
        sessions = list
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_SESSIONS, arr.toString()).apply()
    }

    /** Closes the running focus session and writes it to the log. */
    private fun finishSession(endAt: Long, completed: Boolean) {
        if (sessionStart <= 0L) {
            quickBlockUntil = 0L
            prefs.edit().putLong(KEY_QUICK_UNTIL, 0L).apply()
            return
        }
        val planned = (quickBlockUntil - sessionStart).coerceAtLeast(0L)
        val achieved = (endAt - sessionStart).coerceAtLeast(0L)
        if (achieved >= 10_000L) {
            appendSession(
                FocusSession(
                    startMs = sessionStart,
                    endMs = sessionStart + achieved,
                    plannedMs = planned,
                    completed = completed,
                    label = sessionLabel,
                )
            )
        }
        sessionStart = 0L
        sessionLabel = ""
        quickBlockUntil = 0L
        prefs.edit()
            .putLong(KEY_SESSION_START, 0L)
            .putString(KEY_SESSION_LABEL, "")
            .putLong(KEY_QUICK_UNTIL, 0L)
            .apply()
    }

    /** Logs a session whose time ran out while the app was closed. Safe to call any time. */
    @Synchronized
    fun finishExpiredSession() {
        if (sessionStart > 0L && quickBlockUntil > 0L && System.currentTimeMillis() >= quickBlockUntil) {
            finishSession(quickBlockUntil, completed = true)
        }
    }

    fun focusMsToday(): Long {
        val zone = ZoneId.systemDefault()
        val dayStart = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
        return sessions.filter { it.endMs >= dayStart }.sumOf { it.achievedMs(dayStart) }
    }

    fun focusMsTotal(): Long = sessions.sumOf { it.achievedMs(0L) }

    fun currentQuickScheduleId(): String? = quickScheduleId

    /** Content blocks can always be turned on; turning OFF is refused under Strict Mode. */
    fun setBlockYtShorts(value: Boolean): Boolean {
        if (!value && isStrict) return false
        blockYtShorts = value
        prefs.edit().putBoolean(KEY_BLOCK_YT_SHORTS, value).apply()
        return true
    }

    fun setBlockYtMusic(value: Boolean): Boolean {
        if (!value && isStrict) return false
        blockYtMusic = value
        prefs.edit().putBoolean(KEY_BLOCK_YT_MUSIC, value).apply()
        return true
    }

    private fun migrateLegacyLists() {
        if (prefs.contains(KEY_SCHEDULES)) return
        val apps = prefs.getStringSet(KEY_LEGACY_APPS, emptySet())!!
        val sites = prefs.getStringSet(KEY_LEGACY_SITES, emptySet())!!
        if (apps.isEmpty() && sites.isEmpty()) return
        val always = Schedule(
            name = "تشغيل دائم",
            emoji = "🛡️",
            apps = apps.toSet(),
            sites = sites.toSet(),
        )
        prefs.edit()
            .putString(KEY_SCHEDULES, JSONArray().put(always.toJson()).toString())
            .remove(KEY_LEGACY_APPS)
            .remove(KEY_LEGACY_SITES)
            .apply()
    }

    private fun loadSchedules(): List<Schedule> {
        val raw = prefs.getString(KEY_SCHEDULES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { Schedule.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun persistSchedules(list: List<Schedule>) {
        schedules = list
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_SCHEDULES, arr.toString()).apply()
    }

    // ---- Mutations. Each returns false when Strict Mode forbids the change. ----

    fun upsertSchedule(s: Schedule): Boolean {
        val exists = schedules.any { it.id == s.id }
        if (exists && isStrict) return false
        persistSchedules(
            if (exists) schedules.map { if (it.id == s.id) s else it } else schedules + s
        )
        return true
    }

    fun deleteSchedule(id: String): Boolean {
        if (isStrict) return false
        persistSchedules(schedules.filterNot { it.id == id })
        return true
    }

    fun setScheduleEnabled(id: String, enabled: Boolean): Boolean {
        if (!enabled && isStrict) return false
        persistSchedules(schedules.map { if (it.id == id) it.copy(enabled = enabled) else it })
        return true
    }

    /**
     * Starts a focus session of [durationMs]. [scheduleId] picks which focus type's lists
     * are blocked; null blocks every list. Starting while a session runs extends it.
     */
    @Synchronized
    fun startQuickBlock(durationMs: Long, scheduleId: String? = null) {
        finishExpiredSession()
        val now = System.currentTimeMillis()
        if (quickBlockUntil > now && sessionStart > 0L) {
            quickBlockUntil += durationMs
        } else {
            sessionStart = now
            quickBlockUntil = now + durationMs
            quickScheduleId = scheduleId
            sessionLabel = schedules.firstOrNull { it.id == scheduleId }
                ?.let { "${it.emoji} ${it.name}" } ?: "كل القوائم"
        }
        prefs.edit()
            .putLong(KEY_QUICK_UNTIL, quickBlockUntil)
            .putLong(KEY_SESSION_START, sessionStart)
            .putString(KEY_SESSION_LABEL, sessionLabel)
            .putString(KEY_QUICK_SCHEDULE, quickScheduleId)
            .apply()
        Reminders.scheduleSessionEnd(appContext, quickBlockUntil)
    }

    @Synchronized
    fun stopQuickBlock(): Boolean {
        if (isStrict) return false
        finishSession(System.currentTimeMillis(), completed = false)
        Reminders.cancelSessionEnd(appContext)
        return true
    }

    /**
     * Starts the lock, or extends it by [durationMs] when already active.
     * Time only ever gets added — the lock can never be shortened.
     */
    fun activateStrict(durationMs: Long) {
        val base = maxOf(strictUntil, System.currentTimeMillis())
        strictUntil = base + durationMs
        prefs.edit().putLong(KEY_STRICT_UNTIL, strictUntil).apply()
    }

    fun recordBlocked() {
        val today = LocalDate.now().toEpochDay()
        blockedToday = if (prefs.getLong(KEY_ATTEMPTS_DAY, 0L) == today) blockedToday + 1 else 1
        blockedTotal += 1
        prefs.edit()
            .putLong(KEY_ATTEMPTS_DAY, today)
            .putInt(KEY_ATTEMPTS_DAY_COUNT, blockedToday)
            .putInt(KEY_ATTEMPTS_TOTAL, blockedTotal)
            .apply()
    }

    // ---- Queries used by the accessibility service. ----

    /** Lists used by the running focus session: the chosen focus type, or all lists. */
    private fun quickPool(): List<Schedule> {
        val chosen = schedules.filter { it.id == quickScheduleId }
        return if (chosen.isEmpty()) schedules else chosen
    }

    /** Human-readable reason ("جلسة التركيز" or the schedule name) or null if allowed. */
    fun blockReasonForApp(pkg: String): String? {
        if (isQuickBlocking && quickPool().any { pkg in it.apps }) return "جلسة التركيز"
        return schedules.firstOrNull { it.isActiveNow() && pkg in it.apps }
            ?.let { "${it.emoji} ${it.name}" }
    }

    fun blockReasonForSite(host: String): String? {
        val h = host.lowercase().removePrefix("www.")
        fun Set<String>.matches() = any { h == it || h.endsWith(".$it") }
        if (isQuickBlocking && quickPool().any { it.sites.matches() }) return "جلسة التركيز"
        return schedules.firstOrNull { it.isActiveNow() && it.sites.matches() }
            ?.let { "${it.emoji} ${it.name}" }
    }

    /** Turns user input like "https://www.YouTube.com/watch" into "youtube.com". */
    fun normalizeDomain(input: String): String? {
        var s = input.trim().lowercase()
        if (s.isEmpty()) return null
        s = s.substringAfter("://")
        s = s.substringBefore('/').substringBefore('?').substringBefore(':')
        s = s.removePrefix("www.")
        if (s.isEmpty() || !s.contains('.')) return null
        return s
    }
}
