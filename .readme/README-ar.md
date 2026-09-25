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

يوفر الإصدار 1.0.0 مهام باللغة الطبيعية واستدعاء البرامج النصية المسجلة وإجراءات الجهاز مع تأكيد حسب مستوى المخاطر. راجع [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) للحالات التي تم التحقق منها وقيود النماذج واختبارات الأجهزة المتبقية. استدعاء الأدوات الأصلي والإدخال المرئي وتوليد البرامج النصية ديناميكيا مخطط لها في 1.1.0.

******

### الميزات

******

يوفر التنفيذ الحالي القدرات التالية:

- اختيار السكربتات: تعرض على النموذج السكربتات المسجلة عبر `project.json` أو تعليق رأسي `@agent` مع أوصافها ومخططات معاملاتها; يختار الوكيل أحدها, ويكمل المعاملات, ويطلب التأكيد عند الحاجة, ويشغله داخل AutoJs6 ويقرأ نتيجته المهيكلة.
- تشغيل الشاشة خطوة بخطوة: يراقب الوكيل شجرة عقد إمكانية الوصول بصيغة نصية مضغوطة (ونص الشاشة عبر مكون OCR إضافي عند تثبيته), ثم ينقر ويكتب ويمرر ويضغط المفاتيح عبر وسيط قدرات AutoJs6 حتى يتمكن من التحقق من الهدف.
- الأمان بالتصميم: تعمل أدوات القراءة فقط تلقائيا, وتتطلب الإجراءات الحساسة (الدفع, الإرسال, الحذف, كتابة الملفات, shell, إيماءات الإحداثيات, السكربتات المسجلة كحساسة) تأكيدا, ولكل تشغيل ميزانيات للخطوات واستدعاءات النموذج والمدة والرموز.
- واجهة السكربت وواجهة المستخدم: تعيد `ai.agent.run(goal, options)` مقبض `AgentRun` مع الأحداث والردود والإلغاء; ويوفر التطبيق المستقل مساحة مهام مع السجل والإعدادات المسبقة وذاكرة التفضيلات والإعدادات وسجل الإصدارات.

### لقطات الواجهة

واجهة إنجليزية فعلية على Android API 37.1 بمهام تجريبية ونموذج ذي ردود محددة مسبقا. الصور توضح الواجهة ولا تثبت نجاح مهام بنموذج حقيقي. لا تتضمن بيانات حسابات خاصة. [طريقة الالتقاط](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/README.md).

| لوحة المهام | تفاصيل المهمة |
| --- | --- |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/workbench.png?raw=true" alt="لوحة المهام" width="288" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/detail.png?raw=true" alt="تفاصيل المهمة" width="288" /> |
| تأكيد الإجراء | إدخال المهمة العائم |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/confirmation.png?raw=true" alt="تأكيد الإجراء" width="288" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/floating.png?raw=true" alt="إدخال المهمة العائم" width="288" /> |

******

### التثبيت

******

1. ثبت ملف APK للمكون الإضافي من [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) على جهاز به AutoJs6 بالبناء 5293 أو أحدث.
2. افتح مركز المكونات الإضافية في AutoJs6, وتأكد من التعرف على `AI Agent`, ثم فعله. تجتاز حزم الإصدار الرسمية التحقق من التوقيع تلقائيا.

ثبت وفعل [3-Stone AI](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stone-AI) ثم اضبط نموذجا عبر الإنترنت أو استورد نموذجا محليا مدعوما فيه. يختار وسيط المضيف الحالي 3-Stone AI; يحتاج أي Provider آخر إلى تكامل في المضيف. اختر النموذج من AI Agent > Presets. تشير Connected to AutoJs6 إلى اتصال المضيف; اختيار النموذج موجود في الإعدادات المسبقة.

### التوافق

Android 7.0+ (API 24). يتطلب الاتصال AutoJs6 6.8.0 / build 5289+; تتطلب واجهة المهام الكاملة وهذا المثال build 5293+. استخدم بناء للمضيف يتضمن تغييرات Agent. تتطلب عمليات الشاشة خدمة تسهيل الاستخدام في المضيف. OCR اختياري ويتطلب إضافة مثبتة ومصرحا بها يبلغ المضيف بتوفرها. لا يحتفظ AI Agent ببيانات اعتماد النموذج ولا يملك خدمة تسهيل استخدام مستقلة.

### البدء من الواجهة

افتح AI Agent واتصل بـ AutoJs6 وأدخل هدفا وابدأ بالإعداد الافتراضي. أجب أو أكد الإجراءات في بطاقة المهمة وافتح المهام الأخيرة لعرض تفاصيلها.

### البدء من سكربت

شغل JavaScript التالي في AutoJs6 بعد اتصال AI Agent وإعداد نموذج. تتولى واجهة الإضافة الأسئلة والتأكيدات. لاستخدام إعداد محفوظ أضف `preset: "your-preset-name"` إلى الخيارات.

```javascript
let run = ai.agent.run('اقرأ إصدار Android وأبلغ بالقيمة المرصودة.', {
    tools: ['observe', 'user'],
    interaction: 'plugin',
    budget: { maxSteps: 8 },
});
run.on('progress', (event) => console.log(event.message));
run.result.then(
    (result) => console.log(result.status, result.summary),
    (error) => console.error(error.code, error.message),
);
```

افحص `result.status`: قد تكون النتيجة completed أو partial أو failed أو blocked أو cancelled حتى عند تحقق Promise. يوقف `run.cancel()` المهمة. راجع [ai.agent API](https://docs.autojs6.com/#ai) لاختيار النموذج والأحداث والميزانيات وردود السكربت.

### تسجيل سكربت

احفظ المثال باسم `text-counter.js` في مجلد عمل AutoJs6 أو مجلد سكربتات معتمد من المضيف. تسجل كتلة JSDoc الأولى التي تحتوي `@agent` الملف في الفهرس. اطلب حساب حروف نص محدد; تسأل المهمة عن المعاملات المطلوبة الناقصة قبل التنفيذ.

```javascript
/**
 * @agent
 * @description Count Unicode characters in the supplied text
 * @param {string} text Text to count
 * @risk readonly
 * @confirm never
 * @timeout 10000
 */
let context = ai.agent.context();
if (!context) throw Error('Start this registered script through AI Agent');
let text = new java.lang.String(context.parameters.text);
ai.agent.result({ characters: text.codePointCount(0, text.length()) });
```

يمكن بدلا من ذلك وضع `project.json` التالي بجانب `main.js` الذي يقرأ `ai.agent.context().parameters` ويستدعي `ai.agent.result(...)` كما سبق. يوضع تسجيل المشروع في كائن `agent`.

```json
{
  "name": "Text counter",
  "main": "main.js",
  "agent": {
    "id": "text-counter",
    "description": "Count Unicode characters in the supplied text",
    "parameters": {
      "type": "object",
      "properties": { "text": { "type": "string" } },
      "required": ["text"],
      "additionalProperties": false
    },
    "risk": "readonly",
    "confirm": "never",
    "timeoutMs": 10000
  }
}
```

أنواع المعاملات هي string وnumber وinteger وboolean; الكائنات المتداخلة والمصفوفات غير مدعومة. تتطلب سكربتات sensitive تأكيدا قبل كل تشغيل. سجل سكربتات راجعتها فقط; وصف المخاطر لا يعزل JavaScript. [صيغة التسجيل الكاملة](https://github.com/SuperMonster003/AutoJs6/blob/master/docs/dev/agent-script-manifest-v1.md).

### دليل الأدوات

يولد الجدول من ToolCatalog المضمن. قد يرتفع الخطر بحسب هدف الشاشة; يؤكد الوضع الحذر أيضا الإجراءات غير المخصصة للقراءة فقط. تحدد الإعدادات والإعدادات المسبقة وخيارات المهمة ومنح المضيف المجموعات المتاحة.

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

### الإعدادات المسبقة والذاكرة

افتح الإعدادات المسبقة من شاشة المهام لحفظ تكوين. الأسماء معرفات ثابتة للسكربتات ونطاقات الذاكرة; انسخ الإعداد لاستخدام اسم آخر. يمكن تعديل default المدمج ولا يمكن حذفه. اختر نموذجا من قائمة المضيف أو احتفظ بالاختيار التلقائي. إذا تعذر استخدام النموذج المحدد تفشل المهمة دون استبداله. يمكن لخيارات المهمة تضييق حدود الإعداد فقط. يشترك السياق الثابت وسياق المهمة في حد 8 KiB. يمكن تضمين الذاكرة العامة وذاكرة الإعداد الحالي, أو أحدهما فقط, أو تعطيل الذاكرة. لا يغير التعديل أو الحذف المهام المنتظرة. التخزين الخاص: حتى 32 إعدادا / 1 MiB.

افتح الذاكرة لعرض التفضيلات أو تعديلها أو حذفها أو نسخها. الحد 500 عنصر / 256 KiB مع النطاق ومهمة المصدر والتواريخ. أكد كل memory_propose وكل عنصر مستورد على حدة. أنشئ الإعدادات المسبقة المفقودة أولا. يضيف السياق تلقائيا أحدث العناصر الكاملة ضمن النطاق المسموح حتى 4 KiB مع أولوية الإعداد الحالي عند تطابق المفتاح. يعطل memory: false الإضافة التلقائية فقط. عطل مجموعة memory أو نطاق الذاكرة لمنع الاستعلامات والاقتراحات أيضا. يتضمن التصدير القيم الفعلية ومصدرها. لا تحفظ بيانات الاعتماد. ترفض أسماء المفاتيح وأنماط الرموز التي يمكن التعرف عليها.

### الاستخدام

- اضبط المجلدات الإضافية من "مجلدات السكربتات" في شاشة البدء, بمسار مطلق واحد لكل سطر. يتحقق المضيف من المسارات المحفوظة ويطبقها; يمكن للمهمة تضييق نطاق المجلدات المعتمدة فقط.
- حتى 200 مهمة / 32 MiB. تحذف أولا المهام المنتهية التي لم تعرض منذ أطول وقت. تعيد إعادة التشغيل ملء الهدف والإعداد الأصليين في لوحة المهام. راجعهما واضغط زر بدء المهمة للتنفيذ. يحتفظ مسح السجل بالمهام الجارية. يحتفظ التصدير بالعدادات وأسماء الأدوات ونتائج التأكيد. تتم إزالة الأهداف والمعلمات والملاحظات ونتائج البرامج النصية. اختر مكان حفظ الملف.
- أجب في لوحة المهام في المقدمة أو افتح الإشعار ذي الأولوية العالية في الخلفية. يعرض التأكيد الأداة والمعلمات والمخاطر والوقت المتبقي. ينطبق السماح المتكرر على الأداة ومستوى المخاطر نفسيهما ضمن هذه المهمة فقط. تتطلب المدفوعات واقتراحات الذاكرة موافقة منفصلة دائما. ينشئ تذكر الإجابة اقتراح memory_propose منفصلا ضمن النطاق المسموح. ينتظر التأكيد عادة 120 ثانية والسؤال حتى 10 دقائق ضمن ميزانية المهمة. تعيد المهلة USER_TIMEOUT ويقرر النموذج السؤال مجددا أو الإبلاغ عن إكمال جزئي. لا تجيب الطلبات القديمة عن الطلبات الجديدة. تعتمد إشعارات الخلفية على الأذونات وإعدادات القنوات.
- افتح الإعدادات من المهام لاختيار مجموعات الأدوات والميزانيات والوضع الحذر والصوت والإعداد الافتراضي. تسري التغييرات على المهام الجديدة. تبدأ gesture/files/shell معطلة ويتطلب OCR إضافة مضيف متاحة ومصرحا بها. ترث الميزانية الفارغة القيم الأولية ضمن حدود البروتوكول. لا يمكن للإعدادات المسبقة والخيارات إلا تضييق الحدود. تعرض إدارة البيانات عدد العناصر والبايتات ويستلزم مسح الفئة التأكيد وعدم وجود مهمة نشطة. يعيد مسح الإعدادات المسبقة default المدمج. تتوفر أيضا مجلدات النصوص والتراخيص والمصدر.
- سجل الإصدارات والإشعارات القانونية متاحان دون اتصال. فحص GitHub Releases يدوي مع تخزين النتائج الناجحة 24 ساعة وإمكانية الإلغاء وتجاهل الإصدار. يفتح الحوار السجل الداخلي أو صفحة الإصدار في المتصفح. لا توجد فحوص تلقائية أو تنزيلات APK.
- فعّل الكرة العائمة من الإعدادات واسمح بالظهور فوق التطبيقات ثم احفظ. هي معطلة افتراضيا وتظهر فقط عند اتصال AutoJs6 وتختفي عند القفل أو الانقطاع دون خدمة أمامية أثناء الخمول. اسحب لتحريكها واضغط لإدخال هدف واختيار إعداد مسبق والرد أو الإيقاف. يعيد طي البطاقة إشعارات التأكيد في الخلفية. شارك نصا عاديا أو استخدم اختصار مهمة جديدة أو ثبّت إعدادا مسبقا مع هدف اختياري. تفتح جميع المداخل مسودة قابلة للتحرير وتتطلب بدءا صريحا. لا يستبدل الإعداد المحذوف تلقائيا. يتبع التعرف الصوتي لغة الواجهة ويختفي عند عدم توفره ويملأ النص دون إرساله.

### الأسئلة الشائعة

**لماذا يلزم AutoJs6?**

تدير الإضافة حلقة المهمة والواجهة. يدير AutoJs6 الوصول إلى النماذج وإجراءات تسهيل الاستخدام وتشغيل السكربتات المسجلة. يمكن قراءة السجل دون مضيف متوافق متصل لكن لا يمكن بدء مهام جهاز جديدة. انقطاع المضيف يحجب المهام النشطة; إعادة الاتصال لا تعيد تشغيلها تلقائيا.

**لماذا يحتاج كل دفع إلى تأكيد?**

الدفع إجراء حساس مستقل. الموافقة على طلب أو سكربت أو إجراءات مشابهة لا توافق على الدفع. كل إجراء دفع مكتشف يتطلب تأكيده الخاص وانتهاء المهلة يعني الرفض. راجع المتجر والمنتجات والعنوان والمبلغ قبل الموافقة.

**ما قيود النماذج المحلية?**

تعتمد المهام على اتباع التعليمات وقرارات JSON صالحة والسياق المتاح. قد تفشل النماذج الصغيرة رغم نجاح تحميلها; لم ينجح اختبار Wi-Fi المسجل لـ Gemma 4 E2B IT في التحقق من القرار. ابدأ بمهام صغيرة وراجع نتائج partial/failed. يستخدم 1.0.0 نصوص العقد وOCR وحلقة قرارات JSON; الإدخال المرئي واستدعاء الأدوات الأصلي وتوليد السكربتات ضمن خطة 1.1.0.

******

### الصلاحيات والأمان

******

يلتزم المكون الإضافي بحدود صريحة:

- تحمي صلاحية التوقيع org.autojs.permission.PLUGIN مداخل عقد Binder. المشغل (بما فيه الاختصارات) وهدف المشاركة text/plain ACTION_SEND عامان ويقبلان مسودات هدف وإعداد مسبق محدودة فقط. لا يمكن لـ Intent خارجي تنفيذ المهام أو تأكيدها أو تغيير الصلاحيات. تبقى الإعدادات والنتائج الصوتية والتحكم خاصة.
- لا يحتفظ المكون الإضافي بمفاتيح API, ولا يرتبط أبدا بمزود نموذج, ولا يطلب إذن إمكانية الوصول: تمر استدعاءات النموذج وإجراءات الجهاز عبر وسطاء يعيرهم AutoJs6 لرابط مرفق واحد ويسحبهم عند الفصل, وكل منهم مقيد بمنحة (الأساليب المسموح بها, المعدلات, الأحجام, حصة النموذج).
- تستخدم INTERNET فقط لفحص إصدارات GitHub يدويا. تدعم FOREGROUND_SERVICE وFOREGROUND_SERVICE_SPECIAL_USE المهام النشطة وتعرض POST_NOTIFICATIONS التقدم والتأكيدات. تطلب SYSTEM_ALERT_WINDOW فقط عند تفعيل الكرة من الإعدادات. لا تطلب صلاحيات تسهيل الاستخدام أو التخزين أو الميكروفون.
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

_2026/09/25_

- `تلميح` يوفر الإصدار 1.0.0 مهام باللغة الطبيعية واستدعاء البرامج النصية المسجلة وإجراءات الجهاز مع تأكيد حسب مستوى المخاطر. راجع [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) للحالات التي تم التحقق منها وقيود النماذج واختبارات الأجهزة المتبقية. استدعاء الأدوات الأصلي والإدخال المرئي وتوليد البرامج النصية ديناميكيا مخطط لها في 1.1.0.
- `تلميح` يتطلب Android 7+ وAutoJs6 6.8.0 / build 5293+ لواجهة المهام, مع تفعيل 3-Stone AI وإعداد نموذج. OCR اختياري. بروتوكول الاتصال وحده يتطلب build 5289+ من المضيف.
- `ميزة` لوحة مهام باللغة الطبيعية مع أسئلة وتقدم وإيقاف ونتائج; إدخال عائم اختياري ومشاركة نص واختصارات إعدادات مسبقة ومسودات صوتية
- `ميزة` واجهة ai.agent لإنشاء المهام وأحداثها واستعلاماتها وردودها وإلغائها, بما يشمل المهام detached ونتائج وسياق السكربتات المسجلة
- `ميزة` تسجيل سكربتات project.json / @agent مع بحث الفهرس والتحقق من المعاملات وقيمها الافتراضية وطلب الناقص والتأكيد والتنفيذ المحدود والنتائج المنظمة
- `ميزة` مراقبة الشاشة عبر نصوص العقد وOCR المصرح به اختياريا, والنقر والإدخال والتمرير والمفاتيح بمراجع العقد مع فحص تغير الشاشة ودليل الإنجاز
- `ميزة` نماذج محلية وعبر الإنترنت من وسيط AutoJs6 دون تخزين بيانات اعتماد; يفشل الهدف المحدد غير المتاح دون تبديل صامت للنموذج
- `ميزة` ميزانيات الخطوات واستدعاءات النموذج والوقت وtoken, ومهل الأدوات ومحاولتا إصلاح قرار كحد أقصى لكل خطوة وحماية من تكرار الإجراءات غير الفعالة
- `ميزة` إعدادات مسبقة مسماة وإعدادات عامة للنماذج والسياق ومجموعات الأدوات والميزانيات والحذر ومجلدات السكربت والذاكرة; gesture/files/shell معطلة افتراضيا
- `ميزة` ذاكرة تفضيلات ذات نطاق مع موافقة فردية للاقتراح والاستيراد وتحرير وحذف ونسخ JSON, بحد 500 إدخال / 256 KiB; الحقن التلقائي بحد 4 KiB
- `ميزة` تفاصيل وخطوات المهام وترشيحها ومسودات إعادة التشغيل وتصدير JSON منقح, بسجل خاص بحد 200 مهمة / 32 MiB
- `ميزة` تأكيد حسب المخاطر في اللوحة والإشعارات والبطاقة العائمة; الدفع والذاكرة يتطلبان موافقة فردية دائما; فقد المضيف يحجب المهام ولا يستأنفها إعادة تشغيل العملية
- `ميزة` إعدادات وسجل إصدارات وإشعارات قانونية دون اتصال بعشر لغات; فحص GitHub اليدوي يدعم الإلغاء والتخزين اليومي وتجاهل إصدارات دون تنزيل APK تلقائيا
- `إصلاح` إنهاء المهمة مبكرا عند تفسير الميزانية المتبقية على أنها ميزانية مستهلكة
- `إصلاح` مساحات لمس النماذج والمرشحات والتفاف نصوص القوائم وأعمدة معاملات السكربت وتخطيط عناصر التحكم العائمة مع الخطوط الكبيرة وعلى Android 7
- `إصلاح` تجاوز التحقق من بيانات الاعتماد في ذاكرة التفضيلات باستخدام أحرف كاملة العرض أو أحرف بعرض صفري أو بعض أسماء بيانات الاعتماد
- `إصلاح` بقاء كرة المهام العائمة مخفية عند إيقاظ جهاز بلا قفل آمن قبل استقرار حالة الشاشة
- `إصلاح` تسجيل المهام المنقطعة بسبب انتهاء عملية الإضافة كمهام فاشلة بعد إعادة التشغيل, وإيقاف إجراءات الشاشة عند اكتشاف قفلها
- `إصلاح` ترفض أدوات الملفات اجتياز المسارات والمسارات المطلقة ومسارات مساحة العمل غير الصالحة قبل التأكيد أو الإرسال إلى المضيف; يسجل سجل المهام فئات الرفض المحدودة دون نص النموذج المرفوض
- `إصلاح` يعود التأكيد إلى التطبيق المستهدف قبل استئناف الإجراءات, ويعالج الرد بعد توقف الشاشة, ويطوي البطاقة العائمة قبل التنفيذ
- `إصلاح` إصلاح تعطل فتح التطبيق على Android 13 عند قراءة متحكم أشرطة النظام قبل إنشاء نافذة العرض
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
- `تحسين` إعادة استخدام أجزاء التعليمات والملاحظات غير المتغيرة عند تقليص سجل المهام الطويل لتقليل وقت معالجة كل خطوة
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
