// complete_challenge
// - Idempotent via client_id.
// - Grants XP by appending to xp_events. Recomputes level from log.
// - Updates streak in user timezone.
// - Unlocks first-tier achievements.
import { createClient } from 'npm:@insforge/sdk';

const CORS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization',
};
const JSON_H = { ...CORS, 'Content-Type': 'application/json' };

interface Body { user_challenge_id: string; client_id: string; progress_value?: number; }

function levelFromXp(xp: number) {
  let level = 1;
  while (50 * (level + 1) * (level + 1) + 50 * (level + 1) <= xp) level++;
  return { level, xpInLevel: xp - (50 * level * level + 50 * level) };
}
function localDate(tz: string): string {
  try {
    return new Intl.DateTimeFormat('en-CA', {
      timeZone: tz, year: 'numeric', month: '2-digit', day: '2-digit',
    }).format(new Date());
  } catch { return new Date().toISOString().slice(0, 10); }
}
function daysBetween(a: string, b: string) {
  return Math.round((+new Date(b + 'T00:00:00Z') - +new Date(a + 'T00:00:00Z')) / 86400000);
}

export default async function (req: Request): Promise<Response> {
  if (req.method === 'OPTIONS') return new Response(null, { status: 204, headers: CORS });
  if (req.method !== 'POST') return json({ error: 'POST only' }, 405);

  const authHeader = req.headers.get('Authorization');
  if (!authHeader) return json({ error: 'missing auth' }, 401);
  const userToken = authHeader.replace('Bearer ', '');

  const userClient = createClient({
    baseUrl: Deno.env.get('INSFORGE_BASE_URL')!,
    accessToken: userToken,
  });
  const { data: userData } = await userClient.auth.getCurrentUser();
  const userId = userData?.user?.id;
  if (!userId) return json({ error: 'invalid token' }, 401);

  const body = await req.json().catch(() => null) as Body | null;
  if (!body?.user_challenge_id || !body?.client_id) {
    return json({ error: 'user_challenge_id and client_id required' }, 400);
  }

  const admin = createClient({
    baseUrl: Deno.env.get('INSFORGE_BASE_URL')!,
    anonKey: Deno.env.get('API_KEY')!,
  });

  // Idempotency: same client_id already completed?
  const { data: existingByClient } = await admin.database.from('user_challenges')
    .select('*').eq('client_id', body.client_id).limit(1);
  const dupe = (existingByClient as any[] | null)?.[0];
  if (dupe && dupe.status === 'completed') {
    return json({ ok: true, already: true, user_challenge: dupe });
  }

  const { data: ucData } = await admin.database.from('user_challenges')
    .select('*').eq('id', body.user_challenge_id).limit(1);
  const uc = (ucData as any[] | null)?.[0];
  if (!uc) return json({ error: 'challenge not found' }, 404);
  if (uc.user_id !== userId) return json({ error: 'forbidden' }, 403);
  if (uc.status === 'completed') return json({ ok: true, already: true, user_challenge: uc });

  const now = new Date().toISOString();
  await admin.database.from('user_challenges').update({
    status: 'completed',
    completed_at: now,
    progress_value: body.progress_value ?? uc.target_value ?? 0,
    client_id: body.client_id,
  }).eq('id', uc.id);

  await admin.database.from('xp_events').insert([{
    user_id: userId, amount: uc.xp_reward, reason: 'challenge', user_challenge_id: uc.id,
  }]);

  const { data: xpAgg } = await admin.database.from('xp_events')
    .select('amount').eq('user_id', userId);
  const xpTotal = (xpAgg ?? []).reduce((s: number, r: any) => s + r.amount, 0);
  const { level, xpInLevel } = levelFromXp(xpTotal);

  const { data: profData } = await admin.database.from('profiles')
    .select('timezone').eq('id', userId).limit(1);
  const timezone = (profData as any[] | null)?.[0]?.timezone ?? 'UTC';
  const today = localDate(timezone);

  const { data: statsData } = await admin.database.from('profile_stats')
    .select('*').eq('user_id', userId).limit(1);
  const stats = (statsData as any[] | null)?.[0] ?? {};

  let streakDays = stats.streak_days ?? 0;
  const lastDay = stats.streak_last_day as string | null;
  if (lastDay !== today) {
    const gap = lastDay ? daysBetween(lastDay, today) : null;
    if (gap === 1) streakDays += 1;
    else if (gap !== 0) streakDays = 1;
  }
  const completedCount = (stats.challenges_completed ?? 0) + 1;

  await admin.database.from('profile_stats').update({
    xp_total: xpTotal, xp_current_level: xpInLevel, level,
    streak_days: streakDays, streak_last_day: today,
    challenges_completed: completedCount, updated_at: now,
  }).eq('user_id', userId);

  interface Unlock { slug: string; title: string; description: string; emoji: string; tier: number; }
  const unlocks: Unlock[] = [];
  const tryUnlock = async (slug: string) => {
    const { data: aData } = await admin.database.from('achievements')
      .select('id, slug, title, description, emoji, tier, xp_reward').eq('slug', slug).limit(1);
    const a = (aData as any[] | null)?.[0];
    if (!a) return;
    const { data: prev } = await admin.database.from('user_achievements')
      .select('achievement_id').eq('user_id', userId).eq('achievement_id', a.id).limit(1);
    if ((prev as any[] | null)?.length) return;
    await admin.database.from('user_achievements').insert([{
      user_id: userId, achievement_id: a.id,
    }]);
    if (a.xp_reward > 0) {
      await admin.database.from('xp_events').insert([{
        user_id: userId, amount: a.xp_reward, reason: 'achievement', achievement_id: a.id,
      }]);
    }
    unlocks.push({
      slug: a.slug, title: a.title, description: a.description,
      emoji: a.emoji ?? '🏅', tier: a.tier,
    });
  };

  if (completedCount === 1) await tryUnlock('first_step');
  if (streakDays >= 7) await tryUnlock('streak_7');
  if (streakDays >= 30) await tryUnlock('streak_30');
  if (level >= 10) await tryUnlock('level_10');

  return json({
    ok: true, xp_gained: uc.xp_reward, xp_total: xpTotal,
    level, streak_days: streakDays, unlocks,
  });
}

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: JSON_H });
}
