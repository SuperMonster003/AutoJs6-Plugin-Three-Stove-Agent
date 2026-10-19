package io.github.supermonster003.autojs6.plugin.three.stove.agent.speech

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.flag
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.json
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.jsonObject
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.number
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.string
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import java.util.Locale

/** Wake word sensitivity (roadmap D65, D75): the keyword trigger threshold handed to 3-Macaw Speech; medium keeps its default. */
internal enum class WakeSensitivity(val id: String, val threshold: Float?) {
    HIGH("high", 0.15f), MEDIUM("medium", null), LOW("low", 0.35f);
    companion object { fun of(id: String?) = entries.firstOrNull { it.id == id } ?: MEDIUM }
}

/** Wake words of the resident listener (roadmap D65, D75): one per line in settings, 1 to 5, each 1 to 32 characters. */
internal object ResidentKeywords {
    const val MAX_KEYWORDS = 5
    const val MAX_CHARACTERS = 32
    /** "Xiao lu xiao lu" (little stove) and "Hey Stove", the counterparts of the 3-Stone AI defaults (its roadmap D15). */
    val DEFAULT = listOf("\u5c0f\u7089\u5c0f\u7089", "Hey Stove")

    fun valid(keywords: List<String>) = keywords.size in 1..MAX_KEYWORDS &&
        keywords.map { it.lowercase(Locale.ROOT) }.distinct().size == keywords.size &&
        keywords.all { keyword ->
            keyword == keyword.trim() && keyword.codePointCount(0, keyword.length) in 1..MAX_CHARACTERS &&
                keyword.none { Character.isISOControl(it) || it.isSurrogate() }
        }

    /** The keywords of the settings field (one per line, blank lines ignored), or null when the list breaks a rule. */
    fun parse(text: String): List<String>? = text.lines().map { it.trim() }.filter { it.isNotEmpty() }.takeIf(::valid)
}

/** How long the resident listener stays on once turned on (roadmap D81, D82); it then turns its switch off. */
internal enum class ListeningDuration(val id: String, val minutes: Int?) {
    MINUTES_10("10", 10), MINUTES_30("30", 30), MINUTES_60("60", 60), ALWAYS("always", null);
    /** When a listener turned on at [now] (epoch milliseconds) turns off, or null when it stays on. */
    fun deadline(now: Long): Long? = minutes?.let { now + it * 60_000L }
    companion object { fun of(id: String?) = entries.firstOrNull { it.id == id } ?: ALWAYS }
}

/**
 * Preferences shared by the settings screen and the :agent service (D75, D82); the switch itself is the component state
 * (D73). [deadline] (epoch milliseconds, wall clock, so that it holds across a reboot) ends the current listening time:
 * turning the switch on, or choosing another [duration] while it is on, sets it; null keeps the listener on.
 */
internal data class ResidentSettings(
    val keywords: List<String> = ResidentKeywords.DEFAULT,
    val sensitivity: WakeSensitivity = WakeSensitivity.MEDIUM,
    val duration: ListeningDuration = ListeningDuration.ALWAYS,
    val chargingOnly: Boolean = false,
    val deadline: Long? = null,
) {
    init { require(ResidentKeywords.valid(keywords)); require(deadline == null || deadline > 0) }

    /** The listening time of a switch turned on at [now]. */
    fun turnedOn(now: Long) = copy(deadline = duration.deadline(now))

    /** Another duration: an enabled listener counts it from [now], a disabled one only remembers it for the next start. */
    fun withDuration(value: ListeningDuration, enabled: Boolean, now: Long) = copy(duration = value, deadline = if (enabled) value.deadline(now) else deadline)

    /** The listening time of an enabled listener is over. */
    fun expired(now: Long) = deadline != null && now >= deadline

    companion object {
        private const val VERSION = 2
        /** A missing, unreadable or invalid file means the defaults; a version 1 file (keywords and sensitivity) keeps both. */
        fun decode(value: JsonObject?): ResidentSettings = runCatching {
            val json = requireNotNull(value)
            val version = json.number("version")?.toInt()
            require(version == 1 || version == VERSION)
            val deadline = if (version == 1) null else json.number("deadline")
            ResidentSettings(json.getAsJsonArray("keywords").map { it.asString }, WakeSensitivity.of(json.string("sensitivity")),
                ListeningDuration.of(json.string("duration")), json.flag("chargingOnly") == true, deadline?.takeIf { it > 0 })
        }.getOrDefault(ResidentSettings())
        fun encode(value: ResidentSettings): JsonObject = jsonObject("version" to VERSION.json(),
            "keywords" to JsonArray().apply { value.keywords.forEach(::add) }, "sensitivity" to value.sensitivity.id.json(),
            "duration" to value.duration.id.json(), "chargingOnly" to value.chargingOnly.json()).apply {
            value.deadline?.let { add("deadline", it.json()) }
        }
    }
}

/**
 * What an enabled resident listener does now (D81, D82). Every start, every screen that resumes it (D67) and every power
 * change asks this first, so the listening time also ends while the service is not running.
 */
internal enum class ResidentGate {
    /** The listening time is over: turn the switch off and withdraw the ongoing notification; nothing resumes or reminds. */
    EXPIRED,
    /** Only while charging, and unplugged: no wake session (3-Macaw Speech stops recording) until the power comes back. */
    PAUSED,
    LISTEN;
    companion object {
        fun of(settings: ResidentSettings, now: Long, plugged: Boolean): ResidentGate = when {
            settings.expired(now) -> EXPIRED
            settings.chargingOnly && !plugged -> PAUSED
            else -> LISTEN
        }
    }
}

/** D67 with D81: what a visible screen, a reboot or an update does with the switch. */
internal enum class ResidentResume {
    NOTHING, TURN_OFF, RESUME;
    companion object {
        /** An enabled listener resumes (or, after a reboot or an update, reminds), unless its listening time ran out meanwhile. */
        fun of(enabled: Boolean, settings: ResidentSettings, now: Long): ResidentResume = when {
            !enabled -> NOTHING
            settings.expired(now) -> TURN_OFF
            else -> RESUME
        }
    }
}

/** What 3-Macaw Speech offers the resident listener, read by binding it (D74). */
internal data class MacawProbe(
    val installed: Boolean,
    val bound: Boolean = false,
    val contractVersion: Int? = null,
    val features: Set<String> = emptySet(),
    val microphoneGranted: Boolean = false,
    val wakeModels: Int = 0,
    val recognitionModels: Int = 0,
)

/** Why the resident listener switch is usable or not (D71, D74); the settings row names the first missing piece. */
internal enum class ResidentAvailability {
    MACAW_MISSING, MACAW_UNAVAILABLE, MACAW_INCOMPATIBLE, NO_WAKE_MODEL, NO_RECOGNITION_MODEL, MICROPHONE_DENIED, READY;
    companion object {
        fun of(probe: MacawProbe): ResidentAvailability = when {
            !probe.installed -> MACAW_MISSING
            !probe.bound -> MACAW_UNAVAILABLE
            probe.contractVersion == null || probe.contractVersion !in SpeechContract.MIN_CONTRACT_VERSION..SpeechContract.MAX_CONTRACT_VERSION ||
                SpeechContract.FEATURE_KWS !in probe.features || SpeechContract.FEATURE_ASR !in probe.features -> MACAW_INCOMPATIBLE
            probe.wakeModels == 0 -> NO_WAKE_MODEL
            probe.recognitionModels == 0 -> NO_RECOGNITION_MODEL
            !probe.microphoneGranted -> MICROPHONE_DENIED
            else -> READY
        }
    }
}

/** Why a running resident listener stopped (D76); the switch stays on and the next visible start retries. */
internal enum class ResidentStop {
    CONFLICT, UNAVAILABLE, INCOMPATIBLE, MICROPHONE, MODEL, KEYWORDS, BUSY, ENDED;
    companion object {
        /** A speech contract error code of an exception or an error event. */
        fun of(code: String?): ResidentStop = when (code) {
            SpeechErrorCodes.PERMISSION_DENIED -> MICROPHONE
            SpeechErrorCodes.MODEL_MISSING -> MODEL
            SpeechErrorCodes.INVALID_OPTIONS -> KEYWORDS
            SpeechErrorCodes.SESSION_BUSY -> BUSY
            SpeechErrorCodes.PLUGIN_INCOMPATIBLE -> INCOMPATIBLE
            SpeechErrorCodes.PLUGIN_UNAVAILABLE, SpeechErrorCodes.PLUGIN_DISABLED -> UNAVAILABLE
            else -> ENDED
        }
        /** The code of an exception thrown across Binder (`CODE: detail`), or null. */
        fun code(error: Throwable): String? = when (error) {
            is SecurityException -> SpeechErrorCodes.PLUGIN_UNAVAILABLE
            is android.os.RemoteException -> SpeechErrorCodes.PLUGIN_UNAVAILABLE
            else -> SpeechErrorCodes.decode(error.message)?.first
        }
    }
}

/** Where a voice draft goes (D62-D64, D77): the floating card, then the workbench when the app may open it, else a notification. */
internal enum class VoiceDraftRoute {
    CARD, WORKBENCH, NOTIFICATION;
    companion object {
        /** Code points of the draft the notification shows (D63). */
        const val PREVIEW_CODE_POINTS = 40
        fun of(cardShown: Boolean, unlocked: Boolean, backgroundStarts: Boolean) = when {
            cardShown -> CARD
            unlocked && backgroundStarts -> WORKBENCH
            else -> NOTIFICATION
        }
        /** The notification text: the draft on one line, cut after [PREVIEW_CODE_POINTS] code points with "...". */
        fun preview(text: String): String {
            val line = text.replace('\n', ' ')
            return if (line.codePointCount(0, line.length) <= PREVIEW_CODE_POINTS) line
            else line.substring(0, line.offsetByCodePoints(0, PREVIEW_CODE_POINTS)).trimEnd() + "..."
        }
    }
}

/** Another app whose resident listener is on (D73); [settings] is the activity its meta-data names, when valid. */
internal data class ResidentListenerApp(val packageName: String, val label: String, val settingsClass: String?)

internal object ResidentListenerApps {
    /** The other apps, one entry each, sorted by label; a relative class name (".Settings") is resolved against the package. */
    fun others(found: List<ResidentListenerApp>, ownPackage: String): List<ResidentListenerApp> = found
        .filter { it.packageName != ownPackage }
        .distinctBy { it.packageName }
        .map { app -> app.copy(settingsClass = app.settingsClass?.trim()?.let { name ->
            if (name.startsWith(".")) app.packageName + name else name
        }?.takeIf { it.matches(CLASS_NAME) }) }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    private val CLASS_NAME = Regex("[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)+")
}

/** The dictation round after a hit (D61, D75). */
internal object ResidentRound {
    const val SILENCE_END_MILLIS = 1200L
    const val MAX_DURATION_MILLIS = 30_000L
    /** No speech for this long after the hit ends the round without a draft. */
    const val TIMEOUT_MILLIS = 8000L

    /** Joins recognized pieces: a space only between ASCII text and an ASCII letter or digit, never around CJK text. */
    fun join(previous: String, next: String?): String {
        val tail = next?.trim().orEmpty()
        if (tail.isEmpty()) return previous
        if (previous.isEmpty()) return tail
        val last = previous.last(); val first = tail.first()
        val space = last.code < 0x80 && !last.isWhitespace() && first.code < 0x80 && first.isLetterOrDigit()
        return if (space) "$previous $tail" else previous + tail
    }

    /** The round's text: the end event's own text, else the joined finals; nothing for a cancelled or failed round. */
    fun draft(reason: String?, text: String?, finals: String): String? = when (reason) {
        SpeechContract.REASON_CANCELLED, SpeechContract.REASON_ERROR -> null
        else -> text?.takeIf { it.isNotBlank() } ?: finals.takeIf { it.isNotBlank() }
    }
}
