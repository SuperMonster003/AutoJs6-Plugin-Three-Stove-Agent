// Follow-up: (1) baseline prose without structured mode, (2) prose request under structured mode + schema again, (3) JSON-shaped request under schema.
var BASE = '/sdcard/ai-agent-spike/';
var id = 'profile:5d99a40b-0ea5-4479-85d4-d34e3c38c1b7';
var out = [];
function flush() { files.write(BASE + 'proxy-probe2.txt', out.join('\n') + '\n'); }
var schema = { type: 'object', additionalProperties: false, required: ['kind'], properties: { kind: { type: 'string', enum: ['tool', 'ask', 'done'] }, reasoning: { type: 'string', maxLength: 600 }, tool: { type: 'string' }, arguments: { type: 'object' } } };
var prose = 'Write a two-line poem about rain. Reply in plain prose only, do not use JSON or code blocks.';
var calls = [
    { name: '1 baseline prose, structuredJson=false, no schema', req: { messages: [{ role: 'user', content: prose }] }, opt: { target: id, timeout: 120000, maxTokens: 200, temperature: 0.2 } },
    { name: '2 prose under structuredJson=true + schema (repeat)', req: { messages: [{ role: 'user', content: prose }] }, opt: { target: id, timeout: 120000, structuredJson: true, responseSchema: schema, maxTokens: 200, temperature: 0.2 } },
    { name: '3 prose under structuredJson=true, no schema', req: { messages: [{ role: 'user', content: prose }] }, opt: { target: id, timeout: 120000, structuredJson: true, maxTokens: 200, temperature: 0.2 } },
    { name: '4 schema-violating request: ask for kind=finished (not in enum) under schema', req: { messages: [{ role: 'user', content: 'Reply with exactly this JSON object and nothing else: {"kind":"finished","reasoning":"test"}' }] }, opt: { target: id, timeout: 120000, structuredJson: true, responseSchema: schema, maxTokens: 200, temperature: 0.2 } }
];
var keep = setInterval(function () {}, 1000);
var i = 0;
function next() {
    if (i >= calls.length) { out.push('done'); flush(); clearInterval(keep); return; }
    var c = calls[i++];
    var started = Date.now();
    ai.chat(c.req, c.opt).then(function (r) {
        out.push(c.name + ' -> OK wall=' + (Date.now() - started) + 'ms finish=' + r.finishReason + ' usage=' + JSON.stringify(r.usage) + ' text=' + String(r.text).replace(/\s+/g, ' ').slice(0, 300));
        flush(); next();
    }).catch(function (e) {
        out.push(c.name + ' -> REJECTED wall=' + (Date.now() - started) + 'ms ' + e + ' code=' + (e && e.code) + ' message=' + (e && e.message));
        flush(); next();
    });
}
next();
