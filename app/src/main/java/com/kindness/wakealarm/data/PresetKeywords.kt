package com.kindness.wakealarm.data

import androidx.annotation.StringRes
import com.kindness.wakealarm.R

/**
 * Preset keywords commonly used in medical/hospital emergency contexts.
 * The keywords themselves stay as written in Indonesian hospital chats; only their
 * descriptions are translated.
 */
object PresetKeywords {

    data class Preset(
        val keyword: String,
        @StringRes val description: Int
    )

    val all: List<Preset> = listOf(
        Preset("jaga", R.string.preset_jaga),
        Preset("urgent", R.string.preset_urgent),
        Preset("segera", R.string.preset_segera),
        Preset("dokter", R.string.preset_dokter),
        Preset("co-ass", R.string.preset_coass),
        Preset("igd", R.string.preset_igd),
        Preset("emergency", R.string.preset_emergency),
        Preset("panggilan", R.string.preset_panggilan),
        Preset("pasien", R.string.preset_pasien),
        Preset("operasi", R.string.preset_operasi),
        Preset("kritis", R.string.preset_kritis),
        Preset("code blue", R.string.preset_code_blue)
    )

    val defaultEnabled: Set<String> = setOf(
        "jaga", "urgent", "segera", "dokter", "co-ass", "igd"
    )
}
