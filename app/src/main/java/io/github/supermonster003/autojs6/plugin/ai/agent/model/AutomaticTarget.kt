package io.github.supermonster003.autojs6.plugin.ai.agent.model

/**
 * The Automatic model choice: the first on-device target, otherwise the first target the host lists.
 * The broker applies it at admission and the model switcher uses it to preview the pick.
 */
internal object AutomaticTarget {
    fun <T> pick(targets: List<T>, locality: (T) -> ModelLocality): T? =
        targets.firstOrNull { locality(it) == ModelLocality.ON_DEVICE } ?: targets.firstOrNull()
}
