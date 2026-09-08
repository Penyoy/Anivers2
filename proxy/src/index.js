import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;
const UPSTREAM = (process.env.UPSTREAM_API_URL || 'https://apps.animekita.org/api/v1.2.5').replace(/\/$/, '');
const ALT_UPSTREAM = (process.env.ALT_UPSTREAM_API_URL || 'https://users.animekita.org/api/v1.2.5').replace(/\/$/, '');
const UPSTREAM_KEY = process.env.UPSTREAM_API_KEY || '';

const UA_DART = 'Dart/3.9 (dart:io)';
const UA_FLUTTER = 'Flutter/2.5.3';

function uaFor(path) {
  return path.includes('/series/episode/data.php') ? UA_FLUTTER : UA_DART;
}

const ALLOWED = new Set([
  '/baruupload.php',
  '/movie.php',
  '/rekomendasi.php',
  '/jadwal.php',
  '/search.php',
  '/series.php',
  '/genreseries.php',
]);
function isAllowed(p) {
  if (ALLOWED.has(p)) return true;
  if (p.startsWith('/home/ongoing.php')) return true;
  if (p.startsWith('/series/episode/data.php')) return true;
  return false;
}

app.use(cors({ origin: '*', methods: ['GET','POST','OPTIONS'], allowedHeaders: ['Content-Type','Accept','Authorization'] }));
app.use(express.text({ type: '*/*', limit: '1mb' }));
app.use(express.json({ limit: '1mb' }));

app.get('/health', (req,res)=> res.json({ status:'ok', upstream: UPSTREAM, time:new Date().toISOString() }) );
app.get('/api/health', (req,res)=> res.json({ status:'ok', upstream: UPSTREAM, time:new Date().toISOString() }) );

app.all('/api/*', async (req,res)=>{
  const targetPath = req.path.replace(/^\/api/, '') || '/';
  if (!isAllowed(targetPath)) return res.status(404).json({ error:'Endpoint tidak dikenal', target: targetPath });
  const query = new URLSearchParams(req.query).toString();
  const queryStr = query ? `?${query}` : '';
  // choose base
  const bases = targetPath.startsWith('/genreseries.php') ? [UPSTREAM, ALT_UPSTREAM] : [UPSTREAM];
  const headers = {
    'accept': 'application/json',
    'user-agent': uaFor(targetPath),
  };
  if (UPSTREAM_KEY) headers['x-api-key'] = UPSTREAM_KEY;
  // body handling: POST jadwal.php expects empty, others expect text/plain json
  let body;
  if (req.method === 'POST') {
    if (req.body && typeof req.body === 'object' && !(req.body instanceof Buffer)) body = JSON.stringify(req.body);
    else if (typeof req.body === 'string' && req.body.length>0) body = req.body;
    if (body) {
      const ct = req.headers['content-type'] || '';
      headers['content-type'] = ct.includes('application/json') ? 'application/json' : 'text/plain; charset=utf-8';
    }
  }
  for (const base of bases) {
    const url = `${base}${targetPath}${queryStr}`;
    try {
      const r = await fetch(url, { method: req.method, headers, body: req.method==='POST' && body ? body : undefined });
      const text = await r.text();
      // detect cloudflare challenge
      const head = text.slice(0,500).toLowerCase();
      const isChallenge = head.includes('just a moment') || head.includes('cf-chl') || (head.includes('<!doctype') && head.includes('cloudflare'));
      if (isChallenge) continue;
      res.status(r.status);
      const ct = r.headers.get('content-type') || '';
      if (ct.includes('application/json') || text.trim().startsWith('{') || text.trim().startsWith('[')) res.setHeader('content-type','application/json');
      else if (ct) res.setHeader('content-type', ct);
      const cfRay = r.headers.get('cf-ray');
      if (cfRay) res.setHeader('x-cf-ray', cfRay);
      return res.send(text);
    } catch(e){ console.error('[proxy]', targetPath, base, e.message); }
  }
  res.status(502).json({ error:'Upstream fetch failed', target: targetPath });
});

app.use((req,res)=> res.status(404).json({ error:'Use /api/<path>.php' }) );

app.listen(PORT, ()=> console.log(`[proxy] listening on :${PORT} → upstream ${UPSTREAM}`));
