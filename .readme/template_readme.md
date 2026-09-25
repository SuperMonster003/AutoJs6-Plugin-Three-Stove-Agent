<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="{{ repo_url }}/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="{{ repo_url }}/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="{{ icon_alt }}" border="0" width="128" />
    </picture>
  </p>

  <p>{{ text_plugin_synopsis }}</p>

  <p>
    <a href="{{ repo_url }}/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/{{ repo_slug }}?label=Release"/></a>
    <a href="{{ repo_url }}/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/{{ repo_slug }}?color=A24232&label=Issues"/></a>
    <a href="{{ license_url }}"><img alt="GitHub License" src="https://img.shields.io/github/license/{{ repo_slug }}?color=534BAE&label=License"/></a>
  </p>
</div>

******

### {{ h3_languages_with_ascii }}

******

{{ p_languages_all_supported_for_readme }}:

{{ placeholder_ul_languages_all_supported }}

******

### {{ h3_introduction }}

******

{{ p_introduction_what }}

{{ p_introduction_how }}

******

### {{ h3_status }}

******

{{ p_status }}

******

### {{ h3_features }}

******

{{ p_features_intro }}:

{{ placeholder_features }}

### {{ h3_screenshots }}

{{ p_screenshots }}

| {{ screenshot_workbench }} | {{ screenshot_detail }} |
| --- | --- |
| <img src="{{ repo_url }}/blob/master/docs/images/workbench.png?raw=true" alt="{{ screenshot_workbench }}" width="288" /> | <img src="{{ repo_url }}/blob/master/docs/images/detail.png?raw=true" alt="{{ screenshot_detail }}" width="288" /> |
| {{ screenshot_confirmation }} | {{ screenshot_floating }} |
| <img src="{{ repo_url }}/blob/master/docs/images/confirmation.png?raw=true" alt="{{ screenshot_confirmation }}" width="288" /> | <img src="{{ repo_url }}/blob/master/docs/images/floating.png?raw=true" alt="{{ screenshot_floating }}" width="288" /> |

******

### {{ h3_installation }}

******

{{ placeholder_installation_steps }}

{{ p_provider_setup }}

### {{ h3_compatibility }}

{{ p_compatibility }}

### {{ h3_quickstart_ui }}

{{ p_quickstart_ui }}

### {{ h3_quickstart_script }}

{{ p_quickstart_script }}

```javascript
let run = ai.agent.run('{{ example_goal }}', {
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

{{ p_script_result }}

### {{ h3_registration }}

{{ p_registration }}

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

{{ p_registration_project }}

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

{{ p_registration_limits }}

### {{ h3_tools }}

{{ p_tools_status }}

{{ placeholder_tool_table }}

### {{ h3_presets_memory }}

{{ p_presets_usage }}

{{ p_memory_usage }}

### {{ h3_usage }}

{{ placeholder_usage_details }}

### {{ h3_faq }}

**{{ faq_host_question }}**

{{ faq_host_answer }}

**{{ faq_payment_question }}**

{{ faq_payment_answer }}

**{{ faq_local_question }}**

{{ faq_local_answer }}

******

### {{ h3_security }}

******

{{ p_security_intro }}

{{ placeholder_security_points }}

{{ p_security_permission }}

******

### {{ h3_plugin_interface }}

******

{{ p_plugin_interface }}:

```text
application id: {{ plugin_application_id }}
plugin id: {{ plugin_id }}
engine: {{ plugin_engine }}
variant: {{ plugin_variant }}
service action: {{ plugin_service_action }}
service category: {{ plugin_service_category }}
service process: {{ plugin_service_process }}
info action: {{ plugin_info_action }}
aidl interface: {{ plugin_aidl_interface }}
minimum host build: {{ required_host_version_code }} ({{ required_host_version_name }})
```

{{ p_contract_service }}

******

### {{ h3_roadmap }}

******

{{ p_roadmap }}

- [{{ text_link_roadmap }}]({{ roadmap_url }})

******

### {{ h3_release_history }}

******

{{ placeholder_latest_release_history }}

##### {{ h5_for_more_release_history }}

* {{ placeholder_read_more_in_changelog_md }}

******

### {{ h3_build }}

******

{{ p_build_intro }}

{{ p_build_debug }}:

```powershell
.\gradlew.bat :app:assembleDebug
```

{{ p_build_test }}:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

{{ p_build_release }}:

```powershell
.\gradlew.bat :app:assembleRelease
```

{{ p_build_digest }}:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

{{ p_build_docs_check }}:

```powershell
py .python\generate_markdown.py --check
```

{{ p_build_requirements }}

******

### {{ h3_resource_layout }}

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

{{ p_resource_layout }}

******

### {{ h3_license }}

******

{{ p_license }}

******

### {{ h3_links }}

******

- {{ text_link_autojs6 }}: {{ autojs6_url }}
- {{ text_link_autojs6_docs }}: {{ docs_autojs6_url }}
- {{ text_link_discussion }}: {{ discussion_url }}
- {{ text_link_third_party_notices }}: {{ third_party_notices_url }}
