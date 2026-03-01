'use strict';

const express = require('express');
const fs = require('fs');
const path = require('path');
const cors = require('cors');

/* ============================================================
   SCHEMA VERSION
   Bump this when the JSON format changes.
   The matching constant lives in shapemodel-editor.js.
   ============================================================ */
const SCHEMA_VERSION = 2;

/* ============================================================
   WORKSPACES — configure your ShapeModel JSON directories here
   ============================================================ */
const WORKSPACES = [
    {
        id: 'common',
        name: 'Common',
        path: path.join(__dirname, '../../common/src/main/resources/assets/shapemodels'),
    },
    // { id: 'ui',   name: 'UI Models', path: path.join(__dirname, '../assets/ui') },
    // { id: 'npcs', name: 'NPCs',      path: path.join(__dirname, '../assets/npcs') },
];

/* ============================================================
   FONT PATHS — directories the editor auto-discovers fonts from
   ============================================================ */
const FONT_PATHS = [
    {
        id: 'game-fonts',
        name: 'Game Fonts',
        path: path.join(__dirname, '../assets/fonts'),
    },
    // { id: 'ui-fonts', name: 'UI Fonts', path: path.join(__dirname, '../assets/ui/fonts') },
];

const FONT_EXTENSIONS = new Set(['.ttf', '.otf', '.woff', '.woff2']);

/* ============================================================
   MIGRATIONS (server-side, mirrors client logic)
   ============================================================ */
function applyMigrations(data) {
    let version = typeof data.version === 'number' ? data.version : 1;
    const log = [];

    if (version < 2) {
        log.push('v1→v2: added shape.name, text.offset, text.fontName');
        (data.shapes || []).forEach((s, i) => {
            if (!s.name) s.name = s.type ? `${s.type} ${i + 1}` : `Shape ${i + 1}`;
            if (s.text) {
                if (!s.text.offset) s.text.offset = {x: 0, y: 0};
                if (s.text.fontName === undefined) s.text.fontName = '';
            }
        });
        version = 2;
    }

    // Future migrations go here:
    // if (version < 3) { ... version = 3; }

    data.version = version;
    return {data, log};
}

/* ============================================================
   HELPERS
   ============================================================ */
const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json({limit: '20mb'}));
app.use(express.static(__dirname));

function getWorkspace(id) {
    return WORKSPACES.find(w => w.id === id) || null;
}

function getFontPath(id) {
    return FONT_PATHS.find(f => f.id === id) || null;
}

function safeResolve(base, filename) {
    const resolved = path.resolve(base, filename);
    const baseAbs = path.resolve(base);
    return resolved.startsWith(baseAbs + path.sep) || resolved === baseAbs
        ? resolved : null;
}

function ensureDir(p) {
    if (!fs.existsSync(p)) fs.mkdirSync(p, {recursive: true});
}

function readJsonFile(filePath) {
    return JSON.parse(fs.readFileSync(filePath, 'utf8'));
}

function writeJsonFile(filePath, obj) {
    fs.writeFileSync(filePath, JSON.stringify(obj, null, 2), 'utf8');
}

/* ============================================================
   API — MISC
   ============================================================ */

// GET /api/version  → current schema version
app.get('/api/version', (req, res) => {
    res.json({schemaVersion: SCHEMA_VERSION});
});

/* ============================================================
   API — WORKSPACES
   ============================================================ */

// GET /api/workspaces
app.get('/api/workspaces', (req, res) => {
    res.json(WORKSPACES.map(w => ({id: w.id, name: w.name, path: w.path})));
});

// GET /api/workspace/:id/files
app.get('/api/workspace/:id/files', (req, res) => {
    const ws = getWorkspace(req.params.id);
    if (!ws) return res.status(404).json({error: 'Workspace not found'});
    ensureDir(ws.path);
    try {
        const files = fs.readdirSync(ws.path)
            .filter(f => f.endsWith('.json'))
            .map(f => {
                const fp = path.join(ws.path, f);
                const stat = fs.statSync(fp);
                let version = null;
                try {
                    version = readJsonFile(fp).version || 1;
                } catch (_) {
                    version = null;
                }
                return {name: f, modified: stat.mtimeMs, version};
            })
            .sort((a, b) => b.modified - a.modified);
        res.json(files);
    } catch (err) {
        res.status(500).json({error: err.message});
    }
});

// GET /api/workspace/:id/file/:filename
app.get('/api/workspace/:id/file/:filename', (req, res) => {
    const ws = getWorkspace(req.params.id);
    if (!ws) return res.status(404).json({error: 'Workspace not found'});
    const fp = safeResolve(ws.path, req.params.filename);
    if (!fp) return res.status(400).json({error: 'Invalid filename'});
    try {
        res.json(readJsonFile(fp));
    } catch (_) {
        res.status(404).json({error: 'File not found'});
    }
});

// POST /api/workspace/:id/file/:filename  (create / overwrite)
app.post('/api/workspace/:id/file/:filename', (req, res) => {
    const ws = getWorkspace(req.params.id);
    if (!ws) return res.status(404).json({error: 'Workspace not found'});
    let filename = req.params.filename;
    if (!filename.endsWith('.json')) filename += '.json';
    const fp = safeResolve(ws.path, filename);
    if (!fp) return res.status(400).json({error: 'Invalid filename'});
    ensureDir(ws.path);
    try {
        writeJsonFile(fp, req.body);
        res.json({ok: true, filename});
    } catch (err) {
        res.status(500).json({error: err.message});
    }
});

// DELETE /api/workspace/:id/file/:filename
app.delete('/api/workspace/:id/file/:filename', (req, res) => {
    const ws = getWorkspace(req.params.id);
    if (!ws) return res.status(404).json({error: 'Workspace not found'});
    const fp = safeResolve(ws.path, req.params.filename);
    if (!fp) return res.status(400).json({error: 'Invalid filename'});
    try {
        fs.unlinkSync(fp);
        res.json({ok: true});
    } catch (err) {
        res.status(500).json({error: err.message});
    }
});

/* ============================================================
   API — VERSIONING  (scan & batch-upgrade workspace files)
   ============================================================ */

// GET /api/workspaces/outdated
// Returns all JSON files across all workspaces that are below SCHEMA_VERSION.
app.get('/api/workspaces/outdated', (req, res) => {
    const outdated = [];
    WORKSPACES.forEach(ws => {
        ensureDir(ws.path);
        try {
            fs.readdirSync(ws.path)
                .filter(f => f.endsWith('.json'))
                .forEach(f => {
                    try {
                        const data = readJsonFile(path.join(ws.path, f));
                        const version = typeof data.version === 'number' ? data.version : 1;
                        if (version < SCHEMA_VERSION) {
                            outdated.push({wsId: ws.id, wsName: ws.name, filename: f, version});
                        }
                    } catch (_) { /* skip unparseable */
                    }
                });
        } catch (_) { /* skip unreadable dir */
        }
    });
    res.json({schemaVersion: SCHEMA_VERSION, outdated});
});

// POST /api/workspaces/upgrade-all
// Migrates every outdated file in-place and returns a summary.
app.post('/api/workspaces/upgrade-all', (req, res) => {
    const results = [];
    WORKSPACES.forEach(ws => {
        ensureDir(ws.path);
        try {
            fs.readdirSync(ws.path)
                .filter(f => f.endsWith('.json'))
                .forEach(f => {
                    const fp = path.join(ws.path, f);
                    try {
                        const data = readJsonFile(fp);
                        const version = typeof data.version === 'number' ? data.version : 1;
                        if (version < SCHEMA_VERSION) {
                            const {data: migrated, log} = applyMigrations(data);
                            writeJsonFile(fp, migrated);
                            results.push({wsId: ws.id, filename: f, fromVersion: version, log});
                        }
                    } catch (err) {
                        results.push({wsId: ws.id, filename: f, error: err.message});
                    }
                });
        } catch (_) { /* skip */
        }
    });
    res.json({upgraded: results.length, results});
});

/* ============================================================
   API — FONT PATHS
   ============================================================ */

// GET /api/font-paths  → list of configured font path groups
app.get('/api/font-paths', (req, res) => {
    res.json(FONT_PATHS.map(f => ({id: f.id, name: f.name, path: f.path})));
});

// GET /api/font-path/:id/files  → list of font files in a group
app.get('/api/font-path/:id/files', (req, res) => {
    const fp = getFontPath(req.params.id);
    if (!fp) return res.status(404).json({error: 'Font path not found'});
    ensureDir(fp.path);
    try {
        const files = fs.readdirSync(fp.path)
            .filter(f => FONT_EXTENSIONS.has(path.extname(f).toLowerCase()))
            .map(f => {
                const stat = fs.statSync(path.join(fp.path, f));
                return {name: f, size: stat.size};
            })
            .sort((a, b) => a.name.localeCompare(b.name));
        res.json(files);
    } catch (err) {
        res.status(500).json({error: err.message});
    }
});

// GET /api/font-path/:id/file/:filename  → serve the font binary
app.get('/api/font-path/:id/file/:filename', (req, res) => {
    const fp = getFontPath(req.params.id);
    if (!fp) return res.status(404).json({error: 'Font path not found'});
    const filePath = safeResolve(fp.path, req.params.filename);
    if (!filePath) return res.status(400).json({error: 'Invalid filename'});
    if (!fs.existsSync(filePath)) return res.status(404).json({error: 'File not found'});
    const ext = path.extname(filePath).toLowerCase();
    const mimeMap = {'.ttf': 'font/ttf', '.otf': 'font/otf', '.woff': 'font/woff', '.woff2': 'font/woff2'};
    res.setHeader('Content-Type', mimeMap[ext] || 'application/octet-stream');
    res.sendFile(filePath);
});

/* ============================================================
   START
   ============================================================ */
app.listen(PORT, () => {
    console.log(`\n  ShapeModel Editor  →  http://localhost:${PORT}\n`);
    console.log('  Workspaces:');
    WORKSPACES.forEach(w => console.log(`    [${w.id}]  ${w.name}  →  ${w.path}`));
    console.log('\n  Font paths:');
    FONT_PATHS.forEach(f => console.log(`    [${f.id}]  ${f.name}  →  ${f.path}`));
    console.log('');
});