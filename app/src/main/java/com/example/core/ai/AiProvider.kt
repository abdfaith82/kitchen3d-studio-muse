package com.example.core.ai

/**
 * AI providers for kitchen-design generation (v1.7 Gemini, v1.8 + Groq).
 *
 * Both are free-key providers, no card, no Firebase. The user picks one in
 * the AI designer dialog; each keeps its own encrypted key (see
 * [AiApiKeyStore]) and its own client. The generation prompt and the JSON
 * parser are shared — only the transport and the model differ.
 */
enum class AiProvider(
    val id: String,
    val arabicTitle: String,
    val subtitleAr: String,
    /** Where the user gets the free key, shown in the dialog. */
    val keySourceAr: String,
    /** Short key-source label for the no-key error message. */
    val keySourceShort: String
) {
    GEMINI(
        id = "gemini",
        arabicTitle = "Gemini",
        subtitleAr = "نماذج Google Flash — مفتاح مجاني من Google AI Studio",
        keySourceAr = "مفتاح مجاني من Google AI Studio: افتح aistudio.google.com من متصفح هاتفك ← Get API key ← أنشئ مفتاحاً والصقه هنا.",
        keySourceShort = "Google AI Studio (aistudio.google.com)"
    ),
    GROQ(
        id = "groq",
        arabicTitle = "Groq",
        subtitleAr = "نماذج مفتوحة فائقة السرعة — 1,000 طلب/يوم مجاناً، بلا بطاقة",
        keySourceAr = "مفتاح مجاني من Groq: افتح console.groq.com من متصفح هاتفك ← سجّل دخولك ← API Keys ← Create API Key ← انسخ المفتاح والصقه هنا.",
        keySourceShort = "Groq Console (console.groq.com)"
    )
}
