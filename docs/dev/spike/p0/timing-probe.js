// Measures local model latency on this device for three prompt sizes (10-minute timeout each).
var BASE = '/sdcard/ai-agent-spike/';
var config = JSON.parse(files.read(BASE + 'config.json'));
var target = config.targets[config.targets.length - 1];
var out = [];
function flush() { files.write(BASE + 'timing-probe.txt', out.join('\n') + '\n'); }
function errorText(e) { var p = []; try { p.push(String(e)); } catch (x) {} try { if (e && e.code) p.push('code=' + e.code); } catch (x) {} return p.join(' | '); }
var schema = { type: 'object', additionalProperties: false, required: ['kind'], properties: { kind: { type: 'string', enum: ['tool', 'ask', 'done'] }, reasoning: { type: 'string' }, tool: { type: 'string' }, arguments: { type: 'object' } } };
var filler = '';
var line = 'The screen shows a list of settings rows with titles and summaries; each row is clickable and has a node reference. ';
while (filler.length < 2600) filler += line;
var prompts = [
    { name: 'tiny ~75 tokens', user: 'Node n30 is the WLAN row. Goal: open WLAN. Reply with kind tool, ui_click, nodeRef n30.' },
    { name: 'medium ~700 tokens', user: filler + '\nNode n30 is the WLAN row. Goal: open WLAN. Reply with kind tool, ui_click, nodeRef n30.' },
    { name: 'settings-home local fixture', user: 'Goal: turn off Wi-Fi.\nCurrent snapshot:\n' + files.read(BASE + 'fixtures/settings-home.local.txt') + '\nReply with one decision JSON object (kind tool, ui_click, nodeRef of the Network & internet row).' }
];
var keep = setInterval(function () {}, 1000);
var i = 0;
out.push('target ' + target.label + ' ' + target.id); flush();
function next() {
    if (i >= prompts.length) { out.push('done'); flush(); clearInterval(keep); return; }
    var p = prompts[i++];
    var started = Date.now();
    ai.chat({ messages: [{ role: 'system', content: 'You are an automation agent. Reply with exactly one JSON object.' }, { role: 'user', content: p.user }] },
        { target: target.id, timeout: 600000, structuredJson: true, responseSchema: schema, maxTokens: 120, temperature: 0.2 })
        .then(function (r) {
            out.push(p.name + ' -> OK wall=' + (Date.now() - started) + 'ms usage=' + JSON.stringify(r.usage) + ' text=' + String(r.text).replace(/\s+/g, ' ').slice(0, 200));
            flush(); next();
        }).catch(function (e) {
            out.push(p.name + ' -> REJECTED wall=' + (Date.now() - started) + 'ms ' + errorText(e));
            flush(); next();
        });
}
next();
