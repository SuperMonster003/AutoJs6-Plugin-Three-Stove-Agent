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
    @Test fun noNetworkConditionEnablesAutomaticChecks() {
        for (metered in listOf(null, false, true)) assertFalse(UpdateSchedulePolicy.automaticCheckAllowed(metered))
    }
}
