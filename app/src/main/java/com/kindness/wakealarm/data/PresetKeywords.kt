package com.kindness.wakealarm.data

/**
 * Preset keywords commonly used in medical/hospital emergency contexts.
 * Each preset has a display label and the actual keyword string.
 */
object PresetKeywords {

    data class Preset(
        val keyword: String,
        val description: String
    )

    val all: List<Preset> = listOf(
        Preset("jaga", "Jadwal jaga / piket"),
        Preset("urgent", "Pesan mendesak"),
        Preset("segera", "Perlu tindakan segera"),
        Preset("dokter", "Panggilan dokter"),
        Preset("co-ass", "Panggilan co-ass / koas"),
        Preset("igd", "Instalasi Gawat Darurat"),
        Preset("emergency", "Keadaan darurat"),
        Preset("panggilan", "Panggilan umum"),
        Preset("pasien", "Terkait pasien"),
        Preset("operasi", "Tindakan operasi"),
        Preset("kritis", "Kondisi kritis"),
        Preset("code blue", "Code Blue / cardiac arrest")
    )

    val defaultEnabled: Set<String> = setOf(
        "jaga", "urgent", "segera", "dokter", "co-ass", "igd"
    )
}
