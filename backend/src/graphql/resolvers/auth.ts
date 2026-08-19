import type { GQLContext } from '../context';
import { requireUser } from '../context';

async function loadMe(ctx: GQLContext) {
  const userId = requireUser(ctx);

  // getCurrentUser() gives us email + verified state.
  const { data: userData } = await ctx.user!.auth.getCurrentUser();
  const user = userData?.user;
  if (!user) throw new Error('UNAUTHENTICATED');

  const [{ data: profile }, { data: stats }] = await Promise.all([
    ctx.admin.database.from('profiles').select('*').eq('id', userId).single(),
    ctx.admin.database.from('profile_stats').select('*').eq('user_id', userId).single(),
  ]);

  return { user, profile, stats };
}

function shapeProfile(row: Record<string, unknown> | null) {
  if (!row) return null;
  return {
    id: row.id,
    username: row.username,
    displayName: row.display_name,
    pronouns: row.pronouns,
    age: row.age,
    avatarEmoji: row.avatar_emoji,
    avatarColor: row.avatar_color,
    timezone: row.timezone,
    categories: row.categories ?? [],
  };
}

function shapeStats(row: Record<string, unknown> | null) {
  if (!row) return null;
  return {
    userId: row.user_id,
    level: row.level,
    xpTotal: row.xp_total,
    xpCurrentLevel: row.xp_current_level,
    streakDays: row.streak_days,
    streakLastDay: row.streak_last_day,
    challengesCompleted: row.challenges_completed,
  };
}

export const authResolvers = {
  Query: {
    me: async (_: unknown, __: unknown, ctx: GQLContext) => {
      if (!ctx.userId) return null;
      const { user, profile, stats } = await loadMe(ctx);
      return {
        id: user.id,
        email: user.email,
        profile: shapeProfile(profile),
        stats: shapeStats(stats),
      };
    },
  },

  Mutation: {
    signUp: async (
      _: unknown,
      { email, password, displayName }: { email: string; password: string; displayName?: string },
      ctx: GQLContext,
    ) => {
      const { data, error } = await ctx.anon.auth.signUp({
        email, password, name: displayName ?? undefined,
      });
      if (error) throw new Error(error.message);
      return {
        accessToken: data?.accessToken ?? null,
        requiresVerification: data?.requireEmailVerification ?? false,
        me: null,
      };
    },

    signIn: async (
      _: unknown,
      { email, password }: { email: string; password: string },
      ctx: GQLContext,
    ) => {
      const { data, error } = await ctx.anon.auth.signInWithPassword({ email, password });
      if (error) throw new Error(error.message);
      return {
        accessToken: data?.accessToken ?? null,
        requiresVerification: false,
        me: null,
      };
    },

    signOut: async (_: unknown, __: unknown, ctx: GQLContext) => {
      if (ctx.user) await ctx.user.auth.signOut();
      return true;
    },

    completeOnboarding: async (
      _: unknown,
      { input }: {
        input: {
          displayName: string; username: string;
          pronouns?: string | null; age?: number | null;
          avatarEmoji: string; avatarColor: string;
          categories: string[];
        };
      },
      ctx: GQLContext,
    ) => {
      const userId = requireUser(ctx);
      const { error } = await ctx.admin.database.from('profiles').update({
        display_name: input.displayName,
        username: input.username,
        pronouns: input.pronouns,
        age: input.age,
        avatar_emoji: input.avatarEmoji,
        avatar_color: input.avatarColor,
        categories: input.categories,
        timezone: 'UTC',
        updated_at: new Date().toISOString(),
      }).eq('id', userId);
      if (error) throw new Error(error.message);

      const { user, profile, stats } = await loadMe(ctx);
      return {
        id: user.id,
        email: user.email,
        profile: shapeProfile(profile),
        stats: shapeStats(stats),
      };
    },
  },
};
