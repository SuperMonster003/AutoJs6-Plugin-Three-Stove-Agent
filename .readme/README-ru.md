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

Предварительная версия: зарегистрированные скрипты поддерживают вопросы о параметрах, подтверждение, результаты и отмену. Работа с экраном продолжается в P4, API задач и рабочая панель появятся в P5/P6. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

******

### Возможности

******

В версии 1.0.0 планируются следующие возможности:

- Выбор скриптов: скрипты, зарегистрированные через `project.json` или заголовочный комментарий `@agent`, представляются модели с описаниями и схемами параметров; агент выбирает один из них, заполняет параметры, при необходимости запрашивает подтверждение, запускает его в AutoJs6 и читает структурированный результат.
- Пошаговое управление экраном: агент наблюдает дерево узлов специальных возможностей в компактном текстовом виде (и текст экрана через плагин OCR, если он установлен), затем нажимает, вводит, прокручивает и использует клавиши через брокер возможностей AutoJs6, пока не сможет проверить достижение цели.
- Безопасность по замыслу: инструменты только для чтения выполняются автоматически, чувствительные действия (оплата, отправка, удаление, запись файлов, shell, жесты по координатам, скрипты, зарегистрированные как чувствительные) требуют подтверждения, а у каждого запуска есть бюджеты по шагам, вызовам модели, длительности и токенам.
- API скриптов и пользовательский интерфейс: `ai.agent.run(goal, options)` возвращает дескриптор `AgentRun` с событиями, ответами и отменой; самостоятельное приложение предлагает рабочее пространство задач с историей, пресетами, памятью предпочтений, настройками и историей выпусков.

### Каталог инструментов

Предварительная версия: зарегистрированные скрипты поддерживают вопросы о параметрах, подтверждение, результаты и отмену. Работа с экраном продолжается в P4, API задач и рабочая панель появятся в P5/P6.

| Инструмент | Группа | Риск | По умолчанию | Описание |
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

### Использование

******

1. Установите APK плагина со страницы [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) на устройство с AutoJs6 сборки 5289 или новее.
2. Откройте центр плагинов AutoJs6, убедитесь, что `AI Agent` распознан, и включите его. Официальные пакеты автоматически проходят проверку подписи.
3. Запрос подключения из стартового экрана, ожидание до 15 секунд и подсказка по включению и авторизации AI Agent в AutoJs6.
4. Укажите дополнительные папки в разделе "Каталоги скриптов" стартового экрана, по одному абсолютному пути в строке. Хост проверяет и применяет сохраненные пути; задачи могут только сузить разрешенную область.

> Предварительная версия: зарегистрированные скрипты поддерживают вопросы о параметрах, подтверждение, результаты и отмену. Работа с экраном продолжается в P4, API задач и рабочая панель появятся в P5/P6.

******

### Разрешения и безопасность

******

Плагин соблюдает явные границы:

- Точки входа Binder защищены разрешением подписи `org.autojs.permission.PLUGIN`, поэтому до них может добраться только AutoJs6; экран запуска является единственным другим экспортируемым компонентом.
- Плагин не хранит ключи API, никогда не привязывается к поставщику модели и не запрашивает разрешение специальных возможностей: вызовы модели и действия на устройстве проходят через брокеры, которые AutoJs6 предоставляет для одной подключенной связи и отзывает при отключении, каждый из которых ограничен разрешением (допустимые методы, частота, размеры, квота модели).
- Без разрешения на сеть. FOREGROUND_SERVICE и FOREGROUND_SERVICE_SPECIAL_USE нужны активным задачам; POST_NOTIFICATIONS показывает прогресс и остановку. Разрешения специальных возможностей и наложения не запрашиваются.
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
minimum host build: 5289 (6.8.0)
```

`AiAgentPluginService` / `IAiAgentPlugin` / `IAiAgentLink`: Проверка хоста при подключении, очередь задач, ответы, отмена, запросы и закрытая история шагов; задачи блокируются при потере хоста и не возобновляются после перезапуска процесса.

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

- `Подсказка` Предварительная версия: зарегистрированные скрипты поддерживают вопросы о параметрах, подтверждение, результаты и отмену. Работа с экраном продолжается в P4, API задач и рабочая панель появятся в P5/P6.
- `Функция` Ограниченное ожидание стабильного состояния экрана после действия и сводка изменений с последнего действия в следующих наблюдениях
- `Функция` Подтверждение действий привязано к проверенным хостом узлам, с добавлением текста, ограниченной прокруткой и отчетом о результате и смене окна
- `Функция` OCR экрана доступен только при наличии разрешенного OCR-плагина по данным хоста, с объединением текста в ограниченные строки с координатами
- `Функция` Наблюдения сохраняют ссылки на снимки узлов хоста, ограничивают вывод узлов и консоли и описывают изменения текста и состояния
- `Функция` Задачи с одним скриптом сохраняют ID, путь, ID выполнения и результат после завершения моделью, различая явный null и помечая усечение больших результатов
- `Функция` Выполнение зарегистрированных скриптов с проверкой подтвержденного манифеста, структурированными наблюдениями, скрытием секретов в конце консоли и остановкой своего скрипта при тайм-ауте или отмене задачи
- `Функция` Передача сохраненных предпочтений нужной области в параметры скриптов, лимит 4 KiB, отметка усечения и отключение для отдельной задачи
- `Функция` Проверка параметров зарегистрированных скриптов, заполнение значений по умолчанию, запрос недостающих данных, проверка текущего риска и полная таблица параметров для подтверждения
- `Функция` `ai-agent`: `AiAgentPluginInfoService`, `WakeActivity`, `AiAgentPluginService`, `ui.LauncherActivity`
- `Функция` README, инструкции центра плагинов и журнал изменений на 10 языках
- `Функция` Каталог ядра Agent из 30 инструментов, допуск групп, схемы параметров, подготовка вызовов bridge, ограниченные наблюдения и повышение чувствительных рисков
- `Функция` Ядро решений Agent со схемами по протоколам, строгим разбором или извлечением JSON, проверкой инструментов/ветвей, максимум двумя попытками исправления и шаблонами на английском/китайском
- `Функция` Бюджеты Agent для шагов, вызовов модели, времени и токенов, с ограничением времени инструментов/диалогов, оценкой использования и лимитом выходных токенов
- `Функция` Подтверждения Agent с обычной/осторожной политикой, разрешениями в рамках задачи для одного инструмента и уровня риска, подтверждением каждого платежа и ключевыми словами на 10 языках
- `Функция` Приватный журнал Agent ограничен 200 шагами и 1 MiB, пароли скрываются, сокращенные итоговые результаты сохраняют статус и счетчики
- `Функция` Проверка хоста при подключении, очередь задач, ответы, отмена, запросы и закрытая история шагов; задачи блокируются при потере хоста и не возобновляются после перезапуска процесса
- `Функция` Детерминированная сборка контекста Agent с лимитом байтов, полными недавними парами шагов, подсказками на английском/китайском и приоритетом узлов; локальный бюджет 3000 токенов и компактные описания инструментов
- `Функция` Клиент модели хоста с проверкой порядка событий, учетом usage, отменой, сроками и ограниченным переходом формата; каждый повтор учитывается как вызов и сохраняет лимит исправления решений
- `Функция` Запрос подключения из стартового экрана, ожидание до 15 секунд и подсказка по включению и авторизации AI Agent в AutoJs6
- `Функция` Уведомления активных задач с прогрессом, остановкой и просмотром; ввод и подтверждение каждого действия из стартового экрана
- `Функция` Каталог скриптов обновляется при запуске задачи: кеш соединения на 60 секунд, детерминированное ранжирование до 24 кандидатов, ограниченные сводки параметров и поиск script_catalog
- `Исправление` Скрытие многострочных параметров до разбиения консоли на строки, без пропуска секретов при совпадении параметра с именем поля учетных данных
- `Исправление` Завершающаяся фоновая служба с уведомлением больше не отклоняет запуск следующей задачи
- `Улучшение` Размер описаний подтверждения учитывает экранирование JSON, чтобы большие таблицы параметров не превышали лимит событий Binder
- `Улучшение` Минимальный хост AutoJs6 6.8.0 / сборка 5289 обеспечивает проверку узлов действий и привязку подтверждения к выполнению
- `Зависимость` Добавлены common-plugin-api, host-capability-api и ai-agent-api из одной release-сборки AutoJs6 6.8.0 / 5289 (MPL 2.0), закреплены SHA-256
- `Зависимость` Добавлен Gson 2.13.2 для строгого ограниченного разбора JSON и деревьев схем

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
