package org.autojs.plugin.three.stove.agent.fakehost;
import android.os.Bundle;
/** Test-only process control; never a published plugin contract. */
interface IFakeHostDriver {
    Bundle attach(in Bundle configuration, String mode);
    Bundle stats();
    void detach();
    void die();
}
