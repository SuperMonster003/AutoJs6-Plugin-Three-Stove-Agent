package io.github.supermonster003.autojs6.plugin.three.stove.agent.service;
import android.os.Bundle;
import io.github.supermonster003.autojs6.plugin.three.stove.agent.service.IPresetStoreCallback;
// Private asynchronous UI operations. No change to the published host AIDL.
oneway interface IPresetStore {
    void query(in Bundle request, IPresetStoreCallback callback);
}
