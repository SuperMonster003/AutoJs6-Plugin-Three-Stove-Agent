var out = '/sdcard/ai-agent-catalog.txt';
var done = false;
function finish(text) { if (done) return; done = true; files.write(out, text); }
try {
    ai.catalog().then(function (c) {
        try { finish(JSON.stringify(c, null, 2)); } catch (e1) { finish('stringify failed: ' + e1 + '\n' + String(c)); }
    }).catch(function (e) { finish('catalog rejected: ' + e); });
} catch (e) {
    finish('catalog threw: ' + e + ' line ' + e.lineNumber);
}
setTimeout(function () { finish('timeout after 40 s'); }, 40000);
