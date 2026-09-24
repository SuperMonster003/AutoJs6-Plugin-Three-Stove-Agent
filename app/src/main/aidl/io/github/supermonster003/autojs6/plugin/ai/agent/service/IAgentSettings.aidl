package io.github.supermonster003.autojs6.plugin.ai.agent.service;
import android.os.Bundle;
import io.github.supermonster003.autojs6.plugin.ai.agent.service.IPresetStoreCallback;
// Private settings use the same bounded reply envelope as other management endpoints.
oneway interface IAgentSettings {
    void query(in Bundle request, IPresetStoreCallback callback);
}
