<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-ai-agent-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>ينفذ مهاما بلغة طبيعية في AutoJs6 عبر اختيار السكربتات المسجلة وتشغيل الشاشة خطوة بخطوة</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-AI-Agent?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-AI-Agent?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-AI-Agent?color=534BAE&label=License"/></a>
  </p>
</div>

******

### اللغات

******

يدعم README.md الحالي اللغات التالية:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ru.md)
- العربية [ar] # الحالي

******

### مقدمة

******

يحول AI Agent هدفا بلغة طبيعية إلى إجراءات على جهاز Android يعمل عليه AutoJs6. فإما أن يختار سكربتا سجله المستخدم لاستخدام الوكيل, ويكمل معاملاته ويشغله; وإما أن يراقب الشاشة عبر شجرة عقد إمكانية الوصول ويتصرف خطوة بخطوة (مراقبة, قرار, تنفيذ, تحقق) حتى يتحقق الهدف, أو يلزم تأكيد, أو تنفد الميزانية. وهو يجيب على [نقاش AutoJs6 رقم 577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

المكون الإضافي هو مكون إضافي لـ AutoJs6 وتطبيق مستقل في آن واحد. تصل إليه السكربتات عبر واجهة `ai.agent` في AutoJs6; ويصل إليه المستخدمون عبر مساحة المهام الخاصة به, ودرج AutoJs6, وكرة عائمة, وقائمة المشاركة في النظام, واختصارات التطبيق, والإدخال الصوتي. تمر استدعاءات النموذج وإجراءات الجهاز دائما عبر AutoJs6 من خلال Binder: يعير المضيف المكون الإضافي وسيط نموذج (مكونات AI Provider الإضافية التي يعرفها المضيف بالفعل, مثل 3-Stone AI) ووسيط قدرات بمنحة محدودة. لا يحتفظ المكون الإضافي أبدا ببيانات اعتماد, ولا يرتبط بنفسه بمزود نموذج, ولا يطلب إذن إمكانية الوصول.

******

### الحالة

******

معاينة تطوير: توفر P6.1-P6.5 المهام والسجل والإعدادات المسبقة وذاكرة التفضيلات. تتطلب واجهة ai.agent إصدار AutoJs6 رقم 5293 أو أحدث. تبقى الواجهات الأخرى ضمن P6.6-P6.7 واختبارات الاعتمادية والإصدار ضمن P7/P8. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

******

### الميزات

******

من المخطط أن يوفر الإصدار 1.0.0 القدرات التالية:

- اختيار السكربتات: تعرض على النموذج السكربتات المسجلة عبر `project.json` أو تعليق رأسي `@agent` مع أوصافها ومخططات معاملاتها; يختار الوكيل أحدها, ويكمل المعاملات, ويطلب التأكيد عند الحاجة, ويشغله داخل AutoJs6 ويقرأ نتيجته المهيكلة.
- تشغيل الشاشة خطوة بخطوة: يراقب الوكيل شجرة عقد إمكانية الوصول بصيغة نصية مضغوطة (ونص الشاشة عبر مكون OCR إضافي عند تثبيته), ثم ينقر ويكتب ويمرر ويضغط المفاتيح عبر وسيط قدرات AutoJs6 حتى يتمكن من التحقق من الهدف.
- الأمان بالتصميم: تعمل أدوات القراءة فقط تلقائيا, وتتطلب الإجراءات الحساسة (الدفع, الإرسال, الحذف, كتابة الملفات, shell, إيماءات الإحداثيات, السكربتات المسجلة كحساسة) تأكيدا, ولكل تشغيل ميزانيات للخطوات واستدعاءات النموذج والمدة والرموز.
- واجهة السكربت وواجهة المستخدم: تعيد `ai.agent.run(goal, options)` مقبض `AgentRun` مع الأحداث والردود والإلغاء; ويوفر التطبيق المستقل مساحة مهام مع السجل والإعدادات المسبقة وذاكرة التفضيلات والإعدادات وسجل الإصدارات.

### دليل الأدوات

معاينة تطوير: تم ربط السكربتات المسجلة وإجراءات الشاشة وواجهة مهام ai.agent. تتطلب الواجهة AutoJs6 build 5293 أو أحدث; تستمر الواجهة الكاملة في P6 واختبارات الموثوقية في P7.

| الأداة | المجموعة | المخاطر | الافتراضي | الوصف |
| --- | --- | --- | --- | --- |
| `app_launch` | `act` | `NORMAL` | `on` | Open an application by package name or display name. |
| `clipboard_get` | `act` | `READ_ONLY` | `on` | Read clipboard text. |
| `clipboard_set` | `act` | `NORMAL` | `on` | Replace clipboard text. |
| `ui_click` | `act` | `NORMAL` | `on` | Click one observed target. |
| `ui_long_click` | `act` | `NORMAL` | `on` | Long-click one observed target. |
| `ui_press_key` | `act` | `NORMAL` | `on` | Use an Android navigation or notification-panel action. |
| `ui_scroll` | `act` | `NORMAL` | `on` | Scroll one observed target a bounded number of times. |
| `ui_set_text` | `act` | `NORMAL` | `on` | Set or append text on one observed editable target. |
| `files_list` | `files` | `NORMAL` | `off` | List workspace files. |
| `files_read` | `files` | `NORMAL` | `off` | Read bounded workspace file text. |
| `files_stat` | `files` | `NORMAL` | `off` | Read workspace file metadata. |
| `files_write` | `files` | `SENSITIVE` | `off` | Write a workspace file after confirmation. |
| `ui_click_xy` | `gesture` | `SENSITIVE` | `off` | Tap coordinates only with the gesture group enabled and confirmation. |
| `ui_gesture` | `gesture` | `SENSITIVE` | `off` | Follow a bounded coordinate path after confirmation. |
| `ui_swipe` | `gesture` | `SENSITIVE` | `off` | Swipe between coordinates after confirmation. |
| `memory_get` | `memory` | `READ_ONLY` | `on` | Read available preference memory in the current scope. |
| `memory_propose` | `memory` | `SENSITIVE` | `on` | Propose a preference for user-approved storage; never store credentials. |
| `app_current` | `observe` | `READ_ONLY` | `on` | Read the current window and application. |
| `console_tail` | `observe` | `READ_ONLY` | `on` | Read bounded recent console lines; they may include unrelated scripts. |
| `device_info` | `observe` | `READ_ONLY` | `on` | Read device information. |
| `screen_state` | `observe` | `READ_ONLY` | `on` | Read whether the screen is on. |
| `ui_dump` | `observe` | `READ_ONLY` | `on` | Observe the current accessibility tree before choosing an action. |
| `ui_find` | `observe` | `READ_ONLY` | `on` | Find nodes matching all selector conditions. |
| `ui_wait_for` | `observe` | `READ_ONLY` | `on` | Wait for a selector to appear or disappear within a deadline. |
| `ocr_screen` | `ocr` | `READ_ONLY` | `auto (OCR)` | Read screen text through the host OCR plugin. |
| `script_catalog` | `script` | `READ_ONLY` | `on` | Find scripts explicitly registered for Agent use. |
| `script_run` | `script` | `NORMAL` | `on` | Run a registered script by id with validated parameters and its registered risk. |
| `script_stop` | `script` | `NORMAL` | `on` | Stop an owned script execution. |
| `shell_exec` | `shell` | `SENSITIVE` | `off` | Execute a bounded non-root shell command after confirmation. |
| `report_progress` | `user` | `READ_ONLY` | `on` | Report bounded progress without declaring task completion. |

******

### الاستخدام

******

1. ثبت ملف APK للمكون الإضافي من [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) على جهاز به AutoJs6 بالبناء 5289 أو أحدث.
2. افتح مركز المكونات الإضافية في AutoJs6, وتأكد من التعرف على `AI Agent`, ثم فعله. تجتاز حزم الإصدار الرسمية التحقق من التوقيع تلقائيا.
3. افتح AI Agent واتصل بـ AutoJs6 وأدخل هدفا وابدأ بالإعداد الافتراضي. أجب أو أكد الإجراءات في بطاقة المهمة وافتح المهام الأخيرة لعرض تفاصيلها.
4. اضبط المجلدات الإضافية من "مجلدات السكربتات" في شاشة البدء, بمسار مطلق واحد لكل سطر. يتحقق المضيف من المسارات المحفوظة ويطبقها; يمكن للمهمة تضييق نطاق المجلدات المعتمدة فقط.
5. حتى 200 مهمة / 32 MiB. تحذف أولا المهام المنتهية التي لم تعرض منذ أطول وقت. تعيد إعادة التشغيل ملء الهدف والإعداد الأصليين في لوحة المهام. راجعهما واضغط زر بدء المهمة للتنفيذ. يحتفظ مسح السجل بالمهام الجارية. يحتفظ التصدير بالعدادات وأسماء الأدوات ونتائج التأكيد. تتم إزالة الأهداف والمعلمات والملاحظات ونتائج البرامج النصية. اختر مكان حفظ الملف.
6. افتح الإعدادات المسبقة من شاشة المهام لحفظ تكوين. الأسماء معرفات ثابتة للسكربتات ونطاقات الذاكرة; انسخ الإعداد لاستخدام اسم آخر. يمكن تعديل default المدمج ولا يمكن حذفه. اختر نموذجا من قائمة المضيف أو احتفظ بالاختيار التلقائي. إذا تعذر استخدام النموذج المحدد تفشل المهمة دون استبداله. يمكن لخيارات المهمة تضييق حدود الإعداد فقط. يشترك السياق الثابت وسياق المهمة في حد 8 KiB. يمكن تضمين الذاكرة العامة وذاكرة الإعداد الحالي, أو أحدهما فقط, أو تعطيل الذاكرة. لا يغير التعديل أو الحذف المهام المنتظرة. التخزين الخاص: حتى 32 إعدادا / 1 MiB.
7. افتح الذاكرة لعرض التفضيلات أو تعديلها أو حذفها أو نسخها. الحد 500 عنصر / 256 KiB مع النطاق ومهمة المصدر والتواريخ. أكد كل memory_propose وكل عنصر مستورد على حدة. أنشئ الإعدادات المسبقة المفقودة أولا. يضيف السياق تلقائيا أحدث العناصر الكاملة ضمن النطاق المسموح حتى 4 KiB مع أولوية الإعداد الحالي عند تطابق المفتاح. يعطل memory: false الإضافة التلقائية فقط. عطل مجموعة memory أو نطاق الذاكرة لمنع الاستعلامات والاقتراحات أيضا. يتضمن التصدير القيم الفعلية ومصدرها. لا تحفظ بيانات الاعتماد. ترفض أسماء المفاتيح وأنماط الرموز التي يمكن التعرف عليها.
8. أجب في لوحة المهام في المقدمة أو افتح الإشعار ذي الأولوية العالية في الخلفية. يعرض التأكيد الأداة والمعلمات والمخاطر والوقت المتبقي. ينطبق السماح المتكرر على الأداة ومستوى المخاطر نفسيهما ضمن هذه المهمة فقط. تتطلب المدفوعات واقتراحات الذاكرة موافقة منفصلة دائما. ينشئ تذكر الإجابة اقتراح memory_propose منفصلا ضمن النطاق المسموح. ينتظر التأكيد عادة 120 ثانية والسؤال حتى 10 دقائق ضمن ميزانية المهمة. تعيد المهلة USER_TIMEOUT ويقرر النموذج السؤال مجددا أو الإبلاغ عن إكمال جزئي. لا تجيب الطلبات القديمة عن الطلبات الجديدة. تعتمد إشعارات الخلفية على الأذونات وإعدادات القنوات. تتبع البطاقات العائمة في P6.7.

> معاينة تطوير: توفر P6.1-P6.5 المهام والسجل والإعدادات المسبقة وذاكرة التفضيلات. تتطلب واجهة ai.agent إصدار AutoJs6 رقم 5293 أو أحدث. تبقى الواجهات الأخرى ضمن P6.6-P6.7 واختبارات الاعتمادية والإصدار ضمن P7/P8.

******

### الصلاحيات والأمان

******

يلتزم المكون الإضافي بحدود صريحة:

- نقاط دخول Binder محمية بإذن التوقيع `org.autojs.permission.PLUGIN`, لذا لا يصل إليها سوى AutoJs6; وشاشة الإطلاق هي المكون المصدر الآخر الوحيد.
- لا يحتفظ المكون الإضافي بمفاتيح API, ولا يرتبط أبدا بمزود نموذج, ولا يطلب إذن إمكانية الوصول: تمر استدعاءات النموذج وإجراءات الجهاز عبر وسطاء يعيرهم AutoJs6 لرابط مرفق واحد ويسحبهم عند الفصل, وكل منهم مقيد بمنحة (الأساليب المسموح بها, المعدلات, الأحجام, حصة النموذج).
- لا إذن للشبكة. تستخدم FOREGROUND_SERVICE و FOREGROUND_SERVICE_SPECIAL_USE للمهام النشطة, و POST_NOTIFICATIONS لإظهار التقدم والإيقاف. لا يطلب إذن إمكانية الوصول أو النوافذ العائمة.
- يبقى سجل المهام والإعدادات المسبقة وذاكرة التفضيلات في التخزين الخاص بالمكون الإضافي; والنسخ الاحتياطي ونقل الجهاز معطلان.

احصل على المكون الإضافي فقط من صفحة [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) الرسمية أو من مركز المكونات الإضافية في AutoJs6. قد تفشل الحزم من مصادر غير معروفة في التحقق من المضيف أو تحمل مخاطر حتى لو بدا رقم الإصدار متطابقا.

******

### واجهة المكون الإضافي

******

المعلومات التالية موجهة لمطوري مضيف AutoJs6 والمكونات الإضافية; يستخدم المضيف هذه المعرفات لاكتشاف المكون الإضافي والتفاوض على التوافق:

```text
application id: io.github.supermonster003.autojs6.plugin.ai.agent
plugin id: ai-agent
engine: ai-agent
variant: default
service action: org.autojs.plugin.AI_AGENT
service category: ai-agent
service process: :agent
info action: org.autojs.plugin.INFO
aidl interface: org.autojs.plugin.ai.agent.api.IAiAgentPlugin
minimum host build: 5289 (6.8.0)
```

`AiAgentPluginService` / `IAiAgentPlugin` / `IAiAgentLink`: اتصال يتحقق من هوية المضيف مع طابور المهام والرد والإلغاء والاستعلام وسجل خطوات خاص; تتوقف المهام عند فقد المضيف ولا تستأنف تلقائيا بعد إعادة تشغيل العملية.

******

### خارطة الطريق

******

تدار خطط المكون الإضافي وتقدمه كقائمة قابلة للتحقق في ROADMAP.md, منظمة حسب المرحلة مع معايير القبول ومستويات الأدلة. تعبر البنود غير المحددة عن النية لا عن القدرات الحالية; والنقاش عبر Issues موضع ترحيب.

- [عرض ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md)

******

### سجل الإصدارات

******

#### v1.0.0

_2026/09/24_

- `تلميح` معاينة تطوير: توفر P6.1-P6.5 المهام والسجل والإعدادات المسبقة وذاكرة التفضيلات. تتطلب واجهة ai.agent إصدار AutoJs6 رقم 5293 أو أحدث. تبقى الواجهات الأخرى ضمن P6.6-P6.7 واختبارات الاعتمادية والإصدار ضمن P7/P8.
- `ميزة` تأكيدات داخل المهام ومن الإشعارات مع المخاطر والعد التنازلي والسماح ضمن المهمة وتأكيد منفصل لحفظ الإجابات
- `ميزة` ذاكرة تفضيلات مع تأكيد كل اقتراح واستعلامات محددة النطاق وحماية من التعارض وحفظ مستقل لكل عنصر وتحرير وحذف ونسخ JSON مع موافقة منفصلة لكل عنصر مستورد
- `ميزة` إعدادات مسبقة مسماة مع الإنشاء والتعديل والنسخ والحذف والتعيين كافتراضي; قائمة نماذج المضيف مع موقع التنفيذ ودعم JSON المنظم, وسياق ثابت وحدود للأدوات والميزانيات وسياسة التأكيد ومجلدات السكربت المسموح بها ونطاق الذاكرة
- `ميزة` تسلسل كامل للخطوات والنتائج ومرشحات الحالة/الإعداد/التاريخ ومسودات إعادة التشغيل والحذف وتصدير JSON منزوع البيانات الخاصة وسجل خاص بإصدارات مع ترحيل وتنظيف LRU (200 مهمة / 32 MiB)
- `ميزة` لوحة مهام بمسار بدء موحد ومظهر المضيف وتفاعلات مضمنة وتقدم الميزانية وحتى 20 مهمة حديثة قابلة للقراءة دون اتصال
- `ميزة` يتطلب الاكتمال أدلة وتعرض النتائج الجزئية العمل المتبقي وتتطلب مهام الطلب أو الدفع حالة الطلب المرصودة
- `ميزة` يحتفظ التحقق بعداد الشاشات التي لم تتغير بعد تقليص السياق ويمنع الطلب الثالث للإجراء نفسه قبل التنفيذ
- `ميزة` انتظار محدود لاستقرار عينة الشاشة بعد الإجراء وإرفاق ملخص التغييرات منذ الإجراء السابق بالمراقبات التالية
- `ميزة` ربط تأكيد إجراءات الشاشة بالعقد التي يفحصها المضيف مع إلحاق النص والتمرير المحدود والإبلاغ عن النتائج وتغير النافذة
- `ميزة` يتاح OCR للشاشة فقط عندما يبلغ المضيف عن إضافة OCR معتمدة ومتاحة مع دمج النص في أسطر محدودة ذات إحداثيات
- `ميزة` تحتفظ مراقبة الشاشة بمراجع لقطات المضيف مع مخرجات محدودة للعقد ووحدة التحكم وملخصات تغير النص والحالة
- `ميزة` تحتفظ مهام السكربت الواحد بالمعرف والمسار ومعرف التنفيذ والنتيجة بعد إنهاء النموذج مع تمييز null الصريح والإشارة إلى اختصار النتائج الكبيرة
- `ميزة` تنفيذ السكربتات المسجلة مع التحقق من البيان المعتمد وملاحظات منظمة وحجب الأسرار في نهاية سجل وحدة التحكم وإيقاف السكربت عند انتهاء المهلة أو إلغاء المهمة
- `ميزة` إدراج ذاكرة التفضيلات حسب النطاق لمعلمات السكربتات مع حد 4 KiB وعلامة اقتطاع وإمكانية التعطيل لكل مهمة
- `ميزة` التحقق من معلمات السكربتات المسجلة مع القيم الافتراضية وأسئلة القيم الناقصة وفحص مخاطر البيان الحالي وجداول المعلمات الكاملة للتأكيد
- `ميزة` `ai-agent`: `AiAgentPluginInfoService`, `WakeActivity`, `AiAgentPluginService`, `ui.LauncherActivity`
- `ميزة` README وتعليمات مركز المكونات الإضافية وسجل التغييرات بعشر لغات
- `ميزة` دليل نواة Agent يضم 30 أداة وضبط المجموعات ومخططات المعاملات وإعداد استدعاءات bridge والملاحظات المحدودة وتصعيد المخاطر الحساسة
- `ميزة` نواة قرارات Agent بمخططات لكل بروتوكول وتحليل JSON صارم أو بالاستخراج والتحقق من الأدوات والفروع ومحاولتي تصحيح كحد أقصى وقوالب بالإنجليزية والصينية
- `ميزة` ميزانيات Agent للخطوات واستدعاءات النموذج والوقت والرموز, مع مهل الأدوات والتفاعل وتقدير الاستخدام وحد رموز الإخراج
- `ميزة` بوابة تأكيد Agent بسياسة افتراضية أو حذرة وصلاحيات داخل المهمة لنفس الأداة والمخاطر وتأكيد كل دفعة وكلمات مفتاحية للدفع بعشر لغات
- `ميزة` سجل خطوات Agent خاص بحد 200 خطوة و1 MiB, مع إخفاء كلمات المرور ونتائج نهائية محدودة تحتفظ بالحالة والعدادات
- `ميزة` اتصال يتحقق من هوية المضيف مع طابور المهام والرد والإلغاء والاستعلام وسجل خطوات خاص; تتوقف المهام عند فقد المضيف ولا تستأنف تلقائيا بعد إعادة تشغيل العملية
- `ميزة` تجميع حتمي لسياق Agent بحدود بايت وأزواج خطوات حديثة كاملة وتعليمات إنجليزية وصينية وأولوية للعقد; تستخدم النماذج المحلية ميزانية إدخال 3000 رمز وتعريفات أدوات موجزة
- `ميزة` عميل نموذج المضيف مع التحقق من ترتيب الأحداث وحساب usage والإلغاء والمهل وتراجع محدود للتنسيق; يحتسب كل تراجع كاستدعاء ويحافظ على حد إصلاح القرار
- `ميزة` طلب اتصال من واجهة التشغيل مع مهلة 15 ثانية وإرشاد لتفعيل AI Agent والتصريح بالاتصال في AutoJs6
- `ميزة` إشعارات أمامية أثناء المهام فقط مع التقدم والإيقاف والعرض; إدخال الرد وتأكيد كل عملية من واجهة التشغيل
- `ميزة` تحديث دليل السكربتات المسجلة عند بدء المهمة مع ذاكرة مؤقتة للاتصال لمدة 60 ثانية وترتيب حتمي لما يصل إلى 24 مرشحا وملخصات محدودة للمعلمات وبحث script_catalog
- `إصلاح` ترتيب السجل والاحتفاظ به حسب وقت بدء المهام لمنع حذف المهام الأحدث عند إعادة كتابة الملفات أثناء إعادة التشغيل
- `إصلاح` التحقق من ملكية interaction للإجابات والتأكيدات لمنع السكربت من الرد نيابة عن واجهة الإضافة
- `إصلاح` تتطلب أزرار تأكيد المعاملة تأكيدا منفصلا للدفع ولا تعيد استخدام أذونات المهمة كاملة
- `إصلاح` تحتفظ النتائج خارج الشاشة ذات الحدود الفارغة أو المعكوسة بالنص وتحدد الإحداثيات كغير قابلة للاستخدام بدلا من خطأ في المعاملات
- `إصلاح` تميز إعادة تحديد العقدة الحدود وقدرات الإجراء لتجنب الخلط بين الحاويات المتداخلة والهدف
- `إصلاح` إرشادات دقيقة لإصلاح مراجع العقد: الحفاظ على البادئة # وحذف snapshotId عند استخدام selector
- `إصلاح` يحمل قبول المهام قواعد نية الطلب مسبقا ويقلل تكلفة تهيئة القواعد
- `إصلاح` يميز التحقق العقد المتشابهة في نوافذ مختلفة ويحافظ على طلب مراقبة الشاشة بعد قراءة الحافظة ولا يصنف نقل الملفات كدفع
- `إصلاح` لم تعد قراءة الشاشة التي لا تستجيب بعد الإجراء تتجاوز مهلة انتظار الاستقرار
- `إصلاح` حجب المعلمات متعددة الأسطر قبل تقسيم سجل وحدة التحكم مع منع كشف الأسرار عندما تطابق المعلمة تسمية بيانات الاعتماد
- `إصلاح` لم تعد الخدمة الأمامية قيد الإنهاء ترفض بدء المهمة التالية
- `تحسين` احتساب ترميز JSON في حجم أوصاف تأكيد السكربتات لتبقى جداول المعلمات الكبيرة ضمن حد أحداث Binder
- `تحسين` الحد الأدنى للمضيف هو AutoJs6 6.8.0 / البناء 5289 لفحص عقد الإجراءات وربط التأكيد بالتنفيذ
- `تبعية` إضافة common-plugin-api و host-capability-api و ai-agent-api من نفس بناء release لـ AutoJs6 6.8.0 / 5289 (MPL 2.0), مثبتة بواسطة SHA-256
- `تبعية` إضافة Gson 2.13.2 للتحليل الصارم والمحدود لـ JSON وأشجار المخططات

##### لمزيد من سجل الإصدارات

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/assets/doc/CHANGELOG-ar.md)

******

### البناء والتحقق

******

يستهدف هذا القسم المطورين الراغبين في بناء المكون الإضافي من المصدر; ويمكن للمستخدمين العاديين ببساطة تثبيت ملف APK الجاهز من صفحة Releases.

بناء APK للتصحيح:

```powershell
.\gradlew.bat :app:assembleDebug
```

تشغيل اختبارات وحدة JVM وبناء APK اختبارات الأجهزة:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

بناء APK الإصدار:

```powershell
.\gradlew.bat :app:assembleRelease
```

جمع ناتج الإصدار وإلحاق الإصدار وملخص CRC32 باسم الملف:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

التحقق من تزامن مصادر التوثيق متعدد اللغات مع النواتج المولدة (يفرض ذلك CI أيضا):

```powershell
py .python\generate_markdown.py --check
```

يتطلب البناء JDK 21 أو أحدث و Android SDK 37; وتدار إصدارات Gradle والمكونات الإضافية مركزيا عبر `version.properties` و `io.github.supermonster003.autojs6-platform-versions`.

******

### التعريب وتوليد التوثيق

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.readme/template_plugin_instruction.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/raw-*/plugin_instruction.md
```

ملفات JSON اللغوية في `.readme/` و `.changelog/` هي المصدر الوحيد لملف README وتعليمات مركز المكونات الإضافية وسجل التغييرات. عدل دائما مصادر JSON هذه وأعد تشغيل `py .python/generate_markdown.py`; ولا تحرر يدويا نواتج README و `plugin_instruction.md` وسجل التغييرات المولدة أبدا. شغل `py .python/generate_markdown.py --check` للتحقق من جميع النواتج المولدة.

******

### الترخيص

******

كود المشروع مرخص بموجب [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/LICENSE). المكونات الخارجية وتراخيصها مدرجة في [إشعارات الجهات الخارجية](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/THIRD_PARTY_NOTICES.md).

******

### روابط

******

- مشروع AutoJs6: https://github.com/SuperMonster003/AutoJs6
- توثيق AutoJs6: https://docs.autojs6.com
- نقاش AutoJs6 رقم 577: https://github.com/SuperMonster003/AutoJs6/discussions/577
- إشعارات الجهات الخارجية: https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/THIRD_PARTY_NOTICES.md
