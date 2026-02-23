/* ============================================================
   CONSTANTS
   ============================================================ */
var TYPES = [
    'RECT', 'CIRCLE', 'TRIANGLE',
    'RECT_OUTLINE', 'CIRCLE_OUTLINE', 'TRIANGLE_OUTLINE'
];
var ICONS = {RECT: '[_]', CIRCLE: '( )', TRIANGLE: '/\\/'};

// 3×3 preset grid: [col, row] where -1=start, 0=center, 1=end
var OFFSET_PRESETS = [
    [-1, -1], [0, -1], [1, -1],
    [-1, 0], [0, 0], [1, 0],
    [-1, 1], [0, 1], [1, 1]
];

function baseType(t) {
    return t.replace('_OUTLINE', '');
}

function isOutline(t) {
    return t.indexOf('_OUTLINE') !== -1;
}

/* ============================================================
   STATE
   ============================================================ */
var shapes = [];   // array of shape objects
var fonts = [];   // array of { name, file, status }
var selected = null; // index | null
var uid = 1;
var placing = false;

// Canvas pan
var pan = {x: 0, y: 0};
var panning = false;
var panRef = {px: 0, py: 0, mx: 0, my: 0};

// Shape drag (canvas)
var dragging = false;
var dragRef = {wx: 0, wy: 0, sx: 0, sy: 0};

// List drag-to-reorder
var listDragSrc = null; // index being dragged in the list

var mouseWorld = {x: 0, y: 0};

// ── Shape factory ──────────────────────────────────────────
function mkShape(x, y) {
    return {
        id: uid++,
        name: 'Shape ' + uid,
        type: 'RECT',
        x: x || 0,
        y: y || 0,
        w: 80,
        h: 50,
        // fill
        r: 1, g: 0, b: 0,
        // text
        text: '',
        fontSize: 32,
        textOffsetX: 0,
        textOffsetY: 0,
        textR: 0, textG: 0, textB: 0,
        fontName: 'minecraft'
    };
}

/* ============================================================
   CANVAS SETUP
   ============================================================ */
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

/* ============================================================
   DRAWING
   ============================================================ */
function draw() {
    var W = cv.width, H = cv.height;
    ctx.clearRect(0, 0, W, H);
    drawGrid(W, H);
    drawAxes(W, H);
    for (var i = 0; i < shapes.length; i++) {
        drawShape(shapes[i], i === selected);
    }
    if (placing) drawGhost();
}

function drawGrid(W, H) {
    var step = 40;
    var ox = (pan.x + W / 2) % step;
    var oy = (pan.y + H / 2) % step;
    if (ox < 0) ox += step;
    if (oy < 0) oy += step;
    ctx.strokeStyle = 'rgba(255,255,255,.028)';
    ctx.lineWidth = 1;
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
    ctx.strokeStyle = 'rgba(232,255,71,.18)';
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
    ctx.strokeStyle = 'rgba(232,255,71,.4)';
    ctx.lineWidth = 1;
    ctx.setLineDash([3, 4]);
    ctx.strokeRect(p.x - 40, p.y - 25, 80, 50);
    ctx.setLineDash([]);
}

function rgba01(r, g, b, a) {
    a = (a === undefined) ? 1 : a;
    return 'rgba(' + Math.round(r * 255) + ',' + Math.round(g * 255) + ',' + Math.round(b * 255) + ',' + a + ')';
}

// Trace the shape path centered at origin
function tracePath(s) {
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

    // Shape fill / stroke
    ctx.beginPath();
    tracePath(s);
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
        tracePath(s);
        ctx.strokeStyle = '#e8ff47';
        ctx.lineWidth = 1.5;
        ctx.stroke();
    }

    // Text — use actual fontSize ← FIX
    if (s.text) {
        ctx.shadowBlur = 0;
        var fontFamily = resolvedFontFamily(s.fontName);
        ctx.font = 'bold ' + s.fontSize + 'px ' + fontFamily;
        ctx.fillStyle = rgba01(s.textR, s.textG, s.textB);
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        var label = s.text.length > 20 ? s.text.slice(0, 19) + '…' : s.text;
        ctx.fillText(label, s.textOffsetX, s.textOffsetY);

        // Offset indicator when selected
        if (sel && (s.textOffsetX !== 0 || s.textOffsetY !== 0)) {
            ctx.strokeStyle = 'rgba(232,255,71,.5)';
            ctx.lineWidth = 1;
            ctx.setLineDash([2, 3]);
            ctx.beginPath();
            ctx.moveTo(0, 0);
            ctx.lineTo(s.textOffsetX, s.textOffsetY);
            ctx.stroke();
            ctx.setLineDash([]);
            ctx.fillStyle = 'rgba(232,255,71,.85)';
            ctx.beginPath();
            ctx.arc(s.textOffsetX, s.textOffsetY, 3, 0, Math.PI * 2);
            ctx.fill();
        }
    }

    ctx.restore();

    // Id / name tag above shape
    var bt = baseType(s.type);
    var tagY = (bt === 'CIRCLE') ? p.y - s.w - 8 : p.y - s.h / 2 - 8;
    var tagX = (bt === 'CIRCLE') ? p.x - s.w : p.x - s.w / 2;
    ctx.font = '9px monospace';
    ctx.fillStyle = sel ? '#e8ff47' : '#3d4352';
    ctx.textAlign = 'left';
    ctx.fillText((s.name || ('#' + s.id)) + ' · ' + s.type, tagX, tagY);
}

// Returns the CSS font family string for a given fontName
function resolvedFontFamily(fontName) {
    for (var i = 0; i < fonts.length; i++) {
        if (fonts[i].name === fontName && fonts[i].status === 'ok') {
            return '"' + fontName + '"';
        }
    }
    return 'monospace';
}

// Mini icon (26×26) for shape list
function drawMini(mc, s, W, H) {
    mc.clearRect(0, 0, W, H);
    mc.strokeStyle = mc.fillStyle = rgba01(s.r, s.g, s.b);
    mc.lineWidth = 1.5;
    var m = 4;
    var bt = baseType(s.type);
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

/* ============================================================
   CANVAS EVENTS
   ============================================================ */
function canvasXY(e) {
    var r = cv.getBoundingClientRect();
    return {sx: e.clientX - r.left, sy: e.clientY - r.top};
}

cv.addEventListener('mousemove', function (e) {
    var xy = canvasXY(e);
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
    var xy = canvasXY(e);
    var w = s2w(xy.sx, xy.sy);

    // Pan: middle button or Alt+left
    if (e.button === 1 || (e.button === 0 && e.altKey)) {
        e.preventDefault();
        panning = true;
        panRef = {px: pan.x, py: pan.y, mx: e.clientX, my: e.clientY};
        return;
    }
    if (e.button !== 0) return;

    // Place a new shape
    if (placing) {
        var s = mkShape(Math.round(w.x), Math.round(w.y));
        shapes.push(s);
        selected = shapes.length - 1;
        placing = false;
        cv.style.cursor = 'default';
        setModeBadge('SELECT');
        refreshList();
        refreshProps();
        draw();
        return;
    }

    // Hit test → select / drag
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
        var s = shapes[i];
        var bt = baseType(s.type);
        if (bt === 'CIRCLE') {
            if ((wx - s.x) * (wx - s.x) + (wy - s.y) * (wy - s.y) <= s.w * s.w) return i;
        } else {
            if (wx >= s.x - s.w / 2 && wx <= s.x + s.w / 2 &&
                wy >= s.y - s.h / 2 && wy <= s.y + s.h / 2) return i;
        }
    }
    return null;
}

/* ============================================================
   SHAPE LIST
   ============================================================ */
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
            item.setAttribute('draggable', 'true');
            item.dataset.idx = idx;

            // ── Drag handle
            var handle = document.createElement('div');
            handle.className = 'drag-handle';
            handle.textContent = '≡';
            handle.title = 'Drag to reorder';

            // ── Mini icon
            var iconWrap = document.createElement('div');
            iconWrap.className = 'shape-icon';
            var ic = document.createElement('canvas');
            ic.width = ic.height = 26;
            drawMini(ic.getContext('2d'), s, 26, 26);
            iconWrap.appendChild(ic);

            // ── Meta
            var meta = document.createElement('div');
            meta.className = 'shape-meta';
            meta.innerHTML =
                '<div class="shape-name">' + htmlesc(s.name || ('#' + s.id)) + '</div>' +
                '<div class="shape-sub">' + s.type + ' · (' + s.x + ', ' + s.y + ')</div>';

            // ── Action buttons
            var actions = document.createElement('div');
            actions.className = 'shape-actions';

            // Up
            var btnUp = makeActBtn('↑', 'Move up');
            btnUp.onclick = function (e) {
                e.stopPropagation();
                if (idx === 0) return;
                arraySwap(shapes, idx, idx - 1);
                if (selected === idx) selected = idx - 1;
                else if (selected === idx - 1) selected = idx;
                refreshList();
                refreshProps();
                draw();
            };

            // Down
            var btnDown = makeActBtn('↓', 'Move down');
            btnDown.onclick = function (e) {
                e.stopPropagation();
                if (idx === shapes.length - 1) return;
                arraySwap(shapes, idx, idx + 1);
                if (selected === idx) selected = idx + 1;
                else if (selected === idx + 1) selected = idx;
                refreshList();
                refreshProps();
                draw();
            };

            // Duplicate
            var btnDup = makeActBtn('⎘', 'Duplicate');
            btnDup.onclick = function (e) {
                e.stopPropagation();
                duplicateShape(idx);
            };

            // Delete
            var btnDel = makeActBtn('×', 'Remove');
            btnDel.classList.add('del');
            btnDel.onclick = function (e) {
                e.stopPropagation();
                removeShape(idx);
            };

            actions.appendChild(btnUp);
            actions.appendChild(btnDown);
            actions.appendChild(btnDup);
            actions.appendChild(btnDel);

            item.appendChild(handle);
            item.appendChild(iconWrap);
            item.appendChild(meta);
            item.appendChild(actions);

            // Click to select
            item.onclick = function () {
                selected = idx;
                refreshList();
                refreshProps();
                draw();
            };

            // ── HTML5 Drag-to-reorder
            item.addEventListener('dragstart', function (e) {
                listDragSrc = idx;
                e.dataTransfer.effectAllowed = 'move';
                setTimeout(function () {
                    item.classList.add('dragging');
                }, 0);
            });
            item.addEventListener('dragend', function () {
                item.classList.remove('dragging');
                listDragSrc = null;
                list.querySelectorAll('.shape-item').forEach(function (el) {
                    el.classList.remove('drag-over');
                });
            });
            item.addEventListener('dragover', function (e) {
                e.preventDefault();
                e.dataTransfer.dropEffect = 'move';
                if (listDragSrc !== null && listDragSrc !== idx) {
                    item.classList.add('drag-over');
                }
            });
            item.addEventListener('dragleave', function () {
                item.classList.remove('drag-over');
            });
            item.addEventListener('drop', function (e) {
                e.preventDefault();
                item.classList.remove('drag-over');
                if (listDragSrc === null || listDragSrc === idx) return;
                var moved = shapes.splice(listDragSrc, 1)[0];
                shapes.splice(idx, 0, moved);
                // Fix selection index
                if (selected === listDragSrc) {
                    selected = idx;
                } else if (listDragSrc < idx) {
                    if (selected > listDragSrc && selected <= idx) selected--;
                } else {
                    if (selected >= idx && selected < listDragSrc) selected++;
                }
                listDragSrc = null;
                refreshList();
                refreshProps();
                draw();
            });

            list.appendChild(item);
        })(i);
    }
}

function makeActBtn(label, title) {
    var btn = document.createElement('button');
    btn.className = 'shape-act-btn';
    btn.textContent = label;
    btn.title = title;
    return btn;
}

function arraySwap(arr, i, j) {
    var tmp = arr[i];
    arr[i] = arr[j];
    arr[j] = tmp;
}

/* ============================================================
   FONT MANAGER
   ============================================================ */
function refreshFontList() {
    document.getElementById('fontCount').textContent = fonts.length;
    var list = document.getElementById('fontList');
    list.innerHTML = '';

    for (var i = 0; i < fonts.length; i++) {
        (function (idx) {
            var f = fonts[idx];
            var item = document.createElement('div');
            item.className = 'font-item';

            var nm = document.createElement('div');
            nm.className = 'font-item-name';
            nm.innerHTML =
                '<div class="font-name-label">' + htmlesc(f.name) + '</div>' +
                '<div class="font-file-label">' + htmlesc(f.file) + '</div>';

            var st = document.createElement('span');
            st.className = 'font-status ' + f.status;
            st.textContent = f.status === 'ok' ? 'loaded' : f.status === 'loading' ? '...' : 'error';

            var del = document.createElement('button');
            del.className = 'shape-act-btn del';
            del.textContent = '×';
            del.title = 'Remove font';
            del.onclick = function () {
                fonts.splice(idx, 1);
                refreshFontList();
                draw();
            };

            item.appendChild(nm);
            item.appendChild(st);
            item.appendChild(del);
            list.appendChild(item);
        })(i);
    }
}

document.getElementById('btnAddFont').addEventListener('click', function () {
    var nameEl = document.getElementById('newFontName');
    var fileEl = document.getElementById('newFontFile');
    var name = nameEl.value.trim();
    var file = fileEl.value.trim();
    if (!name || !file) return;

    for (var i = 0; i < fonts.length; i++) {
        if (fonts[i].name === name) {
            alert('Font name "' + name + '" already registered.');
            return;
        }
    }

    var entry = {name: name, file: file, status: 'loading'};
    fonts.push(entry);
    refreshFontList();

    var ff = new FontFace(name, 'url("' + file + '")');
    ff.load().then(function (loaded) {
        document.fonts.add(loaded);
        entry.status = 'ok';
        refreshFontList();
        draw();
    }).catch(function () {
        entry.status = 'err';
        refreshFontList();
    });

    nameEl.value = '';
    fileEl.value = '';
});

/* ============================================================
   PROPERTIES PANEL
   ============================================================ */
function refreshProps() {
    var panel = document.getElementById('props');

    if (selected === null || !shapes[selected]) {
        panel.innerHTML = '<div class="no-sel">Select a shape<br>to edit its properties.</div>';
        return;
    }

    var s = shapes[selected];
    var bt = baseType(s.type);
    var isCirc = (bt === 'CIRCLE');

    // ── Type buttons HTML
    var typeBtns = TYPES.map(function (t) {
        var b = baseType(t);
        var icon = ICONS[b] || b;
        var lbl = isOutline(t) ? b.slice(0, 3) + '_OL' : b.slice(0, 5);
        return '<button class="type-btn' + (s.type === t ? ' sel' : '') + '" data-t="' + t + '">' +
            icon + '<br>' + lbl + '</button>';
    }).join('');

    // ── Dimension H field (hidden for circles)
    var hField = isCirc ? '' :
        '<div class="prop-field"><span class="prop-field-label">H</span>' +
        '<input type="number" id="ph" value="' + s.h + '" step="1" min="1"></div>';

    // ── Font select
    var fontOpts = '<option value=""' + (!s.fontName ? ' selected' : '') + '>monospace (default)</option>';
    for (var fi = 0; fi < fonts.length; fi++) {
        fontOpts += '<option value="' + htmlesc(fonts[fi].name) + '"' +
            (s.fontName === fonts[fi].name ? ' selected' : '') + '>' +
            htmlesc(fonts[fi].name) +
            (fonts[fi].status !== 'ok' ? ' (' + fonts[fi].status + ')' : '') +
            '</option>';
    }

    // ── 3×3 offset preset grid
    var presetBtns = '';
    var halfW = isCirc ? s.w : s.w / 2;
    var halfH = isCirc ? s.w : s.h / 2;

    for (var pi = 0; pi < OFFSET_PRESETS.length; pi++) {
        var p = OFFSET_PRESETS[pi];
        var ox = Math.round(p[0] * halfW);
        var oy = Math.round(p[1] * halfH);
        var isSel = (s.textOffsetX === ox && s.textOffsetY === oy);
        presetBtns += '<button class="offset-preset-btn' + (isSel ? ' sel' : '') + '"' +
            ' data-ox="' + ox + '" data-oy="' + oy + '"' +
            ' title="Offset (' + ox + ', ' + oy + ')"></button>';
    }

    panel.innerHTML =
        /* ── Name ── */
        '<div class="prop-group">' +
        '<span class="prop-label">Name</span>' +
        '<div class="prop-field">' +
        '<input type="text" id="sname" value="' + htmlesc(s.name || '') + '" placeholder="Shape name...">' +
        '</div>' +
        '</div>' +
        '<hr class="divider">' +
        /* ── Type ── */
        '<div class="prop-group">' +
        '<span class="prop-label">Shape Type</span>' +
        '<div class="type-grid">' + typeBtns + '</div>' +
        '</div>' +
        '<hr class="divider">' +
        /* ── Position ── */
        '<div class="prop-group">' +
        '<span class="prop-label">Position (world units)</span>' +
        '<div class="prop-row">' +
        '<div class="prop-field"><span class="prop-field-label">X</span><input type="number" id="px" value="' + s.x + '" step="1"></div>' +
        '<div class="prop-field"><span class="prop-field-label">Y</span><input type="number" id="py" value="' + s.y + '" step="1"></div>' +
        '</div>' +
        '</div>' +
        /* ── Dimensions ── */
        '<div class="prop-group">' +
        '<span class="prop-label">' + (isCirc ? 'Radius' : 'Dimensions') + '</span>' +
        '<div class="prop-row">' +
        '<div class="prop-field"><span class="prop-field-label">' + (isCirc ? 'R' : 'W') + '</span><input type="number" id="pw" value="' + s.w + '" step="1" min="1"></div>' +
        hField +
        '</div>' +
        '</div>' +
        /* ── Fill color ── */
        '<div class="prop-group">' +
        '<span class="prop-label">Fill color (RGB 0-1)</span>' +
        '<div class="color-row">' +
        '<input type="color" id="cpick" value="' + r1hex(s.r, s.g, s.b) + '">' +
        '<input type="text"  id="chex"  value="' + r1hex(s.r, s.g, s.b) + '" maxlength="7">' +
        '</div>' +
        '<div class="prop-row">' +
        '<div class="prop-field"><span class="prop-field-label">R</span><input type="number" id="cr" value="' + s.r.toFixed(4) + '" step="0.001" min="0" max="1"></div>' +
        '<div class="prop-field"><span class="prop-field-label">G</span><input type="number" id="cg" value="' + s.g.toFixed(4) + '" step="0.001" min="0" max="1"></div>' +
        '<div class="prop-field"><span class="prop-field-label">B</span><input type="number" id="cb" value="' + s.b.toFixed(4) + '" step="0.001" min="0" max="1"></div>' +
        '</div>' +
        '</div>' +
        '<hr class="divider">' +
        /* ── Text ── */
        '<div class="prop-group">' +
        '<span class="prop-label">Text (optional)</span>' +
        '<div class="prop-field" style="margin-bottom:5px">' +
        '<span class="prop-field-label">Content</span>' +
        '<input type="text" id="txt" value="' + htmlesc(s.text) + '" placeholder="Label...">' +
        '</div>' +
        '<div class="prop-row">' +
        '<div class="prop-field"><span class="prop-field-label">Font size</span><input type="number" id="tfs" value="' + s.fontSize + '" step="1" min="1"></div>' +
        '<div class="prop-field"><span class="prop-field-label">Font</span><select id="tfn">' + fontOpts + '</select></div>' +
        '</div>' +
        /* text color */
        '<div class="prop-field" style="margin-bottom:8px">' +
        '<span class="prop-field-label">Text color</span>' +
        '<div class="color-row">' +
        '<input type="color" id="tcpick" value="' + r1hex(s.textR, s.textG, s.textB) + '">' +
        '<input type="text"  id="tchex"  value="' + r1hex(s.textR, s.textG, s.textB) + '" maxlength="7">' +
        '</div>' +
        '</div>' +
        /* text offset presets */
        '<span class="prop-label" style="margin-bottom:3px">Text offset preset</span>' +
        '<div class="offset-presets" id="offsetPresets">' + presetBtns + '</div>' +
        /* manual offset */
        '<div class="prop-row">' +
        '<div class="prop-field"><span class="prop-field-label">Offset X</span><input type="number" id="tox" value="' + s.textOffsetX + '" step="1"></div>' +
        '<div class="prop-field"><span class="prop-field-label">Offset Y</span><input type="number" id="toy" value="' + s.textOffsetY + '" step="1"></div>' +
        '</div>' +
        '</div>';

    // ── Wire type buttons
    panel.querySelectorAll('.type-btn').forEach(function (btn) {
        btn.addEventListener('click', function () {
            s.type = btn.getAttribute('data-t');
            refreshList();
            refreshProps(); // re-render props to update preset offsets for new shape dims
            draw();
        });
    });

    // ── Number binding helper
    function bindNum(id, key) {
        var el = panel.querySelector('#' + id);
        if (!el) return;
        el.addEventListener('input', function () {
            var v = parseFloat(el.value);
            if (!isNaN(v)) s[key] = v;
            refreshList();
            draw();
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

    // ── Name
    var snameEl = panel.querySelector('#sname');
    snameEl.addEventListener('input', function () {
        s.name = snameEl.value;
        refreshList();
    });

    // ── Fill color pickers
    var cpick = panel.querySelector('#cpick');
    var chex = panel.querySelector('#chex');
    var crI = panel.querySelector('#cr');
    var cgI = panel.querySelector('#cg');
    var cbI = panel.querySelector('#cb');

    function applyFillHex(hex) {
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
        applyFillHex(cpick.value);
    });
    chex.addEventListener('input', function () {
        if (/^#[0-9a-fA-F]{6}$/.test(chex.value)) {
            cpick.value = chex.value;
            applyFillHex(chex.value);
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

    // ── Font select
    var tfnEl = panel.querySelector('#tfn');
    tfnEl.addEventListener('change', function () {
        s.fontName = tfnEl.value;
        draw();
    });

    // ── Text color
    var tcpick = panel.querySelector('#tcpick');
    var tchex = panel.querySelector('#tchex');
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

    // ── Offset preset buttons
    var oxEl = panel.querySelector('#tox');
    var oyEl = panel.querySelector('#toy');
    panel.querySelectorAll('.offset-preset-btn').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var ox = parseInt(btn.getAttribute('data-ox'), 10);
            var oy = parseInt(btn.getAttribute('data-oy'), 10);
            s.textOffsetX = ox;
            s.textOffsetY = oy;
            oxEl.value = ox;
            oyEl.value = oy;
            // Refresh preset button highlights
            panel.querySelectorAll('.offset-preset-btn').forEach(function (b) {
                b.classList.remove('sel');
            });
            btn.classList.add('sel');
            draw();
        });
    });
}

/* ============================================================
   ACTIONS
   ============================================================ */
function removeShape(i) {
    shapes.splice(i, 1);
    if (selected >= shapes.length) selected = shapes.length - 1;
    if (!shapes.length) selected = null;
    refreshList();
    refreshProps();
    draw();
}

function duplicateShape(i) {
    var orig = shapes[i];
    var copy = JSON.parse(JSON.stringify(orig)); // deep clone
    copy.id = uid++;
    copy.name = orig.name + ' copy';
    copy.x += 20;
    copy.y += 20;
    shapes.splice(i + 1, 0, copy);
    selected = i + 1;
    refreshList();
    refreshProps();
    draw();
}

document.getElementById('btnAdd').addEventListener('click', function () {
    placing = true;
    cv.style.cursor = 'crosshair';
    setModeBadge('PLACE');
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
    setModeBadge('SELECT');
    refreshList();
    refreshProps();
    draw();
});

function setModeBadge(txt) {
    document.getElementById('modeBadge').textContent = txt;
}

/* ============================================================
   PANEL TABS (LEFT)
   ============================================================ */
document.querySelectorAll('.tab-btn').forEach(function (btn) {
    btn.addEventListener('click', function () {
        document.querySelectorAll('.tab-btn').forEach(function (b) {
            b.classList.remove('active');
        });
        document.querySelectorAll('.tab-pane').forEach(function (p) {
            p.classList.remove('active');
        });
        btn.classList.add('active');
        document.getElementById('tab-' + btn.dataset.tab).classList.add('active');
    });
});

/* ============================================================
   EXPORT TABS
   ============================================================ */
document.querySelectorAll('.export-tab').forEach(function (btn) {
    btn.addEventListener('click', function () {
        document.querySelectorAll('.export-tab').forEach(function (b) {
            b.classList.remove('active');
        });
        document.querySelectorAll('.export-pane').forEach(function (p) {
            p.classList.remove('active');
        });
        btn.classList.add('active');
        document.getElementById('epane-' + btn.dataset.etab).classList.add('active');
    });
});

/* ============================================================
   JAVA EXPORT
   ============================================================ */
function genJava() {
    if (!shapes.length) return '<span class="cm">// No shapes to export.</span>';

    var L = [];
    L.push('<span class="cm">// Generated by ShapeModel Editor</span>');
    L.push('<span class="kw">public</span> <span class="cl">ShapeModel</span> <span class="fn">buildShapeModel</span>() {');
    L.push('    <span class="cl">ShapeModel</span> model = <span class="kw">new</span> <span class="cl">ShapeModel</span>();');
    L.push('    <span class="cl">Shape</span> s;');
    L.push('');

    for (var i = 0; i < shapes.length; i++) {
        var s = shapes[i];
        var bt = baseType(s.type);
        var dim = (bt === 'CIRCLE')
            ? '<span class="kw">new</span> <span class="cl">Vector2d</span>(<span class="nm">' + s.w + '</span>, <span class="nm">' + s.w + '</span>)'
            : '<span class="kw">new</span> <span class="cl">Vector2d</span>(<span class="nm">' + s.w + '</span>, <span class="nm">' + s.h + '</span>)';

        L.push('    <span class="cm">// ' + (s.name || ('Shape #' + s.id)) + ' · ' + s.type + '</span>');
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
            L.push('        <span class="kw">new</span> <span class="cl">Vector3d</span>(' +
                '<span class="nm">' + s.textR.toFixed(3) + 'f</span>, ' +
                '<span class="nm">' + s.textG.toFixed(3) + 'f</span>, ' +
                '<span class="nm">' + s.textB.toFixed(3) + 'f</span>),');
            L.push('        <span class="st">"' + jesc(s.fontName) + '"</span>');
            L.push('    );');
        }
        if (s.textOffsetX !== 0 || s.textOffsetY !== 0) {
            L.push('    s.setTextOffset(<span class="nm">' + s.textOffsetX + '</span>, <span class="nm">' + s.textOffsetY + '</span>);');
        }
        if (s.name) {
            L.push('    s.setName(<span class="st">"' + jesc(s.name) + '"</span>);');
        }
        L.push('    model.addShape(s);');
        if (i < shapes.length - 1) L.push('');
    }

    L.push('');
    L.push('    <span class="kw">return</span> model;');
    L.push('}');
    return L.join('\n');
}

/* ============================================================
   JSON EXPORT
   ============================================================ */
function buildJsonObj() {
    return {
        shapes: shapes.map(function (s) {
            var bt = baseType(s.type);
            var obj = {
                name: s.name || null,
                type: s.type,
                position: {x: s.x, y: s.y},
                dimensions: bt === 'CIRCLE' ? {x: s.w, y: s.w} : {x: s.w, y: s.h},
                color: {r: roundF(s.r), g: roundF(s.g), b: roundF(s.b)}
            };
            if (s.text) {
                obj.text = {
                    content: s.text,
                    fontSize: s.fontSize,
                    color: {r: roundF(s.textR), g: roundF(s.textG), b: roundF(s.textB)},
                    offset: {x: s.textOffsetX, y: s.textOffsetY},
                    fontName: s.fontName
                };
            }
            return obj;
        })
    };
}

function roundF(v) {
    return parseFloat(v.toFixed(4));
}

function syntaxJsonHtml(obj) {
    var raw = JSON.stringify(obj, null, 2);
    // HTML-escape first, then colorize
    raw = raw
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"([^"]+)"(\s*:)/g, '<span class="jk">"$1"</span>$2')
        .replace(/: "([^"]*)"/g, ': <span class="js">"$1"</span>')
        .replace(/: (null|true|false)/g, ': <span class="kw">$1</span>')
        .replace(/: (-?\d+(\.\d+)?)/g, function (m, n) {
            return ': <span class="jv">' + n + '</span>';
        });
    return raw;
}

/* ============================================================
   JAVA LOADER CODE
   ============================================================ */
function genLoader() {
    var L = [];
    L.push('<span class="cm">// ─────────────────────────────────────────────────────────</span>');
    L.push('<span class="cm">// Add to ShapeModel.java</span>');
    L.push('<span class="cm">// Requires Gson: implementation \'com.google.code.gson:gson:2.10.1\'</span>');
    L.push('<span class="cm">// ─────────────────────────────────────────────────────────</span>');
    L.push('');
    L.push('<span class="kw">import</span> com.google.gson.<span class="cl">JsonArray</span>;');
    L.push('<span class="kw">import</span> com.google.gson.<span class="cl">JsonElement</span>;');
    L.push('<span class="kw">import</span> com.google.gson.<span class="cl">JsonObject</span>;');
    L.push('<span class="kw">import</span> com.google.gson.<span class="cl">JsonParser</span>;');
    L.push('');
    L.push('<span class="cm">/**');
    L.push(' * Builds a ShapeModel from a JSON string exported by ShapeModel Editor.');
    L.push(' */</span>');
    L.push('<span class="kw">public static</span> <span class="cl">ShapeModel</span> <span class="fn">fromJson</span>(<span class="cl">String</span> json) {');
    L.push('    <span class="cl">ShapeModel</span> model = <span class="kw">new</span> <span class="cl">ShapeModel</span>();');
    L.push('    <span class="cl">JsonArray</span> arr = <span class="cl">JsonParser</span>.<span class="fn">parseString</span>(json)');
    L.push('                                   .<span class="fn">getAsJsonObject</span>()');
    L.push('                                   .<span class="fn">getAsJsonArray</span>(<span class="st">"shapes"</span>);');
    L.push('');
    L.push('    <span class="kw">for</span> (<span class="cl">JsonElement</span> el : arr) {');
    L.push('        <span class="cl">JsonObject</span> jo  = el.<span class="fn">getAsJsonObject</span>();');
    L.push('        <span class="cl">JsonObject</span> pos = jo.<span class="fn">getAsJsonObject</span>(<span class="st">"position"</span>);');
    L.push('        <span class="cl">JsonObject</span> dim = jo.<span class="fn">getAsJsonObject</span>(<span class="st">"dimensions"</span>);');
    L.push('        <span class="cl">JsonObject</span> col = jo.<span class="fn">getAsJsonObject</span>(<span class="st">"color"</span>);');
    L.push('');
    L.push('        <span class="cl">Shape</span> s = <span class="kw">new</span> <span class="cl">Shape</span>(');
    L.push('            <span class="kw">new</span> <span class="cl">Vector2d</span>(pos.<span class="fn">get</span>(<span class="st">"x"</span>).<span class="fn">getAsDouble</span>(), pos.<span class="fn">get</span>(<span class="st">"y"</span>).<span class="fn">getAsDouble</span>()),');
    L.push('            <span class="kw">new</span> <span class="cl">Vector2d</span>(dim.<span class="fn">get</span>(<span class="st">"x"</span>).<span class="fn">getAsDouble</span>(), dim.<span class="fn">get</span>(<span class="st">"y"</span>).<span class="fn">getAsDouble</span>()),');
    L.push('            <span class="cl">Shape.ShapeType</span>.<span class="fn">valueOf</span>(jo.<span class="fn">get</span>(<span class="st">"type"</span>).<span class="fn">getAsString</span>())');
    L.push('        );');
    L.push('        s.<span class="fn">setColor</span>(col.<span class="fn">get</span>(<span class="st">"r"</span>).<span class="fn">getAsFloat</span>(), col.<span class="fn">get</span>(<span class="st">"g"</span>).<span class="fn">getAsFloat</span>(), col.<span class="fn">get</span>(<span class="st">"b"</span>).<span class="fn">getAsFloat</span>());');
    L.push('');
    L.push('        <span class="kw">if</span> (jo.<span class="fn">has</span>(<span class="st">"name"</span>) && !jo.<span class="fn">get</span>(<span class="st">"name"</span>).<span class="fn">isJsonNull</span>())');
    L.push('            s.<span class="fn">setName</span>(jo.<span class="fn">get</span>(<span class="st">"name"</span>).<span class="fn">getAsString</span>());');
    L.push('');
    L.push('        <span class="kw">if</span> (jo.<span class="fn">has</span>(<span class="st">"text"</span>)) {');
    L.push('            <span class="cl">JsonObject</span> txt    = jo.<span class="fn">getAsJsonObject</span>(<span class="st">"text"</span>);');
    L.push('            <span class="cl">JsonObject</span> tcol   = txt.<span class="fn">getAsJsonObject</span>(<span class="st">"color"</span>);');
    L.push('            <span class="cl">JsonObject</span> offset = txt.<span class="fn">getAsJsonObject</span>(<span class="st">"offset"</span>);');
    L.push('            s.<span class="fn">setText</span>(');
    L.push('                txt.<span class="fn">get</span>(<span class="st">"content"</span>).<span class="fn">getAsString</span>(),');
    L.push('                txt.<span class="fn">get</span>(<span class="st">"fontSize"</span>).<span class="fn">getAsFloat</span>(),');
    L.push('                <span class="kw">new</span> <span class="cl">Vector3d</span>(tcol.<span class="fn">get</span>(<span class="st">"r"</span>).<span class="fn">getAsDouble</span>(), tcol.<span class="fn">get</span>(<span class="st">"g"</span>).<span class="fn">getAsDouble</span>(), tcol.<span class="fn">get</span>(<span class="st">"b"</span>).<span class="fn">getAsDouble</span>()),');
    L.push('                txt.<span class="fn">get</span>(<span class="st">"fontName"</span>).<span class="fn">getAsString</span>()');
    L.push('            );');
    L.push('            <span class="kw">double</span> ox = offset.<span class="fn">get</span>(<span class="st">"x"</span>).<span class="fn">getAsDouble</span>();');
    L.push('            <span class="kw">double</span> oy = offset.<span class="fn">get</span>(<span class="st">"y"</span>).<span class="fn">getAsDouble</span>();');
    L.push('            <span class="kw">if</span> (ox != <span class="nm">0</span> || oy != <span class="nm">0</span>) s.<span class="fn">setTextOffset</span>(ox, oy);');
    L.push('        }');
    L.push('');
    L.push('        model.<span class="fn">addShape</span>(s);');
    L.push('    }');
    L.push('    <span class="kw">return</span> model;');
    L.push('}');
    L.push('');
    L.push('<span class="cm">// Convenience: load from classpath file</span>');
    L.push('<span class="kw">public static</span> <span class="cl">ShapeModel</span> <span class="fn">fromJsonFile</span>(<span class="cl">String</span> path) <span class="kw">throws</span> <span class="cl">Exception</span> {');
    L.push('    <span class="kw">try</span> (<span class="cl">InputStream</span> is = <span class="cl">ShapeModel</span>.<span class="kw">class</span>.<span class="fn">getResourceAsStream</span>(path)) {');
    L.push('        <span class="kw">assert</span> is != <span class="kw">null</span> : <span class="st">"Resource not found: "</span> + path;');
    L.push('        <span class="kw">return</span> <span class="fn">fromJson</span>(<span class="kw">new</span> <span class="cl">String</span>(is.<span class="fn">readAllBytes</span>(), java.nio.charset.<span class="cl">StandardCharsets</span>.UTF_8));');
    L.push('    }');
    L.push('}');
    return L.join('\n');
}

/* ============================================================
   MODAL WIRING
   ============================================================ */
document.getElementById('btnExport').addEventListener('click', function () {
    document.getElementById('exportJava').innerHTML = genJava();
    document.getElementById('exportJson').innerHTML = syntaxJsonHtml(buildJsonObj());
    document.getElementById('exportLoader').innerHTML = genLoader();
    document.getElementById('exportModal').classList.add('open');
});

['btnModalClose', 'btnModalDone'].forEach(function (id) {
    document.getElementById(id).addEventListener('click', function () {
        document.getElementById('exportModal').classList.remove('open');
    });
});

document.getElementById('exportModal').addEventListener('click', function (e) {
    if (e.target === document.getElementById('exportModal'))
        document.getElementById('exportModal').classList.remove('open');
});

document.getElementById('btnCopy').addEventListener('click', function () {
    var active = document.querySelector('.export-pane.active .modal-code');
    if (!active) return;
    navigator.clipboard.writeText(active.innerText).then(function () {
        var b = document.getElementById('btnCopy');
        b.textContent = 'Copied!';
        setTimeout(function () {
            b.textContent = 'Copy to clipboard';
        }, 2200);
    });
});

/* ============================================================
   KEYBOARD SHORTCUTS
   ============================================================ */
window.addEventListener('keydown', function (e) {
    var tag = document.activeElement.tagName;
    var inInput = (tag === 'INPUT' || tag === 'SELECT' || tag === 'TEXTAREA');

    if (e.key === 'Escape') {
        placing = false;
        cv.style.cursor = 'default';
        setModeBadge('SELECT');
        draw();
    }

    if (!inInput && (e.key === 'Delete' || e.key === 'Backspace') && selected !== null) {
        removeShape(selected);
    }

    if (!inInput && e.key === 'd' && selected !== null) {
        duplicateShape(selected);
    }

    var arrowMap = {ArrowLeft: [-1, 0], ArrowRight: [1, 0], ArrowUp: [0, -1], ArrowDown: [0, 1]};
    if (!inInput && arrowMap[e.key] && selected !== null) {
        var step = e.shiftKey ? 10 : 1;
        shapes[selected].x += arrowMap[e.key][0] * step;
        shapes[selected].y += arrowMap[e.key][1] * step;
        refreshList();
        refreshProps();
        draw();
        e.preventDefault();
    }
});

/* ============================================================
   UTILITIES
   ============================================================ */
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

/* ============================================================
   INIT
   ============================================================ */
resize();