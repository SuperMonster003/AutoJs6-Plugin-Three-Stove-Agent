package io.github.supermonster003.autojs6.plugin.three.stove.agent.speech

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Roadmap D81 / D82 (P17.7): the listening time and "only while charging", and how they meet the D67 resume rules. */
class ResidentLimitsTest {
    private val now = 1_790_000_000_000L
    private val minute = 60_000L

    @Test
    fun durationsAreTenThirtySixtyMinutesOrAlwaysOnByDefault() {
        assertEquals(listOf("10", "30", "60", "always"), ListeningDuration.entries.map { it.id })
        assertEquals(now + 10 * minute, ListeningDuration.MINUTES_10.deadline(now))
        assertEquals(now + 30 * minute, ListeningDuration.MINUTES_30.deadline(now))
        assertEquals(now + 60 * minute, ListeningDuration.MINUTES_60.deadline(now))
        assertNull(ListeningDuration.ALWAYS.deadline(now))
        assertEquals(ListeningDuration.MINUTES_30, ListeningDuration.of("30"))
        assertEquals(ListeningDuration.ALWAYS, ListeningDuration.of("15"))
        assertEquals(ListeningDuration.ALWAYS, ListeningDuration.of(null))
        val defaults = ResidentSettings()
        assertEquals(ListeningDuration.ALWAYS, defaults.duration)
        assertFalse(defaults.chargingOnly)
        assertNull(defaults.deadline)
    }

    @Test
    fun turningOnStartsTheListeningTimeAndAnotherDurationCountsFromNowWhileOn() {
        val ten = ResidentSettings(duration = ListeningDuration.MINUTES_10)
        assertEquals(now + 10 * minute, ten.turnedOn(now).deadline)
        assertNull(ResidentSettings().turnedOn(now).deadline)
        // Turned on at now, changed to 60 minutes after 50: the listener stays another hour, not ten minutes.
        val on = ten.turnedOn(now)
        val changed = on.withDuration(ListeningDuration.MINUTES_60, enabled = true, now = now + 50 * minute)
        assertEquals(now + 110 * minute, changed.deadline)
        assertNull(on.withDuration(ListeningDuration.ALWAYS, enabled = true, now = now).deadline)
        // Off: the choice waits for the next turning on; the old deadline no longer matters.
        val off = on.withDuration(ListeningDuration.MINUTES_30, enabled = false, now = now + minute)
        assertEquals(ListeningDuration.MINUTES_30, off.duration)
        assertEquals(on.deadline, off.deadline)
        assertEquals(now + 2 * minute + 30 * minute, off.turnedOn(now + 2 * minute).deadline)
    }

    @Test
    fun theListeningTimeEndsTheListenerAtItsDeadline() {
        val on = ResidentSettings(duration = ListeningDuration.MINUTES_10).turnedOn(now)
        assertEquals(ResidentGate.LISTEN, ResidentGate.of(on, now + 10 * minute - 1, plugged = true))
        assertEquals(ResidentGate.EXPIRED, ResidentGate.of(on, now + 10 * minute, plugged = true))
        // The deadline wins over the power rule: an unplugged listener whose time ran out turns off rather than pausing.
        assertEquals(ResidentGate.EXPIRED, ResidentGate.of(on.copy(chargingOnly = true), now + 11 * minute, plugged = false))
        assertEquals(ResidentGate.LISTEN, ResidentGate.of(ResidentSettings().turnedOn(now), now + 1000 * minute, plugged = true))
    }

    @Test
    fun onlyWhileChargingPausesUnpluggedAndListensAgainWhenPluggedIn() {
        val charging = ResidentSettings(chargingOnly = true, duration = ListeningDuration.MINUTES_60).turnedOn(now)
        assertEquals(ResidentGate.LISTEN, ResidentGate.of(charging, now, plugged = true))
        assertEquals(ResidentGate.PAUSED, ResidentGate.of(charging, now + minute, plugged = false))
        assertEquals(ResidentGate.LISTEN, ResidentGate.of(charging, now + 2 * minute, plugged = true))
        // The listening time keeps counting while paused.
        assertEquals(ResidentGate.EXPIRED, ResidentGate.of(charging, now + 60 * minute, plugged = true))
        // Without the rule the power does not matter.
        assertEquals(ResidentGate.LISTEN, ResidentGate.of(charging.copy(chargingOnly = false), now + minute, plugged = false))
    }

    @Test
    fun visibleScreensRebootsAndUpdatesResumeOnlyWithinTheListeningTime() {
        val on = ResidentSettings(duration = ListeningDuration.MINUTES_30).turnedOn(now)
        // D67: an enabled listener resumes on a visible screen or reminds after a reboot or an update ...
        assertEquals(ResidentResume.RESUME, ResidentResume.of(true, on, now + 29 * minute))
        assertEquals(ResidentResume.RESUME, ResidentResume.of(true, ResidentSettings(), now))
        // ... unless its time ran out while nothing ran (a reboot in between): then the switch turns off, without a reminder.
        assertEquals(ResidentResume.TURN_OFF, ResidentResume.of(true, on, now + 30 * minute))
        // A disabled one does nothing, whatever it stored.
        assertEquals(ResidentResume.NOTHING, ResidentResume.of(false, on, now + 31 * minute))
        assertEquals(ResidentResume.NOTHING, ResidentResume.of(false, on, now))
        // Unplugged with "only while charging" it still resumes; the service then waits for the power.
        assertEquals(ResidentResume.RESUME, ResidentResume.of(true, on.copy(chargingOnly = true), now + minute))
    }

    @Test
    fun settingsFileKeepsTheLimitsAndReadsVersionOneFiles() {
        val value = ResidentSettings(listOf("Hey Stove"), WakeSensitivity.LOW, ListeningDuration.MINUTES_10, chargingOnly = true, deadline = now)
        val encoded = ResidentSettings.encode(value)
        assertEquals("""{"version":2,"keywords":["Hey Stove"],"sensitivity":"low","duration":"10","chargingOnly":true,"deadline":$now}""", encoded.toString())
        assertEquals(value, ResidentSettings.decode(JsonParser.parseString(encoded.toString()).asJsonObject))
        assertFalse(ResidentSettings.encode(ResidentSettings()).has("deadline"))
        // Files of 1.3.0 builds before D81 keep their keywords and sensitivity and get the defaults for the rest.
        val old = ResidentSettings.decode(JsonParser.parseString("""{"version":1,"keywords":["Hey Stove"],"sensitivity":"high","deadline":5}""").asJsonObject)
        assertEquals(ResidentSettings(listOf("Hey Stove"), WakeSensitivity.HIGH), old)
        // Unknown or broken values fall back field by field; a non-positive deadline means none.
        val odd = ResidentSettings.decode(JsonParser.parseString("""{"version":2,"keywords":["Hey Stove"],"duration":"15","chargingOnly":"yes","deadline":0}""").asJsonObject)
        assertEquals(ResidentSettings(listOf("Hey Stove")), odd)
        assertEquals(ResidentSettings(), ResidentSettings.decode(JsonParser.parseString("""{"version":3,"keywords":["Hey Stove"]}""").asJsonObject))
        assertTrue(ResidentSettings(deadline = now).expired(now))
        assertFalse(ResidentSettings(deadline = now).expired(now - 1))
        assertFalse(ResidentSettings().expired(Long.MAX_VALUE))
    }
}
