import type { GQLContext } from '../context';
import { requireUser } from '../context';

function shape(row: Record<string, unknown>, votedByMe: boolean) {
  return {
    id: row.id,
    authorId: row.author_id,
    authorName: row.author_name,
    authorEmoji: row.author_emoji,
    title: row.title,
    description: row.description,
    category: row.category,
    difficulty: row.difficulty,
    status: row.status,
    votes: row.votes ?? 0,
    votedByMe,
    createdAt: row.created_at,
  };
}

export const proposalResolvers = {
  Query: {
    proposals: async (
      _: unknown,
      { sort }: { sort: 'popular' | 'recent' | 'mine' },
      ctx: GQLContext,
    ) => {
      let query = ctx.admin.database.from('proposals_feed').select('*');
      if (sort === 'mine') {
        const userId = requireUser(ctx);
        query = query.eq('author_id', userId);
      }
      query = query.order(sort === 'popular' ? 'votes' : 'created_at', { ascending: false });

      const [feedRes, myVotesRes] = await Promise.all([
        query,
        ctx.userId
          ? ctx.admin.database.from('proposal_votes').select('proposal_id').eq('user_id', ctx.userId)
          : Promise.resolve({ data: [] as { proposal_id: string }[] }),
      ]);
      if (feedRes.error) throw new Error(feedRes.error.message);
      const mine = new Set((myVotesRes.data ?? []).map((v: any) => v.proposal_id));
      return (feedRes.data ?? []).map((r: any) => shape(r, mine.has(r.id)));
    },
  },

  Mutation: {
    submitProposal: async (
      _: unknown,
      { input }: {
        input: {
          title: string; description: string;
          category: string; difficulty: string;
        };
      },
      ctx: GQLContext,
    ) => {
      const userId = requireUser(ctx);
      const { data: inserted, error } = await ctx.admin.database
        .from('challenge_proposals').insert({
          author_id: userId,
          title: input.title,
          description: input.description,
          category: input.category,
          difficulty: input.difficulty,
        }).select().single();
      if (error) throw new Error(error.message);

      await ctx.admin.database.from('proposal_votes').insert({
        proposal_id: (inserted as { id: string }).id,
        user_id: userId,
      });

      const { data: feedRow } = await ctx.admin.database
        .from('proposals_feed').select('*').eq('id', (inserted as { id: string }).id).single();
      return shape(feedRow as Record<string, unknown>, true);
    },

    toggleVote: async (
      _: unknown,
      { proposalId }: { proposalId: string },
      ctx: GQLContext,
    ) => {
      const userId = requireUser(ctx);
      const { data: existing } = await ctx.admin.database.from('proposal_votes')
        .select('proposal_id').eq('proposal_id', proposalId).eq('user_id', userId).limit(1);
      const has = (existing as unknown[] | null)?.length ? true : false;

      if (has) {
        await ctx.admin.database.from('proposal_votes')
          .delete().eq('proposal_id', proposalId).eq('user_id', userId);
      } else {
        await ctx.admin.database.from('proposal_votes').insert({ proposal_id: proposalId, user_id: userId });
      }

      const { data: feedRow } = await ctx.admin.database
        .from('proposals_feed').select('*').eq('id', proposalId).single();
      return shape(feedRow as Record<string, unknown>, !has);
    },
  },
};
