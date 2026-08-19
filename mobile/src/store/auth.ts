import { create } from 'zustand';
import { MOCK_MODE } from '../config';
import { mockState } from '../mocks/data';
import type { Profile, ProfileStats } from '../types';
import { gqlClient, loadToken, saveToken } from '../api/graphql';
import {
  ME_QUERY, SIGN_IN_MUTATION, SIGN_UP_MUTATION, SIGN_OUT_MUTATION,
  COMPLETE_ONBOARDING_MUTATION,
} from '../api/operations';

interface User {
  id: string;
  email?: string;
}

interface AuthState {
  user: User | null;
  profile: Profile | null;
  stats: ProfileStats | null;
  loading: boolean;
  hydrate: () => Promise<void>;
  refreshProfile: () => Promise<void>;
  signIn: (email: string, password: string) => Promise<void>;
  signUp: (email: string, password: string, displayName: string) => Promise<void>;
  signOut: () => Promise<void>;
  completeOnboarding: (data: OnboardingData) => Promise<void>;
}

export interface OnboardingData {
  display_name: string;
  username: string;
  pronouns?: string | null;
  age?: number | null;
  avatar_emoji: string;
  avatar_color: string;
  categories: Profile['categories'];
}

async function fetchMe(): Promise<{
  user: User | null;
  profile: Profile | null;
  stats: ProfileStats | null;
}> {
  const res = await gqlClient.query(ME_QUERY, {}, { requestPolicy: 'network-only' }).toPromise();
  const me = res.data?.me;
  if (!me) return { user: null, profile: null, stats: null };

  const profile = me.profile ? {
    id: me.profile.id,
    username: me.profile.username,
    display_name: me.profile.displayName,
    pronouns: me.profile.pronouns ?? null,
    age: me.profile.age ?? null,
    gender: null,
    avatar_url: null,
    avatar_emoji: me.profile.avatarEmoji ?? null,
    avatar_color: me.profile.avatarColor ?? null,
    timezone: me.profile.timezone,
    categories: me.profile.categories ?? [],
  } as Profile : null;

  const stats = me.stats ? {
    user_id: me.stats.userId,
    level: me.stats.level,
    xp_total: me.stats.xpTotal,
    xp_current_level: me.stats.xpCurrentLevel,
    streak_days: me.stats.streakDays,
    streak_last_day: me.stats.streakLastDay ?? null,
    challenges_completed: me.stats.challengesCompleted,
  } as ProfileStats : null;

  return {
    user: { id: me.id, email: me.email },
    profile,
    stats,
  };
}

export const useAuth = create<AuthState>((set, get) => ({
  user: null,
  profile: null,
  stats: null,
  loading: true,

  hydrate: async () => {
    if (MOCK_MODE) {
      set({
        loading: false,
        user: mockState.isSignedIn ? { id: mockState.profile.id } : null,
        profile: mockState.hasOnboarded ? mockState.profile : null,
        stats: mockState.hasOnboarded ? mockState.stats : null,
      });
      return;
    }
    await loadToken();
    try { set({ ...(await fetchMe()) }); }
    catch { /* offline / unauthenticated */ }
    finally { set({ loading: false }); }
  },

  refreshProfile: async () => {
    if (MOCK_MODE) {
      set({ profile: { ...mockState.profile }, stats: { ...mockState.stats } });
      return;
    }
    try { set({ ...(await fetchMe()) }); } catch {}
  },

  signIn: async (email, password) => {
    if (MOCK_MODE) {
      mockState.isSignedIn = true;
      set({
        user: { id: mockState.profile.id },
        profile: mockState.hasOnboarded ? mockState.profile : null,
        stats: mockState.hasOnboarded ? mockState.stats : null,
      });
      return;
    }
    const res = await gqlClient.mutation(SIGN_IN_MUTATION, { email, password }).toPromise();
    if (res.error) throw new Error(res.error.message);
    const token = res.data?.signIn?.accessToken as string | null;
    if (!token) throw new Error('No access token returned');
    await saveToken(token);
    set({ ...(await fetchMe()) });
  },

  signUp: async (email, password, displayName) => {
    if (MOCK_MODE) {
      mockState.isSignedIn = true;
      set({ user: { id: mockState.profile.id }, profile: null, stats: null });
      return;
    }
    const res = await gqlClient.mutation(SIGN_UP_MUTATION, {
      email, password, displayName,
    }).toPromise();
    if (res.error) throw new Error(res.error.message);
    const token = res.data?.signUp?.accessToken as string | null;
    if (!token) {
      if (res.data?.signUp?.requiresVerification) {
        throw new Error('Verifica tu correo antes de iniciar sesión.');
      }
      throw new Error('No access token returned');
    }
    await saveToken(token);
    set({ ...(await fetchMe()) });
  },

  signOut: async () => {
    if (MOCK_MODE) {
      mockState.isSignedIn = false;
      mockState.hasOnboarded = false;
      set({ user: null, profile: null, stats: null });
      return;
    }
    try { await gqlClient.mutation(SIGN_OUT_MUTATION, {}).toPromise(); } catch {}
    await saveToken(null);
    set({ user: null, profile: null, stats: null });
  },

  completeOnboarding: async (data) => {
    if (MOCK_MODE) {
      Object.assign(mockState.profile, {
        display_name: data.display_name,
        username: data.username,
        pronouns: data.pronouns ?? null,
        age: data.age ?? null,
        avatar_emoji: data.avatar_emoji,
        avatar_color: data.avatar_color,
        categories: data.categories,
      });
      mockState.hasOnboarded = true;
      set({ profile: { ...mockState.profile }, stats: { ...mockState.stats } });
      return;
    }
    const res = await gqlClient.mutation(COMPLETE_ONBOARDING_MUTATION, {
      input: {
        displayName: data.display_name,
        username: data.username,
        pronouns: data.pronouns ?? null,
        age: data.age ?? null,
        avatarEmoji: data.avatar_emoji,
        avatarColor: data.avatar_color,
        categories: data.categories,
      },
    }).toPromise();
    if (res.error) throw new Error(res.error.message);
    set({ ...(await fetchMe()) });
  },
}));
