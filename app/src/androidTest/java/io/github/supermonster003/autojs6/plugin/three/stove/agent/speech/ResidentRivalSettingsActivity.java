package io.github.supermonster003.autojs6.plugin.three.stove.agent.speech;

import android.app.Activity;

/**
 * The settings screen {@link ResidentRivalService} names in its meta-data: "Open xxx settings" of the conflict dialog opens it
 * (roadmap D73). It stays in front long enough for the test to see it, then returns to the 3-Stove Agent settings.
 * Standalone test-APK class: no target APK classes or Kotlin runtime on its process classpath.
 */
public final class ResidentRivalSettingsActivity extends Activity {
    @Override protected void onResume() {
        super.onResume();
        getWindow().getDecorView().postDelayed(this::finish, 2000);
    }
}
