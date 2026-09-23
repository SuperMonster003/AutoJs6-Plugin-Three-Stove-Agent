يحول AI Agent هدفا بلغة طبيعية إلى إجراءات على جهاز Android يعمل عليه AutoJs6. فإما أن يختار سكربتا سجله المستخدم لاستخدام الوكيل, ويكمل معاملاته ويشغله; وإما أن يراقب الشاشة عبر شجرة عقد إمكانية الوصول ويتصرف خطوة بخطوة (مراقبة, قرار, تنفيذ, تحقق) حتى يتحقق الهدف, أو يلزم تأكيد, أو تنفد الميزانية. وهو يجيب على [نقاش AutoJs6 رقم 577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

تعرض النسخة المثبتة حالة المضيف فقط. تم تنفيذ واختبار واجهات P1 ونواة الأدوات والقرارات والتشغيل والسياق وعميل النموذج P2.1-P2.4. يحتاج التنفيذ الفعلي إلى ربط Binder وخدمة المقدمة P2.5 ومحولات التنفيذ P3/P4. تأتي واجهة السكربتات ولوحة المهام في P5/P6. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

### الاستخدام

1. ثبت ملف APK للمكون الإضافي من [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) على جهاز به AutoJs6 بالبناء 5285 أو أحدث.
2. افتح مركز المكونات الإضافية في AutoJs6, وتأكد من التعرف على `AI Agent`, ثم فعله. تجتاز حزم الإصدار الرسمية التحقق من التوقيع تلقائيا.
3. افتح AI Agent من المشغل أو خيار الإدارة في القائمة الجانبية لـ AutoJs6. تعرض هذه المعاينة حالة المضيف فقط; تصل مساحة المهام وواجهة `ai.agent` في مراحل لاحقة.

راجع [README المشروع](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent) و [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) للاطلاع على دليل الاتصال والتقدم الحالي.
