# ميزة Gemini AI بعد الإصلاح — v1.7 🔑

## ما الذي كان يحدث؟
عند فتح «مهندس المطابخ الذكي بـ Gemini AI» والضغط على زر التوليد، كان يظهر خطأ أحمر بالإنجليزية:
> Default FirebaseApp is not initialized in this process com.aistudio.kitchendesigner.wovqp. Make sure to call FirebaseApp.initializeApp(Context) first

## السبب الجذري (مؤكد بالفحص)
- ميزة التوليد كانت تستدعي **Firebase AI Logic** (`Firebase.ai.generativeModel("gemini-3.8-flash")`)، وهذا يتطلب تطبيق Firebase مهيأً من ملف `google-services.json`.
- **الملف غير موجود في المستودع أصلاً — وبقرار مقصود** (انظر `.env.example`: «The repo has no google-services.json… Keep the placeholder "not-configured" to build without Google Sign-In»). لذلك لم يتهيأ Firebase أبداً، وفشل التوليد عند كل مستخدم، بينما كان تسجيل الدخول يتخطى المشكلة بهدوء (try/catch في MainActivity) — أما التوليد فكان يعرض نص الخطأ الإنجليزي الخام كما هو.

## الإصلاح (v1.7)
- **لا Firebase إطلاقاً في هذه الميزة**: التوليد الآن يستدعي Gemini REST API مباشرة (`generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`) بمفتاح مجاني من المستخدم.
- **المفتاح**: مجاني من Google AI Studio — افتح `aistudio.google.com` من متصفح الهاتف ← Get API key ← أنشئ مفتاحاً والصقه في حقل المفتاح أعلى النافذة. يُحفظ مشفّراً AES-256/GCM في AndroidKeyStore على الجهاز فقط (`GeminiApiKeyStore.kt`)، ولا يُرسل إلا إلى Google.
- **النماذج**: أسماء النماذج تتقاعد بسرعة (مثلاً `gemini-2.0-flash` صار يعيد 404 منذ منتصف 2026)، لذا يجرّب العميل سلسلة احتياطية بالترتيب: `gemini-2.5-flash` ← `gemini-3.5-flash` ← `gemini-3.8-flash`، وينتقل للتالي فقط عند 404.
- **الأخطاء كلها بالعربية**: مفتاح غير صحيح / انتهاء الحصة / انقطاع الإنترنت / انتهاء المهلة — لا يصل المستخدم أي نص إنجليزي خام بعد الآن.
- **الاعتماد `firebase.ai` أُزيل** من `app/build.gradle.kts` (لم يكن مستخدماً في أي مكان آخر).
- **الاختبارات**: `GeminiDirectClientTest.kt` (7 اختبارات) — استخراج نص الاستجابة، تجريد JSON من أسوار markdown، سلسلة النماذج، واختبار أن المفتاح الفارغ يعيد إرشاداً عربياً دون أي اتصال بالشبكة.

## حدود صادقة
- المفتاح المجاني له حصة (طلبات/دقيقة ويومية) من Google — عند انتهائها يظهر «انتهت حصة المفتاح المجاني» وتعمل بعد الانتظار.
- التوليد يحتاج إنترنت؛ بدونها تظهر رسالة عربية واضحة.
- لا حاجة لأي حساب Firebase أو بطاقة أو ملف `google-services.json` — وهذا هو المقصود أصلاً في هذا المستودع.
