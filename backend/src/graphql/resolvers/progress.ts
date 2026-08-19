import type { GQLContext } from '../context';
import { requireUser } from '../context';

const DAYS_WEEK = 7;
const WEEKS_GRID = 12;

/**
 * Aggregate weekly XP, category totals, streak grid, and recent milestones
 * for the ProgressScreen. Read-only.
 */
export const progressResolvers = {
  Query: {
    weeklyProgress: async (_: unknown, __: unknown, ctx: GQLContext) => {
      const userId = requireUser(ctx);
      const now = new Date();

      const weekStart = new Date(now);
      weekStart.setDate(now.getDate() - (DAYS_WEEK - 1));
      weekStart.setHours(0, 0, 0, 0);

      const gridStart = new Date(now);
      gridStart.setDate(now.getDate() - (WEEKS_GRID * DAYS_WEEK - 1));
      gridStart.setHours(0, 0, 0, 0);

      const [xpRes, completedRes] = await Promise.all([
        ctx.admin.database.from('xp_events')
          .select('amount, created_at')
          .eq('user_id', userId)
          .gte('created_at', gridStart.toISOString()),
        ctx.admin.database.from('user_challenges')
          .select('completed_at, xp_reward, title, challenge_templates(category, description)')
          .eq('user_id', userId)
          .eq('status', 'completed')
          .order('completed_at', { ascending: false })
          .limit(30),
      ]);
      if (xpRes.error) throw new Error(xpRes.error.message);

      const xpByDay = new Array(DAYS_WEEK).fill(0);
      const streakGrid: number[][] = Array.from(
        { length: WEEKS_GRID }, () => new Array(DAYS_WEEK).fill(0),
      );

      for (const e of (xpRes.data ?? [])) {
        const t = new Date((e as { created_at: string }).created_at);
        const dayDiff = Math.floor((now.getTime() - t.getTime()) / 86400000);
        if (dayDiff >= 0 && dayDiff < DAYS_WEEK) {
          xpByDay[DAYS_WEEK - 1 - dayDiff] += (e as { amount: number }).amount;
        }
        const gridDiff = Math.floor((now.getTime() - t.getTime()) / 86400000);
        if (gridDiff >= 0 && gridDiff < WEEKS_GRID * DAYS_WEEK) {
          const w = WEEKS_GRID - 1 - Math.floor(gridDiff / DAYS_WEEK);
          const d = DAYS_WEEK - 1 - (gridDiff % DAYS_WEEK);
          if (w >= 0 && d >= 0) streakGrid[w][d] = 1;
        }
      }

      // Category totals (last 30 days) via completed challenges join.
      const categoryTotals: Record<string, number> = {};
      const recent = [];
      for (const c of (completedRes.data ?? []) as any[]) {
        const cat = c.challenge_templates?.category as string | undefined;
        if (cat) categoryTotals[cat] = (categoryTotals[cat] ?? 0) + 1;
        recent.push({
          id: c.id ?? `${c.completed_at}`,
          when: c.completed_at,
          kind: 'challenge',
          title: c.title,
          detail: null,
          xp: c.xp_reward,
          category: cat ?? null,
        });
      }

      return {
        xpByDay,
        categoryTotals,
        streakGrid,
        recent: recent.slice(0, 10),
      };
    },
  },
};
