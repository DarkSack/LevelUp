import type { Profile, ProfileStats, UserChallenge, Category, ChallengeProposal } from '../types';

export const mockProfile: Profile = {
  id: 'mock-user',
  username: 'sackito',
  display_name: 'Sackito',
  pronouns: 'he/him',
  age: 24,
  gender: null,
  avatar_url: null,
  avatar_emoji: '🎮',
  avatar_color: '#3B82F6',
  timezone: 'America/Argentina/Buenos_Aires',
  categories: ['physical', 'creativity', 'mental'],
};

export const mockStats: ProfileStats = {
  user_id: 'mock-user',
  level: 7,
  xp_total: 3120,
  xp_current_level: 220,
  streak_days: 12,
  streak_last_day: new Date().toISOString().slice(0, 10),
  challenges_completed: 48,
};

const today = new Date().toISOString().slice(0, 10);

export const mockChallenges: UserChallenge[] = [
  {
    id: 'c1', user_id: 'mock-user', template_id: 't1', assigned_date: today,
    title: 'Muévete',
    description: 'Camina 5,000 pasos hoy.',
    xp_reward: 35, target_value: 5000, progress_value: 2140,
    status: 'pending', completed_at: null, category: 'physical',
  },
  {
    id: 'c2', user_id: 'mock-user', template_id: 't2', assigned_date: today,
    title: 'Momento creativo',
    description: 'Dedica 20 minutos a tu hobby.',
    xp_reward: 40, target_value: 20, progress_value: 0,
    status: 'pending', completed_at: null, category: 'creativity',
  },
  {
    id: 'c3', user_id: 'mock-user', template_id: 't3', assigned_date: today,
    title: 'Gratitud',
    description: 'Escribe 3 cosas por las que estás agradecido.',
    xp_reward: 20, target_value: 3, progress_value: 3,
    status: 'completed',
    completed_at: new Date().toISOString(), category: 'mental',
  },
];

export const mockAchievements = [
  { id: 'a1', slug: 'first_step',  title: 'Primer paso',     description: 'Completa tu primer reto.',              emoji: '🥇', tier: 1, hidden: false, unlocked: true  },
  { id: 'a2', slug: 'streak_7',    title: 'Constante',       description: 'Mantén una racha de 7 días.',           emoji: '🔥', tier: 2, hidden: false, unlocked: true  },
  { id: 'a3', slug: 'streak_30',   title: 'Imparable',       description: 'Mantén una racha de 30 días.',          emoji: '💪', tier: 3, hidden: false, unlocked: false },
  { id: 'a4', slug: 'social_5',    title: 'Rompe el hielo',  description: 'Completa 5 retos sociales.',            emoji: '🗣️', tier: 1, hidden: false, unlocked: false },
  { id: 'a5', slug: 'mental_10',   title: 'Mente tranquila', description: 'Completa 10 retos de bienestar.',       emoji: '🧠', tier: 2, hidden: false, unlocked: true  },
  { id: 'a6', slug: 'level_10',    title: 'Aventurero',      description: 'Alcanza el nivel 10.',                  emoji: '⭐', tier: 2, hidden: false, unlocked: false },
  { id: 'a7', slug: 'beta_tester', title: 'Beta Tester',     description: 'Usaste LevelUp durante la beta.',       emoji: '🧪', tier: 1, hidden: true,  unlocked: true  },
  { id: 'a8', slug: 'bug_catcher', title: 'Bug Catcher',     description: 'Reportaste un bug.',                    emoji: '🐞', tier: 1, hidden: true,  unlocked: false },
  { id: 'a9', slug: 'explorer',    title: 'Explorador',      description: 'Visitaste todas las secciones.',        emoji: '🔎', tier: 1, hidden: true,  unlocked: false },
];

// Mutable in-memory state so completing a challenge feels real in mock mode.
export const mockState = {
  profile: { ...mockProfile },
  stats: { ...mockStats },
  challenges: mockChallenges.map((c) => ({ ...c })),
  achievements: mockAchievements.map((a) => ({ ...a })),
  hasOnboarded: false,      // toggled by OnboardingScreen
  isSignedIn: false,        // toggled by AuthScreen
};

export function resetMock() {
  mockState.profile = { ...mockProfile };
  mockState.stats = { ...mockStats };
  mockState.challenges = mockChallenges.map((c) => ({ ...c }));
  mockState.achievements = mockAchievements.map((a) => ({ ...a }));
  mockState.hasOnboarded = false;
  mockState.isSignedIn = false;
}

// ---- Extra mock: activity history and unlocked titles ------------------

export interface ActivityEntry {
  id: string;
  when: string;              // ISO
  kind: 'challenge' | 'level_up' | 'achievement' | 'streak';
  title: string;
  detail?: string;
  xp?: number;
  category?: Category;
}

export const mockActivity: ActivityEntry[] = [
  { id: 'e1',  when: iso(0, 9),  kind: 'challenge',   title: 'Gratitud',           detail: 'Escribiste 3 cosas',    xp: 20, category: 'mental' },
  { id: 'e2',  when: iso(1, 20), kind: 'level_up',    title: '¡Subiste al nivel 7!',                              xp: 0 },
  { id: 'e3',  when: iso(1, 18), kind: 'challenge',   title: 'Muévete',            detail: '5,000 pasos',           xp: 35, category: 'physical' },
  { id: 'e4',  when: iso(2, 21), kind: 'achievement', title: '🔥 Constante',        detail: 'Racha de 7 días',       xp: 100 },
  { id: 'e5',  when: iso(2, 10), kind: 'challenge',   title: 'Momento creativo',   detail: '20 min de dibujo',      xp: 40, category: 'creativity' },
  { id: 'e6',  when: iso(3, 22), kind: 'streak',      title: 'Racha de 12 días',                                    xp: 0 },
  { id: 'e7',  when: iso(3, 19), kind: 'challenge',   title: 'Ordena',             detail: 'Escritorio',            xp: 35, category: 'organization' },
  { id: 'e8',  when: iso(4, 8),  kind: 'challenge',   title: 'Hidrátate',          detail: '6 vasos',               xp: 15, category: 'physical' },
  { id: 'e9',  when: iso(5, 12), kind: 'challenge',   title: 'Mente en calma',     detail: '5 min de meditación',   xp: 40, category: 'mental' },
  { id: 'e10', when: iso(6, 17), kind: 'challenge',   title: 'Escribe libre',      detail: '200 palabras',          xp: 30, category: 'creativity' },
];

// Daily XP over the last 7 days (index 0 = 6 days ago, index 6 = today)
export const mockWeeklyXp = [120, 85, 210, 45, 180, 95, 55];

export const mockCategoryTotals: Record<Category, number> = {
  physical:     14,
  mental:       10,
  creativity:   9,
  organization: 8,
  sociability:  5,
  finance:      2,
};

// Last 12 weeks streak calendar (each column = 7 days).
// 0 = missed, 1 = active
export const mockStreakGrid: number[][] = Array.from({ length: 12 }, (_, week) =>
  Array.from({ length: 7 }, (_, day) => {
    if (week === 11) return day <= 4 ? 1 : 0;
    if (week >= 8) return Math.random() > 0.15 ? 1 : 0;
    if (week >= 4) return Math.random() > 0.35 ? 1 : 0;
    return Math.random() > 0.55 ? 1 : 0;
  }),
);

export const mockTitles = [
  { slug: 'newcomer',     label: 'Novato',              unlocked: true,  active: false },
  { slug: 'consistent',   label: 'Constante 🔥',        unlocked: true,  active: true  },
  { slug: 'social',       label: 'Social Master 🗣️',    unlocked: false, active: false },
  { slug: 'zen',          label: 'Mente Zen 🧘',        unlocked: true,  active: false },
  { slug: 'creator',      label: 'Creador ✨',           unlocked: false, active: false },
  { slug: 'legend',       label: 'Leyenda 👑',           unlocked: false, active: false },
];

function iso(daysAgo: number, hour: number) {
  const d = new Date();
  d.setDate(d.getDate() - daysAgo);
  d.setHours(hour, 0, 0, 0);
  return d.toISOString();
}

// ---- Community proposals -----------------------------------------------

export const mockProposals: ChallengeProposal[] = [
  {
    id: 'p1', author_id: 'u2', author_name: 'Lucía', author_emoji: '🦊',
    title: 'Escribe una carta a tu yo del futuro',
    description: 'Reflexiona sobre dónde te ves en 1 año y escríbelo. Guárdalo para volver a leerlo.',
    category: 'mental', difficulty: 'normal',
    status: 'pending', votes: 128, voted_by_me: false,
    created_at: iso(2, 14),
  },
  {
    id: 'p2', author_id: 'u3', author_name: 'Marco', author_emoji: '🐺',
    title: 'Aprende 5 palabras en otro idioma',
    description: 'Elige un idioma que te interese y aprende 5 palabras nuevas hoy.',
    category: 'creativity', difficulty: 'easy',
    status: 'approved', votes: 89, voted_by_me: true,
    created_at: iso(3, 10),
  },
  {
    id: 'p3', author_id: 'u4', author_name: 'Sofía', author_emoji: '🧙',
    title: 'Camina 10 km sin auriculares',
    description: 'Salir a caminar prestando atención al entorno, sin música ni podcasts.',
    category: 'physical', difficulty: 'hard',
    status: 'pending', votes: 64, voted_by_me: false,
    created_at: iso(1, 20),
  },
  {
    id: 'p4', author_id: 'u5', author_name: 'Diego', author_emoji: '🥷',
    title: 'Cena con la familia sin celulares',
    description: 'Guarda todos los teléfonos y disfruta una cena hablando cara a cara.',
    category: 'sociability', difficulty: 'normal',
    status: 'pending', votes: 42, voted_by_me: false,
    created_at: iso(4, 21),
  },
  {
    id: 'p5', author_id: 'u6', author_name: 'Elena', author_emoji: '🎯',
    title: 'Revisa tus últimos 30 días de gastos',
    description: 'Categoriza y encuentra 1 gasto que puedas eliminar el próximo mes.',
    category: 'finance', difficulty: 'normal',
    status: 'promoted', votes: 210, voted_by_me: true,
    created_at: iso(8, 11),
  },
  {
    id: 'p6', author_id: 'u7', author_name: 'Rafa', author_emoji: '🦁',
    title: 'Ordena una carpeta de tu escritorio digital',
    description: 'Elige la carpeta más caótica y ordénala en 15 minutos.',
    category: 'organization', difficulty: 'easy',
    status: 'pending', votes: 18, voted_by_me: false,
    created_at: iso(0, 8),
  },
];

// Mutable state so voting/submitting feels real in demo.
export const mockProposalsState = {
  list: mockProposals.map((p) => ({ ...p })),
};

export const AVAILABLE_CATEGORIES: { key: Category; label: string; emoji: string }[] = [
  { key: 'sociability',  label: 'Sociabilidad',    emoji: '🗣️' },
  { key: 'physical',     label: 'Salud física',    emoji: '🏃' },
  { key: 'mental',       label: 'Bienestar mental', emoji: '🧘' },
  { key: 'organization', label: 'Organización',    emoji: '📋' },
  { key: 'creativity',   label: 'Creatividad',     emoji: '🎨' },
  { key: 'finance',      label: 'Finanzas',        emoji: '💰' },
];
