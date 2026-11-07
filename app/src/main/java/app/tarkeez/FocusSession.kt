package app.tarkeez

import org.json.JSONObject

/** One finished focus session, kept in the log. */
data class FocusSession(
    val startMs: Long,
    val endMs: Long,
    val plannedMs: Long,
    val completed: Boolean,
    val label: String,
) {
    /** Time actually focused, counting only the part after [fromMs]. */
    fun achievedMs(fromMs: Long): Long = (endMs - maxOf(startMs, fromMs)).coerceAtLeast(0L)

    fun toJson(): JSONObject = JSONObject().apply {
        put("s", startMs)
        put("e", endMs)
        put("p", plannedMs)
        put("c", completed)
        put("l", label)
    }

    companion object {
        fun fromJson(o: JSONObject) = FocusSession(
            startMs = o.getLong("s"),
            endMs = o.getLong("e"),
            plannedMs = o.optLong("p", 0L),
            completed = o.optBoolean("c", false),
            label = o.optString("l", ""),
        )
    }
}
