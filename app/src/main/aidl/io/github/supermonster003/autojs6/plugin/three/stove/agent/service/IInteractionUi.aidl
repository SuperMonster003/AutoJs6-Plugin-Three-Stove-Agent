package io.github.supermonster003.autojs6.plugin.three.stove.agent.service;
// Private, same-UID visibility lease. A dead UI process cannot suppress notifications.
oneway interface IInteractionUi {
    void present(IBinder owner, String runId, String requestId);
}
