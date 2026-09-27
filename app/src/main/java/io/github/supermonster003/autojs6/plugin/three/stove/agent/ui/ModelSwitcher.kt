package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.ModelSelectionState

/**
 * Ties the shared model choice to one screen: the capsule shows it, the sheet changes it and the
 * catalog checks it. Every screen that starts tasks reads the same stored choice.
 */
internal class ModelSwitcher(private val activity: HostAppearanceActivity, private val changed: () -> Unit) {
    var selection: ModelSelectionState = ModelSelection.read(activity); private set
    val catalog = ModelCatalog(activity, ::catalogChanged)
    val capsule = ModelCapsule(activity.kit) { open() }
    val sheet = ModelSheet(activity.kit, catalog, { selection }, ::update)

    /** False only when the loaded catalog no longer offers the chosen model. */
    val available get() = catalog.available(selection.current)
    val targetId: String? get() = selection.current?.targetId

    private var connected = false

    init { render() }

    /** Another screen or the floating ball may have changed the choice while this one was stopped. */
    fun start() { selection = ModelSelection.read(activity); render(); catalog.start() }
    fun stop() { catalog.stop(); sheet.dismiss() }
    fun close() { catalog.close() }
    fun attached(value: Boolean) { if (connected != value) { connected = value; render() }; catalog.attached(value) }

    fun open() { sheet.show(); catalog.refresh() }

    fun update(change: (ModelSelectionState) -> ModelSelectionState) {
        selection = runCatching { ModelSelection.update(activity, change) }.getOrElse { change(selection) }
        render(); sheet.render(); changed()
    }

    private fun catalogChanged() {
        if (catalog.status == ModelCatalog.Status.READY) {
            val names = catalog.entries.associate { it.targetId to it.name }
            if (selection.renamed(names) != selection) update { it.renamed(names) }
        }
        render(); sheet.render(); changed()
    }

    private fun render() = capsule.render(selection, catalog.automatic?.name, available, connected)
}
