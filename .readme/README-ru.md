<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-ai-agent-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Выполняет задачи на естественном языке в AutoJs6, выбирая зарегистрированные скрипты и пошагово управляя экраном</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-AI-Agent?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-AI-Agent?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-AI-Agent?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Языки

******

Текущий README.md поддерживает следующие языки:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ko.md)
- Русский [ru] # текущий
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ar.md)

******

### Введение

******

AI Agent превращает цель на естественном языке в действия на Android-устройстве с AutoJs6. Он либо выбирает скрипт, который пользователь зарегистрировал для агента, заполняет его параметры и запускает; либо наблюдает экран через дерево узлов специальных возможностей и действует шаг за шагом (наблюдать, решать, действовать, проверять), пока цель не достигнута, не потребуется подтверждение или не исчерпан бюджет. Это ответ на [обсуждение AutoJs6 #577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

Плагин одновременно является плагином AutoJs6 и самостоятельным приложением. Скрипты обращаются к нему через API `ai.agent` в AutoJs6; пользователи работают с ним через собственное рабочее пространство задач, панель AutoJs6, плавающую кнопку, системное меню отправки, ярлыки приложения и голосовой ввод. Вызовы модели и действия на устройстве всегда проходят через AutoJs6 по Binder: хост предоставляет плагину брокер модели (плагины AI Provider, уже известные хосту, например 3-Stone AI) и брокер возможностей с ограниченным разрешением. Плагин никогда не хранит учетные данные, не привязывается к поставщику модели самостоятельно и не запрашивает разрешение специальных возможностей.

******

### Состояние

******

Версия 1.0.0 является предварительной версией этапа P0 дорожной карты: идентичность плагина, контракт обнаружения AutoJs6 (служба INFO, Wake Activity и заглушка службы `org.autojs.plugin.AI_AGENT`) и экран запуска, который показывает состояние хоста. Цикл агента, каталог скриптов, API `ai.agent` и рабочее пространство задач еще не реализованы; прогресс и доказательства фиксируются в [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md). Плагину потребуется AutoJs6 сборки 5283 или новее.

******

### Возможности

******

В версии 1.0.0 планируются следующие возможности:

- Выбор скриптов: скрипты, зарегистрированные через `project.json` или заголовочный комментарий `@agent`, представляются модели с описаниями и схемами параметров; агент выбирает один из них, заполняет параметры, при необходимости запрашивает подтверждение, запускает его в AutoJs6 и читает структурированный результат.
- Пошаговое управление экраном: агент наблюдает дерево узлов специальных возможностей в компактном текстовом виде (и текст экрана через плагин OCR, если он установлен), затем нажимает, вводит, прокручивает и использует клавиши через брокер возможностей AutoJs6, пока не сможет проверить достижение цели.
- Безопасность по замыслу: инструменты только для чтения выполняются автоматически, чувствительные действия (оплата, отправка, удаление, запись файлов, shell, жесты по координатам, скрипты, зарегистрированные как чувствительные) требуют подтверждения, а у каждого запуска есть бюджеты по шагам, вызовам модели, длительности и токенам.
- API скриптов и пользовательский интерфейс: `ai.agent.run(goal, options)` возвращает дескриптор `AgentRun` с событиями, ответами и отменой; самостоятельное приложение предлагает рабочее пространство задач с историей, пресетами, памятью предпочтений, настройками и историей выпусков.

******

### Использование

******

1. Установите APK плагина со страницы [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) на устройство с AutoJs6 сборки 5283 или новее.
2. Откройте центр плагинов AutoJs6, убедитесь, что `AI Agent` распознан, и включите его. Официальные пакеты автоматически проходят проверку подписи.
3. Откройте AI Agent из лаунчера: в этой предварительной версии экран лишь сообщает, установлен ли совместимый хост AutoJs6. Рабочее пространство задач, пункт панели и API `ai.agent` появятся на следующих этапах дорожной карты.

> В этой предварительной версии экран запуска показывает только состояние хоста; пункт панели AutoJs6, API `ai.agent` и рабочее пространство задач появятся на этапах P1, P5 и P6 дорожной карты.

******

### Разрешения и безопасность

******

Плагин соблюдает явные границы:

- Точки входа Binder защищены разрешением подписи `org.autojs.permission.PLUGIN`, поэтому до них может добраться только AutoJs6; экран запуска является единственным другим экспортируемым компонентом.
- Плагин не хранит ключи API, никогда не привязывается к поставщику модели и не запрашивает разрешение специальных возможностей: вызовы модели и действия на устройстве проходят через брокеры, которые AutoJs6 предоставляет для одной подключенной связи и отзывает при отключении, каждый из которых ограничен разрешением (допустимые методы, частота, размеры, квота модели).
- Плагин не использует сеть. Эта предварительная версия не объявляет никаких разрешений, кроме разрешения плагина; разрешения на службу переднего плана, уведомления и наложение будут добавлены вместе с функциями, которым они нужны, и описаны здесь.
- История задач, пресеты и память предпочтений остаются в закрытом хранилище плагина; резервное копирование и перенос между устройствами отключены.

Получайте плагин только со страницы официальных [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) или из центра плагинов AutoJs6. Пакеты из неизвестных источников могут не пройти проверку хоста или нести риски, даже если номер версии выглядит одинаково.

******

### Интерфейс плагина

******

Следующая информация предназначена разработчикам хоста AutoJs6 и плагинов; хост использует эти идентификаторы для обнаружения плагина и согласования совместимости:

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
minimum host build: 5283 (6.8.0)
```

`AiAgentPluginService` отвечает на `org.autojs.plugin.AI_AGENT` (категория `ai-agent`) в процессе `:agent`; в этой предварительной версии он предоставляет заглушку Binder с дескриптором `org.autojs.plugin.ai.agent.api.IAiAgentPlugin`, пока не подготовлен модуль контракта хоста. `AiAgentPluginInfoService` отвечает на `org.autojs.plugin.INFO` объектом PluginInfo. `WakeActivity` позволяет хосту активировать плагин.

******

### Дорожная карта

******

Планы и прогресс плагина ведутся в виде списка с отметками в ROADMAP.md, организованного по этапам с критериями приемки и уровнями доказательств. Неотмеченные пункты выражают намерение, а не текущие возможности; обсуждение через Issues приветствуется.

- [Открыть ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md)

******

### История выпусков

******

#### v1.0.0

_2026/09/23_

- `Подсказка` Предварительная версия этапа P0: идентичность плагина, контракт обнаружения AutoJs6 и экран запуска, показывающий состояние хоста. Цикл агента, каталог скриптов, API ai.agent и рабочее пространство задач еще не реализованы. См. ROADMAP.md.
- `Подсказка` Контракт AI Agent, посредники доступа к функциям и моделям, наблюдение за экраном и выполнение зарегистрированных скриптов реализованы в хосте. Выполнение задач плагином остается в разработке
- `Функция` Идентичность плагина `ai-agent` со службой INFO, Wake Activity, заглушкой службы `org.autojs.plugin.AI_AGENT` в процессе `:agent` и экраном запуска, который сообщает, установлен ли совместимый хост AutoJs6
- `Функция` README, инструкции центра плагинов и журнал изменений на 10 языках
- `Зависимость` Добавлен `common-plugin-api.aar` (модуль AutoJs6 `plugin-api/common-plugin-api`, сборка хоста 6.8.0 / 5282, MPL 2.0) как общий контракт плагинов, зафиксированный хешем в `locks/host-api-aars.lock`

##### Полная история выпусков

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/assets/doc/CHANGELOG-ru.md)

******

### Сборка и проверка

******

Этот раздел предназначен разработчикам, желающим собрать плагин из исходного кода; обычные пользователи могут просто установить готовый APK со страницы Releases.

Собрать отладочный APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

Запустить модульные тесты JVM и собрать APK инструментальных тестов:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

Собрать выпускной APK:

```powershell
.\gradlew.bat :app:assembleRelease
```

Собрать выпускной артефакт и добавить версию и контрольную сумму CRC32 к имени файла:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

Проверить, что источники многоязычной документации и сгенерированные артефакты синхронизированы (также проверяется в CI):

```powershell
py .python\generate_markdown.py --check
```

Для сборки требуются JDK 21 или новее и Android SDK 37; версии Gradle и плагинов централизованно управляются через `version.properties` и `io.github.supermonster003.autojs6-platform-versions`.

******

### Локализация и генерация документации

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

Языковые JSON-файлы в `.readme/` и `.changelog/` являются единственным источником README, инструкций центра плагинов и журнала изменений. Всегда редактируйте эти JSON-источники и перезапускайте `py .python/generate_markdown.py`; сгенерированные README, `plugin_instruction.md` и журнал изменений никогда не правятся вручную. Запустите `py .python/generate_markdown.py --check`, чтобы проверить все сгенерированные артефакты.

******

### Лицензия

******

Код проекта распространяется по лицензии [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/LICENSE). Сторонние компоненты и их лицензии перечислены в [уведомлениях о сторонних компонентах](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/THIRD_PARTY_NOTICES.md).

******

### Ссылки

******

- Проект AutoJs6: https://github.com/SuperMonster003/AutoJs6
- Документация AutoJs6: https://docs.autojs6.com
- Обсуждение AutoJs6 #577: https://github.com/SuperMonster003/AutoJs6/discussions/577
- Уведомления о сторонних компонентах: https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/THIRD_PARTY_NOTICES.md
