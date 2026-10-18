package io.github.supermonster003.autojs6.plugin.three.stove.agent.speech;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;

/**
 * Another app's resident listener for the conflict case of roadmap D73: disabled in the test APK manifest, turned on and
 * off by {@link Switch}. Only its component state counts; other apps query it and never start it. Standalone test-APK
 * class: no target APK classes or Kotlin runtime on its process classpath.
 */
public final class ResidentRivalService extends Service {
    public static final String EXTRA_ENABLED = "enabled";

    @Override public IBinder onBind(Intent intent) { return null; }

    /**
     * Sets the rival's state from the test APK's own process, the only caller allowed to without a permission. Unlike
     * `pm enable` it keeps the test APK's processes alive (DONT_KILL_APP); off restores the Manifest default.
     */
    public static final class Switch extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            int state = intent.getBooleanExtra(EXTRA_ENABLED, false)
                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
            context.getPackageManager().setComponentEnabledSetting(new ComponentName(context, ResidentRivalService.class), state,
                PackageManager.DONT_KILL_APP);
        }
    }
}
