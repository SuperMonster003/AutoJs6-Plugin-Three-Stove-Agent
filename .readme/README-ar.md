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

يظل وقت تشغيل المكون الإضافي في الإصدار 1.0.0 معاينة تطوير P0: خدمة INFO و Wake Activity وخدمة `org.autojs.plugin.AI_AGENT` المؤقتة وشاشة تعرض حالة المضيف. نفذ المضيف عقود P1 والوسطاء ومراقبة الشاشة وتنفيذ السكربتات المسجلة ومداخل القائمة الجانبية ومركز الإضافات. تبقى حلقة الوكيل واختيار السكربتات وواجهة `ai.agent` ومساحة المهام لمراحل لاحقة. يلزم AutoJs6 بالبناء 5285; راجع [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) للتقدم والأدلة.

******

### الميزات

******

من المخطط أن يوفر الإصدار 1.0.0 القدرات التالية:

- اختيار السكربتات: تعرض على النموذج السكربتات المسجلة عبر `project.json` أو تعليق رأسي `@agent` مع أوصافها ومخططات معاملاتها; يختار الوكيل أحدها, ويكمل المعاملات, ويطلب التأكيد عند الحاجة, ويشغله داخل AutoJs6 ويقرأ نتيجته المهيكلة.
- تشغيل الشاشة خطوة بخطوة: يراقب الوكيل شجرة عقد إمكانية الوصول بصيغة نصية مضغوطة (ونص الشاشة عبر مكون OCR إضافي عند تثبيته), ثم ينقر ويكتب ويمرر ويضغط المفاتيح عبر وسيط قدرات AutoJs6 حتى يتمكن من التحقق من الهدف.
- الأمان بالتصميم: تعمل أدوات القراءة فقط تلقائيا, وتتطلب الإجراءات الحساسة (الدفع, الإرسال, الحذف, كتابة الملفات, shell, إيماءات الإحداثيات, السكربتات المسجلة كحساسة) تأكيدا, ولكل تشغيل ميزانيات للخطوات واستدعاءات النموذج والمدة والرموز.
- واجهة السكربت وواجهة المستخدم: تعيد `ai.agent.run(goal, options)` مقبض `AgentRun` مع الأحداث والردود والإلغاء; ويوفر التطبيق المستقل مساحة مهام مع السجل والإعدادات المسبقة وذاكرة التفضيلات والإعدادات وسجل الإصدارات.

### دليل الأدوات

تحدد نواة P2.1 هذه الأدوات الثلاثين. لا يزال التنفيذ والتأكيد قيد التطوير; لا يعني هذا الجدول أن المعاينة تنفذ المهام. تولد الأوصاف من دليل أدوات النموذج بالإنجليزية أو الصينية. تتحقق نواة P2.2 أيضا من قرارات الخطوة الواحدة وتوفر قوالب تعليمات بالإنجليزية والصينية; لم يتم ربط تنفيذ المهام بعد.

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

1. ثبت ملف APK للمكون الإضافي من [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) على جهاز به AutoJs6 بالبناء 5285 أو أحدث.
2. افتح مركز المكونات الإضافية في AutoJs6, وتأكد من التعرف على `AI Agent`, ثم فعله. تجتاز حزم الإصدار الرسمية التحقق من التوقيع تلقائيا.
3. افتح AI Agent من المشغل أو خيار الإدارة في القائمة الجانبية لـ AutoJs6. تعرض هذه المعاينة حالة المضيف فقط; تصل مساحة المهام وواجهة `ai.agent` في مراحل لاحقة.

> توفر القائمة الجانبية للمضيف الآن أدوات الاتصال والإدارة. لا يستطيع المكون الحالي تشغيل المهام بعد; تبقى واجهة `ai.agent` ومساحة المهام ضمن P5 و P6 على الترتيب.

******

### الصلاحيات والأمان

******

يلتزم المكون الإضافي بحدود صريحة:

- نقاط دخول Binder محمية بإذن التوقيع `org.autojs.permission.PLUGIN`, لذا لا يصل إليها سوى AutoJs6; وشاشة الإطلاق هي المكون المصدر الآخر الوحيد.
- لا يحتفظ المكون الإضافي بمفاتيح API, ولا يرتبط أبدا بمزود نموذج, ولا يطلب إذن إمكانية الوصول: تمر استدعاءات النموذج وإجراءات الجهاز عبر وسطاء يعيرهم AutoJs6 لرابط مرفق واحد ويسحبهم عند الفصل, وكل منهم مقيد بمنحة (الأساليب المسموح بها, المعدلات, الأحجام, حصة النموذج).
- لا يستخدم المكون الإضافي الشبكة. لا تعلن هذه المعاينة أي إذن سوى إذن المكون الإضافي; وستضاف أذونات خدمة المقدمة والإشعارات والتراكب مع الميزات التي تحتاجها وتوثق هنا.
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
minimum host build: 5285 (6.8.0)
```

تجيب `AiAgentPluginService` على `org.autojs.plugin.AI_AGENT` (الفئة `ai-agent`) في العملية `:agent`; وفي هذه المعاينة تعرض Binder مؤقتا يحمل الواصف `org.autojs.plugin.ai.agent.api.IAiAgentPlugin` حتى تجهز وحدة عقد المضيف. تجيب `AiAgentPluginInfoService` على `org.autojs.plugin.INFO` بـ PluginInfo. وتتيح `WakeActivity` للمضيف تفعيل المكون الإضافي.

******

### خارطة الطريق

******

تدار خطط المكون الإضافي وتقدمه كقائمة قابلة للتحقق في ROADMAP.md, منظمة حسب المرحلة مع معايير القبول ومستويات الأدلة. تعبر البنود غير المحددة عن النية لا عن القدرات الحالية; والنقاش عبر Issues موضع ترحيب.

- [عرض ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md)

******

### سجل الإصدارات

******

#### v1.0.0

_2026/09/23_

- `تلميح` معاينة التطوير P0: هوية المكون الإضافي, وعقد اكتشاف AutoJs6, وشاشة إطلاق تعرض حالة المضيف. لم تنفذ بعد حلقة الوكيل وفهرس السكربتات وواجهة ai.agent ومساحة المهام. راجع ROADMAP.md.
- `تلميح` نفذت عقود AI Agent ووسطاء القدرات والنماذج ومراقبة الشاشة وتنفيذ السكربتات المسجلة ومداخل القائمة الجانبية ومركز الإضافات في المضيف; لا يزال تنفيذ مهام المكون قيد التطوير
- `ميزة` هوية المكون الإضافي `ai-agent` مع خدمة INFO و Wake Activity والخدمة المؤقتة `org.autojs.plugin.AI_AGENT` في العملية `:agent` وشاشة إطلاق تعرض ما إذا كان مضيف AutoJs6 متوافق مثبتا
- `ميزة` README وتعليمات مركز المكونات الإضافية وسجل التغييرات بعشر لغات
- `ميزة` دليل نواة Agent يضم 30 أداة وضبط المجموعات ومخططات المعاملات وإعداد استدعاءات bridge والملاحظات المحدودة وتصعيد المخاطر الحساسة; يأتي ربط التنفيذ في مراحل لاحقة
- `ميزة` نواة قرارات Agent بمخططات لكل بروتوكول وتحليل JSON صارم أو بالاستخراج والتحقق من الأدوات والفروع ومحاولتي تصحيح كحد أقصى وقوالب بالإنجليزية والصينية; تنفيذ المهام غير متصل بعد
- `تحسين` حدد الحد الأدنى للمضيف عند AutoJs6 6.8.0 / البناء 5285 ليتوافق مع تسليم واجهات المضيف ومداخله في P1
- `تبعية` إضافة `common-plugin-api.aar` (وحدة AutoJs6 `plugin-api/common-plugin-api`, بناء المضيف 6.8.0 / 5282, MPL 2.0) كعقد مشترك للمكونات الإضافية, مثبتة بالتجزئة في `locks/host-api-aars.lock`
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
