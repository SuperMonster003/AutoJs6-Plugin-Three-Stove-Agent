# Android 13 launch crash, 2026-09-24

用户报告 Sony XQ-DQ72 (QV770340J7) 无法打开 AI Agent. 设备安装的是 1.0.0 / build 56, API 33. crash buffer 保留了多次相同的主进程启动异常, 最近记录为当日 21:52:41:

```text
Unable to start activity ...ui.LauncherActivity
NullPointerException: DecorView.getWindowInsetsController() on a null object reference
PhoneWindow.getInsetsController(PhoneWindow.java:3939)
HostAppearanceActivity.onCreate(HostAppearanceActivity.kt:62)
```

`HostAppearanceActivity` 在 `setContentView` 之前读取 `window.insetsController`. 该设备的 `PhoneWindow` 实现直接访问尚未创建的 decor. Kotlin 的 `?.` 只处理 getter 返回 null, 不能防止 getter 内部抛出异常.

修正: 应用主题并调用 `super.onCreate` 后, 先获取 `window.decorView` 使其完成初始化, 再配置系统栏颜色和 controller. 不吞掉异常, 不更改主题或设备设置作为兼容手段. 所有继承该基类的页面受益.

回归: `HostAppearanceActivityTest.launcherInitializesDecorBeforeApplyingSystemBars` 在 Sony XQ-DQ72 API 33, Sony G8441 API 28 与 AVD API 37 验证实际 Launcher 启动及重建, decor 已附着, API 30+ 的 controller 可读. XQ-DQ72 另以 `am force-stop` 后 `am start -W` 验证桌面 Activity 冷启动成功. 保留应用数据.

后续设备列表加入 Sony XQ-DQ72 (QV770340J7). 本项只证明界面启动兼容性, 不替代真实模型或任务验收. 原始日志存于忽略的 `build/p66-*` 文件, 不提交设备 crash buffer 中无关应用的信息.
