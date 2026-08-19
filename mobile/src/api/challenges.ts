import { MOCK_MODE } from '../config';
import { mockState } from '../mocks/data';
import type { UserChallenge } from '../types';
import { gqlClient } from './graphql';
import {
  TODAY_CHALLENGES_QUERY, GENERATE_DAILY_MUTATION, COMPLETE_CHALLENGE_MUTATION,
} from './operations';

export interface UnlockInfo {
  slug: string;
  title: string;
  description: string;
  emoji: string;
  tier: number;
}

export interface CompleteResult {
  ok: boolean;
  already?: boolean;
  xp_gained?: number;
  xp_total?: number;
  level?: number;
  streak_days?: number;
  unlocks?: UnlockInfo[];
}

function shape(row: Record<string, unknown>): UserChallenge {
  return {
    id: row.id as string,
    user_id: '',
    template_id: row.templateId as string,
    assigned_date: row.assignedDate as string,
    title: row.title as string,
    description: row.description as string,
    xp_reward: row.xpReward as number,
    target_value: (row.targetValue as number | null) ?? null,
    progress_value: row.progressValue as number,
    status: row.status as UserChallenge['status'],
    completed_at: (row.completedAt as string | null) ?? null,
    category: (row.category as UserChallenge['category']) ?? undefined,
  };
}

export async function fetchTodayChallenges(_userId: string): Promise<UserChallenge[]> {
  if (MOCK_MODE) return mockState.challenges.map((c) => ({ ...c }));
  const res = await gqlClient.query(TODAY_CHALLENGES_QUERY, {}, { requestPolicy: 'network-only' }).toPromise();
  if (res.error) throw new Error(res.error.message);
  return (res.data?.todayChallenges ?? []).map(shape);
}

export async function requestDailyGeneration(_userId: string): Promise<{ count: number }> {
  if (MOCK_MODE) return { count: mockState.challenges.length };
  const res = await gqlClient.mutation(GENERATE_DAILY_MUTATION, {}).toPromise();
  if (res.error) throw new Error(res.error.message);
  return { count: res.data?.generateDailyChallenges ?? 0 };
}

export async function completeChallenge(
  userChallengeId: string,
  clientId: string,
  progressValue?: number,
): Promise<CompleteResult> {
  if (MOCK_MODE) {
    const c = mockState.challenges.find((x) => x.id === userChallengeId);
    if (!c) return { ok: false };
    if (c.status === 'completed') {
      return { ok: true, already: true, xp_gained: 0, level: mockState.stats.level };
    }
    c.status = 'completed';
    c.completed_at = new Date().toISOString();
    c.progress_value = progressValue ?? c.target_value ?? c.progress_value;

    mockState.stats.xp_total += c.xp_reward;
    mockState.stats.xp_current_level += c.xp_reward;
    mockState.stats.challenges_completed += 1;

    const xpForLevel = (l: number) => 50 * l * l + 50 * l;
    while (mockState.stats.xp_total >= xpForLevel(mockState.stats.level + 1)) {
      mockState.stats.level += 1;
      mockState.stats.xp_current_level =
        mockState.stats.xp_total - xpForLevel(mockState.stats.level);
    }
    const unlocks: UnlockInfo[] = [];
    const nextLocked = mockState.achievements.find(
      (a) => !a.unlocked && !a.hidden,
    );
    if (nextLocked) {
      nextLocked.unlocked = true;
      unlocks.push({
        slug: nextLocked.slug,
        title: nextLocked.title,
        description: nextLocked.description,
        emoji: nextLocked.emoji ?? '🏅',
        tier: nextLocked.tier,
      });
    }
    return {
      ok: true,
      xp_gained: c.xp_reward,
      xp_total: mockState.stats.xp_total,
      level: mockState.stats.level,
      streak_days: mockState.stats.streak_days,
      unlocks,
    };
  }

  const res = await gqlClient.mutation(COMPLETE_CHALLENGE_MUTATION, {
    userChallengeId, clientId, progressValue,
  }).toPromise();
  if (res.error) throw new Error(res.error.message);
  const r = res.data?.completeChallenge;
  return {
    ok: !!r?.ok,
    already: r?.already ?? false,
    xp_gained: r?.xpGained ?? 0,
    xp_total: r?.xpTotal ?? 0,
    level: r?.level ?? 1,
    streak_days: r?.streakDays ?? 0,
    unlocks: (r?.unlocks ?? []) as UnlockInfo[],
  };
}
