import type { GQLContext } from '../context';
import { requireUser } from '../context';

function shape(row: Record<string, unknown>) {
  return {
    id: row.id,
    templateId: row.template_id,
    assignedDate: row.assigned_date,
    title: row.title,
    description: row.description,
    xpReward: row.xp_reward,
    targetValue: row.target_value,
    progressValue: row.progress_value,
    status: row.status,
    completedAt: row.completed_at,
    category: row.category ?? null,
  };
}

/**
 * Forward the request to the existing InsForge edge function. Keeps the
 * server-authoritative logic in one place (the function already exists and
 * uses the admin key correctly).
 */
async function invokeFn(ctx: GQLContext, slug: string, body: unknown) {
  const baseUrl = process.env.INSFORGE_BASE_URL!;
  const url = `${baseUrl}/api/functions/${slug}`;
  const res = await fetch(url, {
    method: 'POST',
    headers: {
      'content-type': 'application/json',
      ...(ctx.accessToken ? { authorization: `Bearer ${ctx.accessToken}` } : {}),
    },
    body: JSON.stringify(body ?? {}),
  });
  if (!res.ok) throw new Error(`${slug} failed: ${res.status} ${await res.text()}`);
  return res.json();
}

export const challengeResolvers = {
  Query: {
    todayChallenges: async (_: unknown, __: unknown, ctx: GQLContext) => {
      const userId = requireUser(ctx);
      const today = new Date().toISOString().slice(0, 10);
      const { data, error } = await ctx.admin.database
        .from('user_challenges')
        .select('*, challenge_templates(category)')
        .eq('user_id', userId)
        .eq('assigned_date', today)
        .order('xp_reward', { ascending: true });
      if (error) throw new Error(error.message);
      return (data ?? []).map((row: any) => shape({
        ...row,
        category: row.challenge_templates?.category ?? null,
      }));
    },
  },

  Mutation: {
    generateDailyChallenges: async (_: unknown, __: unknown, ctx: GQLContext) => {
      const userId = requireUser(ctx);
      const res = await invokeFn(ctx, 'generate_daily_challenges', { user_id: userId });
      return res.inserted ?? 0;
    },

    completeChallenge: async (
      _: unknown,
      args: { userChallengeId: string; clientId: string; progressValue?: number },
      ctx: GQLContext,
    ) => {
      requireUser(ctx);
      const res = await invokeFn(ctx, 'complete_challenge', {
        user_challenge_id: args.userChallengeId,
        client_id: args.clientId,
        progress_value: args.progressValue,
      });
      return {
        ok: res.ok ?? false,
        already: res.already ?? false,
        xpGained: res.xp_gained ?? null,
        xpTotal: res.xp_total ?? null,
        level: res.level ?? null,
        streakDays: res.streak_days ?? null,
        unlocks: (res.unlocks ?? []).map((u: any) => ({
          slug: u.slug, title: u.title, description: u.description,
          emoji: u.emoji, tier: u.tier,
        })),
      };
    },
  },
};
