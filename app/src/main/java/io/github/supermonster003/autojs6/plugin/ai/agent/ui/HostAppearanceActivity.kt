package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.net.Uri
import android.os.*
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.*
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import org.autojs.plugin.common.api.AutoJs6HostSettingsContract as C
import java.util.Locale
import java.util.concurrent.Executors

internal data class HostAppearance(val language: String, val dark: Boolean, val primary: Int, val accent: Int) {
    fun wrap(context: Context): Context = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
        val locale = Locale.forLanguageTag(language)
        setLocales(LocaleList(locale)); setLayoutDirection(locale)
        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
    })
    companion object {
        @Volatile var cached: HostAppearance? = null
        val worker = Executors.newSingleThreadExecutor()
        fun read(context: Context): HostAppearance? = runCatching {
            context.contentResolver.acquireUnstableContentProviderClient(Uri.parse(C.CONTENT_URI))?.use {
                it.call(C.METHOD_GET_SETTINGS, null, null)?.let(::decode)
            }
        }.getOrNull()
        fun decode(value: Bundle): HostAppearance? = runCatching {
            require(!value.hasFileDescriptors() && value.getInt(C.KEY_PROTOCOL_VERSION) == C.PROTOCOL_VERSION &&
                value.getString(C.KEY_HOST_PACKAGE_NAME) == C.HOST_PACKAGE_NAME)
            require(listOf(C.KEY_DARK_MODE_ACTIVE, C.KEY_THEME_COLOR_PRIMARY, C.KEY_THEME_COLOR_ACCENT).all(value::containsKey))
            val tag = requireNotNull(value.getString(C.KEY_RESOLVED_LANGUAGE_TAG))
            require(tag.length in 2..80 && tag.matches(Regex("[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*")) && Locale.forLanguageTag(tag).language.isNotBlank())
            HostAppearance(tag, value.getBoolean(C.KEY_DARK_MODE_ACTIVE), value.getInt(C.KEY_THEME_COLOR_PRIMARY), value.getInt(C.KEY_THEME_COLOR_ACCENT))
        }.getOrNull()
    }
}

/** Provider IO stays off the main thread; an unavailable snapshot restores system appearance. */
abstract class HostAppearanceActivity : Activity() {
    protected open val dialogTheme = false
    private var applied: HostAppearance? = null
    private var appearanceGeneration = 0
    override fun attachBaseContext(newBase: Context) {
        applied = HostAppearance.cached
        super.attachBaseContext(applied?.wrap(newBase) ?: newBase)
    }
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        val dark = applied?.dark ?: (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
        setTheme(if (dialogTheme) {
            if (dark) R.style.Theme_AiAgent_Dialog_Dark else R.style.Theme_AiAgent_Dialog_Light
        } else if (dark) R.style.Theme_AiAgent_Dark else R.style.Theme_AiAgent_Light)
        super.onCreate(savedInstanceState)
        // PhoneWindow.getInsetsController() on Android 13 dereferences its decor directly.
        // Materialize it before querying the controller, even before setContentView().
        val decor = window.decorView
        val attribute = android.util.TypedValue().also { theme.resolveAttribute(android.R.attr.colorBackground, it, true) }
        val background = if (attribute.resourceId != 0) getColor(attribute.resourceId) else attribute.data
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = background
        if (Build.VERSION.SDK_INT >= 26) window.navigationBarColor = background
        if (Build.VERSION.SDK_INT >= 30) window.insetsController?.setSystemBarsAppearance(
            if (dark) 0 else android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
            android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
        else decor.systemUiVisibility = if (dark) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
            if (Build.VERSION.SDK_INT >= 26) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0
    }
    override fun onStart() {
        super.onStart()
        val expected = ++appearanceGeneration
        HostAppearance.worker.execute {
            val next = HostAppearance.read(applicationContext)
            runOnUiThread {
                if (expected == appearanceGeneration && !isFinishing && !isDestroyed && applied != next) {
                    HostAppearance.cached = next; recreate()
                }
            }
        }
    }
    override fun onStop() { appearanceGeneration++; super.onStop() }
    internal fun tint(view: View) {
        val colors = applied ?: return
        // Keep platform text contrast/disabled states; use host colors on accents only.
        val accent = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_enabled), intArrayOf()),
            intArrayOf(colors.accent, (colors.accent and 0x00ffffff) or 0x61000000))
        when (view) {
            is CompoundButton -> view.buttonTintList = accent
            is Button -> {
                view.backgroundTintList = accent
                val foreground = if (Color.luminance(colors.accent or (0xff shl 24)) > 0.179f) Color.BLACK else Color.WHITE
                view.setTextColor(ColorStateList(arrayOf(intArrayOf(android.R.attr.state_enabled), intArrayOf()),
                    intArrayOf(foreground, (foreground and 0xffffff) or 0x61000000)))
            }
            is EditText -> view.backgroundTintList = accent
            is ProgressBar -> { view.progressTintList = accent; view.indeterminateTintList = accent }
        }
        if (view.tag == "host-primary") view.backgroundTintList = ColorStateList.valueOf(colors.primary)
        if (view is ViewGroup) for (index in 0 until view.childCount) tint(view.getChildAt(index))
    }
}
