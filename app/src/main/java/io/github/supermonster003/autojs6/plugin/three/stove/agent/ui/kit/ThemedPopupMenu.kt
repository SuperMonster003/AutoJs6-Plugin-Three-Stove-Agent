package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit

import android.annotation.SuppressLint
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ListView
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.view.menu.ShowableListMenu

/**
 * AppCompat's standard menu presentation with the runtime control palette. PopupMenu hides its
 * inflated list, so use the same AppCompat presenters directly instead of reflecting private fields.
 */
@SuppressLint("RestrictedApi")
internal class ThemedPopupMenu(private val kit: Kit, anchor: View) {
    private val builder = MenuBuilder(kit.context)
    val menu: Menu get() = builder
    private val presenter = MenuPopupHelper(kit.context, builder, anchor)
    internal var listView: ListView? = null; private set
    private var click: (MenuItem) -> Boolean = { false }

    init {
        builder.setCallback(object : MenuBuilder.Callback {
            override fun onMenuItemSelected(menu: MenuBuilder, item: MenuItem) = click(item)
            override fun onMenuModeChange(menu: MenuBuilder) = Unit
        })
        presenter.setOnDismissListener { listView = null }
    }

    fun setOnMenuItemClickListener(listener: (MenuItem) -> Boolean) { click = listener }

    fun show() {
        presenter.show()
        val popup: ShowableListMenu = presenter.popup
        listView = popup.listView?.also { list ->
            kit.applyThemeToControls(list)
            list.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ -> kit.applyThemeToControls(view) }
        }
    }

    fun dismiss() = presenter.dismiss()
}
