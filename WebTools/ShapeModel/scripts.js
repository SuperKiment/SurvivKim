// ================================================================
//  CONSTANTS
// ================================================================
var TYPES = ['RECT', 'CIRCLE', 'TRIANGLE', 'RECT_OUTLINE', 'CIRCLE_OUTLINE', 'TRIANGLE_OUTLINE'];
var ICONS = {RECT: '[_]', CIRCLE: '( )', TRIANGLE: '/\\/'};

// 3x3 anchor grid: [anchorId, label, fractional offsets relative to shape (fx, fy)]
// fx/fy are multipliers on half-width / half-height
var ANCHORS = [
    {id: 'TL', label: '↖', fx: -1, fy: -1},
    {id: 'TC', label: '↑', fx: 0, fy: -1},
    {id: 'TR', label: '↗', fx: 1, fy: -1},
    {id: 'ML', label: '←', fx: -1, fy: 0},
    {id: 'MC', label: '·', fx: 0, fy: 0},
    {id: 'MR', label: '→', fx: 1, fy: 0},
    {id: 'BL', label: '↙', fx: -1, fy: 1},
    {id: 'BC', label: '↓', fx: 0, fy: 1},
    {id: 'BR', label: '↘', fx: 1, fy: 1},
];
var ANCHOR_PADDING = 6; // pixels inset from edge

// ================================================================
//  STATE
// ================================================================
var shapes = [];
var selected = null;
var uid = 1;
var placing = false;

// Font registry: [{name: 'minecraft', family: 'minecraft', loaded: true}]
var fonts = [];

var pan = {x: 0, y: 0};
var panning = false;
var panRef = {px: 0, py: 0, mx: 0, my: 0};
var dragging = false;
var dragRef = {wx: 0, wy: 0, sx: 0, sy: 0};
var mouseWorld = {x: 0, y: 0};

function mkShape(x, y) {
    return {
        id: uid++, type: 'RECT',
        x: x || 0, y: y || 0, w: 80, h: 50,
        r: 1, g: 0, b: 0,
        text: '', fontSize: 32,
        textR: 0, textG: 0, textB: 0,
        fontName: 'minecraft',
        // text offset relative to shape center (world units)
        textOffsetX: 0,
        textOffsetY: 0
    };
}

function baseType(t) {
    return t.replace('_OUTLINE', '');
}

function isOutline(t) {
    return t.indexOf('_OUTLINE') !== -1;
}

// ================================================================
//  FONT LOADING
// ================================================================
function loadFontFiles(files) {
    for (var i = 0; i < files.length; i++) {
        (function (file) {
            // Derive a clean family name from filename (strip extension, spaces→-)
            var rawName = file.name.replace(/\.[^.]+$/, '');
            var family = rawName.replace(/\s+/g, '-');

            var reader = new FileReader();
            reader.onload = function (e) {
                var ff = new FontFace(family, e.target.result);
                ff.load().then(function (loaded) {
                    document.fonts.add(loaded);
                    // Avoid duplicates
                    for (var k = 0; k < fonts.length; k++) {
                        if (fonts[k].family === family) return;
                    }
                    fonts.push({name: rawName, family: family});
                    refreshFontList();
                    refreshProps(); // update the select if props open
                }).catch(function (err) {
                    alert('Failed to load font "' + rawName + '":\n' + err);
                });
            };
            reader.readAsArrayBuffer(file);
        })(files[i]);
    }
}

function refreshFontList() {
    document.getElementById('fontCount').textContent = fonts.length;
    var list = document.getElementById('fontList');
    list.innerHTML = '';
    for (var i = 0; i < fonts.length; i++) {
        (function (idx) {
            var f = fonts[idx];
            var item = document.createElement('div');
            item.className = 'font-item';

            var swatch = document.createElement('div');
            swatch.className = 'font-swatch';
            // Preview the font
            var preview = document.createElement('span');
            preview.style.fontFamily = "'" + f.family + "', monospace";
            preview.style.fontSize = '13px';
            preview.textContent = 'Aa';
            var nameLabel = document.createElement('div');
            nameLabel.className = 'font-name-label';
            nameLabel.textContent = f.name;
            swatch.appendChild(preview);
            swatch.appendChild(nameLabel);

            var del = document.createElement('button');
            del.className = 'font-del';
            del.textContent = 'x';
            del.title = 'Remove font';
            del.onclick = function () {
                fonts.splice(idx, 1);
                refreshFontList();
                refreshProps();
            };

            item.appendChild(swatch);
            item.appendChild(del);
            list.appendChild(item);
        })(i);
    }
}

// Drop zone wiring
var dropZone = document.getElementById('fontDropZone');
dropZone.addEventListener('dragover', function (e) {
    e.preventDefault();
    dropZone.classList.add('over');
});
dropZone.addEventListener('dragleave', function () {
    dropZone.classList.remove('over');
});
dropZone.addEventListener('drop', function (e) {
    e.preventDefault();
    dropZone.classList.remove('over');
    loadFontFiles(e.dataTransfer.files);
});
document.getElementById('fontFilePicker').addEventListener('change', function (e) {
    loadFontFiles(e.target.files);
    e.target.value = ''; // reset so same file can be re-added
});

// ================================================================
//  CANVAS
// ================================================================
var cv = document.getElementById('canvas');
var ctx = cv.getContext('2d');
var area = document.getElementById('canvasArea');

function resize() {
    cv.width = area.clientWidth;
    cv.height = area.clientHeight;
    draw();
}

window.addEventListener('resize', resize);

function w2s(wx, wy) {
    return {x: cv.width / 2 + pan.x + wx, y: cv.height / 2 + pan.y + wy};
}

function s2w(sx, sy) {
    return {x: sx - cv.width / 2 - pan.x, y: sy - cv.height / 2 - pan.y};
}

// ----------------------------------------------------------------
//  Draw loop
// ----------------------------------------------------------------
function draw() {
    var W = cv.width, H = cv.height;
    ctx.clearRect(0, 0, W, H);
    drawGrid(W, H);
    drawAxes(W, H);
    for (var i = 0; i < shapes.length; i++) drawShape(shapes[i], i === selected);
    if (placing) drawGhost();
}

function drawGrid(W, H) {
    var step = 40;
    ctx.strokeStyle = 'rgba(255,255,255,0.028)';
    ctx.lineWidth = 1;
    var ox = (pan.x + W / 2) % step;
    if (ox < 0) ox += step;
    var oy = (pan.y + H / 2) % step;
    if (oy < 0) oy += step;
    for (var x = ox; x < W; x += step) {
        ctx.beginPath();
        ctx.moveTo(x, 0);
        ctx.lineTo(x, H);
        ctx.stroke();
    }
    for (var y = oy; y < H; y += step) {
        ctx.beginPath();
        ctx.moveTo(0, y);
        ctx.lineTo(W, y);
        ctx.stroke();
    }
}

function drawAxes(W, H) {
    var o = w2s(0, 0);
    ctx.setLineDash([4, 5]);
    ctx.lineWidth = 1;
    ctx.strokeStyle = 'rgba(232,255,71,0.18)';
    ctx.beginPath();
    ctx.moveTo(o.x, 0);
    ctx.lineTo(o.x, H);
    ctx.stroke();
    ctx.beginPath();
    ctx.moveTo(0, o.y);
    ctx.lineTo(W, o.y);
    ctx.stroke();
    ctx.setLineDash([]);
}

function drawGhost() {
    var p = w2s(mouseWorld.x, mouseWorld.y);
    ctx.strokeStyle = 'rgba(232,255,71,0.4)';
    ctx.lineWidth = 1;
    ctx.setLineDash([3, 4]);
    ctx.strokeRect(p.x - 40, p.y - 25, 80, 50);
    ctx.setLineDash([]);
}

function rgba01(r, g, b, a) {
    a = (a === undefined) ? 1 : a;
    return 'rgba(' + Math.round(r * 255) + ',' + Math.round(g * 255) + ',' + Math.round(b * 255) + ',' + a + ')';
}

function shapePathFn(s) {
    var bt = baseType(s.type);
    if (bt === 'RECT') {
        ctx.rect(-s.w / 2, -s.h / 2, s.w, s.h);
    } else if (bt === 'CIRCLE') {
        ctx.arc(0, 0, s.w, 0, Math.PI * 2);
    } else if (bt === 'TRIANGLE') {
        ctx.moveTo(0, -s.h / 2);
        ctx.lineTo(s.w / 2, s.h / 2);
        ctx.lineTo(-s.w / 2, s.h / 2);
        ctx.closePath();
    }
}

function drawShape(s, sel) {
    var p = w2s(s.x, s.y);
    var out = isOutline(s.type);
    var fill = rgba01(s.r, s.g, s.b);

    ctx.save();
    ctx.translate(p.x, p.y);
    if (sel) {
        ctx.shadowColor = '#e8ff47';
        ctx.shadowBlur = 14;
    }

    // Shape body
    ctx.beginPath();
    shapePathFn(s);
    if (out) {
        ctx.strokeStyle = fill;
        ctx.lineWidth = 2.5;
        ctx.stroke();
    } else {
        ctx.fillStyle = fill;
        ctx.fill();
    }

    // Selection outline
    if (sel) {
        ctx.shadowBlur = 0;
        ctx.beginPath();
        shapePathFn(s);
        ctx.strokeStyle = '#e8ff47';
        ctx.lineWidth = 1.5;
        ctx.stroke();
    }

    // Text
    if (s.text) {
        ctx.shadowBlur = 0;
        var fontFamily = "'" + s.fontName + "', monospace";
        ctx.font = 'bold ' + Math.round(s.fontSize * 0.35) + 'px ' + fontFamily;
        ctx.fillStyle = rgba01(s.textR, s.textG, s.textB);
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        var label = s.text.length > 16 ? s.text.slice(0, 15) + '…' : s.text;
        ctx.fillText(label, s.textOffsetX, s.textOffsetY);

        // Draw a small crosshair at text anchor when selected
        if (sel && (s.textOffsetX !== 0 || s.textOffsetY !== 0)) {
            ctx.strokeStyle = 'rgba(255,158,100,0.7)';
            ctx.lineWidth = 1;
            ctx.setLineDash([2, 2]);
            var tx = s.textOffsetX, ty = s.textOffsetY;
            ctx.beginPath();
            ctx.moveTo(tx - 5, ty);
            ctx.lineTo(tx + 5, ty);
            ctx.stroke();
            ctx.beginPath();
            ctx.moveTo(tx, ty - 5);
            ctx.lineTo(tx, ty + 5);
            ctx.stroke();
            ctx.setLineDash([]);
            // line from center to text anchor
            ctx.strokeStyle = 'rgba(255,158,100,0.3)';
            ctx.lineWidth = 1;
            ctx.beginPath();
            ctx.moveTo(0, 0);
            ctx.lineTo(tx, ty);
            ctx.stroke();
        }
    }

    ctx.restore();

    // ID tag above shape
    var bt = baseType(s.type);
    var tagY = (bt === 'CIRCLE') ? p.y - s.w - 8 : p.y - s.h / 2 - 8;
    var tagX = (bt === 'CIRCLE') ? p.x - s.w : p.x - s.w / 2;
    ctx.font = '9px monospace';
    ctx.fillStyle = sel ? '#e8ff47' : '#3d4352';
    ctx.textAlign = 'left';
    ctx.fillText('#' + s.id + ' ' + s.type, tagX, tagY);
}

// Mini icon for shape list
function drawMini(mc, s, W, H) {
    mc.clearRect(0, 0, W, H);
    mc.strokeStyle = mc.fillStyle = rgba01(s.r, s.g, s.b);
    mc.lineWidth = 1.5;
    var m = 4, bt = baseType(s.type);
    mc.beginPath();
    if (bt === 'RECT') {
        mc.rect(m, m, W - m * 2, H - m * 2);
    } else if (bt === 'CIRCLE') {
        mc.arc(W / 2, H / 2, W / 2 - m, 0, Math.PI * 2);
    } else {
        mc.moveTo(W / 2, m);
        mc.lineTo(W - m, H - m);
        mc.lineTo(m, H - m);
        mc.closePath();
    }
    if (isOutline(s.type)) mc.stroke(); else mc.fill();
}

// ================================================================
//  CANVAS EVENTS
// ================================================================
function evXY(e) {
    var r = cv.getBoundingClientRect();
    return {sx: e.clientX - r.left, sy: e.clientY - r.top};
}

cv.addEventListener('mousemove', function (e) {
    var xy = evXY(e);
    mouseWorld = s2w(xy.sx, xy.sy);
    document.getElementById('coords').textContent =
        Math.round(mouseWorld.x) + ', ' + Math.round(mouseWorld.y);

    if (panning) {
        pan.x = panRef.px + (e.clientX - panRef.mx);
        pan.y = panRef.py + (e.clientY - panRef.my);
        draw();
        return;
    }
    if (dragging && selected !== null) {
        shapes[selected].x = Math.round(dragRef.sx + mouseWorld.x - dragRef.wx);
        shapes[selected].y = Math.round(dragRef.sy + mouseWorld.y - dragRef.wy);
        refreshList();
        refreshProps();
        draw();
        return;
    }
    if (placing) draw();
});

cv.addEventListener('mousedown', function (e) {
    var xy = evXY(e);
    var w = s2w(xy.sx, xy.sy);
    if (e.button === 1 || (e.button === 0 && e.altKey)) {
        e.preventDefault();
        panning = true;
        panRef = {px: pan.x, py: pan.y, mx: e.clientX, my: e.clientY};
        return;
    }
    if (e.button !== 0) return;
    if (placing) {
        var s = mkShape(Math.round(w.x), Math.round(w.y));
        shapes.push(s);
        selected = shapes.length - 1;
        placing = false;
        cv.style.cursor = 'default';
        document.getElementById('modeBadge').textContent = 'SELECT';
        refreshList();
        refreshProps();
        draw();
        return;
    }
    var hit = hitTest(w.x, w.y);
    if (hit !== null) {
        selected = hit;
        dragging = true;
        dragRef = {wx: w.x, wy: w.y, sx: shapes[hit].x, sy: shapes[hit].y};
        refreshList();
        refreshProps();
        draw();
    } else {
        selected = null;
        refreshList();
        refreshProps();
        draw();
    }
});
window.addEventListener('mouseup', function () {
    panning = false;
    dragging = false;
});
cv.addEventListener('contextmenu', function (e) {
    e.preventDefault();
});

function hitTest(wx, wy) {
    for (var i = shapes.length - 1; i >= 0; i--) {
        var s = shapes[i], bt = baseType(s.type);
        if (bt === 'CIRCLE') {
            if ((wx - s.x) * (wx - s.x) + (wy - s.y) * (wy - s.y) <= s.w * s.w) return i;
        } else {
            if (wx >= s.x - s.w / 2 && wx <= s.x + s.w / 2 && wy >= s.y - s.h / 2 && wy <= s.y + s.h / 2) return i;
        }
    }
    return null;
}

// ================================================================
//  SHAPE LIST
// ================================================================
function refreshList() {
    var list = document.getElementById('shapeList');
    document.getElementById('shapeCount').textContent = shapes.length;
    if (!shapes.length) {
        list.innerHTML = '<div class="empty-state">No shapes yet.<br>Click "+ Add Shape"<br>to begin.</div>';
        return;
    }
    list.innerHTML = '';
    for (var i = 0; i < shapes.length; i++) {
        (function (idx) {
            var s = shapes[idx];
            var item = document.createElement('div');
            item.className = 'shape-item' + (idx === selected ? ' selected' : '');

            var iconWrap = document.createElement('div');
            iconWrap.className = 'shape-icon';
            var ic = document.createElement('canvas');
            ic.width = ic.height = 26;
            drawMini(ic.getContext('2d'), s, 26, 26);
            iconWrap.appendChild(ic);

            var meta = document.createElement('div');
            meta.className = 'shape-meta';
            meta.innerHTML = '<div class="shape-type">' + s.type + '</div>' +
                '<div class="shape-sub">(' + s.x + ', ' + s.y + ') &middot; ' + s.w + '&times;' + s.h + '</div>';

            var del = document.createElement('button');
            del.className = 'shape-del';
            del.textContent = 'x';
            del.title = 'Remove';
            del.onclick = function (e) {
                e.stopPropagation();
                removeShape(idx);
            };

            item.appendChild(iconWrap);
            item.appendChild(meta);
            item.appendChild(del);
            item.onclick = function () {
                selected = idx;
                refreshList();
                refreshProps();
                draw();
            };
            list.appendChild(item);
        })(i);
    }
}

// ================================================================
//  PROPERTIES
// ================================================================

// Returns the font <select> options HTML
function fontSelectOptions(current) {
    var html = '<option value="">-- type font name --</option>';
    for (var i = 0; i < fonts.length; i++) {
        var f = fonts[i];
        html += '<option value="' + htmlesc(f.family) + '"' + (current === f.family ? ' selected' : '') + '>' + htmlesc(f.name) + '</option>';
    }
    return html;
}

// Detect which anchor is currently closest to the offset
function detectAnchor(s) {
    var hw = baseType(s.type) === 'CIRCLE' ? s.w : s.w / 2;
    var hh = baseType(s.type) === 'CIRCLE' ? s.w : s.h / 2;
    var p = ANCHOR_PADDING;
    var best = null, bestDist = Infinity;
    for (var i = 0; i < ANCHORS.length; i++) {
        var a = ANCHORS[i];
        var tx = a.fx * (hw - p);
        var ty = a.fy * (hh - p);
        var d = (s.textOffsetX - tx) * (s.textOffsetX - tx) + (s.textOffsetY - ty) * (s.textOffsetY - ty);
        if (d < bestDist) {
            bestDist = d;
            best = a.id;
        }
    }
    return bestDist < 100 ? best : null;
}

function anchorOffsets(s, anchor) {
    var hw = baseType(s.type) === 'CIRCLE' ? s.w : s.w / 2;
    var hh = baseType(s.type) === 'CIRCLE' ? s.w : s.h / 2;
    var p = ANCHOR_PADDING;
    for (var i = 0; i < ANCHORS.length; i++) {
        if (ANCHORS[i].id === anchor) {
            return {
                x: Math.round(ANCHORS[i].fx * (hw - p)),
                y: Math.round(ANCHORS[i].fy * (hh - p))
            };
        }
    }
    return {x: 0, y: 0};
}

function refreshProps() {
    var panel = document.getElementById('props');
    if (selected === null || !shapes[selected]) {
        panel.innerHTML = '<div class="no-sel">Select a shape<br>to edit its properties.</div>';
        return;
    }
    var s = shapes[selected];
    var bt = baseType(s.type);
    var isCirc = bt === 'CIRCLE';

    var typeButtons = TYPES.map(function (t) {
        var bt2 = baseType(t);
        var icon = ICONS[bt2] || bt2;
        var label = isOutline(t) ? bt2.slice(0, 3) + '_OL' : bt2.slice(0, 5);
        return '<button class="type-btn' + (s.type === t ? ' sel' : '') + '" data-t="' + t + '">' + icon + '<br>' + label + '</button>';
    }).join('');

    var hField = isCirc ? '' : '<div class="prop-field"><span class="prop-field-label">H</span>' +
        '<input type="number" id="ph" value="' + s.h + '" step="1" min="1"></div>';

    // Anchor widget
    var activeAnchor = detectAnchor(s);
    var anchorCells = ANCHORS.map(function (a) {
        var isMC = (a.id === 'MC');
        return '<div class="anchor-cell' + (isMC ? ' center-cell' : '') + (activeAnchor === a.id ? ' active' : '') +
            '" data-anchor="' + a.id + '" title="' + a.id + '">' + a.label + '</div>';
    }).join('');

    panel.innerHTML =
        '<div class="prop-group">' +
        '<span class="prop-label">Shape Type</span>' +
        '<div class="type-grid">' + typeButtons + '</div>' +
        '</div>' +
        '<hr class="divider">' +
        '<div class="prop-group">' +
        '<span class="prop-label">Position (world units)</span>' +
        '<div class="prop-row">' +
        '<div class="prop-field"><span class="prop-field-label">X</span><input type="number" id="px" value="' + s.x + '" step="1"></div>' +
        '<div class="prop-field"><span class="prop-field-label">Y</span><input type="number" id="py" value="' + s.y + '" step="1"></div>' +
        '</div>' +
        '</div>' +
        '<div class="prop-group">' +
        '<span class="prop-label">' + (isCirc ? 'Radius' : 'Dimensions') + '</span>' +
        '<div class="prop-row">' +
        '<div class="prop-field"><span class="prop-field-label">' + (isCirc ? 'R' : 'W') + '</span><input type="number" id="pw" value="' + s.w + '" step="1" min="1"></div>' +
        hField +
        '</div>' +
        '</div>' +
        '<div class="prop-group">' +
        '<span class="prop-label">Fill color (RGB 0-1)</span>' +
        '<div class="color-row">' +
        '<input type="color" id="cpick" value="' + r1hex(s.r, s.g, s.b) + '">' +
        '<input type="text" id="chex" value="' + r1hex(s.r, s.g, s.b) + '" maxlength="7">' +
        '</div>' +
        '<div class="prop-row">' +
        '<div class="prop-field"><span class="prop-field-label">R</span><input type="number" id="cr" value="' + s.r.toFixed(4) + '" step="0.001" min="0" max="1"></div>' +
        '<div class="prop-field"><span class="prop-field-label">G</span><input type="number" id="cg" value="' + s.g.toFixed(4) + '" step="0.001" min="0" max="1"></div>' +
        '<div class="prop-field"><span class="prop-field-label">B</span><input type="number" id="cb" value="' + s.b.toFixed(4) + '" step="0.001" min="0" max="1"></div>' +
        '</div>' +
        '</div>' +
        '<hr class="divider">' +
        '<div class="prop-group">' +
        '<span class="prop-label">Text</span>' +
        '<div class="prop-field" style="margin-bottom:5px">' +
        '<span class="prop-field-label">Content</span>' +
        '<input type="text" id="txt" value="' + htmlesc(s.text) + '" placeholder="Label...">' +
        '</div>' +
        '<div class="prop-row">' +
        '<div class="prop-field"><span class="prop-field-label">Size</span><input type="number" id="tfs" value="' + s.fontSize + '" step="1" min="6"></div>' +
        '<div class="prop-field"><span class="prop-field-label">Color</span>' +
        '<div class="color-row" style="margin-bottom:0">' +
        '<input type="color" id="tcpick" value="' + r1hex(s.textR, s.textG, s.textB) + '">' +
        '<input type="text" id="tchex" value="' + r1hex(s.textR, s.textG, s.textB) + '" maxlength="7">' +
        '</div>' +
        '</div>' +
        '</div>' +
        '<div class="prop-field" style="margin-bottom:7px">' +
        '<span class="prop-field-label">Font</span>' +
        '<select id="tfsel">' + fontSelectOptions(s.fontName) + '</select>' +
        '<input type="text" id="tfn" value="' + htmlesc(s.fontName) + '" placeholder="font-name" style="margin-top:3px">' +
        '</div>' +
        '</div>' +
        '<hr class="divider">' +
        '<div class="prop-group">' +
        '<span class="anchor-label">Text position</span>' +
        '<div class="anchor-widget" id="anchorWidget">' + anchorCells + '</div>' +
        '<div class="prop-row">' +
        '<div class="prop-field"><span class="prop-field-label">Offset X</span><input type="number" id="tox" value="' + s.textOffsetX + '" step="1"></div>' +
        '<div class="prop-field"><span class="prop-field-label">Offset Y</span><input type="number" id="toy" value="' + s.textOffsetY + '" step="1"></div>' +
        '</div>' +
        '</div>';

    // ── Type buttons
    var typeBtns = panel.querySelectorAll('.type-btn');
    for (var ti = 0; ti < typeBtns.length; ti++) {
        (function (btn) {
            btn.addEventListener('click', function () {
                s.type = btn.getAttribute('data-t');
                refreshList();
                refreshProps();
                draw();
            });
        })(typeBtns[ti]);
    }

    // ── Number bindings
    function bindNum(id, key) {
        var el = panel.querySelector('#' + id);
        if (!el) return;
        el.addEventListener('input', function () {
            var v = parseFloat(el.value);
            if (!isNaN(v)) {
                s[key] = v;
                refreshList();
                draw();
            }
        });
    }

    bindNum('px', 'x');
    bindNum('py', 'y');
    bindNum('pw', 'w');
    bindNum('ph', 'h');
    bindNum('cr', 'r');
    bindNum('cg', 'g');
    bindNum('cb', 'b');
    bindNum('tfs', 'fontSize');
    bindNum('tox', 'textOffsetX');
    bindNum('toy', 'textOffsetY');

    // Refresh anchor highlight when offset fields change manually
    ['tox', 'toy'].forEach(function (id) {
        var el = panel.querySelector('#' + id);
        if (!el) return;
        el.addEventListener('input', function () {
            // Re-highlight anchor cells
            var ac = detectAnchor(s);
            panel.querySelectorAll('.anchor-cell').forEach(function (cell) {
                cell.classList.toggle('active', cell.getAttribute('data-anchor') === ac);
            });
        });
    });

    // ── Fill color sync
    var cpick = panel.querySelector('#cpick'), chex = panel.querySelector('#chex');
    var crI = panel.querySelector('#cr'), cgI = panel.querySelector('#cg'), cbI = panel.querySelector('#cb');

    function applyHex(hex) {
        var rgb = hex2r1(hex);
        if (!rgb) return;
        s.r = rgb.r;
        s.g = rgb.g;
        s.b = rgb.b;
        crI.value = s.r.toFixed(4);
        cgI.value = s.g.toFixed(4);
        cbI.value = s.b.toFixed(4);
        draw();
        refreshList();
    }

    cpick.addEventListener('input', function () {
        chex.value = cpick.value;
        applyHex(cpick.value);
    });
    chex.addEventListener('input', function () {
        if (/^#[0-9a-fA-F]{6}$/.test(chex.value)) {
            cpick.value = chex.value;
            applyHex(chex.value);
        }
    });
    [crI, cgI, cbI].forEach(function (inp) {
        if (!inp) return;
        inp.addEventListener('input', function () {
            s.r = cl01(parseFloat(crI.value));
            s.g = cl01(parseFloat(cgI.value));
            s.b = cl01(parseFloat(cbI.value));
            var h = r1hex(s.r, s.g, s.b);
            cpick.value = h;
            chex.value = h;
            draw();
            refreshList();
        });
    });

    // ── Text content
    var txtEl = panel.querySelector('#txt');
    txtEl.addEventListener('input', function () {
        s.text = txtEl.value;
        draw();
    });

    // ── Font select + manual name input (synced)
    var tfsel = panel.querySelector('#tfsel');
    var tfnEl = panel.querySelector('#tfn');
    tfsel.addEventListener('change', function () {
        if (tfsel.value) {
            s.fontName = tfsel.value;
            tfnEl.value = tfsel.value;
            draw();
        }
    });
    tfnEl.addEventListener('input', function () {
        s.fontName = tfnEl.value;
        draw();
        // Keep select in sync if value matches a loaded font
        for (var k = 0; k < tfsel.options.length; k++) {
            if (tfsel.options[k].value === tfnEl.value) {
                tfsel.selectedIndex = k;
                break;
            }
        }
    });

    // ── Text color
    var tcpick = panel.querySelector('#tcpick'), tchex = panel.querySelector('#tchex');
    tcpick.addEventListener('input', function () {
        tchex.value = tcpick.value;
        var rgb = hex2r1(tcpick.value);
        if (!rgb) return;
        s.textR = rgb.r;
        s.textG = rgb.g;
        s.textB = rgb.b;
        draw();
    });
    tchex.addEventListener('input', function () {
        if (/^#[0-9a-fA-F]{6}$/.test(tchex.value)) {
            tcpick.value = tchex.value;
            var rgb = hex2r1(tchex.value);
            if (!rgb) return;
            s.textR = rgb.r;
            s.textG = rgb.g;
            s.textB = rgb.b;
            draw();
        }
    });

    // ── Anchor widget
    panel.querySelectorAll('.anchor-cell').forEach(function (cell) {
        cell.addEventListener('click', function () {
            var anchorId = cell.getAttribute('data-anchor');
            var off = anchorOffsets(s, anchorId);
            s.textOffsetX = off.x;
            s.textOffsetY = off.y;
            // update inputs
            var toxEl = panel.querySelector('#tox'), toyEl = panel.querySelector('#toy');
            if (toxEl) toxEl.value = off.x;
            if (toyEl) toyEl.value = off.y;
            // update active class
            panel.querySelectorAll('.anchor-cell').forEach(function (c) {
                c.classList.toggle('active', c.getAttribute('data-anchor') === anchorId);
            });
            draw();
        });
    });
}

// ================================================================
//  ACTIONS
// ================================================================
function removeShape(i) {
    shapes.splice(i, 1);
    if (selected >= shapes.length) selected = shapes.length - 1;
    if (!shapes.length) selected = null;
    refreshList();
    refreshProps();
    draw();
}

document.getElementById('btnAdd').addEventListener('click', function () {
    placing = true;
    cv.style.cursor = 'crosshair';
    document.getElementById('modeBadge').textContent = 'PLACE';
    selected = null;
    refreshList();
    refreshProps();
    draw();
});

document.getElementById('btnClear').addEventListener('click', function () {
    if (!shapes.length) return;
    if (!confirm('Clear all shapes?')) return;
    shapes = [];
    selected = null;
    uid = 1;
    placing = false;
    cv.style.cursor = 'default';
    document.getElementById('modeBadge').textContent = 'SELECT';
    refreshList();
    refreshProps();
    draw();
});

// ================================================================
//  JAVA EXPORT
// ================================================================
function genJava() {
    if (!shapes.length) return '<span class="cm">// No shapes to export.</span>';

    var hasOffset = shapes.some(function (s) {
        return s.textOffsetX !== 0 || s.textOffsetY !== 0;
    });

    var L = [];
    L.push('<span class="cm">// Generated by ShapeModel Editor</span>');
    if (hasOffset) {
        L.push('<span class="cm">// NOTE: textOffsetX / textOffsetY require adding these public fields</span>');
        L.push('<span class="cm">//   to Shape.java:  public double textOffsetX = 0, textOffsetY = 0;</span>');
        L.push('');
    }
    L.push('<span class="kw">public</span> <span class="cl">ShapeModel</span> <span class="fn">buildShapeModel</span>() {');
    L.push('    <span class="cl">ShapeModel</span> model = <span class="kw">new</span> <span class="cl">ShapeModel</span>();');
    L.push('    <span class="cl">Shape</span> s;');
    L.push('');

    for (var i = 0; i < shapes.length; i++) {
        var s = shapes[i];
        var bt = baseType(s.type);
        var dim = bt === 'CIRCLE'
            ? '<span class="kw">new</span> <span class="cl">Vector2d</span>(<span class="nm">' + s.w + '</span>, <span class="nm">' + s.w + '</span>)'
            : '<span class="kw">new</span> <span class="cl">Vector2d</span>(<span class="nm">' + s.w + '</span>, <span class="nm">' + s.h + '</span>)';

        L.push('    <span class="cm">// Shape #' + s.id + ': ' + s.type + '</span>');
        L.push('    s = <span class="kw">new</span> <span class="cl">Shape</span>(');
        L.push('        <span class="kw">new</span> <span class="cl">Vector2d</span>(<span class="nm">' + s.x + '</span>, <span class="nm">' + s.y + '</span>),');
        L.push('        ' + dim + ',');
        L.push('        <span class="cl">Shape</span>.<span class="cl">ShapeType</span>.' + s.type);
        L.push('    );');
        L.push('    s.setColor(<span class="nm">' + s.r.toFixed(3) + 'f</span>, <span class="nm">' + s.g.toFixed(3) + 'f</span>, <span class="nm">' + s.b.toFixed(3) + 'f</span>);');

        if (s.text) {
            L.push('    s.setText(');
            L.push('        <span class="st">"' + jesc(s.text) + '"</span>,');
            L.push('        <span class="nm">' + s.fontSize + 'f</span>,');
            L.push('        <span class="kw">new</span> <span class="cl">Vector3d</span>(<span class="nm">' + s.textR.toFixed(3) + 'f</span>, <span class="nm">' + s.textG.toFixed(3) + 'f</span>, <span class="nm">' + s.textB.toFixed(3) + 'f</span>),');
            L.push('        <span class="st">"' + jesc(s.fontName) + '"</span>');
            L.push('    );');
        }

        if (s.textOffsetX !== 0 || s.textOffsetY !== 0) {
            L.push('    s.textOffsetX = <span class="nm">' + s.textOffsetX.toFixed(1) + '</span>;');
            L.push('    s.textOffsetY = <span class="nm">' + s.textOffsetY.toFixed(1) + '</span>;');
        }

        L.push('    model.addShape(s);');
        if (i < shapes.length - 1) L.push('');
    }

    L.push('');
    L.push('    <span class="kw">return</span> model;');
    L.push('}');
    return L.join('\n');
}

document.getElementById('btnExport').addEventListener('click', function () {
    document.getElementById('exportCode').innerHTML = genJava();
    document.getElementById('exportModal').classList.add('open');
});
document.getElementById('btnModalClose').addEventListener('click', function () {
    document.getElementById('exportModal').classList.remove('open');
});
document.getElementById('btnModalDone').addEventListener('click', function () {
    document.getElementById('exportModal').classList.remove('open');
});
document.getElementById('exportModal').addEventListener('click', function (e) {
    if (e.target === document.getElementById('exportModal'))
        document.getElementById('exportModal').classList.remove('open');
});
document.getElementById('btnCopy').addEventListener('click', function () {
    var code = document.getElementById('exportCode').innerText;
    navigator.clipboard.writeText(code).then(function () {
        var b = document.getElementById('btnCopy');
        b.textContent = 'Copied!';
        setTimeout(function () {
            b.textContent = 'Copy to clipboard';
        }, 2200);
    });
});

// ================================================================
//  KEYBOARD
// ================================================================
window.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') {
        placing = false;
        cv.style.cursor = 'default';
        document.getElementById('modeBadge').textContent = 'SELECT';
        draw();
    }
    if ((e.key === 'Delete' || e.key === 'Backspace') && selected !== null) {
        var tag = document.activeElement.tagName;
        if (tag === 'INPUT' || tag === 'SELECT' || tag === 'TEXTAREA') return;
        removeShape(selected);
    }
    var arrowMap = {ArrowLeft: [-1, 0], ArrowRight: [1, 0], ArrowUp: [0, -1], ArrowDown: [0, 1]};
    if (arrowMap[e.key] && selected !== null) {
        var tag = document.activeElement.tagName;
        if (tag === 'INPUT' || tag === 'SELECT' || tag === 'TEXTAREA') return;
        var d = e.shiftKey ? 10 : 1;
        shapes[selected].x += arrowMap[e.key][0] * d;
        shapes[selected].y += arrowMap[e.key][1] * d;
        refreshList();
        refreshProps();
        draw();
        e.preventDefault();
    }
});

// ================================================================
//  UTILS
// ================================================================
function r1hex(r, g, b) {
    function h(v) {
        return Math.round(v * 255).toString(16).padStart(2, '0');
    }

    return '#' + h(r) + h(g) + h(b);
}

function hex2r1(hex) {
    var m = hex.match(/^#?([0-9a-fA-F]{6})$/);
    if (!m) return null;
    var n = parseInt(m[1], 16);
    return {r: ((n >> 16) & 255) / 255, g: ((n >> 8) & 255) / 255, b: (n & 255) / 255};
}

function cl01(v) {
    return Math.max(0, Math.min(1, isNaN(v) ? 0 : v));
}

function jesc(s) {
    return s.replace(/\\/g, '\\\\').replace(/"/g, '\\"');
}

function htmlesc(s) {
    return (s || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
}

// Init
resize();