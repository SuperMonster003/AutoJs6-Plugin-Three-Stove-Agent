// Captures compact node trees of real screens for the P0.2 spike fixtures.
// Output: /sdcard/ai-agent-fixture-<name>.txt (one per screen) + /sdcard/ai-agent-fixture-log.txt
var log = [];
function note(s) { log.push(s); files.write('/sdcard/ai-agent-fixture-log.txt', log.join('\n')); }

function compact(root, maxNodes) {
    var lines = [];
    var n = 0;
    var total = 0;
    var truncated = false;
    function walk(node, depth) {
        if (!node) return;
        if (n >= maxNodes) { truncated = true; return; }
        total++;
        var printed = false;
        try {
            if (node.visibleToUser()) {
                var t = node.text() || '';
                var d = node.desc() || '';
                var id = node.id() || '';
                var flags = [];
                if (node.clickable()) flags.push('clickable');
                if (node.longClickable()) flags.push('long-clickable');
                if (node.scrollable()) flags.push('scrollable');
                if (node.checkable()) flags.push(node.checked() ? 'checked' : 'unchecked');
                if (node.editable()) flags.push('editable');
                if (!node.enabled()) flags.push('disabled');
                if (node.selected()) flags.push('selected');
                if (node.focused()) flags.push('focused');
                if (t || d || id || flags.length) {
                    var b = node.bounds();
                    var cls = String(node.className() || '').split('.').pop();
                    var parts = ['#n' + n, cls];
                    if (t) parts.push('text="' + String(t).replace(/\s+/g, ' ').replace(/"/g, "'").slice(0, 60) + '"');
                    if (d) parts.push('desc="' + String(d).replace(/\s+/g, ' ').replace(/"/g, "'").slice(0, 60) + '"');
                    if (id) parts.push('id=' + String(id).split('/').pop());
                    parts.push('(' + b.centerX() + ',' + b.centerY() + ')');
                    parts.push('[' + b.left + ',' + b.top + ',' + b.right + ',' + b.bottom + ']');
                    if (flags.length) parts.push(flags.join(','));
                    lines.push(new Array(depth + 1).join('  ') + parts.join(' '));
                    n++;
                    printed = true;
                }
            }
        } catch (e) {
            lines.push('!! ' + e);
        }
        var children = node.children();
        var count = children.size();
        for (var i = 0; i < count; i++) {
            walk(children.get(i), printed ? depth + 1 : depth);
        }
    }
    walk(root, 0);
    return { text: lines.join('\n'), nodes: n, total: total, truncated: truncated };
}

function capture(name, wait) {
    sleep(wait || 2500);
    var root = auto.root;
    var pkg = currentPackage();
    var act = currentActivity();
    var r = compact(root, 160);
    var header = 'screen: ' + name + '\npackage: ' + pkg + '\nactivity: ' + act + '\nnodes: ' + r.nodes + ' (walked ' + r.total + (r.truncated ? ', truncated' : '') + ')\n';
    files.write('/sdcard/ai-agent-fixture-' + name + '.txt', header + r.text);
    note(name + ': ' + pkg + ' / ' + act + ' nodes=' + r.nodes + ' walked=' + r.total);
}

auto.waitFor();
note('a11y ready, sdk ' + device.sdkInt);
home();
capture('launcher', 2500);
app.startActivity({ action: 'android.settings.SETTINGS', flags: ['activity_new_task', 'activity_clear_task'] });
capture('settings-home', 3500);
app.startActivity({ action: 'android.settings.WIFI_SETTINGS', flags: ['activity_new_task', 'activity_clear_task'] });
capture('wifi', 3500);
home();
note('done');
