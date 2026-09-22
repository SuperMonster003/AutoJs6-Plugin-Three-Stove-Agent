يحول AI Agent هدفا بلغة طبيعية إلى إجراءات على جهاز Android يعمل عليه AutoJs6. فإما أن يختار سكربتا سجله المستخدم لاستخدام الوكيل, ويكمل معاملاته ويشغله; وإما أن يراقب الشاشة عبر شجرة عقد إمكانية الوصول ويتصرف خطوة بخطوة (مراقبة, قرار, تنفيذ, تحقق) حتى يتحقق الهدف, أو يلزم تأكيد, أو تنفد الميزانية. وهو يجيب على [نقاش AutoJs6 رقم 577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

الإصدار 1.0.0 هو معاينة التطوير P0 من خارطة الطريق: هوية المكون الإضافي, وعقد اكتشاف AutoJs6 (خدمة INFO و Wake Activity والخدمة المؤقتة `org.autojs.plugin.AI_AGENT`), وشاشة إطلاق تعرض حالة المضيف. لم تنفذ بعد حلقة الوكيل وفهرس السكربتات وواجهة `ai.agent` ومساحة المهام; ويسجل التقدم والأدلة في [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md). سيتطلب المكون الإضافي AutoJs6 بالبناء 5283 أو أحدث.

### الاستخدام

1. ثبت ملف APK للمكون الإضافي من [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) على جهاز به AutoJs6 بالبناء 5283 أو أحدث.
2. افتح مركز المكونات الإضافية في AutoJs6, وتأكد من التعرف على `AI Agent`, ثم فعله. تجتاز حزم الإصدار الرسمية التحقق من التوقيع تلقائيا.
3. افتح AI Agent من المشغل: في هذه المعاينة تعرض الشاشة فقط ما إذا كان مضيف AutoJs6 متوافق مثبتا. تصل مساحة المهام ومدخل الدرج وواجهة `ai.agent` مع مراحل خارطة الطريق اللاحقة.

راجع [README المشروع](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent) و [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) للاطلاع على دليل الاتصال والتقدم الحالي.
