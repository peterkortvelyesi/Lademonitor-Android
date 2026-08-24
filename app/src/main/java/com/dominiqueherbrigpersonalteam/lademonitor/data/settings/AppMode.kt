package com.dominiqueherbrigpersonalteam.lademonitor.data.settings

/**
 * Whether the app runs fully local (no server), or against a Lademonitor server.
 * [UNDECIDED] only applies on the very first launch, before the user picks (see ModeSelectionScreen).
 */
enum class AppMode(val raw: String) {
    UNDECIDED("undecided"),
    LOCAL_ONLY("localOnly"),
    SERVER("server");

    companion object {
        fun from(raw: String?): AppMode = entries.firstOrNull { it.raw == raw } ?: UNDECIDED
    }
}
