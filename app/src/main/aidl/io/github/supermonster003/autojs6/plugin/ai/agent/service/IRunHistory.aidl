package io.github.supermonster003.autojs6.plugin.ai.agent.service;
import android.os.Bundle;
import io.github.supermonster003.autojs6.plugin.ai.agent.service.IRunHistoryCallback;
// Private, same-UID UI endpoint. Does not change the published host contract.
oneway interface IRunHistory {
    void query(in Bundle request, IRunHistoryCallback callback);
}
