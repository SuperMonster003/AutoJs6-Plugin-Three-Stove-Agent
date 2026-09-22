// Does the OpenAI-compatible proxy target enforce response_format json_schema? One call: ask for prose under the decision schema.
var BASE = '/sdcard/ai-agent-spike/';
var target = { id: 'profile:5d99a40b-0ea5-4479-85d4-d34e3c38c1b7', label: 'sony-remote-poloapi-claude-opus-4-8' };
var out = [];
function flush() { files.write(BASE + 'proxy-probe.txt', out.join('\n') + '\n'); }
var schema = { type: 'object', additionalProperties: false, required: ['kind'], properties: { kind: { type: 'string', enum: ['tool', 'ask', 'done'] }, reasoning: { type: 'string', maxLength: 600 }, tool: { type: 'string' }, arguments: { type: 'object' } } };
var keep = setInterval(function () {}, 1000);
var started = Date.now();
ai.chat({ messages: [{ role: 'user', content: 'Write a two-line poem about rain. Reply in plain prose only, do not use JSON or code blocks.' }] },
    { target: target.id, timeout: 120000, structuredJson: true, responseSchema: schema, maxTokens: 200, temperature: 0.2 })
    .then(function (r) { out.push('prose-under-schema -> OK wall=' + (Date.now() - started) + 'ms finish=' + r.finishReason + ' usage=' + JSON.stringify(r.usage) + '\ntext=' + r.text); flush(); clearInterval(keep); })
    .catch(function (e) { out.push('prose-under-schema -> REJECTED ' + e + ' code=' + (e && e.code)); flush(); clearInterval(keep); });
