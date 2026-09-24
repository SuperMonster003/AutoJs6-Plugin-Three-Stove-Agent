package io.github.supermonster003.autojs6.plugin.ai.agent.service;
import android.os.Bundle;
import io.github.supermonster003.autojs6.plugin.ai.agent.service.IMemoryStoreCallback;
oneway interface IMemoryStore {
    void query(in Bundle request, IMemoryStoreCallback callback);
}
