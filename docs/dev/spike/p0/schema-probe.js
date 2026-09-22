// Probes which JSON Schema features the target accepts (structuredJson + responseSchema) using ai.chat.
var BASE = '/sdcard/ai-agent-spike/';
var config = JSON.parse(files.read(BASE + 'config.json'));
var target = config.targets[config.targets.length - 1];
var out = [];
function flush() { files.write(BASE + 'schema-probe.txt', out.join('\n') + '\n'); }
function errorText(e) {
    var parts = [];
    try { parts.push('String=' + String(e)); } catch (x) {}
    try { if (e && e.message) parts.push('message=' + e.message); } catch (x) {}
    try { if (e && e.code) parts.push('code=' + e.code); } catch (x) {}
    try { parts.push('json=' + JSON.stringify(e)); } catch (x) {}
    try { if (e && e.javaException) parts.push('java=' + e.javaException); } catch (x) {}
    try { if (e && e.cause) parts.push('cause=' + e.cause); } catch (x) {}
    return parts.join(' | ');
}
var full = {
    type: 'object', additionalProperties: false, required: ['kind'],
    properties: {
        kind: { type: 'string', enum: ['tool', 'ask', 'done'] },
        reasoning: { type: 'string', maxLength: 600 },
        tool: { type: 'string' },
        arguments: { type: 'object' },
        ask: { type: 'object', additionalProperties: false, required: ['question'], properties: {
            question: { type: 'string', maxLength: 500 }, kind: { type: 'string', enum: ['text', 'choice', 'confirm'] },
            choices: { type: 'array', items: { type: 'string' }, maxItems: 8 }, memoryKey: { type: 'string', maxLength: 64 } } },
        done: { type: 'object', additionalProperties: false, required: ['status', 'summary'], properties: {
            status: { type: 'string', enum: ['completed', 'partial', 'failed', 'blocked'] }, summary: { type: 'string', maxLength: 1000 },
            evidence: { type: 'array', items: { type: 'string', maxLength: 200 }, maxItems: 8 },
            unfinished: { type: 'array', items: { type: 'string', maxLength: 200 }, maxItems: 8 },
            orderStatus: { type: 'string', enum: ['none', 'cart', 'pending_payment', 'submitted', 'paid'] } } }
    }
};
function clone(v) { return JSON.parse(JSON.stringify(v)); }
function stripLimits(s) {
    var c = clone(s);
    (function walk(o) {
        if (!o || typeof o !== 'object') return;
        delete o.maxLength; delete o.maxItems;
        Object.keys(o).forEach(function (k) { walk(o[k]); });
    })(c);
    return c;
}
var variants = [];
variants.push({ name: 'V1 full appendix D (arguments object)', schema: full });
var v2 = stripLimits(full); variants.push({ name: 'V2 no maxLength/maxItems', schema: v2 });
var v3 = clone(v2); v3.properties.arguments = { type: 'object', additionalProperties: true }; variants.push({ name: 'V3 arguments additionalProperties true', schema: v3 });
var v4 = clone(v2); v4.properties.arguments = { type: 'object', properties: { nodeRef: { type: 'string' }, text: { type: 'string' }, app: { type: 'string' }, scriptId: { type: 'string' } } }; variants.push({ name: 'V4 arguments with typed optional properties', schema: v4 });
var v5 = clone(v2); v5.properties.arguments = { type: 'string' }; variants.push({ name: 'V5 arguments as string (B)', schema: v5 });
var v6 = clone(v5); delete v6.properties.ask; delete v6.properties.done; variants.push({ name: 'V6 B without nested ask/done', schema: v6 });
var v7 = clone(v5); delete v7.additionalProperties; delete v7.properties.ask.additionalProperties; delete v7.properties.done.additionalProperties; variants.push({ name: 'V7 B without additionalProperties:false', schema: v7 });
var v8 = clone(full); delete v8.properties.arguments; variants.push({ name: 'V8 full minus arguments', schema: v8 });
var keep = setInterval(function () {}, 1000);
var i = 0;
function next() {
    if (i >= variants.length) { out.push('done'); flush(); clearInterval(keep); return; }
    var v = variants[i++];
    var started = Date.now();
    var p;
    try {
        p = ai.chat({ messages: [{ role: 'system', content: 'You answer with exactly one JSON object describing a decision.' }, { role: 'user', content: 'The screen shows a Settings list with a row "WLAN" as node n30. Goal: open WLAN settings. Reply with a tool decision: kind "tool", tool "ui_click", arguments with nodeRef "n30".' }] },
            { target: target.id, timeout: 180000, structuredJson: true, responseSchema: v.schema, maxTokens: 300, temperature: 0.2 });
    } catch (e) { out.push(v.name + ' -> THREW ' + errorText(e)); flush(); return next(); }
    p.then(function (r) {
        var keys = '';
        try { keys = Object.keys(r).join(','); } catch (x) {}
        var text = null;
        try { text = r.text; } catch (x) {}
        var usage = null;
        try { usage = JSON.stringify(r.usage); } catch (x) {}
        out.push(v.name + ' -> OK ' + (Date.now() - started) + 'ms keys=[' + keys + '] usage=' + usage + ' finish=' + r.finishReason + ' text=' + String(text).slice(0, 300));
        flush(); next();
    }).catch(function (e) {
        out.push(v.name + ' -> REJECTED ' + (Date.now() - started) + 'ms ' + errorText(e));
        flush(); next();
    });
}
next();
