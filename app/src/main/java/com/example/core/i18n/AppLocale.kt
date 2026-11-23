package com.example.core.i18n

/**
 * Global Studio v2.0 — real in-app localisation.
 *
 * v1.1 shipped Arabic-only UI text hardcoded in Kotlin (only `app_name`
 * lived in strings.xml). This file is the first true locale layer: 8
 * languages covering the regions the reference kitchens come from —
 * Arabic, English, French, Spanish, Brazilian Portuguese (photo 1 uses
 * Portuguese dimensions: 2,40 m / 3,00 m), Turkish, German and Italian.
 *
 * Pure Kotlin, no Android dependency, so unit tests can prove every key
 * exists in every language. The dialog reads [AppStrings.t]; any missing
 * key falls back to English, then Arabic — never a blank label.
 * Existing Arabic screens are untouched: this dictionary covers the new
 * Global Studio dialog only, so nothing that already worked can regress.
 */
enum class AppLanguage(
    val code: String,
    val nativeName: String,
    val englishName: String,
    val isRtl: Boolean
) {
    ARABIC("ar", "العربية", "Arabic", true),
    ENGLISH("en", "English", "English", false),
    FRENCH("fr", "Français", "French", false),
    SPANISH("es", "Español", "Spanish", false),
    PORTUGUESE("pt", "Português", "Portuguese", false),
    TURKISH("tr", "Türkçe", "Turkish", false),
    GERMAN("de", "Deutsch", "German", false),
    ITALIAN("it", "Italiano", "Italian", false);

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code == code } ?: ARABIC

        /** Best default for a device locale tag such as "pt-BR" or "fr-FR". */
        fun fromLocaleTag(tag: String): AppLanguage {
            val lower = tag.lowercase()
            return entries.firstOrNull { lower.startsWith(it.code) } ?: ENGLISH
        }
    }
}

object AppStrings {
    // Key constants — the dialog and the tests both use these, never raw strings.
    const val GLOBAL_TITLE = "global_title"
    const val GLOBAL_SUBTITLE = "global_subtitle"
    const val TAB_GLOBAL = "tab_global"
    const val TAB_REFERENCE = "tab_reference"
    const val TAB_REALISM = "tab_realism"
    const val LANGUAGE = "language"
    const val LANGUAGE_NOTE = "language_note"
    const val UNITS = "units"
    const val REGION = "region"
    const val CURRENCY = "currency"
    const val STANDARDS = "standards"
    const val COUNTERTOP_HEIGHT = "countertop_height"
    const val WALKWAY = "walkway"
    const val BASE_DEPTH = "base_depth"
    const val REF_TITLE = "ref_title"
    const val REF_SUBTITLE = "ref_subtitle"
    const val REF_DIMENSIONS = "ref_dimensions"
    const val REF_CABINETS = "ref_cabinets"
    const val REF_COUNTERTOP = "ref_countertop"
    const val REF_LIGHTING = "ref_lighting"
    const val REF_FEATURES = "ref_features"
    const val REF_APPLY = "ref_apply"
    const val REF_APPLIED = "ref_applied"
    const val REAL_TITLE = "real_title"
    const val REAL_SUBTITLE = "real_subtitle"
    const val SOFT_SHADOWS = "soft_shadows"
    const val AMBIENT_OCCLUSION = "ambient_occlusion"
    const val REFLECTIONS = "reflections"
    const val FLOOR_REFLECTION = "floor_reflection"
    const val LED_GLOW = "led_glow"
    const val ACES = "aces"
    const val DENOISER = "denoiser"
    const val QUALITY = "quality"
    const val CONTACT_SHADOWS = "contact_shadows"
    const val GLASS_REFRACTION = "glass_refraction"
    const val APPLY_SETTINGS = "apply_settings"
    const val CLOSE = "close"

    val ALL_KEYS: List<String> = listOf(
        GLOBAL_TITLE, GLOBAL_SUBTITLE, TAB_GLOBAL, TAB_REFERENCE, TAB_REALISM,
        LANGUAGE, LANGUAGE_NOTE, UNITS, REGION, CURRENCY, STANDARDS,
        COUNTERTOP_HEIGHT, WALKWAY, BASE_DEPTH,
        REF_TITLE, REF_SUBTITLE, REF_DIMENSIONS, REF_CABINETS, REF_COUNTERTOP,
        REF_LIGHTING, REF_FEATURES, REF_APPLY, REF_APPLIED,
        REAL_TITLE, REAL_SUBTITLE, SOFT_SHADOWS, AMBIENT_OCCLUSION, REFLECTIONS,
        FLOOR_REFLECTION, LED_GLOW, ACES, DENOISER, QUALITY, CONTACT_SHADOWS,
        GLASS_REFRACTION, APPLY_SETTINGS, CLOSE
    )

    private val STRINGS: Map<AppLanguage, Map<String, String>> = mapOf(
        AppLanguage.ARABIC to mapOf(
            GLOBAL_TITLE to "الاستوديو العالمي 🌍",
            GLOBAL_SUBTITLE to "لغة ووحدات ومعايير عالمية، ومطابخ مرجعية حقيقية، ورندر أكثر واقعية — في مكان واحد.",
            TAB_GLOBAL to "🌍 عالمي",
            TAB_REFERENCE to "📸 مرجعية",
            TAB_REALISM to "✨ واقعية",
            LANGUAGE to "لغة الواجهة",
            LANGUAGE_NOTE to "تُترجم واجهة الاستوديو العالمي فوراً. بقية الشاشات ما زالت بالعربية وستُترجم تباعاً.",
            UNITS to "وحدات القياس",
            REGION to "المنطقة والمعايير",
            CURRENCY to "العملة",
            STANDARDS to "المعايير الإقليمية",
            COUNTERTOP_HEIGHT to "ارتفاع سطح العمل",
            WALKWAY to "عرض الممر الأدنى",
            BASE_DEPTH to "عمق الخزائن السفلية",
            REF_TITLE to "مطابخ مرجعية من صور حقيقية",
            REF_SUBTITLE to "اضغط «تطبيق» على أي مطبخ: تُضبط أبعاد الغرفة والخامات والإضاءة والكاميرا دفعة واحدة.",
            REF_DIMENSIONS to "الأبعاد",
            REF_CABINETS to "الخزائن",
            REF_COUNTERTOP to "سطح العمل",
            REF_LIGHTING to "الإضاءة",
            REF_FEATURES to "مميزات الصورة",
            REF_APPLY to "تطبيق هذا المطبخ",
            REF_APPLIED to "تم التطبيق ✓",
            REAL_TITLE to "واقعية الرندر الاحترافية",
            REAL_SUBTITLE to "ظلال ناعمة، انعكاسات أرضية، توهج LED، وتصحيح ACES — القيم تُحفظ وتُطبق عند تصدير 4K.",
            SOFT_SHADOWS to "ظلال ناعمة (Soft Shadows)",
            AMBIENT_OCCLUSION to "تظليل محيطي (AO)",
            REFLECTIONS to "انعكاسات عامة",
            FLOOR_REFLECTION to "انعكاس الأرضية المصقولة",
            LED_GLOW to "توهج شرائط LED",
            ACES to "تصحيح ألوان ACES السينمائي",
            DENOISER to "تنعيم الضجيج (Denoiser)",
            QUALITY to "جودة الرندر",
            CONTACT_SHADOWS to "ظلال التماس تحت الخزائن",
            GLASS_REFRACTION to "انكسار زجاج الفيترينة",
            APPLY_SETTINGS to "تطبيق إعدادات الواقعية",
            CLOSE to "إغلاق"
        ),
        AppLanguage.ENGLISH to mapOf(
            GLOBAL_TITLE to "Global Studio 🌍",
            GLOBAL_SUBTITLE to "Language, units and world standards, real reference kitchens, and more photoreal rendering — in one place.",
            TAB_GLOBAL to "🌍 Global",
            TAB_REFERENCE to "📸 Reference",
            TAB_REALISM to "✨ Realism",
            LANGUAGE to "Interface language",
            LANGUAGE_NOTE to "The Global Studio UI translates instantly. Other screens are still Arabic and will follow.",
            UNITS to "Measurement units",
            REGION to "Region & standards",
            CURRENCY to "Currency",
            STANDARDS to "Regional standards",
            COUNTERTOP_HEIGHT to "Countertop height",
            WALKWAY to "Minimum walkway",
            BASE_DEPTH to "Base cabinet depth",
            REF_TITLE to "Reference kitchens from real photos",
            REF_SUBTITLE to "Tap Apply on any kitchen: room size, materials, lighting and camera are set in one tap.",
            REF_DIMENSIONS to "Dimensions",
            REF_CABINETS to "Cabinets",
            REF_COUNTERTOP to "Countertop",
            REF_LIGHTING to "Lighting",
            REF_FEATURES to "Photo features",
            REF_APPLY to "Apply this kitchen",
            REF_APPLIED to "Applied ✓",
            REAL_TITLE to "Photoreal render settings",
            REAL_SUBTITLE to "Soft shadows, floor reflections, LED glow and ACES grading — values are saved and applied on 4K export.",
            SOFT_SHADOWS to "Soft shadows",
            AMBIENT_OCCLUSION to "Ambient occlusion (AO)",
            REFLECTIONS to "Reflections",
            FLOOR_REFLECTION to "Polished floor reflection",
            LED_GLOW to "LED strip glow",
            ACES to "ACES cinematic colour grading",
            DENOISER to "Denoiser",
            QUALITY to "Render quality",
            CONTACT_SHADOWS to "Contact shadows under cabinets",
            GLASS_REFRACTION to "Showcase glass refraction",
            APPLY_SETTINGS to "Apply realism settings",
            CLOSE to "Close"
        ),
        AppLanguage.FRENCH to mapOf(
            GLOBAL_TITLE to "Studio Mondial 🌍",
            GLOBAL_SUBTITLE to "Langue, unités et normes mondiales, cuisines de référence réelles et rendu plus photoréaliste — en un seul endroit.",
            TAB_GLOBAL to "🌍 Mondial",
            TAB_REFERENCE to "📸 Référence",
            TAB_REALISM to "✨ Réalisme",
            LANGUAGE to "Langue de l'interface",
            LANGUAGE_NOTE to "L'interface du Studio Mondial se traduit instantanément. Les autres écrans sont encore en arabe.",
            UNITS to "Unités de mesure",
            REGION to "Région et normes",
            CURRENCY to "Devise",
            STANDARDS to "Normes régionales",
            COUNTERTOP_HEIGHT to "Hauteur du plan de travail",
            WALKWAY to "Passage minimum",
            BASE_DEPTH to "Profondeur des meubles bas",
            REF_TITLE to "Cuisines de référence d'après de vraies photos",
            REF_SUBTITLE to "Appuyez sur Appliquer : dimensions, matériaux, éclairage et caméra sont réglés en un geste.",
            REF_DIMENSIONS to "Dimensions",
            REF_CABINETS to "Meubles",
            REF_COUNTERTOP to "Plan de travail",
            REF_LIGHTING to "Éclairage",
            REF_FEATURES to "Détails de la photo",
            REF_APPLY to "Appliquer cette cuisine",
            REF_APPLIED to "Appliquée ✓",
            REAL_TITLE to "Réglages de rendu photoréaliste",
            REAL_SUBTITLE to "Ombres douces, reflets du sol, halo LED et étalonnage ACES — appliqués à l'export 4K.",
            SOFT_SHADOWS to "Ombres douces",
            AMBIENT_OCCLUSION to "Occlusion ambiante (AO)",
            REFLECTIONS to "Reflets",
            FLOOR_REFLECTION to "Reflet du sol poli",
            LED_GLOW to "Halo des bandes LED",
            ACES to "Étalonnage cinéma ACES",
            DENOISER to "Débruitage",
            QUALITY to "Qualité du rendu",
            CONTACT_SHADOWS to "Ombres de contact sous les meubles",
            GLASS_REFRACTION to "Réfraction du verre de la vitrine",
            APPLY_SETTINGS to "Appliquer les réglages",
            CLOSE to "Fermer"
        ),
        AppLanguage.SPANISH to mapOf(
            GLOBAL_TITLE to "Estudio Global 🌍",
            GLOBAL_SUBTITLE to "Idioma, unidades y normas mundiales, cocinas reales de referencia y un render más fotorrealista, en un solo lugar.",
            TAB_GLOBAL to "🌍 Global",
            TAB_REFERENCE to "📸 Referencia",
            TAB_REALISM to "✨ Realismo",
            LANGUAGE to "Idioma de la interfaz",
            LANGUAGE_NOTE to "La interfaz del Estudio Global se traduce al instante. Las demás pantallas siguen en árabe.",
            UNITS to "Unidades de medida",
            REGION to "Región y normas",
            CURRENCY to "Moneda",
            STANDARDS to "Normas regionales",
            COUNTERTOP_HEIGHT to "Altura de la encimera",
            WALKWAY to "Pasillo mínimo",
            BASE_DEPTH to "Profundidad de muebles bajos",
            REF_TITLE to "Cocinas de referencia de fotos reales",
            REF_SUBTITLE to "Pulsa Aplicar en cualquier cocina: tamaño, materiales, luz y cámara se ajustan de una vez.",
            REF_DIMENSIONS to "Dimensiones",
            REF_CABINETS to "Muebles",
            REF_COUNTERTOP to "Encimera",
            REF_LIGHTING to "Iluminación",
            REF_FEATURES to "Detalles de la foto",
            REF_APPLY to "Aplicar esta cocina",
            REF_APPLIED to "Aplicada ✓",
            REAL_TITLE to "Ajustes de render fotorrealista",
            REAL_SUBTITLE to "Sombras suaves, reflejos del suelo, brillo LED y corrección ACES, aplicados al exportar en 4K.",
            SOFT_SHADOWS to "Sombras suaves",
            AMBIENT_OCCLUSION to "Oclusión ambiental (AO)",
            REFLECTIONS to "Reflejos",
            FLOOR_REFLECTION to "Reflejo del suelo pulido",
            LED_GLOW to "Brillo de las tiras LED",
            ACES to "Corrección de color ACES",
            DENOISER to "Reducción de ruido",
            QUALITY to "Calidad del render",
            CONTACT_SHADOWS to "Sombras de contacto bajo los muebles",
            GLASS_REFRACTION to "Refracción del vidrio de la vitrina",
            APPLY_SETTINGS to "Aplicar ajustes de realismo",
            CLOSE to "Cerrar"
        ),
        AppLanguage.PORTUGUESE to mapOf(
            GLOBAL_TITLE to "Estúdio Global 🌍",
            GLOBAL_SUBTITLE to "Idioma, unidades e normas mundiais, cozinhas reais de referência e render mais fotorrealista — em um só lugar.",
            TAB_GLOBAL to "🌍 Global",
            TAB_REFERENCE to "📸 Referência",
            TAB_REALISM to "✨ Realismo",
            LANGUAGE to "Idioma da interface",
            LANGUAGE_NOTE to "A interface do Estúdio Global traduz na hora. As outras telas ainda estão em árabe.",
            UNITS to "Unidades de medida",
            REGION to "Região e normas",
            CURRENCY to "Moeda",
            STANDARDS to "Normas regionais",
            COUNTERTOP_HEIGHT to "Altura da bancada",
            WALKWAY to "Circulação mínima",
            BASE_DEPTH to "Profundidade dos armários baixos",
            REF_TITLE to "Cozinhas de referência de fotos reais",
            REF_SUBTITLE to "Toque em Aplicar em qualquer cozinha: tamanho, materiais, luz e câmera são ajustados de uma vez.",
            REF_DIMENSIONS to "Dimensões",
            REF_CABINETS to "Armários",
            REF_COUNTERTOP to "Bancada",
            REF_LIGHTING to "Iluminação",
            REF_FEATURES to "Detalhes da foto",
            REF_APPLY to "Aplicar esta cozinha",
            REF_APPLIED to "Aplicada ✓",
            REAL_TITLE to "Ajustes de render fotorrealista",
            REAL_SUBTITLE to "Sombras suaves, reflexos do piso, brilho de LED e correção ACES — aplicados na exportação 4K.",
            SOFT_SHADOWS to "Sombras suaves",
            AMBIENT_OCCLUSION to "Oclusão de ambiente (AO)",
            REFLECTIONS to "Reflexos",
            FLOOR_REFLECTION to "Reflexo do piso polido",
            LED_GLOW to "Brilho das fitas de LED",
            ACES to "Correção de cor ACES",
            DENOISER to "Redução de ruído",
            QUALITY to "Qualidade do render",
            CONTACT_SHADOWS to "Sombras de contato sob os armários",
            GLASS_REFRACTION to "Refração do vidro da cristaleira",
            APPLY_SETTINGS to "Aplicar ajustes de realismo",
            CLOSE to "Fechar"
        ),
        AppLanguage.TURKISH to mapOf(
            GLOBAL_TITLE to "Küresel Stüdyo 🌍",
            GLOBAL_SUBTITLE to "Dil, birimler ve dünya standartları, gerçek referans mutfaklar ve daha fotogerçekçi render — tek yerde.",
            TAB_GLOBAL to "🌍 Küresel",
            TAB_REFERENCE to "📸 Referans",
            TAB_REALISM to "✨ Gerçekçilik",
            LANGUAGE to "Arayüz dili",
            LANGUAGE_NOTE to "Küresel Stüdyo arayüzü anında çevrilir. Diğer ekranlar hâlâ Arapça.",
            UNITS to "Ölçü birimleri",
            REGION to "Bölge ve standartlar",
            CURRENCY to "Para birimi",
            STANDARDS to "Bölgesel standartlar",
            COUNTERTOP_HEIGHT to "Tezgâh yüksekliği",
            WALKWAY to "En az koridor genişliği",
            BASE_DEPTH to "Alt dolap derinliği",
            REF_TITLE to "Gerçek fotoğraflardan referans mutfaklar",
            REF_SUBTITLE to "Herhangi bir mutfakta Uygula'ya dokunun: oda boyutu, malzeme, ışık ve kamera tek dokunuşta ayarlanır.",
            REF_DIMENSIONS to "Boyutlar",
            REF_CABINETS to "Dolaplar",
            REF_COUNTERTOP to "Tezgâh",
            REF_LIGHTING to "Aydınlatma",
            REF_FEATURES to "Fotoğraf detayları",
            REF_APPLY to "Bu mutfağı uygula",
            REF_APPLIED to "Uygulandı ✓",
            REAL_TITLE to "Fotogerçekçi render ayarları",
            REAL_SUBTITLE to "Yumuşak gölgeler, zemin yansımaları, LED parlaması ve ACES renk düzeltme — 4K dışa aktarımda uygulanır.",
            SOFT_SHADOWS to "Yumuşak gölgeler",
            AMBIENT_OCCLUSION to "Ortam tıkanması (AO)",
            REFLECTIONS to "Yansımalar",
            FLOOR_REFLECTION to "Cilalı zemin yansıması",
            LED_GLOW to "LED şerit parlaması",
            ACES to "ACES sinematik renk düzeltme",
            DENOISER to "Gürültü azaltma",
            QUALITY to "Render kalitesi",
            CONTACT_SHADOWS to "Dolap altı temas gölgeleri",
            GLASS_REFRACTION to "Vitrin camı kırılması",
            APPLY_SETTINGS to "Gerçekçilik ayarlarını uygula",
            CLOSE to "Kapat"
        ),
        AppLanguage.GERMAN to mapOf(
            GLOBAL_TITLE to "Globales Studio 🌍",
            GLOBAL_SUBTITLE to "Sprache, Einheiten und weltweite Normen, echte Referenzküchen und fotorealistischeres Rendering — an einem Ort.",
            TAB_GLOBAL to "🌍 Global",
            TAB_REFERENCE to "📸 Referenz",
            TAB_REALISM to "✨ Realismus",
            LANGUAGE to "Sprache der Oberfläche",
            LANGUAGE_NOTE to "Die Oberfläche des Globalen Studios wird sofort übersetzt. Andere Bildschirme sind noch auf Arabisch.",
            UNITS to "Maßeinheiten",
            REGION to "Region und Normen",
            CURRENCY to "Währung",
            STANDARDS to "Regionale Normen",
            COUNTERTOP_HEIGHT to "Arbeitsplattenhöhe",
            WALKWAY to "Mindestdurchgang",
            BASE_DEPTH to "Tiefe der Unterschränke",
            REF_TITLE to "Referenzküchen nach echten Fotos",
            REF_SUBTITLE to "Tippen Sie auf Anwenden: Raumgröße, Materialien, Licht und Kamera werden in einem Schritt eingestellt.",
            REF_DIMENSIONS to "Maße",
            REF_CABINETS to "Schränke",
            REF_COUNTERTOP to "Arbeitsplatte",
            REF_LIGHTING to "Beleuchtung",
            REF_FEATURES to "Fotodetails",
            REF_APPLY to "Diese Küche anwenden",
            REF_APPLIED to "Angewendet ✓",
            REAL_TITLE to "Fotorealistische Render-Einstellungen",
            REAL_SUBTITLE to "Weiche Schatten, Bodenreflexionen, LED-Glühen und ACES-Farbkorrektur — beim 4K-Export angewendet.",
            SOFT_SHADOWS to "Weiche Schatten",
            AMBIENT_OCCLUSION to "Umgebungsverdeckung (AO)",
            REFLECTIONS to "Reflexionen",
            FLOOR_REFLECTION to "Reflexion des polierten Bodens",
            LED_GLOW to "LED-Streifen-Glühen",
            ACES to "ACES-Farbkorrektur",
            DENOISER to "Rauschunterdrückung",
            QUALITY to "Renderqualität",
            CONTACT_SHADOWS to "Kontaktschatten unter Schränken",
            GLASS_REFRACTION to "Lichtbrechung des Vitrinenglases",
            APPLY_SETTINGS to "Realismus-Einstellungen anwenden",
            CLOSE to "Schließen"
        ),
        AppLanguage.ITALIAN to mapOf(
            GLOBAL_TITLE to "Studio Globale 🌍",
            GLOBAL_SUBTITLE to "Lingua, unità e standard mondiali, cucine reali di riferimento e un render più fotorealistico — in un unico posto.",
            TAB_GLOBAL to "🌍 Globale",
            TAB_REFERENCE to "📸 Riferimento",
            TAB_REALISM to "✨ Realismo",
            LANGUAGE to "Lingua dell'interfaccia",
            LANGUAGE_NOTE to "L'interfaccia dello Studio Globale si traduce subito. Le altre schermate sono ancora in arabo.",
            UNITS to "Unità di misura",
            REGION to "Regione e standard",
            CURRENCY to "Valuta",
            STANDARDS to "Standard regionali",
            COUNTERTOP_HEIGHT to "Altezza del piano di lavoro",
            WALKWAY to "Passaggio minimo",
            BASE_DEPTH to "Profondità dei mobili bassi",
            REF_TITLE to "Cucine di riferimento da foto reali",
            REF_SUBTITLE to "Tocca Applica su una cucina: dimensioni, materiali, luce e fotocamera si impostano in un solo gesto.",
            REF_DIMENSIONS to "Dimensioni",
            REF_CABINETS to "Mobili",
            REF_COUNTERTOP to "Piano di lavoro",
            REF_LIGHTING to "Illuminazione",
            REF_FEATURES to "Dettagli della foto",
            REF_APPLY to "Applica questa cucina",
            REF_APPLIED to "Applicata ✓",
            REAL_TITLE to "Impostazioni di render fotorealistico",
            REAL_SUBTITLE to "Ombre morbide, riflessi del pavimento, bagliore LED e correzione ACES — applicati all'esportazione 4K.",
            SOFT_SHADOWS to "Ombre morbide",
            AMBIENT_OCCLUSION to "Occlusione ambientale (AO)",
            REFLECTIONS to "Riflessi",
            FLOOR_REFLECTION to "Riflesso del pavimento lucido",
            LED_GLOW to "Bagliore delle strisce LED",
            ACES to "Correzione colore ACES",
            DENOISER to "Riduzione del rumore",
            QUALITY to "Qualità del render",
            CONTACT_SHADOWS to "Ombre di contatto sotto i mobili",
            GLASS_REFRACTION to "Rifrazione del vetro della vetrina",
            APPLY_SETTINGS to "Applica le impostazioni",
            CLOSE to "Chiudi"
        )
    )

    /** Localised string for [key] in [language]; falls back English → Arabic. */
    fun t(key: String, language: AppLanguage): String =
        STRINGS[language]?.get(key)
            ?: STRINGS[AppLanguage.ENGLISH]?.get(key)
            ?: STRINGS[AppLanguage.ARABIC]?.get(key)
            ?: key

    /** True when every key has a non-blank translation in every language. */
    fun isComplete(): Boolean =
        AppLanguage.entries.all { lang ->
            ALL_KEYS.all { key -> STRINGS[lang]?.get(key)?.isNotBlank() == true }
        }

    fun missingKeys(): List<String> {
        val missing = mutableListOf<String>()
        for (lang in AppLanguage.entries) {
            for (key in ALL_KEYS) {
                if (STRINGS[lang]?.get(key).isNullOrBlank()) missing += "${lang.code}:$key"
            }
        }
        return missing
    }
}
