package com.example.core.i18n

import com.example.core.units.MeasurementUnitSystem

/**
 * Global Studio v2.0 — regional standards.
 *
 * The v1.1 [WorldKitchenStandards] held one worldwide set of numbers
 * (90 cm countertop, 60 cm base depth, 120 cm walkway). Real showrooms
 * differ by region, so each region now carries its own numbers, default
 * language, default unit system and currency. Picking a region in the
 * Global Studio sets the language + units to that region's defaults
 * (the user can still override either one afterwards).
 *
 * Numbers are the common showroom values for the region, not building
 * code: US countertops are usually 36 in (914 mm) and walkways 42–48 in;
 * Gulf / MENA and Europe use 90 cm; Brazil uses metric like Europe.
 */
enum class KitchenRegion(
    val titleAr: String,
    val titleEn: String,
    val defaultLanguage: AppLanguage,
    val defaultUnits: MeasurementUnitSystem,
    val currencyCode: String,
    val currencySymbol: String,
    val countertopHeightMm: Float,
    val baseCabinetDepthMm: Float,
    val upperCabinetDepthMm: Float,
    val minWalkwayMm: Float
) {
    MENA_GULF(
        titleAr = "الشرق الأوسط والخليج 🇦🇪",
        titleEn = "Middle East & Gulf",
        defaultLanguage = AppLanguage.ARABIC,
        defaultUnits = MeasurementUnitSystem.METRIC,
        currencyCode = "AED", currencySymbol = "د.إ",
        countertopHeightMm = 900f, baseCabinetDepthMm = 600f,
        upperCabinetDepthMm = 350f, minWalkwayMm = 1200f
    ),
    EUROPE(
        titleAr = "أوروبا 🇪🇺",
        titleEn = "Europe",
        defaultLanguage = AppLanguage.ENGLISH,
        defaultUnits = MeasurementUnitSystem.METRIC,
        currencyCode = "EUR", currencySymbol = "€",
        countertopHeightMm = 900f, baseCabinetDepthMm = 600f,
        upperCabinetDepthMm = 350f, minWalkwayMm = 1200f
    ),
    NORTH_AMERICA(
        titleAr = "أمريكا الشمالية 🇺🇸",
        titleEn = "North America",
        defaultLanguage = AppLanguage.ENGLISH,
        defaultUnits = MeasurementUnitSystem.IMPERIAL,
        currencyCode = "USD", currencySymbol = "$",
        countertopHeightMm = 914f, baseCabinetDepthMm = 610f,
        upperCabinetDepthMm = 356f, minWalkwayMm = 1067f
    ),
    BRAZIL_LATAM(
        titleAr = "البرازيل وأمريكا اللاتينية 🇧🇷",
        titleEn = "Brazil & Latin America",
        defaultLanguage = AppLanguage.PORTUGUESE,
        defaultUnits = MeasurementUnitSystem.METRIC,
        currencyCode = "BRL", currencySymbol = "R$",
        countertopHeightMm = 900f, baseCabinetDepthMm = 600f,
        upperCabinetDepthMm = 350f, minWalkwayMm = 1200f
    ),
    EAST_ASIA(
        titleAr = "شرق آسيا 🇯🇵",
        titleEn = "East Asia",
        defaultLanguage = AppLanguage.ENGLISH,
        defaultUnits = MeasurementUnitSystem.METRIC,
        currencyCode = "JPY", currencySymbol = "¥",
        countertopHeightMm = 850f, baseCabinetDepthMm = 600f,
        upperCabinetDepthMm = 350f, minWalkwayMm = 1100f
    );

    fun standardsSummaryAr(): String =
        "سطح العمل ${countertopHeightMm.toInt()} مم • عمق سفلي ${baseCabinetDepthMm.toInt()} مم • " +
            "عمق علوي ${upperCabinetDepthMm.toInt()} مم • ممر ${minWalkwayMm.toInt()} مم • $currencyCode ($currencySymbol)"

    fun standardsSummaryEn(): String =
        "Countertop ${countertopHeightMm.toInt()} mm • base ${baseCabinetDepthMm.toInt()} mm • " +
            "upper ${upperCabinetDepthMm.toInt()} mm • walkway ${minWalkwayMm.toInt()} mm • $currencyCode ($currencySymbol)"
}
