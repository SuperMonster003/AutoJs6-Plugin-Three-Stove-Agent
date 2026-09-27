package io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog

/**
 * Built-in tool names as the Kotlin adapters refer to them. The catalog data itself lives in
 * `assets/catalog/tools.json`; `ToolCatalogTest` asserts that every constant here names a packaged
 * tool and that every packaged tool has a constant, so adapters cannot drift from the data table.
 */
object ToolNames {
    const val UI_DUMP = "ui_dump"
    const val UI_FIND = "ui_find"
    const val UI_WAIT_FOR = "ui_wait_for"
    const val APP_CURRENT = "app_current"
    const val SCREEN_STATE = "screen_state"
    const val SCREEN_CAPTURE = "screen_capture"
    const val DEVICE_INFO = "device_info"
    const val CONSOLE_TAIL = "console_tail"
    const val OCR_SCREEN = "ocr_screen"
    const val UI_CLICK = "ui_click"
    const val UI_LONG_CLICK = "ui_long_click"
    const val UI_SET_TEXT = "ui_set_text"
    const val UI_SCROLL = "ui_scroll"
    const val UI_PRESS_KEY = "ui_press_key"
    const val APP_LAUNCH = "app_launch"
    const val CLIPBOARD_GET = "clipboard_get"
    const val CLIPBOARD_SET = "clipboard_set"
    const val UI_CLICK_XY = "ui_click_xy"
    const val UI_SWIPE = "ui_swipe"
    const val UI_GESTURE = "ui_gesture"
    const val SCRIPT_CATALOG = "script_catalog"
    const val SCRIPT_RUN = "script_run"
    const val SCRIPT_RUN_SOURCE = "script_run_source"
    const val SCRIPT_STOP = "script_stop"
    const val FILES_LIST = "files_list"
    const val FILES_STAT = "files_stat"
    const val FILES_READ = "files_read"
    const val FILES_WRITE = "files_write"
    const val SHELL_EXEC = "shell_exec"
    const val MEMORY_GET = "memory_get"
    const val MEMORY_PROPOSE = "memory_propose"
    const val REPORT_PROGRESS = "report_progress"

    /** Observations that read the screen; an action must be followed by one of these (LoopRules). */
    val SCREEN_OBSERVATIONS = setOf(UI_DUMP, UI_FIND, UI_WAIT_FOR, OCR_SCREEN, APP_CURRENT, SCREEN_STATE, SCREEN_CAPTURE)
    val OBSERVATIONS = SCREEN_OBSERVATIONS + setOf(DEVICE_INFO, CONSOLE_TAIL)
    val NODE_ACTIONS = setOf(UI_CLICK, UI_LONG_CLICK, UI_SET_TEXT, UI_SCROLL)
    val GESTURES = setOf(UI_CLICK_XY, UI_SWIPE, UI_GESTURE)
    val ACTIONS = NODE_ACTIONS + GESTURES + setOf(UI_PRESS_KEY, APP_LAUNCH, CLIPBOARD_GET, CLIPBOARD_SET)
    val SCRIPT_EXECUTIONS = setOf(SCRIPT_RUN, SCRIPT_RUN_SOURCE)
    val FILES = setOf(FILES_LIST, FILES_STAT, FILES_READ, FILES_WRITE)
    val MEMORY = setOf(MEMORY_GET, MEMORY_PROPOSE)
    /** Tools the host capability broker adapter executes; scripts and memory have their own adapters. */
    val HOST_DISPATCHED = OBSERVATIONS + ACTIONS + setOf(SCRIPT_CATALOG, SCRIPT_STOP) + FILES + setOf(SHELL_EXEC, REPORT_PROGRESS)
    val ALL = HOST_DISPATCHED + SCRIPT_EXECUTIONS + MEMORY
}
