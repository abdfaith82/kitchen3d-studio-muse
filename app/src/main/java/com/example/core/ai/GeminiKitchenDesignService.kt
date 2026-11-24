package com.example.core.ai

import android.util.Log
import com.example.core.math.Vector3
import com.example.domain.model.KitchenElement
import com.example.domain.model.KitchenElementType
import com.example.domain.model.MaterialFinish
import com.example.domain.model.RoomLayoutShape
import com.example.domain.model.RoomStructure
import com.example.domain.repository.KitchenProject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

private const val TAG = "GeminiKitchenDesigner"

/**
 * Style preferences selectable by the user for AI-generated kitchen layouts.
 */
enum class KitchenAiStylePreference(
    val id: String,
    val arabicTitle: String,
    val subtitleAr: String,
    val defaultCabinetMaterial: MaterialFinish,
    val defaultUpperMaterial: MaterialFinish,
    val defaultCountertop: MaterialFinish,
    val defaultFloor: MaterialFinish,
    val promptGuidance: String
) {
    ROYAL_OAK_AND_CHARCOAL(
        id = "ROYAL_OAK",
        arabicTitle = "ملكي خشب بلوط طبيعي ورمادي أنثراسايت",
        subtitleAr = "خزائن تاج مزدوجة للسقف مع فيترينة زجاج مدخن وإضاءة 3000K",
        defaultCabinetMaterial = MaterialFinish.NATURAL_OAK,
        defaultUpperMaterial = MaterialFinish.CHARCOAL_MATTE,
        defaultCountertop = MaterialFinish.BLACK_GRANITE,
        defaultFloor = MaterialFinish.MARBLE_WHITE_FLOOR,
        promptGuidance = "Warm Natural Oak base cabinets, Charcoal Matte crown/bridge upper cabinets, Smoked Glass vitrine with warm 3000K LED, Black Galaxy Granite countertop."
    ),
    CHAMPAGNE_LUXURY_ACRYLIC(
        id = "CHAMPAGNE_LUXURY",
        arabicTitle = "شامبين أكريليك فاخر وذهبي مصقول",
        subtitleAr = "واجهات لامعة شامبين وكابوتشينو مع بار إفطار مضاء",
        defaultCabinetMaterial = MaterialFinish.CHAMPAGNE_GLOSS,
        defaultUpperMaterial = MaterialFinish.CAPPUCCINO_MATTE,
        defaultCountertop = MaterialFinish.CARRARA_MARBLE,
        defaultFloor = MaterialFinish.MARBLE_WHITE_FLOOR,
        promptGuidance = "High-gloss Champagne Acrylic and Cappuccino Matte cabinets, Carrara Marble countertop, illuminated glass display units."
    ),
    MODERN_MINIMALIST_WHITE(
        id = "MINIMALIST_WHITE",
        arabicTitle = "مودرن مينيمال أبيض لامع ورخام كرارا",
        subtitleAr = "تصميم عصري مشرق بدون مقابض بارزة ومثلث عمل مثالي",
        defaultCabinetMaterial = MaterialFinish.WHITE_HIGH_GLOSS,
        defaultUpperMaterial = MaterialFinish.WHITE_HIGH_GLOSS,
        defaultCountertop = MaterialFinish.CARRARA_MARBLE,
        defaultFloor = MaterialFinish.PARQUET_OAK_FLOOR,
        promptGuidance = "Sleek Minimalist High-Gloss White cabinets, Carrara White Marble countertop, warm oak parquet floor, handleless Aventos lift wall units."
    ),
    INDUSTRIAL_WALNUT_DARK(
        id = "INDUSTRIAL_WALNUT",
        arabicTitle = "كلاسيك عصري جوز داكن وستانلس ستيل",
        subtitleAr = "فخامة خشب الجوز الأمريكي مع جزيرة عمل احترافية",
        defaultCabinetMaterial = MaterialFinish.WALNUT_DARK,
        defaultUpperMaterial = MaterialFinish.SMOKED_GLASS_ALU,
        defaultCountertop = MaterialFinish.BLACK_GRANITE,
        defaultFloor = MaterialFinish.GREY_SLATE_FLOOR,
        promptGuidance = "Rich Dark American Walnut wood cabinets, Smoked Glass aluminum frame wall cabinets, Black Granite countertops, professional chef layout."
    ),
    MEDITERRANEAN_SAGE_GREEN(
        id = "SAGE_GREEN",
        arabicTitle = "أخضر ميرمية هادئ (Sage Green) ورخام أبيض",
        subtitleAr = "طابع أوروبي دافئ مريح للعين مع إضاءة طبيعية",
        defaultCabinetMaterial = MaterialFinish.SAGE_GREEN,
        defaultUpperMaterial = MaterialFinish.WHITE_HIGH_GLOSS,
        defaultCountertop = MaterialFinish.CARRARA_MARBLE,
        defaultFloor = MaterialFinish.PARQUET_OAK_FLOOR,
        promptGuidance = "Soft Mediterranean Sage Green base units, crisp White upper units, Carrara Marble countertops, ergonomic work triangle."
    )
}

/**
 * UI State for Gemini AI Kitchen Design generation.
 */
sealed interface AiDesignUiState {
    data object Idle : AiDesignUiState
    data object Loading : AiDesignUiState
    data class Success(
        val proposalTitle: String,
        val architecturalSummaryAr: String,
        val ergonomicNotesAr: String,
        val lightingRecommendationAr: String,
        val suggestedAccessoriesAr: List<String>,
        val generatedProject: KitchenProject
    ) : AiDesignUiState
    data class Error(val message: String) : AiDesignUiState
}

/**
 * v1.7 — Service that generates complete 3D kitchen layouts with Gemini.
 *
 * It used to call Firebase AI Logic (`Firebase.ai.generativeModel`), which
 * can never work in this app: the repo has no google-services.json by design
 * (see .env.example), so no FirebaseApp ever initializes and every generation
 * failed with "Default FirebaseApp is not initialized" — the exact red error
 * in the user's video. It now calls the Gemini REST API directly with the
 * user's free key from Google AI Studio (stored encrypted via
 * [GeminiApiKeyStore]); no Firebase project, no account, no card.
 *
 * The prompt and the JSON parser are unchanged from the Firebase version —
 * only the transport changed. The response is requested as raw JSON
 * (responseMimeType application/json) and the parser tolerates markdown
 * fences around it, via [GeminiDirectClient.extractJsonObject].
 */
class GeminiKitchenDesignService {

    suspend fun generateKitchenDesign(
        roomWidthMm: Float,
        roomDepthMm: Float,
        roomHeightMm: Float,
        layoutShape: RoomLayoutShape,
        stylePreference: KitchenAiStylePreference,
        includeIsland: Boolean,
        customNotes: String,
        apiKey: String
    ): AiDesignUiState {
        val safeW = roomWidthMm.coerceIn(2400f, 8000f).toInt()
        val safeD = roomDepthMm.coerceIn(2200f, 8000f).toInt()
        val safeH = roomHeightMm.coerceIn(2400f, 3600f).toInt()

        // v1.7 — no key, no generation: guide the user instead of ever
        // surfacing a raw English exception (the old Firebase error).
        if (apiKey.isBlank()) {
            return AiDesignUiState.Error(
                "التوليد يحتاج مفتاح Gemini مجاني: افتح Google AI Studio من متصفح هاتفك (aistudio.google.com) ← Get API key ← أنشئ مفتاحاً والصقه في حقل المفتاح أعلى هذه النافذة، ثم اضغط توليد."
            )
        }

        val prompt = buildString {
            appendLine("أنت مهندس معماري خبير في تصميم المطابخ ثلاثية الأبعاد (CAD Kitchen Architect).")
            appendLine("قم باقتراح تصميم مطبخ كامل ومتكامل هندسياً بدون أي تداخل بين الوحدات بناءً على المعطيات التالية:")
            appendLine("- عرض الغرفة (X): $safeW مم")
            appendLine("- عمق الغرفة (Z): $safeD مم")
            appendLine("- ارتفاع السقف (Y): $safeH مم")
            appendLine("- شكل التوزيع المطلوب: ${layoutShape.titleAr} (${layoutShape.name})")
            appendLine("- النمط المعماري المفضل: ${stylePreference.arabicTitle} - ${stylePreference.promptGuidance}")
            appendLine("- تضمين جزيرة وسطية أو بار: ${if (includeIsland && safeW >= 3800 && safeD >= 3400) "نعم (ضعها في منتصف الغرفة مع ترك ممر حركة لا يقل عن 1000 مم)" else "لا"}")
            if (customNotes.isNotBlank()) {
                appendLine("- ملاحظات إضافية من العميل: $customNotes")
            }
            appendLine()
            appendLine("القواعد الهندسية الإلزامية لتوزيع الوحدات داخل نظام الإحداثيات (X من 0 إلى $safeW، Z من 0 إلى $safeD):")
            appendLine("1. الجدار الشمالي الخلفي (North Wall): zMm = 300 للخزائن السفلية والطويلة، و zMm = 175 للخزائن العلوية، و rotationYDeg = 0. رتب الوحدات على محور X جنبًا إلى جنب بدون تداخل (مثلاً X = 700, 1400, 2100, 2750, 3350...).")
            appendLine("2. إذا كان الشكل L_SHAPE أو U_SHAPE: ضع خزائن على الجدار الغربي (West Wall) عند xMm = 300 و rotationYDeg = 90، وزعها على محور Z (مثلاً zMm = 1350, 2200...).")
            appendLine("3. إذا كان الشكل U_SHAPE: أضف خزائن على الجدار الشرقي (East Wall) عند xMm = ${safeW - 300} و rotationYDeg = 270.")
            appendLine("4. يجب أن يتضمن التصميم مثلث عمل متكامل: حوض غسيل (BASE_SINK_UNIT أو BASE_SINK_VENTED_100)، خزانة أدراج (BASE_4_DRAWERS أو BASE_3_DRAWERS)، منطقة طهي (BASE_UNDER_OVEN + COOKTOP_INDUCTION + RANGE_HOOD_BOX)، برج فرن أو مؤونة (TALL_OVEN_TOWER أو TALL_PANTRY)، وثلاجة (REFRIGERATOR_FROST)، بالإضافة إلى خزائن علوية (WALL_GLASS_BLACK_FRAME / WALL_LIFT_AVENTOS) وإضاءة سقف (CEILING_CHANDELIER_MODERN أو CEILING_LINEAR_LED_PANEL).")
            appendLine("5. اترك مسافة أمامية كافية لا تقل عن 900 مم أمام جميع الأبواب والأدراج لضمان عدم تعارض فتح الأبواب مع معايير التركيب الهندسية.")
            appendLine()
            appendLine("صيغة الإخراج الإلزامية: أرجع كائن JSON واحداً فقط بهذه الحقول بالضبط — proposalTitleAr, architecturalSummaryAr, ergonomicNotesAr, lightingRecommendationAr, suggestedAccessoriesAr (مصفوفة نصوص), floorMaterialName, units (مصفوفة عناصر، كل عنصر به nameAr, typeCode, xMm, yMm, zMm, rotationYDeg, widthMm, heightMm, depthMm, materialName, countertopMaterialName) — بدون أي نص إضافي وبدون تنسيق markdown.")
        }

        return try {
            val result = withTimeout(60_000L) {
                GeminiDirectClient.generateJson(prompt, apiKey.trim())
            }
            when (result) {
                is GeminiDirectResult.Failure -> mapFailureToError(result)
                is GeminiDirectResult.Success -> {
                    if (result.jsonText.isBlank()) {
                        AiDesignUiState.Error("أرجع نموذج Gemini استجابة فارغة. يرجى المحاولة مرة أخرى.")
                    } else {
                        parseStructuredKitchenResponse(
                            jsonString = result.jsonText,
                            roomWidthMm = safeW.toFloat(),
                            roomDepthMm = safeD.toFloat(),
                            roomHeightMm = safeH.toFloat(),
                            layoutShape = layoutShape,
                            stylePreference = stylePreference
                        )
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini kitchen design generation timed out", e)
            AiDesignUiState.Error("انتهت مهلة توليد التصميم عبر الذكاء الاصطناعي (60 ثانية). يرجى المحاولة مجدداً.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini kitchen design generation failed", e)
            AiDesignUiState.Error("تعذر الاتصال بخدمة Gemini AI لتوليد تصميم المطبخ. تحقق من الإنترنت والمفتاح ثم حاول مجدداً.")
        }
    }

    /** Every failure the user can see is Arabic — never a raw English exception. */
    private fun mapFailureToError(failure: GeminiDirectResult.Failure): AiDesignUiState.Error {
        val message = when (failure.kind) {
            GeminiDirectFailure.INVALID_KEY ->
                "المفتاح غير صحيح أو غير مفعّل. تأكد من نسخه كاملاً من Google AI Studio (aistudio.google.com) والصقه مجدداً."
            GeminiDirectFailure.QUOTA_EXCEEDED ->
                "انتهت حصة المفتاح المجاني لهذه الدقيقة/اليوم. انتظر قليلاً ثم حاول مجدداً."
            GeminiDirectFailure.MODEL_UNAVAILABLE ->
                "نماذج Gemini المجانية غير متاحة حالياً. حاول مجدداً بعد قليل."
            GeminiDirectFailure.NETWORK ->
                "تعذّر الاتصال بالإنترنت. تحقق من الاتصال ثم حاول مجدداً."
            GeminiDirectFailure.TIMEOUT ->
                "انتهت مهلة توليد التصميم عبر الذكاء الاصطناعي (60 ثانية). يرجى المحاولة مجدداً."
            GeminiDirectFailure.EMPTY_RESPONSE ->
                "أرجع نموذج Gemini استجابة فارغة. يرجى المحاولة مرة أخرى."
            GeminiDirectFailure.SERVER_ERROR ->
                "تعذّر توليد التصميم حالياً (خطأ من الخدمة). حاول مجدداً."
        }
        return AiDesignUiState.Error(message)
    }

    private fun parseStructuredKitchenResponse(
        jsonString: String,
        roomWidthMm: Float,
        roomDepthMm: Float,
        roomHeightMm: Float,
        layoutShape: RoomLayoutShape,
        stylePreference: KitchenAiStylePreference
    ): AiDesignUiState {
        val json = JSONObject(jsonString)
        val proposalTitle = json.optString("proposalTitleAr", "تصميم مطبخ ذكي - ${stylePreference.arabicTitle}")
        val architecturalSummary = json.optString("architecturalSummaryAr", "")
        val ergonomicNotes = json.optString("ergonomicNotesAr", "")
        val lightingRecommendation = json.optString("lightingRecommendationAr", "")

        val accessoriesArray = json.optJSONArray("suggestedAccessoriesAr")
        val suggestedAccessories = mutableListOf<String>()
        if (accessoriesArray != null) {
            for (i in 0 until accessoriesArray.length()) {
                suggestedAccessories.add(accessoriesArray.optString(i))
            }
        }

        val floorMatName = json.optString("floorMaterialName", stylePreference.defaultFloor.id)
        val floorMaterial = MaterialFinish.ALL_FINISHES.find { it.id == floorMatName || it.name == floorMatName }
            ?: stylePreference.defaultFloor

        val unitsArray = json.optJSONArray("units")
        val parsedElements = mutableListOf<KitchenElement>()
        if (unitsArray != null) {
            for (i in 0 until unitsArray.length()) {
                val unitObj = unitsArray.optJSONObject(i) ?: continue
                val typeCode = unitObj.optString("typeCode", KitchenElementType.BASE_DOUBLE_DOOR.name)
                val elementType = KitchenElementType.entries.find { it.name == typeCode }
                    ?: KitchenElementType.BASE_DOUBLE_DOOR

                val width = unitObj.optInt("widthMm", elementType.defaultDimensions.x.toInt())
                    .toFloat().coerceIn(200f, 2000f)
                val height = unitObj.optInt("heightMm", elementType.defaultDimensions.y.toInt())
                    .toFloat().coerceIn(45f, 2750f)
                val depth = unitObj.optInt("depthMm", elementType.defaultDimensions.z.toInt())
                    .toFloat().coerceIn(150f, 1200f)

                val halfW = width / 2f
                val halfD = depth / 2f
                val x = unitObj.optInt("xMm", (roomWidthMm / 2f).toInt())
                    .toFloat().coerceIn(halfW + 20f, (roomWidthMm - halfW - 20f).coerceAtLeast(halfW + 20f))
                val y = unitObj.optInt("yMm", elementType.defaultElevationY.toInt())
                    .toFloat().coerceIn(0f, (roomHeightMm - height).coerceAtLeast(0f))
                val z = unitObj.optInt("zMm", (depth / 2f).toInt())
                    .toFloat().coerceIn(halfD + 20f, (roomDepthMm - halfD - 20f).coerceAtLeast(halfD + 20f))

                val rotRaw = unitObj.optInt("rotationYDeg", 0)
                val rotY = (((rotRaw / 90) * 90) % 360 + 360) % 360

                val matName = unitObj.optString("materialName", stylePreference.defaultCabinetMaterial.id)
                val material = MaterialFinish.ALL_FINISHES.find { it.id == matName || it.name == matName }
                    ?: stylePreference.defaultCabinetMaterial

                val counterName = unitObj.optString("countertopMaterialName", stylePreference.defaultCountertop.id)
                val countertop = MaterialFinish.ALL_FINISHES.find { it.id == counterName || it.name == counterName }
                    ?: stylePreference.defaultCountertop

                val nameAr = unitObj.optString("nameAr", elementType.title).ifBlank { elementType.title }

                parsedElements.add(
                    KitchenElement(
                        id = "ai_${UUID.randomUUID().toString().take(8)}",
                        name = nameAr,
                        type = elementType,
                        position = Vector3(x, y, z),
                        rotationY = rotY.toFloat(),
                        dimensions = Vector3(width, height, depth),
                        material = material,
                        countertopMaterial = countertop
                    )
                )
            }
        }

        val room = RoomStructure(
            widthMm = roomWidthMm,
            depthMm = roomDepthMm,
            heightMm = roomHeightMm,
            wallThicknessMm = 150f,
            layoutShape = layoutShape,
            walls = RoomStructure.createDefaultWalls(roomWidthMm, roomDepthMm, roomHeightMm, 150f),
            floorMaterial = floorMaterial,
            wallFinish = MaterialFinish.WHITE_HIGH_GLOSS
        ).applyLayoutShape(layoutShape)

        val generatedProject = KitchenProject(
            id = "ai_project_${UUID.randomUUID().toString().take(8)}",
            name = proposalTitle,
            room = room,
            elements = parsedElements
        )

        return AiDesignUiState.Success(
            proposalTitle = proposalTitle,
            architecturalSummaryAr = architecturalSummary,
            ergonomicNotesAr = ergonomicNotes,
            lightingRecommendationAr = lightingRecommendation,
            suggestedAccessoriesAr = suggestedAccessories,
            generatedProject = generatedProject
        )
    }
}
