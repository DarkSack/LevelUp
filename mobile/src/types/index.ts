export type Category =
  | 'sociability' | 'physical' | 'mental'
  | 'organization' | 'creativity' | 'finance';

export type Difficulty = 'easy' | 'normal' | 'hard' | 'epic';
export type ChallengeStatus = 'pending' | 'completed' | 'skipped' | 'expired';

export interface Profile {
  id: string;
  username: string;
  display_name: string;
  pronouns: string | null;
  age: number | null;
  gender: string | null;
  avatar_url: string | null;
  avatar_emoji: string | null;
  avatar_color: string | null;
  timezone: string;
  categories: Category[];
}

export interface ProfileStats {
  user_id: string;
  level: number;
  xp_total: number;
  xp_current_level: number;
  streak_days: number;
  streak_last_day: string | null;
  challenges_completed: number;
}

export type ProposalStatus = 'pending' | 'approved' | 'rejected' | 'promoted';

export interface ChallengeProposal {
  id: string;
  author_id: string;
  author_name: string;
  author_emoji: string | null;
  title: string;
  description: string;
  category: Category;
  difficulty: Difficulty;
  status: ProposalStatus;
  votes: number;
  voted_by_me: boolean;
  created_at: string;
}

export interface UserChallenge {
  id: string;
  user_id: string;
  template_id: string;
  assigned_date: string;
  title: string;
  description: string;
  xp_reward: number;
  target_value: number | null;
  progress_value: number;
  status: ChallengeStatus;
  completed_at: string | null;
  category?: Category;   // included in mock/joined queries for UI theming
}
