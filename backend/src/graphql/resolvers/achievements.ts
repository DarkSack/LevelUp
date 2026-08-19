import type { GQLContext } from '../context';

export const achievementResolvers = {
  Query: {
    achievements: async (_: unknown, __: unknown, ctx: GQLContext) => {
      const [{ data: all }, unlockedRes] = await Promise.all([
        ctx.admin.database.from('achievements').select('*').order('tier', { ascending: true }),
        ctx.userId
          ? ctx.admin.database.from('user_achievements')
              .select('achievement_id, unlocked_at').eq('user_id', ctx.userId)
          : Promise.resolve({ data: [] as { achievement_id: string; unlocked_at: string }[] }),
      ]);
      const unlockedMap = new Map(
        (unlockedRes.data ?? []).map((u: any) => [u.achievement_id as string, u.unlocked_at as string]),
      );
      return (all ?? []).map((a: any) => ({
        id: a.id,
        slug: a.slug,
        title: a.title,
        description: a.description,
        emoji: a.emoji,
        tier: a.tier,
        hidden: a.hidden,
        unlocked: unlockedMap.has(a.id),
        unlockedAt: unlockedMap.get(a.id) ?? null,
      }));
    },
  },
};
