# EcoGuard Home Android App

هذا المجلد يحتوي على تطبيق Android كامل لمشروع EcoGuard Home للتحكم بالمنزل الذكي من الهاتف عبر Bluetooth HC-05، مع استقبال تنبيهات الحريق وتسرب الغاز من Arduino Mega 2560.

## هل يحتاج تغيير في الهاردوير؟

لا يحتاج تغيير في الهاردوير، بشرط أن يكون HC-05 متصلًا بالـ Arduino Mega بشكل صحيح وأن يكون الهاتف مقترنًا Pairing مع HC-05. التنبيهات تحتاج فقط تحديث كود Arduino حتى يرسل رسائل Bluetooth إلى التطبيق عند اكتشاف Flame Sensor أو MQ-135 لحالة خطر.

## الملفات المهمة

- `app/src/main/java/com/ecoguard/home/MainActivity.java`: كود تطبيق Android.
- `firmware/EcoGuard_Home_App_Ready_Firmware.ino`: كود Arduino Mega المتوافق مع التطبيق.
- `docs/COMMAND_PROTOCOL.md`: بروتوكول الأوامر والرسائل بين الهاتف والـ Arduino.

## طريقة تشغيل التطبيق

1. افتح المجلد `EcoGuardHomeAndroid` باستخدام Android Studio.
2. انتظر مزامنة Gradle.
3. من القائمة اختر: Build > Build APK(s).
4. ثبت ملف APK الناتج على هاتف Android.
5. من إعدادات الهاتف اعمل Pair مع HC-05. غالبًا كلمة المرور الافتراضية: `1234` أو `0000`.
6. افتح التطبيق واضغط Refresh ثم اختر HC-05 واضغط Connect.

## طريقة تشغيل التنبيهات

ارفع الملف `firmware/EcoGuard_Home_App_Ready_Firmware.ino` على Arduino Mega 2560. الكود يرسل رسائل `ALERT` للتطبيق عند اكتشاف Flame Sensor أو MQ-135 للخطر، ويعرض التطبيق إشعارًا مع اهتزاز وتنبيه صوتي حسب صلاحيات الهاتف.

## ملاحظات مهمة

- التطبيق يستخدم Bluetooth Classic SPP، لذلك هو مناسب لـ HC-05.
- التطبيق مخصص لأجهزة Android، وليس iPhone، لأن HC-05 Classic Bluetooth غير مدعوم عادةً من iOS لتطبيقات SPP العادية.
- على Android 12 وما بعده يجب السماح بصلاحية Bluetooth Connect.
- على Android 13 وما بعده يجب السماح بصلاحية Notifications حتى تظهر الإشعارات خارج التطبيق.
- إذا كان كود Arduino القديم مستخدمًا بدون تحديث، سيعمل التحكم اليدوي فقط، أما التنبيهات داخل التطبيق فتحتاج الكود الجديد لأنه يرسل حالة الحريق والغاز إلى الهاتف.
