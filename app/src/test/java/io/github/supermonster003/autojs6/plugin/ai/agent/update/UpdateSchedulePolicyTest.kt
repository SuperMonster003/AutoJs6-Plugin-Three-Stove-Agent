package io.github.supermonster003.autojs6.plugin.ai.agent.update

import org.junit.Assert.*
import org.junit.Test

class UpdateSchedulePolicyTest {
    @Test fun successfulFetchIsReusedForTwentyFourHoursButClockRollbackRecovers() {
        assertTrue(UpdateSchedulePolicy.manualFetchDue(null, 0))
        assertFalse(UpdateSchedulePolicy.manualFetchDue(100, 100))
        assertFalse(UpdateSchedulePolicy.manualFetchDue(100, 100 + UpdateSchedulePolicy.INTERVAL_MS - 1))
        assertTrue(UpdateSchedulePolicy.manualFetchDue(100, 100 + UpdateSchedulePolicy.INTERVAL_MS))
        assertTrue(UpdateSchedulePolicy.manualFetchDue(100, 99))
    }
    @Test fun automaticChecksRequireOptInAndThrottleFailuresAsWellAsSuccesses() {
        assertFalse(UpdateSchedulePolicy.automaticFetchDue(false, null, 100))
        assertTrue(UpdateSchedulePolicy.automaticFetchDue(true, null, 100))
        assertFalse(UpdateSchedulePolicy.automaticFetchDue(true, 100, 101))
        assertFalse(UpdateSchedulePolicy.automaticFetchDue(true, 100, 100 + UpdateSchedulePolicy.AUTOMATIC_INTERVAL_MS - 1))
        assertTrue(UpdateSchedulePolicy.automaticFetchDue(true, 100, 100 + UpdateSchedulePolicy.AUTOMATIC_INTERVAL_MS))
        assertTrue(UpdateSchedulePolicy.automaticFetchDue(true, 100, 99))
    }
}
