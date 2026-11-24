# Kitchen 3D Studio — Guest Mode v1.3 👤

الدخول بدون حساب: زر «الدخول كضيف — بدون حساب» في شاشة الدخول يفتح الاستوديو فوراً، بلا Google وبلا Firebase وبلا إنترنت — بدون حذف أي ملف من v1.2.

Enter with no account: a "Continue as Guest" button on the sign-in screen opens the studio immediately — no Google, no Firebase, no internet needed.

## كيف يعمل؟ / How it works

1. في شاشة الدخول، تحت زر Google، يوجد زر أخضر **«الدخول كضيف — بدون حساب»** (`guest_sign_in_button`) وتحته نص الحدود صراحة.
2. الضغطة الواحدة تدخل الاستوديو مباشرة. يُحفظ الاختيار (ومعرّف ضيف ثابت) في SharedPreferences، فعند إعادة فتح التطبيق يدخل الضيف مباشرة دون المرور بالبوابة.
3. داخل الاستوديو يظهر شريط **«ضيف — على الجهاز فقط»** (`guest_mode_chip`) في الشريط العلوي، وبجانبه زر ترقية (`upgrade_to_account_btn`) — الضغط عليه يعيدك للبوابة لتسجيل الدخول بحساب Google. زر الخروج نفسه يخرج من وضع الضيف.

## ما الذي يعمل كضيف؟ وما حدوده؟ / What works, what doesn't

| يعمل كضيف ✅ | لا يعمل كضيف ❌ |
|---|---|
| كل التصميم 2D/3D، الخزائن، الغرف | المزامنة السحابية Firestore |
| الاستوديو العالمي والسينمائي والمرجعي | استرجاع المشاريع من جهاز آخر |
| الرندر والتصدير (PDF/DXF/JSON/JPG/4K) | حفظ التصاميم على حساب سحابي |
| الحفظ المحلي (Room) على جهازك | |
| حاسبة التكلفة وجدول الكميات | |

**نقطة مهمة:** مشاريع الضيف محفوظة محلياً في قاعدة Room على الجهاز، فهي **تبقى على جهازك حتى بعد تسجيل الدخول بحساب Google لاحقاً** — لا تضيع بالترقية.

## لماذا ضيف محلي فقط، وليس حساب Firebase مجهول؟

حساب Firebase المجهول (Anonymous) كان يبدو حلاً أسهل، لكنه يحتاج تفعيل Anonymous في لوحة Firebase (لا يمكن التحقق منه من هنا)، وقد ترفضه قواعد Firestore، فيفشل الدخول تماماً عند من يحتاجه دون إنترنت. وضع الضيف المحلي لا يعتمد على أي إعداد خارجي ولا على الشبكة: يعمل دائماً. المزامنة السحابية تبقى حكراً على حساب Google مسجَّل، ودوال الحفظ السحابي تعود مبكراً أصلاً عندما يكون `currentUserUid` فارغاً — والضيف لا يملك UID أبداً.

## الملفات الجديدة (2)
- `app/src/main/java/com/example/core/auth/GuestSession.kt` — سياسة الضيف الخالصة القابلة للاختبار (`GuestAuthPolicy`) + حفظ الاختيار في SharedPreferences (`GuestSession`)
- `app/src/test/java/com/example/GuestModeTest.kt` — 4 اختبارات وحدة (الدخول، غياب السحابة، الهوية، صراحة الحدود)

## الملفات المعدّلة (3 فقط)
- `MainActivity.kt` — زر الضيف في البوابة + تذكّر الضيف + تخطّي تسجيل الدخول الصامت للضيف + الخروج/الترقية للبوابة
- `presentation/viewmodel/KitchenEditorViewModel.kt` — حالة `isGuestMode`/`guestId` + `enterGuestMode`/`exitGuestMode`
- `presentation/ui/KitchenStudioScreen.kt` — شريط الضيف + زر الترقية في الشريط العلوي
- `app/build.gradle.kts` — الإصدار 1.2 ← **1.3** (versionCode 4)

## البناء
نفس خطوات v1.2 إلى المستودع نفسه `kitchen3d-studio-muse` — بعد الدفع يبدأ GitHub Actions تلقائياً، والـ APK من **Actions ← Artifacts**.
