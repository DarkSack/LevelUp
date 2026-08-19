import { MOCK_MODE } from '../config';
import { mockProposalsState, mockState } from '../mocks/data';
import { newUuid } from '../utils/uuid';
import type { ChallengeProposal, Category, Difficulty } from '../types';
import { gqlClient } from './graphql';
import {
  PROPOSALS_QUERY, SUBMIT_PROPOSAL_MUTATION, TOGGLE_VOTE_MUTATION,
} from './operations';

export type ProposalSort = 'popular' | 'recent' | 'mine';

function shape(row: any): ChallengeProposal {
  return {
    id: row.id,
    author_id: row.authorId,
    author_name: row.authorName,
    author_emoji: row.authorEmoji ?? null,
    title: row.title,
    description: row.description,
    category: row.category,
    difficulty: row.difficulty,
    status: row.status,
    votes: row.votes ?? 0,
    voted_by_me: row.votedByMe ?? false,
    created_at: row.createdAt,
  };
}

export async function listProposals(sort: ProposalSort, userId?: string): Promise<ChallengeProposal[]> {
  if (MOCK_MODE) {
    let items = mockProposalsState.list.slice();
    if (sort === 'mine') items = items.filter((p) => p.author_id === (userId ?? mockState.profile.id));
    if (sort === 'popular') items.sort((a, b) => b.votes - a.votes);
    if (sort === 'recent') items.sort((a, b) => +new Date(b.created_at) - +new Date(a.created_at));
    return items.map((p) => ({ ...p }));
  }
  const res = await gqlClient.query(PROPOSALS_QUERY, { sort }, { requestPolicy: 'network-only' }).toPromise();
  if (res.error) throw new Error(res.error.message);
  return (res.data?.proposals ?? []).map(shape);
}

export interface NewProposal {
  title: string;
  description: string;
  category: Category;
  difficulty: Difficulty;
}

export async function submitProposal(p: NewProposal, _userId: string): Promise<ChallengeProposal> {
  if (MOCK_MODE) {
    const created: ChallengeProposal = {
      id: newUuid(),
      author_id: mockState.profile.id,
      author_name: mockState.profile.display_name,
      author_emoji: mockState.profile.avatar_emoji,
      title: p.title, description: p.description,
      category: p.category, difficulty: p.difficulty,
      status: 'pending', votes: 1, voted_by_me: true,
      created_at: new Date().toISOString(),
    };
    mockProposalsState.list.unshift(created);
    return created;
  }
  const res = await gqlClient.mutation(SUBMIT_PROPOSAL_MUTATION, {
    input: {
      title: p.title, description: p.description,
      category: p.category, difficulty: p.difficulty,
    },
  }).toPromise();
  if (res.error) throw new Error(res.error.message);
  return shape(res.data?.submitProposal);
}

export async function toggleVote(proposalId: string, _userId: string): Promise<ChallengeProposal | null> {
  if (MOCK_MODE) {
    const p = mockProposalsState.list.find((x) => x.id === proposalId);
    if (!p) return null;
    p.voted_by_me = !p.voted_by_me;
    p.votes += p.voted_by_me ? 1 : -1;
    return { ...p };
  }
  const res = await gqlClient.mutation(TOGGLE_VOTE_MUTATION, { proposalId }).toPromise();
  if (res.error) throw new Error(res.error.message);
  return res.data?.toggleVote as ChallengeProposal ?? null;
}
