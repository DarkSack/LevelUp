// generate_daily_challenges
// Idempotent per (user_id, date). Uses admin key to insert on behalf of users.
import { createClient } from 'npm:@insforge/sdk';

const CORS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization',
};
const JSON_H = { ...CORS, 'Content-Type': 'application/json' };
const PER_DAY = 3;

function hash32(s: string) {
  let h = 2166136261 >>> 0;
  for (let i = 0; i < s.length; i++) { h ^= s.charCodeAt(i); h = Math.imul(h, 16777619); }
  return h >>> 0;
}
function mulberry32(seed: number) {
  let a = seed >>> 0;
  return () => {
    a = (a + 0x6d2b79f5) >>> 0;
    let t = a;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}
function pickN<T>(rng: () => number, arr: T[], n: number): T[] {
  const c = [...arr]; const o: T[] = [];
  const take = Math.min(n, c.length);
  for (let i = 0; i < take; i++) o.push(c.splice(Math.floor(rng() * c.length), 1)[0]);
  return o;
}

function render(t: any, level: number) {
  const base = t.params?.base;
  const scale = t.params?.scale_per_level ?? 0;
  const target = base != null ? base + scale * (level - 1) : null;
  const desc = target != null ? String(t.description).replace('{target}', String(target)) : t.description;
  return {
    title: t.title, description: desc, target_value: target,
    xp_reward: Math.round(t.xp_reward * (1 + (level - 1) * 0.05)),
  };
}

export default async function (req: Request): Promise<Response> {
  if (req.method === 'OPTIONS') return new Response(null, { status: 204, headers: CORS });

  const admin = createClient({
    baseUrl: Deno.env.get('INSFORGE_BASE_URL')!,
    anonKey: Deno.env.get('API_KEY')!,
  });

  const body = req.method === 'POST' ? await req.json().catch(() => ({})) : {};
  const authUserId = await getUserId(req);
  const targetUserId = body.user_id ?? authUserId;
  if (!targetUserId) return json({ error: 'user_id required' }, 400);

  const date = body.date ?? new Date().toISOString().slice(0, 10);

  const { data: profile, error: pErr } = await admin.database
    .from('profiles').select('id, categories').eq('id', targetUserId).single();
  if (pErr || !profile) return json({ error: 'profile not found' }, 404);

  const { data: stats } = await admin.database
    .from('profile_stats').select('user_id, level').eq('user_id', targetUserId).single();
  const level = (stats as { level?: number } | null)?.level ?? 1;

  const { data: templates } = await admin.database
    .from('challenge_templates').select('*').eq('active', true);

  const cats = (profile.categories?.length ? profile.categories : ['physical', 'mental', 'organization']) as string[];
  const rng = mulberry32(hash32(`${targetUserId}:${date}`));
  const eligible = (templates ?? []).filter((t: any) => cats.includes(t.category) && t.min_level <= level);
  if (!eligible.length) return json({ inserted: 0, reason: 'no eligible templates' });

  const byCat = new Map<string, any[]>();
  for (const t of eligible) {
    const list = byCat.get(t.category) ?? [];
    list.push(t); byCat.set(t.category, list);
  }
  const chosen: any[] = [];
  const catOrder = pickN(rng, [...byCat.keys()], byCat.size);
  for (const c of catOrder) {
    if (chosen.length >= PER_DAY) break;
    const [pick] = pickN(rng, byCat.get(c)!, 1);
    if (pick) chosen.push(pick);
  }
  while (chosen.length < PER_DAY) {
    const rest = eligible.filter((t: any) => !chosen.includes(t));
    if (!rest.length) break;
    const [pick] = pickN(rng, rest, 1);
    chosen.push(pick);
  }

  const rows = chosen.map((t: any) => {
    const r = render(t, level);
    return {
      user_id: targetUserId, template_id: t.id, assigned_date: date,
      title: r.title, description: r.description,
      xp_reward: r.xp_reward, target_value: r.target_value, status: 'pending',
    };
  });

  const { data: existing } = await admin.database
    .from('user_challenges').select('template_id')
    .eq('user_id', targetUserId).eq('assigned_date', date);
  const already = new Set((existing ?? []).map((r: any) => r.template_id));
  const fresh = rows.filter((r) => !already.has(r.template_id));
  if (fresh.length === 0) return json({ inserted: 0, already: rows.length });

  const { error: insErr } = await admin.database.from('user_challenges').insert(fresh);
  if (insErr) return json({ error: insErr.message }, 500);
  return json({ inserted: fresh.length, date });
}

async function getUserId(req: Request): Promise<string | null> {
  const auth = req.headers.get('Authorization');
  if (!auth) return null;
  const token = auth.replace('Bearer ', '');
  const client = createClient({
    baseUrl: Deno.env.get('INSFORGE_BASE_URL')!,
    accessToken: token,
  });
  const { data } = await client.auth.getCurrentUser();
  return data?.user?.id ?? null;
}

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: JSON_H });
}
