'use strict';

/* ============================================================
   SCHEMA VERSION — bump when JSON format changes
   Must match SCHEMA_VERSION in server.js
   ============================================================ */
const SCHEMA_VERSION = 2;

/* Migrations: transform older JSON data to current schema */
function applyMigrations(data) {
    let version = typeof data.version === 'number' ? data.version : 1;
    const log = [];

    if (version < 2) {
        log.push('v1→v2: added shape.name, text.offset, text.fontName');
        (data.shapes || []).forEach((s, i) => {
            if (!s.name) s.name = `${s.type || 'Shape'} ${i + 1}`;
            if (s.text) {
                if (!s.text.offset) s.text.offset = {x: 0, y: 0};
                if (s.text.fontName === undefined) s.text.fontName = '';
            }
        });
        version = 2;
    }

    // Future: if (version < 3) { ... version = 3; }

    data.version = version;
    return {data, log};
}

/* ============================================================
   CONSTANTS
   ============================================================ */
const TYPES = [
    'RECT', 'CIRCLE', 'TRIANGLE',
    'RECT_OUTLINE', 'CIRCLE_OUTLINE', 'TRIANGLE_OUTLINE'
];
const ICONS = {RECT: '[_]', CIRCLE: '()', TRIANGLE: '/\\'};

const OFFSET_PRESETS = [
    [-1, -1], [0, -1], [1, -1],
    [-1, 0], [0, 0], [1, 0],
    [-1, 1], [0, 1], [1, 1]
];

function baseType(t) {
    return t.replace('_OUTLINE', '');
}

function isOutline(t) {
    return t.includes('_OUTLINE');
}

/* ============================================================
   STATE
   ============================================================ */
let shapes = [];
let fonts = [];          // { name, file, url, status, source }
let workspaces = [];
let fontPaths = [];
let serverOnline = false;

let selectedSet = new Set();
let focused = null;
let lastClicked = null;

let uid = 1;
let placing = false;

let pan = {x: 0, y: 0};
let panning = false, panRef = {};

let dragging = false, dragOrigins = [], dragWorldRef = {};
let listDragSrc = null;

let mouseWorld = {x: 0, y: 0};

let currentFile = {wsId: null, filename: null};

/* ============================================================
   SHAPE FACTORY
   ============================================================ */
function mkShape(x, y) {
    return {
        id: uid++, name: `Shape ${uid}`, type: 'RECT',
        x: x || 0, y: y || 0, w: 80, h: 50,
        r: 1, g: 0, b: 0,
        text: '', fontSize: 32, textOffsetX: 0, textOffsetY: 0,
        textR: 0, textG: 0, textB: 0, fontName: ''
    };
}

/* ============================================================
   SELECTION
   ============================================================ */
function selectOnly(i) {
    selectedSet.clear();
    if (i !== null) selectedSet.add(i);
    focused = i;
    lastClicked = i;
}

function toggleSelect(i) {
    selectedSet.has(i) ? selectedSet.delete(i) : selectedSet.add(i);
    focused = selectedSet.size > 0 ? [...selectedSet].at(-1) : null;
    lastClicked = i;
}

function selectRange(from, to) {
    if (from === null) {
        selectOnly(to);
        return;
    }
    const lo = Math.min(from, to), hi = Math.max(from, to);
    for (let i = lo; i <= hi; i++) selectedSet.add(i);
    focused = to;
    lastClicked = to;
}

function clearSelection() {
    selectedSet.clear();
    focused = null;
    lastClicked = null;
}

/* ============================================================
   CANVAS
   ============================================================ */
const cv = document.getElementById('canvas');
const ctx = cv.getContext('2d');
const area = document.getElementById('canvasArea');

function resize() {
    cv.width = area.clientWidth;
    cv.height = area.clientHeight;
    draw();
}

window.addEventListener('resize', resize);

const w2s = (wx, wy) => ({x: cv.width / 2 + pan.x + wx, y: cv.height / 2 + pan.y + wy});
const s2w = (sx, sy) => ({x: sx - cv.width / 2 - pan.x, y: sy - cv.height / 2 - pan.y});

/* ============================================================
   DRAWING
   ============================================================ */
function draw() {
    const W = cv.width, H = cv.height;
    ctx.clearRect(0, 0, W, H);
    drawGrid(W, H);
    drawAxes(W, H);
    shapes.forEach((s, i) => drawShape(s, i === focused, selectedSet.has(i)));
    if (placing) drawGhost();
}

function drawGrid(W, H) {
    const step = 40;
    let ox = (pan.x + W / 2) % step, oy = (pan.y + H / 2) % step;
    if (ox < 0) ox += step;
    if (oy < 0) oy += step;
    ctx.strokeStyle = 'rgba(255,255,255,.026)';
    ctx.lineWidth = 1;
    for (let x = ox; x < W; x += step) {
        ctx.beginPath();
        ctx.moveTo(x, 0);
        ctx.lineTo(x, H);
        ctx.stroke();
    }
    for (let y = oy; y < H; y += step) {
        ctx.beginPath();
        ctx.moveTo(0, y);
        ctx.lineTo(W, y);
        ctx.stroke();
    }
}

function drawAxes(W, H) {
    const o = w2s(0, 0);
    ctx.setLineDash([4, 5]);
    ctx.lineWidth = 1;
    ctx.strokeStyle = 'rgba(232,255,71,.16)';
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
    const p = w2s(mouseWorld.x, mouseWorld.y);
    ctx.strokeStyle = 'rgba(232,255,71,.38)';
    ctx.lineWidth = 1;
    ctx.setLineDash([3, 4]);
    ctx.strokeRect(p.x - 40, p.y - 25, 80, 50);
    ctx.setLineDash([]);
}

const rgba01 = (r, g, b, a = 1) =>
    `rgba(${Math.round(r * 255)},${Math.round(g * 255)},${Math.round(b * 255)},${a})`;

function tracePath(s) {
    const bt = baseType(s.type);
    if (bt === 'RECT') ctx.rect(-s.w / 2, -s.h / 2, s.w, s.h);
    else if (bt === 'CIRCLE') ctx.arc(0, 0, s.w, 0, Math.PI * 2);
    else if (bt === 'TRIANGLE') {
        ctx.moveTo(0, -s.h / 2);
        ctx.lineTo(s.w / 2, s.h / 2);
        ctx.lineTo(-s.w / 2, s.h / 2);
        ctx.closePath();
    }
}

function drawShape(s, isFoc, isSel) {
    const p = w2s(s.x, s.y);
    const out = isOutline(s.type);
    const fill = rgba01(s.r, s.g, s.b);

    ctx.save();
    ctx.translate(p.x, p.y);
    if (isFoc) {
        ctx.shadowColor = '#e8ff47';
        ctx.shadowBlur = 14;
    } else if (isSel) {
        ctx.shadowColor = '#47c4ff';
        ctx.shadowBlur = 8;
    }

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

    if (isFoc || isSel) {
        ctx.shadowBlur = 0;
        ctx.beginPath();
        tracePath(s);
        ctx.strokeStyle = isFoc ? '#e8ff47' : '#47c4ff';
        ctx.lineWidth = isFoc ? 1.8 : 1.2;
        ctx.stroke();
    }

    if (s.text) {
        ctx.shadowBlur = 0;
        ctx.font = `bold ${s.fontSize}px ${resolvedFontFamily(s.fontName)}`;
        ctx.fillStyle = rgba01(s.textR, s.textG, s.textB);
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        const label = s.text.length > 22 ? s.text.slice(0, 21) + '…' : s.text;
        ctx.fillText(label, s.textOffsetX, s.textOffsetY);
        if (isFoc && (s.textOffsetX || s.textOffsetY)) {
            ctx.strokeStyle = 'rgba(232,255,71,.45)';
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

    const bt = baseType(s.type);
    const tagY = (bt === 'CIRCLE') ? p.y - s.w - 8 : p.y - s.h / 2 - 8;
    const tagX = (bt === 'CIRCLE') ? p.x - s.w : p.x - s.w / 2;
    ctx.font = '9px monospace';
    ctx.fillStyle = isFoc ? '#e8ff47' : isSel ? '#47c4ff' : '#3a3f50';
    ctx.textAlign = 'left';
    ctx.fillText(`${s.name || '#' + s.id} · ${s.type}`, tagX, tagY);
}

function resolvedFontFamily(fontName) {
    const f = fonts.find(f => f.name === fontName && f.status === 'ok');
    return f ? `"${fontName}"` : 'monospace';
}

function drawMini(mc, s, W, H) {
    mc.clearRect(0, 0, W, H);
    mc.strokeStyle = mc.fillStyle = rgba01(s.r, s.g, s.b);
    mc.lineWidth = 1.5;
    const m = 4, bt = baseType(s.type);
    mc.beginPath();
    if (bt === 'RECT') mc.rect(m, m, W - m * 2, H - m * 2);
    else if (bt === 'CIRCLE') mc.arc(W / 2, H / 2, W / 2 - m, 0, Math.PI * 2);
    else {
        mc.moveTo(W / 2, m);
        mc.lineTo(W - m, H - m);
        mc.lineTo(m, H - m);
        mc.closePath();
    }
    isOutline(s.type) ? mc.stroke() : mc.fill();
}

/* ============================================================
   CANVAS EVENTS
   ============================================================ */
const cvXY = e => {
    const r = cv.getBoundingClientRect();
    return {sx: e.clientX - r.left, sy: e.clientY - r.top};
};

cv.addEventListener('mousemove', e => {
    const xy = cvXY(e);
    mouseWorld = s2w(xy.sx, xy.sy);
    document.getElementById('coords').textContent = `${Math.round(mouseWorld.x)}, ${Math.round(mouseWorld.y)}`;
    if (panning) {
        pan.x = panRef.px + (e.clientX - panRef.mx);
        pan.y = panRef.py + (e.clientY - panRef.my);
        draw();
        return;
    }
    if (dragging && dragOrigins.length) {
        const dx = mouseWorld.x - dragWorldRef.x, dy = mouseWorld.y - dragWorldRef.y;
        dragOrigins.forEach(d => {
            shapes[d.idx].x = Math.round(d.ox + dx);
            shapes[d.idx].y = Math.round(d.oy + dy);
        });
        refreshList();
        refreshProps();
        draw();
        return;
    }
    if (placing) draw();
});

cv.addEventListener('mousedown', e => {
    const xy = cvXY(e), w = s2w(xy.sx, xy.sy);
    if (e.button === 1 || (e.button === 0 && e.altKey)) {
        e.preventDefault();
        panning = true;
        panRef = {px: pan.x, py: pan.y, mx: e.clientX, my: e.clientY};
        return;
    }
    if (e.button !== 0) return;
    if (placing) {
        const s = mkShape(Math.round(w.x), Math.round(w.y));
        shapes.push(s);
        selectOnly(shapes.length - 1);
        placing = false;
        cv.style.cursor = 'default';
        setModeBadge('SELECT');
        refreshList();
        refreshProps();
        draw();
        return;
    }
    const hit = hitTest(w.x, w.y);
    if (hit !== null) {
        if (e.shiftKey) selectRange(lastClicked, hit);
        else if (e.ctrlKey || e.metaKey) toggleSelect(hit);
        else {
            if (!selectedSet.has(hit)) selectOnly(hit); else focused = hit;
        }
        dragging = true;
        dragWorldRef = {x: w.x, y: w.y};
        dragOrigins = [...selectedSet].map(idx => ({idx, ox: shapes[idx].x, oy: shapes[idx].y}));
        refreshList();
        refreshProps();
        draw();
    } else {
        clearSelection();
        refreshList();
        refreshProps();
        draw();
    }
});

window.addEventListener('mouseup', () => {
    panning = false;
    dragging = false;
    dragOrigins = [];
});
cv.addEventListener('contextmenu', e => e.preventDefault());

function hitTest(wx, wy) {
    for (let i = shapes.length - 1; i >= 0; i--) {
        const s = shapes[i], bt = baseType(s.type);
        if (bt === 'CIRCLE') {
            if ((wx - s.x) ** 2 + (wy - s.y) ** 2 <= s.w ** 2) return i;
        } else {
            if (wx >= s.x - s.w / 2 && wx <= s.x + s.w / 2 && wy >= s.y - s.h / 2 && wy <= s.y + s.h / 2) return i;
        }
    }
    return null;
}

/* ============================================================
   RESIZABLE PANELS
   ============================================================ */
function initResizeHandles() {
    const layout = document.getElementById('workspaceLayout');
    const panelL = document.getElementById('panelLeft');
    const panelR = document.getElementById('panelRight');
    const handleL = document.getElementById('handleLeft');
    const handleR = document.getElementById('handleRight');

    function makeResizer(handle, panel, side) {
        let startX, startW;
        handle.addEventListener('mousedown', e => {
            if (e.button !== 0) return;
            startX = e.clientX;
            startW = panel.offsetWidth;
            handle.classList.add('resizing');
            document.body.style.cursor = 'col-resize';
            document.body.style.userSelect = 'none';

            const onMove = e => {
                const delta = side === 'left' ? e.clientX - startX : startX - e.clientX;
                const minW = parseInt(getComputedStyle(panel).minWidth) || 150;
                const maxW = layout.offsetWidth * 0.45;
                const newW = Math.max(minW, Math.min(maxW, startW + delta));
                panel.style.width = newW + 'px';
                resize();
            };
            const onUp = () => {
                handle.classList.remove('resizing');
                document.body.style.cursor = '';
                document.body.style.userSelect = '';
                window.removeEventListener('mousemove', onMove);
                window.removeEventListener('mouseup', onUp);
            };
            window.addEventListener('mousemove', onMove);
            window.addEventListener('mouseup', onUp);
            e.preventDefault();
        });
    }

    makeResizer(handleL, panelL, 'left');
    makeResizer(handleR, panelR, 'right');
}

/* ============================================================
   SHAPE LIST
   ============================================================ */
function refreshList() {
    const list = document.getElementById('shapeList');
    document.getElementById('shapeCount').textContent = shapes.length;
    refreshMultiToolbar();
    if (!shapes.length) {
        list.innerHTML = '<div class="empty-state">No shapes yet.<br>Click "+ Add Shape"<br>to begin.</div>';
        return;
    }
    list.innerHTML = '';
    shapes.forEach((s, idx) => {
        const isFoc = idx === focused, isSel = selectedSet.has(idx);
        const item = document.createElement('div');
        item.className = 'shape-item' + (isFoc ? ' focused' : isSel ? ' in-selection' : '');
        item.setAttribute('draggable', 'true');
        item.dataset.idx = idx;

        const handle = document.createElement('div');
        handle.className = 'drag-handle';
        handle.textContent = '≡';
        handle.title = 'Drag to reorder';

        const iconWrap = document.createElement('div');
        iconWrap.className = 'shape-icon';
        const ic = document.createElement('canvas');
        ic.width = ic.height = 26;
        drawMini(ic.getContext('2d'), s, 26, 26);
        iconWrap.appendChild(ic);

        const meta = document.createElement('div');
        meta.className = 'shape-meta';
        meta.innerHTML = `<div class="shape-name">${htmlesc(s.name || '#' + s.id)}</div>` +
            `<div class="shape-sub">${s.type} · (${s.x}, ${s.y})</div>`;

        const actions = document.createElement('div');
        actions.className = 'shape-actions';
        const btnUp = makeActBtn('↑', 'Move up');
        btnUp.onclick = e => {
            e.stopPropagation();
            moveShape(idx, -1);
        };
        const btnDn = makeActBtn('↓', 'Move down');
        btnDn.onclick = e => {
            e.stopPropagation();
            moveShape(idx, 1);
        };
        const btnDp = makeActBtn('⎘', 'Duplicate (D)');
        btnDp.onclick = e => {
            e.stopPropagation();
            duplicateShape(idx);
        };
        const btnDl = makeActBtn('×', 'Delete');
        btnDl.classList.add('del');
        btnDl.onclick = e => {
            e.stopPropagation();
            removeShapes([idx]);
        };
        actions.append(btnUp, btnDn, btnDp, btnDl);
        item.append(handle, iconWrap, meta, actions);

        item.onclick = e => {
            if (e.shiftKey) selectRange(lastClicked, idx);
            else if (e.ctrlKey || e.metaKey) toggleSelect(idx);
            else selectOnly(idx);
            refreshList();
            refreshProps();
            draw();
        };

        // Drag-to-reorder
        item.addEventListener('dragstart', e => {
            listDragSrc = idx;
            e.dataTransfer.effectAllowed = 'move';
            setTimeout(() => item.classList.add('reorder-drag'), 0);
        });
        item.addEventListener('dragend', () => {
            item.classList.remove('reorder-drag');
            listDragSrc = null;
            list.querySelectorAll('.shape-item').forEach(el => el.classList.remove('drag-over'));
        });
        item.addEventListener('dragover', e => {
            e.preventDefault();
            e.dataTransfer.dropEffect = 'move';
            if (listDragSrc !== null && listDragSrc !== idx) item.classList.add('drag-over');
        });
        item.addEventListener('dragleave', () => item.classList.remove('drag-over'));
        item.addEventListener('drop', e => {
            e.preventDefault();
            item.classList.remove('drag-over');
            if (listDragSrc === null || listDragSrc === idx) return;
            const moved = shapes.splice(listDragSrc, 1)[0];
            shapes.splice(idx, 0, moved);
            const newSel = new Set();
            selectedSet.forEach(i => newSel.add(reorderIdx(i, listDragSrc, idx)));
            selectedSet = newSel;
            focused = reorderIdx(focused, listDragSrc, idx);
            listDragSrc = null;
            refreshList();
            refreshProps();
            draw();
        });

        list.appendChild(item);
    });
}

function reorderIdx(i, from, to) {
    if (i === null) return null;
    if (i === from) return to;
    if (from < to) {
        if (i > from && i <= to) return i - 1;
    } else {
        if (i >= to && i < from) return i + 1;
    }
    return i;
}

function refreshMultiToolbar() {
    const bar = document.getElementById('multiToolbar');
    const cnt = selectedSet.size;
    if (cnt > 1) {
        bar.classList.add('visible');
        document.getElementById('multiLabel').textContent = `${cnt} shapes selected`;
    } else bar.classList.remove('visible');
}

const makeActBtn = (label, title) => {
    const b = document.createElement('button');
    b.className = 'act-btn';
    b.textContent = label;
    b.title = title;
    return b;
};

function moveShape(idx, dir) {
    const ni = idx + dir;
    if (ni < 0 || ni >= shapes.length) return;
    [shapes[idx], shapes[ni]] = [shapes[ni], shapes[idx]];
    if (focused === idx) focused = ni; else if (focused === ni) focused = idx;
    const ns = new Set();
    selectedSet.forEach(i => {
        ns.add(i === idx ? ni : i === ni ? idx : i);
    });
    selectedSet = ns;
    refreshList();
    refreshProps();
    draw();
}

/* ============================================================
   FONT MANAGER
   ============================================================ */
function refreshFontList() {
    document.getElementById('fontCount').textContent = fonts.length;
    const list = document.getElementById('fontList');
    list.innerHTML = '';
    fonts.forEach((f, idx) => {
        const item = document.createElement('div');
        item.className = 'font-item';
        const nm = document.createElement('div');
        nm.className = 'font-item-name';
        nm.innerHTML = `<div class="font-name-label">${htmlesc(f.name)}</div>` +
            `<div class="font-file-label">${htmlesc(f.source || f.file)}</div>`;
        const st = document.createElement('span');
        st.className = `font-status ${f.status}`;
        st.textContent = f.status === 'ok' ? 'loaded' : f.status === 'loading' ? '...' : 'error';
        const del = document.createElement('button');
        del.className = 'act-btn del';
        del.textContent = '×';
        del.onclick = () => {
            fonts.splice(idx, 1);
            refreshFontList();
            draw();
        };
        item.append(nm, st, del);
        list.appendChild(item);
    });
}

// Load a font by name + URL (generic, used by both manual and path-loaded)
function loadFont(name, url, sourceLabel) {
    if (fonts.find(f => f.name === name)) {
        alert(`Font "${name}" already registered.`);
        return false;
    }
    const entry = {name, file: url, url, source: sourceLabel || url, status: 'loading'};
    fonts.push(entry);
    refreshFontList();
    new FontFace(name, `url("${url}")`).load()
        .then(loaded => {
            document.fonts.add(loaded);
            entry.status = 'ok';
            refreshFontList();
            draw();
        })
        .catch(() => {
            entry.status = 'err';
            refreshFontList();
        });
    return true;
}

// Manual register
document.getElementById('btnAddFont').addEventListener('click', () => {
    const name = document.getElementById('newFontName').value.trim();
    const file = document.getElementById('newFontFile').value.trim();
    if (!name || !file) return;
    if (loadFont(name, file, file)) {
        document.getElementById('newFontName').value = '';
        document.getElementById('newFontFile').value = '';
    }
});

// Refresh discovered fonts from font paths
async function refreshDiscoveredFonts() {
    const container = document.getElementById('discoveredFonts');
    if (!serverOnline) {
        container.innerHTML = '';
        return;
    }
    container.innerHTML = '';
    for (const fp of fontPaths) {
        let files;
        try {
            files = await apiFetch(`/api/font-path/${fp.id}/files`);
        } catch (_) {
            continue;
        }
        if (!files.length) continue;

        const group = document.createElement('div');
        group.className = 'font-path-group';
        group.innerHTML = `<div class="font-path-name">${htmlesc(fp.name)}</div>`;
        files.forEach(f => {
            const row = document.createElement('div');
            row.className = 'font-discovered-item';
            const alreadyLoaded = fonts.some(lf => lf.source === `${fp.id}:${f.name}`);
            row.innerHTML = `<div class="font-disc-name">${htmlesc(f.name)}</div>` +
                `<div class="font-disc-size">${fmtSize(f.size)}</div>`;
            const btn = document.createElement('button');
            btn.className = `btn xs ${alreadyLoaded ? '' : 'secondary'}`;
            btn.textContent = alreadyLoaded ? 'loaded' : 'Load';
            btn.disabled = alreadyLoaded;
            btn.onclick = () => {
                const nameGuess = f.name.replace(/\.[^.]+$/, ''); // strip extension
                const url = `/api/font-path/${fp.id}/file/${encodeURIComponent(f.name)}`;
                if (loadFont(nameGuess, url, `${fp.id}:${f.name}`)) {
                    btn.textContent = 'loaded';
                    btn.disabled = true;
                }
            };
            row.appendChild(btn);
            group.appendChild(row);
        });
        container.appendChild(group);
    }
}

/* ============================================================
   PROPERTIES PANEL
   ============================================================ */
function refreshProps() {
    const panel = document.getElementById('props');
    if (selectedSet.size > 1) {
        panel.innerHTML =
            `<div class="multi-sel-info">
        <span class="multi-sel-count">${selectedSet.size}</span>
        <span class="multi-sel-label">shapes selected</span>
        <div class="multi-sel-actions">
          <button class="btn sm secondary" id="pDupAll">⎘ Duplicate all</button>
          <button class="btn sm danger-outline" id="pDelAll">× Delete all</button>
        </div>
      </div>`;
        document.getElementById('pDupAll').onclick = () => {
            [...selectedSet].sort((a, b) => b - a).forEach(i => duplicateShape(i));
        };
        document.getElementById('pDelAll').onclick = () => removeShapes([...selectedSet]);
        return;
    }
    if (focused === null || !shapes[focused]) {
        panel.innerHTML = '<div class="no-sel">Select a shape<br>to edit its properties.</div>';
        return;
    }
    const s = shapes[focused], bt = baseType(s.type), isCirc = (bt === 'CIRCLE');

    const typeBtns = TYPES.map(t => {
        const b = baseType(t), icon = ICONS[b] || b;
        const lbl = isOutline(t) ? b.slice(0, 3) + '_OL' : b.slice(0, 5);
        return `<button class="type-btn${s.type === t ? ' sel' : ''}" data-t="${t}">${icon}<br>${lbl}</button>`;
    }).join('');

    const hField = isCirc ? '' :
        `<div class="prop-field"><span class="prop-field-label">H</span>` +
        `<input type="number" id="ph" value="${s.h}" step="1" min="1"></div>`;

    let fontOpts = `<option value=""${s.fontName === '' ? ' selected' : ''}>monospace (default)</option>`;
    fonts.forEach(f => {
        fontOpts += `<option value="${htmlesc(f.name)}"${s.fontName === f.name ? ' selected' : ''}>${htmlesc(f.name)}${f.status !== 'ok' ? ' (' + f.status + ')' : ''}</option>`;
    });

    const halfW = isCirc ? s.w : s.w / 2, halfH = isCirc ? s.w : s.h / 2;
    const presetBtns = OFFSET_PRESETS.map(p => {
        const ox = Math.round(p[0] * halfW), oy = Math.round(p[1] * halfH);
        const sel = (s.textOffsetX === ox && s.textOffsetY === oy);
        return `<button class="op-btn${sel ? ' sel' : ''}" data-ox="${ox}" data-oy="${oy}" title="(${ox}, ${oy})"></button>`;
    }).join('');

    panel.innerHTML =
        /* Name */
        `<div class="prop-group">
      <span class="prop-label">Name</span>
      <div class="prop-field"><input type="text" id="sname" value="${htmlesc(s.name || '')}" placeholder="Shape name..."></div>
    </div><hr class="divider">` +
        /* Type */
        `<div class="prop-group">
      <span class="prop-label">Shape Type</span>
      <div class="type-grid">${typeBtns}</div>
    </div><hr class="divider">` +
        /* Position */
        `<div class="prop-group">
      <span class="prop-label">Position</span>
      <div class="prop-row">
        <div class="prop-field"><span class="prop-field-label">X</span><input type="number" id="px" value="${s.x}" step="1"></div>
        <div class="prop-field"><span class="prop-field-label">Y</span><input type="number" id="py" value="${s.y}" step="1"></div>
      </div>
    </div>` +
        /* Dimensions */
        `<div class="prop-group">
      <span class="prop-label">${isCirc ? 'Radius' : 'Dimensions'}</span>
      <div class="prop-row">
        <div class="prop-field"><span class="prop-field-label">${isCirc ? 'R' : 'W'}</span><input type="number" id="pw" value="${s.w}" step="1" min="1"></div>
        ${hField}
      </div>
    </div>` +
        /* Fill */
        `<div class="prop-group">
      <span class="prop-label">Fill color (RGB 0–1)</span>
      <div class="color-row">
        <input type="color" id="cpick" value="${r1hex(s.r, s.g, s.b)}">
        <input type="text"  id="chex"  value="${r1hex(s.r, s.g, s.b)}" maxlength="7">
      </div>
      <div class="prop-row">
        <div class="prop-field"><span class="prop-field-label">R</span><input type="number" id="cr" value="${s.r.toFixed(4)}" step="0.001" min="0" max="1"></div>
        <div class="prop-field"><span class="prop-field-label">G</span><input type="number" id="cg" value="${s.g.toFixed(4)}" step="0.001" min="0" max="1"></div>
        <div class="prop-field"><span class="prop-field-label">B</span><input type="number" id="cb" value="${s.b.toFixed(4)}" step="0.001" min="0" max="1"></div>
      </div>
    </div><hr class="divider">` +
        /* Text */
        `<div class="prop-group">
      <span class="prop-label">Text (optional)</span>
      <div class="prop-field" style="margin-bottom:5px">
        <span class="prop-field-label">Content</span>
        <input type="text" id="txt" value="${htmlesc(s.text)}" placeholder="Label...">
      </div>
      <div class="prop-row">
        <div class="prop-field"><span class="prop-field-label">Font size</span><input type="number" id="tfs" value="${s.fontSize}" step="1" min="1"></div>
        <div class="prop-field"><span class="prop-field-label">Font</span><select id="tfn">${fontOpts}</select></div>
      </div>
      <div class="prop-field" style="margin-bottom:8px">
        <span class="prop-field-label">Text color</span>
        <div class="color-row">
          <input type="color" id="tcpick" value="${r1hex(s.textR, s.textG, s.textB)}">
          <input type="text"  id="tchex"  value="${r1hex(s.textR, s.textG, s.textB)}" maxlength="7">
        </div>
      </div>
      <span class="prop-label" style="margin-bottom:3px">Text offset preset</span>
      <div class="offset-presets">${presetBtns}</div>
      <div class="prop-row">
        <div class="prop-field"><span class="prop-field-label">Offset X</span><input type="number" id="tox" value="${s.textOffsetX}" step="1"></div>
        <div class="prop-field"><span class="prop-field-label">Offset Y</span><input type="number" id="toy" value="${s.textOffsetY}" step="1"></div>
      </div>
    </div>`;

    // Wire type buttons
    panel.querySelectorAll('.type-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            s.type = btn.dataset.t;
            refreshList();
            refreshProps();
            draw();
        });
    });

    // Generic number binder
    const bindNum = (id, key) => {
        const el = panel.querySelector('#' + id);
        if (!el) return;
        el.addEventListener('input', () => {
            const v = parseFloat(el.value);
            if (!isNaN(v)) s[key] = v;
            refreshList();
            draw();
        });
    };
    ['px', 'x'], ['py', 'y'], ['pw', 'w'], ['ph', 'h'], ['cr', 'r'], ['cg', 'g'], ['cb', 'b'],
        ['tfs', 'fontSize'], ['tox', 'textOffsetX'], ['toy', 'textOffsetY']
    ; // assignment trick — use forEach instead:
    [['px', 'x'], ['py', 'y'], ['pw', 'w'], ['ph', 'h'], ['cr', 'r'], ['cg', 'g'], ['cb', 'b'],
        ['tfs', 'fontSize'], ['tox', 'textOffsetX'], ['toy', 'textOffsetY']
    ].forEach(([id, key]) => bindNum(id, key));

    panel.querySelector('#sname').addEventListener('input', e => {
        s.name = e.target.value;
        refreshList();
    });

    // Fill color sync
    const cpick = panel.querySelector('#cpick'), chex = panel.querySelector('#chex');
    const crI = panel.querySelector('#cr'), cgI = panel.querySelector('#cg'), cbI = panel.querySelector('#cb');
    const applyFillHex = hex => {
        const rgb = hex2r1(hex);
        if (!rgb) return;
        s.r = rgb.r;
        s.g = rgb.g;
        s.b = rgb.b;
        crI.value = s.r.toFixed(4);
        cgI.value = s.g.toFixed(4);
        cbI.value = s.b.toFixed(4);
        draw();
        refreshList();
    };
    cpick.addEventListener('input', () => {
        chex.value = cpick.value;
        applyFillHex(cpick.value);
    });
    chex.addEventListener('input', () => {
        if (/^#[0-9a-fA-F]{6}$/.test(chex.value)) {
            cpick.value = chex.value;
            applyFillHex(chex.value);
        }
    });
    [crI, cgI, cbI].forEach(inp => {
        if (!inp) return;
        inp.addEventListener('input', () => {
            s.r = cl01(parseFloat(crI.value));
            s.g = cl01(parseFloat(cgI.value));
            s.b = cl01(parseFloat(cbI.value));
            const h = r1hex(s.r, s.g, s.b);
            cpick.value = h;
            chex.value = h;
            draw();
            refreshList();
        });
    });

    panel.querySelector('#txt').addEventListener('input', e => {
        s.text = e.target.value;
        draw();
    });
    panel.querySelector('#tfn').addEventListener('change', e => {
        s.fontName = e.target.value;
        draw();
    });

    const tcpick = panel.querySelector('#tcpick'), tchex = panel.querySelector('#tchex');
    tcpick.addEventListener('input', () => {
        tchex.value = tcpick.value;
        const rgb = hex2r1(tcpick.value);
        if (!rgb) return;
        s.textR = rgb.r;
        s.textG = rgb.g;
        s.textB = rgb.b;
        draw();
    });
    tchex.addEventListener('input', () => {
        if (/^#[0-9a-fA-F]{6}$/.test(tchex.value)) {
            tcpick.value = tchex.value;
            const rgb = hex2r1(tchex.value);
            if (!rgb) return;
            s.textR = rgb.r;
            s.textG = rgb.g;
            s.textB = rgb.b;
            draw();
        }
    });

    const oxEl = panel.querySelector('#tox'), oyEl = panel.querySelector('#toy');
    panel.querySelectorAll('.op-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            s.textOffsetX = parseInt(btn.dataset.ox);
            s.textOffsetY = parseInt(btn.dataset.oy);
            oxEl.value = s.textOffsetX;
            oyEl.value = s.textOffsetY;
            panel.querySelectorAll('.op-btn').forEach(b => b.classList.remove('sel'));
            btn.classList.add('sel');
            draw();
        });
    });
}

/* ============================================================
   ACTIONS
   ============================================================ */
function removeShapes(indices) {
    indices.sort((a, b) => b - a).forEach(i => shapes.splice(i, 1));
    const newSel = new Set();
    selectedSet.forEach(i => {
        const shift = indices.filter(r => r < i).length;
        const ni = i - shift;
        if (!indices.includes(i) && ni >= 0 && ni < shapes.length) newSel.add(ni);
    });
    selectedSet = newSel;
    focused = newSel.size > 0 ? [...newSel].at(-1) : null;
    if (focused !== null && focused >= shapes.length) focused = shapes.length - 1;
    refreshList();
    refreshProps();
    draw();
}

function duplicateShape(i) {
    const copy = JSON.parse(JSON.stringify(shapes[i]));
    copy.id = uid++;
    copy.name = shapes[i].name + ' copy';
    copy.x += 20;
    copy.y += 20;
    shapes.splice(i + 1, 0, copy);
    selectOnly(i + 1);
    refreshList();
    refreshProps();
    draw();
}

document.getElementById('btnAdd').addEventListener('click', () => {
    placing = true;
    cv.style.cursor = 'crosshair';
    setModeBadge('PLACE');
    clearSelection();
    refreshList();
    refreshProps();
    draw();
});

document.getElementById('btnClear').addEventListener('click', () => {
    if (!shapes.length) return;
    if (!confirm('Clear all shapes?')) return;
    shapes = [];
    clearSelection();
    uid = 1;
    placing = false;
    currentFile = {wsId: null, filename: null};
    cv.style.cursor = 'default';
    setModeBadge('SELECT');
    updateTitle();
    refreshList();
    refreshProps();
    draw();
});

document.getElementById('multiBtnDup').addEventListener('click', () => {
    [...selectedSet].sort((a, b) => b - a).forEach(i => duplicateShape(i));
});
document.getElementById('multiBtnDel').addEventListener('click', () => removeShapes([...selectedSet]));

const setModeBadge = txt => document.getElementById('modeBadge').textContent = txt;

/* ============================================================
   TABS
   ============================================================ */
document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
        document.querySelectorAll('.tab-pane').forEach(p => p.classList.remove('active'));
        btn.classList.add('active');
        document.getElementById('tab-' + btn.dataset.tab).classList.add('active');
        if (btn.dataset.tab === 'files') refreshWorkspaceTab();
        if (btn.dataset.tab === 'fonts') refreshDiscoveredFonts();
    });
});
document.querySelectorAll('.export-tab').forEach(btn => {
    btn.addEventListener('click', () => {
        document.querySelectorAll('.export-tab').forEach(b => b.classList.remove('active'));
        document.querySelectorAll('.export-pane').forEach(p => p.classList.remove('active'));
        btn.classList.add('active');
        document.getElementById('epane-' + btn.dataset.etab).classList.add('active');
    });
});

/* ============================================================
   SERVER / API
   ============================================================ */
const apiFetch = (url, opts) => fetch(url, opts).then(async r => {
    if (!r.ok) {
        const e = await r.json().catch(() => ({error: r.status}));
        throw new Error(e.error || r.status);
    }
    return r.json();
});

async function initServer() {
    try {
        workspaces = await apiFetch('/api/workspaces');
        fontPaths = await apiFetch('/api/font-paths');
        serverOnline = true;
        document.getElementById('tabBtnFiles').style.display = '';
        populateSaveWsSelect();
        checkOutdatedFiles();   // show upgrade banner if needed
    } catch (_) {
        serverOnline = false;
        document.getElementById('tabBtnFiles').style.display = 'none';
    }
}

function populateSaveWsSelect() {
    ['saveWsSelect', 'saveWsSelectModal'].forEach(id => {
        const sel = document.getElementById(id);
        if (!sel) return;
        sel.innerHTML = '';
        workspaces.forEach(ws => {
            const opt = document.createElement('option');
            opt.value = ws.id;
            opt.textContent = ws.name;
            sel.appendChild(opt);
        });
        if (currentFile.wsId) sel.value = currentFile.wsId;
    });
}

/* ── Workspace file browser ───────────────────────────────── */
async function refreshWorkspaceTab() {
    const container = document.getElementById('wsFileList');
    if (!serverOnline) {
        container.innerHTML =
            `<div class="ws-offline-notice">
        <strong>Server not running.</strong><br>
        Run <code>npm install &amp;&amp; npm start</code> to enable workspace features.
      </div>`;
        return;
    }
    container.innerHTML = '<div style="color:var(--text-dim);font-size:11px;padding:8px">Loading…</div>';
    const results = await Promise.all(workspaces.map(ws =>
        apiFetch(`/api/workspace/${ws.id}/files`).then(files => ({ws, files})).catch(() => ({
            ws,
            files: [],
            error: true
        }))
    ));
    container.innerHTML = '';
    results.forEach(({ws, files, error}) => {
        const group = document.createElement('div');
        group.className = 'ws-group';
        group.innerHTML = `<div class="ws-group-name">${htmlesc(ws.name)}</div><div class="ws-group-path">${htmlesc(ws.path)}</div>`;
        const filesDiv = document.createElement('div');
        filesDiv.className = 'ws-files';
        if (error) {
            filesDiv.innerHTML = '<div class="ws-no-files" style="color:var(--danger)">Error reading directory</div>';
        } else if (!files.length) {
            filesDiv.innerHTML = '<div class="ws-no-files">No .json files yet</div>';
        } else {
            files.forEach(f => {
                const isOpen = (currentFile.wsId === ws.id && currentFile.filename === f.name);
                const vNum = f.version || 1;
                const vCurrent = (vNum >= SCHEMA_VERSION);
                const vBadge = `<span class="version-badge ${f.version ? vCurrent ? 'current' : 'outdated' : 'unknown'}">v${vNum}</span>`;
                const row = document.createElement('div');
                row.className = 'ws-file';
                row.innerHTML =
                    `<div class="ws-file-name" title="${htmlesc(f.name)}">${isOpen ? '▶ ' : ''}${htmlesc(f.name)}</div>` +
                    `<div class="ws-file-right">${vBadge}<div class="ws-file-date">${fmtDate(f.modified)}</div></div>`;
                const acts = document.createElement('div');
                acts.className = 'ws-file-actions';
                const btnOpen = document.createElement('button');
                btnOpen.className = 'btn xs secondary';
                btnOpen.textContent = 'Open';
                btnOpen.onclick = () => loadFromWorkspace(ws.id, f.name);
                const btnDel = document.createElement('button');
                btnDel.className = 'btn xs ghost-danger';
                btnDel.textContent = 'Del';
                btnDel.onclick = () => {
                    if (!confirm(`Delete "${f.name}"?`)) return;
                    deleteFromWorkspace(ws.id, f.name);
                };
                acts.append(btnOpen, btnDel);
                row.appendChild(acts);
                filesDiv.appendChild(row);
            });
        }
        group.appendChild(filesDiv);
        container.appendChild(group);
    });
}

async function loadFromWorkspace(wsId, filename) {
    const data = await apiFetch(`/api/workspace/${wsId}/file/${encodeURIComponent(filename)}`);
    loadJson(data);
    currentFile = {wsId, filename};
    updateTitle();
    refreshWorkspaceTab();
    switchTab('shapes');
}

async function deleteFromWorkspace(wsId, filename) {
    await apiFetch(`/api/workspace/${wsId}/file/${encodeURIComponent(filename)}`, {method: 'DELETE'});
    if (currentFile.wsId === wsId && currentFile.filename === filename) {
        currentFile = {wsId: null, filename: null};
        updateTitle();
    }
    refreshWorkspaceTab();
}

/* ── Upgrade banner ──────────────────────────────────────── */
async function checkOutdatedFiles() {
    if (!serverOnline) return;
    try {
        const res = await apiFetch('/api/workspaces/outdated');
        const n = res.outdated.length;
        const banner = document.getElementById('upgradeBanner');
        const label = document.getElementById('upgradeBannerLabel');
        if (n > 0) {
            label.textContent = `${n} file${n > 1 ? 's' : ''} below schema v${res.schemaVersion} found across workspaces.`;
            banner.style.display = 'flex';
        } else {
            banner.style.display = 'none';
        }
    } catch (_) {
    }
}

document.getElementById('btnUpgradeAll').addEventListener('click', async () => {
    if (!confirm('Migrate all outdated workspace files in-place?')) return;
    const res = await apiFetch('/api/workspaces/upgrade-all', {method: 'POST'});
    let msg = `Migrated ${res.upgraded} file(s).\n\n`;
    res.results.forEach(r => {
        msg += `• ${r.wsId}/${r.filename}: v${r.fromVersion}→${SCHEMA_VERSION}\n` + (r.log ? r.log.map(l => '  ' + l).join('\n') + '\n' : '');
    });
    alert(msg);
    checkOutdatedFiles();
    refreshWorkspaceTab();
});

/* ── Save ────────────────────────────────────────────────── */
document.getElementById('btnSave').addEventListener('click', () => {
    if (!serverOnline) {
        alert('Server not running. Use "Export ↗" to get the JSON.');
        return;
    }
    if (currentFile.wsId && currentFile.filename) doSave(currentFile.wsId, currentFile.filename);
    else openSaveModal();
});
document.getElementById('btnSaveAs').addEventListener('click', () => {
    if (!serverOnline) {
        alert('Server not running. Use "Export ↗" to get the JSON.');
        return;
    }
    openSaveModal();
});

function openSaveModal() {
    populateSaveWsSelect();
    document.getElementById('saveFilenameModal').value = currentFile.filename ? currentFile.filename.replace('.json', '') : 'model';
    document.getElementById('saveFeedback').textContent = '';
    document.getElementById('saveModal').classList.add('open');
}

document.getElementById('btnSaveConfirm').addEventListener('click', () => {
    const wsId = document.getElementById('saveWsSelectModal').value;
    const name = document.getElementById('saveFilenameModal').value.trim();
    if (!name) {
        document.getElementById('saveFeedback').textContent = 'Enter a filename.';
        return;
    }
    doSave(wsId, name.endsWith('.json') ? name : name + '.json');
});

async function doSave(wsId, filename) {
    const payload = buildJsonObj();
    await apiFetch(`/api/workspace/${wsId}/file/${encodeURIComponent(filename)}`, {
        method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(payload)
    });
    currentFile = {wsId, filename};
    updateTitle();
    document.getElementById('saveModal').classList.remove('open');
    const btn = document.getElementById('btnSave');
    const orig = btn.textContent;
    btn.textContent = '✓ Saved';
    btn.classList.add('primary');
    setTimeout(() => {
        btn.textContent = orig;
        btn.classList.remove('primary');
    }, 2000);
    refreshWorkspaceTab();
    checkOutdatedFiles();
}

['btnSaveModalClose', 'btnSaveCancel'].forEach(id =>
    document.getElementById(id).addEventListener('click', () => document.getElementById('saveModal').classList.remove('open'))
);

/* ── Import ──────────────────────────────────────────────── */
document.getElementById('btnImportJson').addEventListener('click', () => {
    document.getElementById('importTextarea').value = '';
    document.getElementById('importError').textContent = '';
    document.getElementById('importModal').classList.add('open');
});
document.getElementById('btnImportConfirm').addEventListener('click', () => {
    const raw = document.getElementById('importTextarea').value.trim();
    const errEl = document.getElementById('importError');
    if (!raw) {
        errEl.textContent = 'Paste some JSON first.';
        return;
    }
    try {
        loadJson(JSON.parse(raw));
        currentFile = {wsId: null, filename: null};
        updateTitle();
        document.getElementById('importModal').classList.remove('open');
    } catch (e) {
        errEl.textContent = 'Invalid JSON: ' + e.message;
    }
});
['btnImportModalClose', 'btnImportCancel'].forEach(id =>
    document.getElementById(id).addEventListener('click', () => document.getElementById('importModal').classList.remove('open'))
);

/* ── Load JSON data ────────────────────────────────────────── */
function loadJson(raw) {
    if (!raw || !Array.isArray(raw.shapes)) {
        alert('Invalid ShapeModel JSON.');
        return;
    }
    // Run client-side migrations
    const {data, log} = applyMigrations(raw);
    if (log.length) console.log('[ShapeModel] Migrations applied:', log);

    shapes = data.shapes.map(s => ({
        id: uid++,
        name: s.name || (s.type ? `${s.type} ${uid}` : `Shape ${uid}`),
        type: s.type || 'RECT',
        x: s.position?.x ?? 0, y: s.position?.y ?? 0,
        w: s.dimensions?.x ?? 80, h: s.dimensions?.y ?? 50,
        r: s.color?.r ?? 1, g: s.color?.g ?? 0, b: s.color?.b ?? 0,
        text: s.text?.content ?? '',
        fontSize: s.text?.fontSize ?? 32,
        textOffsetX: s.text?.offset?.x ?? 0,
        textOffsetY: s.text?.offset?.y ?? 0,
        textR: s.text?.color?.r ?? 0,
        textG: s.text?.color?.g ?? 0,
        textB: s.text?.color?.b ?? 0,
        fontName: s.text?.fontName ?? ''
    }));
    clearSelection();
    refreshList();
    refreshProps();
    draw();
}

function updateTitle() {
    document.title = currentFile.filename ? `${currentFile.filename} — ShapeModel Editor` : 'ShapeModel Editor';
    const badge = document.getElementById('currentFileBadge');
    badge.textContent = currentFile.filename || '';
    badge.style.display = currentFile.filename ? '' : 'none';
}

/* ============================================================
   EXPORT
   ============================================================ */
document.getElementById('btnExport').addEventListener('click', () => {
    document.getElementById('exportJava').innerHTML = genJava();
    document.getElementById('exportJson').innerHTML = syntaxJsonHtml(buildJsonObj());
    document.getElementById('exportLoader').innerHTML = genLoader();
    document.getElementById('exportModal').classList.add('open');
});
['btnModalClose', 'btnModalDone'].forEach(id =>
    document.getElementById(id).addEventListener('click', () => document.getElementById('exportModal').classList.remove('open'))
);
document.getElementById('exportModal').addEventListener('click', e => {
    if (e.target === document.getElementById('exportModal')) document.getElementById('exportModal').classList.remove('open');
});
document.getElementById('btnCopy').addEventListener('click', () => {
    const active = document.querySelector('.export-pane.active .modal-code');
    if (!active) return;
    navigator.clipboard.writeText(active.innerText).then(() => {
        const b = document.getElementById('btnCopy');
        b.textContent = 'Copied!';
        setTimeout(() => b.textContent = 'Copy to clipboard', 2200);
    });
});

function buildJsonObj() {
    return {
        version: SCHEMA_VERSION,
        shapes: shapes.map(s => {
            const bt = baseType(s.type);
            const obj = {
                name: s.name || null,
                type: s.type,
                position: {x: s.x, y: s.y},
                dimensions: bt === 'CIRCLE' ? {x: s.w, y: s.w} : {x: s.w, y: s.h},
                color: {r: rf(s.r), g: rf(s.g), b: rf(s.b)}
            };
            if (s.text) obj.text = {
                content: s.text,
                fontSize: s.fontSize,
                color: {r: rf(s.textR), g: rf(s.textG), b: rf(s.textB)},
                offset: {x: s.textOffsetX, y: s.textOffsetY},
                fontName: s.fontName
            };
            return obj;
        })
    };
}

const rf = v => parseFloat(v.toFixed(4));

function syntaxJsonHtml(obj) {
    return JSON.stringify(obj, null, 2)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"([^"]+)"(\s*:)/g, '<span class="jk">"$1"</span>$2')
        .replace(/: "([^"]*)"/g, ': <span class="js">"$1"</span>')
        .replace(/: (null|true|false)/g, ': <span class="kw">$1</span>')
        .replace(/: (-?\d+(\.\d+)?)/g, (_, n) => `: <span class="jv">${n}</span>`);
}

function genJava() {
    if (!shapes.length) return '<span class="cm">// No shapes to export.</span>';
    const L = [
        '<span class="cm">// Generated by ShapeModel Editor</span>',
        '<span class="kw">public</span> <span class="cl">ShapeModel</span> <span class="fn">buildShapeModel</span>() {',
        '    <span class="cl">ShapeModel</span> model = <span class="kw">new</span> <span class="cl">ShapeModel</span>();',
        '    <span class="cl">Shape</span> s;', ''
    ];
    shapes.forEach((s, i) => {
        const bt = baseType(s.type);
        const dim = bt === 'CIRCLE'
            ? `<span class="kw">new</span> <span class="cl">Vector2d</span>(<span class="nm">${s.w}</span>, <span class="nm">${s.w}</span>)`
            : `<span class="kw">new</span> <span class="cl">Vector2d</span>(<span class="nm">${s.w}</span>, <span class="nm">${s.h}</span>)`;
        L.push(`    <span class="cm">// ${s.name || 'Shape #' + s.id} · ${s.type}</span>`);
        L.push(`    s = <span class="kw">new</span> <span class="cl">Shape</span>(`);
        L.push(`        <span class="kw">new</span> <span class="cl">Vector2d</span>(<span class="nm">${s.x}</span>, <span class="nm">${s.y}</span>),`);
        L.push(`        ${dim},`);
        L.push(`        <span class="cl">Shape</span>.<span class="cl">ShapeType</span>.${s.type}`);
        L.push(`    );`);
        L.push(`    s.setColor(<span class="nm">${s.r.toFixed(3)}f</span>, <span class="nm">${s.g.toFixed(3)}f</span>, <span class="nm">${s.b.toFixed(3)}f</span>);`);
        if (s.name) L.push(`    s.setName(<span class="st">"${jesc(s.name)}"</span>);`);
        if (s.text) {
            L.push(`    s.setText(<span class="st">"${jesc(s.text)}"</span>, <span class="nm">${s.fontSize}f</span>,`);
            L.push(`        <span class="kw">new</span> <span class="cl">Vector3d</span>(<span class="nm">${s.textR.toFixed(3)}f</span>, <span class="nm">${s.textG.toFixed(3)}f</span>, <span class="nm">${s.textB.toFixed(3)}f</span>),`);
            L.push(`        <span class="st">"${jesc(s.fontName)}"</span>);`);
        }
        if (s.textOffsetX || s.textOffsetY) L.push(`    s.setTextOffset(<span class="nm">${s.textOffsetX}</span>, <span class="nm">${s.textOffsetY}</span>);`);
        L.push(`    model.addShape(s);`);
        if (i < shapes.length - 1) L.push('');
    });
    L.push('', '    <span class="kw">return</span> model;', '}');
    return L.join('\n');
}

function genLoader() {
    return [
        '<span class="cm">// Add to ShapeModel.java — requires com.google.code.gson:gson:2.10.1</span>', '',
        '<span class="kw">public static</span> <span class="cl">ShapeModel</span> <span class="fn">fromJson</span>(<span class="cl">String</span> json) {',
        '    <span class="cl">ShapeModel</span> model = <span class="kw">new</span> <span class="cl">ShapeModel</span>();',
        '    <span class="cl">JsonObject</span> root = <span class="cl">JsonParser</span>.<span class="fn">parseString</span>(json).<span class="fn">getAsJsonObject</span>();',
        '    <span class="cm">// int version = root.has("version") ? root.get("version").getAsInt() : 1;</span>',
        '    <span class="cl">JsonArray</span> arr = root.<span class="fn">getAsJsonArray</span>(<span class="st">"shapes"</span>);',
        '    <span class="kw">for</span> (<span class="cl">JsonElement</span> el : arr) {',
        '        <span class="cl">JsonObject</span> jo=el.<span class="fn">getAsJsonObject</span>();',
        '        <span class="cl">Shape</span> s = <span class="kw">new</span> <span class="cl">Shape</span>(',
        '            vec2(jo,<span class="st">"position"</span>), vec2(jo,<span class="st">"dimensions"</span>),',
        '            <span class="cl">Shape.ShapeType</span>.<span class="fn">valueOf</span>(jo.<span class="fn">get</span>(<span class="st">"type"</span>).<span class="fn">getAsString</span>()));',
        '        <span class="cl">JsonObject</span> col=jo.<span class="fn">getAsJsonObject</span>(<span class="st">"color"</span>);',
        '        s.<span class="fn">setColor</span>(col.<span class="fn">get</span>(<span class="st">"r"</span>).<span class="fn">getAsFloat</span>(),col.<span class="fn">get</span>(<span class="st">"g"</span>).<span class="fn">getAsFloat</span>(),col.<span class="fn">get</span>(<span class="st">"b"</span>).<span class="fn">getAsFloat</span>());',
        '        <span class="kw">if</span>(jo.<span class="fn">has</span>(<span class="st">"name"</span>)&&!jo.<span class="fn">get</span>(<span class="st">"name"</span>).<span class="fn">isJsonNull</span>()) s.<span class="fn">setName</span>(jo.<span class="fn">get</span>(<span class="st">"name"</span>).<span class="fn">getAsString</span>());',
        '        <span class="kw">if</span>(jo.<span class="fn">has</span>(<span class="st">"text"</span>)){',
        '            <span class="cl">JsonObject</span> t=jo.<span class="fn">getAsJsonObject</span>(<span class="st">"text"</span>);',
        '            <span class="cl">JsonObject</span> tc=t.<span class="fn">getAsJsonObject</span>(<span class="st">"color"</span>);',
        '            s.<span class="fn">setText</span>(t.<span class="fn">get</span>(<span class="st">"content"</span>).<span class="fn">getAsString</span>(),t.<span class="fn">get</span>(<span class="st">"fontSize"</span>).<span class="fn">getAsFloat</span>(),',
        '                <span class="kw">new</span> <span class="cl">Vector3d</span>(tc.<span class="fn">get</span>(<span class="st">"r"</span>).<span class="fn">getAsDouble</span>(),tc.<span class="fn">get</span>(<span class="st">"g"</span>).<span class="fn">getAsDouble</span>(),tc.<span class="fn">get</span>(<span class="st">"b"</span>).<span class="fn">getAsDouble</span>()),',
        '                t.<span class="fn">get</span>(<span class="st">"fontName"</span>).<span class="fn">getAsString</span>());',
        '            <span class="cl">JsonObject</span> off=t.<span class="fn">getAsJsonObject</span>(<span class="st">"offset"</span>);',
        '            <span class="kw">double</span> ox=off.<span class="fn">get</span>(<span class="st">"x"</span>).<span class="fn">getAsDouble</span>(),oy=off.<span class="fn">get</span>(<span class="st">"y"</span>).<span class="fn">getAsDouble</span>();',
        '            <span class="kw">if</span>(ox!=0||oy!=0) s.<span class="fn">setTextOffset</span>(ox,oy);',
        '        }',
        '        model.<span class="fn">addShape</span>(s);',
        '    }',
        '    <span class="kw">return</span> model;',
        '}', '',
        '<span class="kw">private static</span> <span class="cl">Vector2d</span> <span class="fn">vec2</span>(<span class="cl">JsonObject</span> jo, <span class="cl">String</span> key){',
        '    <span class="cl">JsonObject</span> o=jo.<span class="fn">getAsJsonObject</span>(key);',
        '    <span class="kw">return</span> <span class="kw">new</span> <span class="cl">Vector2d</span>(o.<span class="fn">get</span>(<span class="st">"x"</span>).<span class="fn">getAsDouble</span>(),o.<span class="fn">get</span>(<span class="st">"y"</span>).<span class="fn">getAsDouble</span>());',
        '}', '',
        '<span class="kw">public static</span> <span class="cl">ShapeModel</span> <span class="fn">fromJsonFile</span>(<span class="cl">String</span> path) <span class="kw">throws</span> <span class="cl">Exception</span> {',
        '    <span class="kw">try</span>(<span class="cl">InputStream</span> is=<span class="cl">ShapeModel</span>.<span class="kw">class</span>.<span class="fn">getResourceAsStream</span>(path)){',
        '        <span class="kw">assert</span> is!=<span class="kw">null</span>;',
        '        <span class="kw">return</span> <span class="fn">fromJson</span>(<span class="kw">new</span> <span class="cl">String</span>(is.<span class="fn">readAllBytes</span>(),<span class="cl">StandardCharsets</span>.UTF_8));',
        '    }',
        '}', '',
        '<span class="kw">public</span> <span class="cl">Shape</span> <span class="fn">getShapeByName</span>(<span class="cl">String</span> name){',
        '    <span class="kw">for</span>(<span class="cl">Shape</span> s:shapes) <span class="kw">if</span>(name.<span class="fn">equals</span>(s.name)) <span class="kw">return</span> s;',
        '    <span class="kw">return</span> <span class="kw">null</span>;',
        '}'
    ].join('\n');
}

/* ============================================================
   KEYBOARD
   ============================================================ */
window.addEventListener('keydown', e => {
    const inInput = ['INPUT', 'SELECT', 'TEXTAREA'].includes(document.activeElement.tagName);
    if (e.key === 'Escape') {
        placing = false;
        cv.style.cursor = 'default';
        setModeBadge('SELECT');
        draw();
    }
    if (!inInput && (e.key === 'Delete' || e.key === 'Backspace') && selectedSet.size) removeShapes([...selectedSet]);
    if (!inInput && e.key === 'd' && focused !== null) duplicateShape(focused);
    if ((e.ctrlKey || e.metaKey) && e.key === 'a' && !inInput) {
        e.preventDefault();
        shapes.forEach((_, i) => selectedSet.add(i));
        focused = shapes.length - 1;
        refreshList();
        refreshProps();
        draw();
    }
    if ((e.ctrlKey || e.metaKey) && e.key === 's' && !inInput) {
        e.preventDefault();
        document.getElementById('btnSave').click();
    }
    const arrow = {ArrowLeft: [-1, 0], ArrowRight: [1, 0], ArrowUp: [0, -1], ArrowDown: [0, 1]}[e.key];
    if (!inInput && arrow && selectedSet.size) {
        const step = e.shiftKey ? 10 : 1;
        selectedSet.forEach(i => {
            shapes[i].x += arrow[0] * step;
            shapes[i].y += arrow[1] * step;
        });
        refreshList();
        refreshProps();
        draw();
        e.preventDefault();
    }
});

/* ============================================================
   UTILITIES
   ============================================================ */
const r1hex = (r, g, b) => '#' + [r, g, b].map(v => Math.round(v * 255).toString(16).padStart(2, '0')).join('');

function hex2r1(hex) {
    const m = hex.match(/^#?([0-9a-fA-F]{6})$/);
    if (!m) return null;
    const n = parseInt(m[1], 16);
    return {r: ((n >> 16) & 255) / 255, g: ((n >> 8) & 255) / 255, b: (n & 255) / 255};
}

const cl01 = v => Math.max(0, Math.min(1, isNaN(v) ? 0 : v));
const jesc = s => s.replace(/\\/g, '\\\\').replace(/"/g, '\\"');
const htmlesc = s => (s || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
const fmtDate = ms => {
    const d = new Date(ms);
    return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'});
};
const fmtSize = b => b > 1024 * 1024 ? `${(b / 1024 / 1024).toFixed(1)} MB` : b > 1024 ? `${(b / 1024).toFixed(0)} KB` : `${b} B`;

function switchTab(tab) {
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.tab-pane').forEach(p => p.classList.remove('active'));
    document.querySelector(`[data-tab="${tab}"]`)?.classList.add('active');
    document.getElementById('tab-' + tab)?.classList.add('active');
}

/* ============================================================
   INIT
   ============================================================ */
initResizeHandles();
resize();
initServer();
updateTitle();