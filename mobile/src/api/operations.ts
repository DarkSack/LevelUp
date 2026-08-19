// Centralized GraphQL operations as tagged strings for urql.

export const ME_QUERY = /* GraphQL */ `
  query Me {
    me {
      id
      email
      profile {
        id username displayName pronouns age
        avatarEmoji avatarColor timezone categories
      }
      stats {
        userId level xpTotal xpCurrentLevel
        streakDays streakLastDay challengesCompleted
      }
    }
  }
`;

export const TODAY_CHALLENGES_QUERY = /* GraphQL */ `
  query TodayChallenges {
    todayChallenges {
      id templateId assignedDate title description
      xpReward targetValue progressValue status completedAt category
    }
  }
`;

export const ACHIEVEMENTS_QUERY = /* GraphQL */ `
  query Achievements {
    achievements {
      id slug title description emoji tier hidden unlocked unlockedAt
    }
  }
`;

export const PROPOSALS_QUERY = /* GraphQL */ `
  query Proposals($sort: ProposalSort!) {
    proposals(sort: $sort) {
      id authorId authorName authorEmoji
      title description category difficulty status
      votes votedByMe createdAt
    }
  }
`;

export const WEEKLY_PROGRESS_QUERY = /* GraphQL */ `
  query WeeklyProgress {
    weeklyProgress {
      xpByDay
      categoryTotals
      streakGrid
      recent { id when kind title detail xp category }
    }
  }
`;

export const SIGN_IN_MUTATION = /* GraphQL */ `
  mutation SignIn($email: String!, $password: String!) {
    signIn(email: $email, password: $password) {
      accessToken requiresVerification
    }
  }
`;

export const SIGN_UP_MUTATION = /* GraphQL */ `
  mutation SignUp($email: String!, $password: String!, $displayName: String) {
    signUp(email: $email, password: $password, displayName: $displayName) {
      accessToken requiresVerification
    }
  }
`;

export const SIGN_OUT_MUTATION = /* GraphQL */ `
  mutation SignOut { signOut }
`;

export const COMPLETE_ONBOARDING_MUTATION = /* GraphQL */ `
  mutation CompleteOnboarding($input: OnboardingInput!) {
    completeOnboarding(input: $input) {
      id
      profile {
        id username displayName pronouns age
        avatarEmoji avatarColor timezone categories
      }
      stats {
        userId level xpTotal xpCurrentLevel
        streakDays streakLastDay challengesCompleted
      }
    }
  }
`;

export const GENERATE_DAILY_MUTATION = /* GraphQL */ `
  mutation GenerateDaily { generateDailyChallenges }
`;

export const COMPLETE_CHALLENGE_MUTATION = /* GraphQL */ `
  mutation CompleteChallenge($userChallengeId: ID!, $clientId: ID!, $progressValue: Float) {
    completeChallenge(userChallengeId: $userChallengeId, clientId: $clientId, progressValue: $progressValue) {
      ok already xpGained xpTotal level streakDays
      unlocks { slug title description emoji tier }
    }
  }
`;

export const SUBMIT_PROPOSAL_MUTATION = /* GraphQL */ `
  mutation SubmitProposal($input: ProposalInput!) {
    submitProposal(input: $input) {
      id authorId authorName authorEmoji
      title description category difficulty status
      votes votedByMe createdAt
    }
  }
`;

export const TOGGLE_VOTE_MUTATION = /* GraphQL */ `
  mutation ToggleVote($proposalId: ID!) {
    toggleVote(proposalId: $proposalId) {
      id votes votedByMe status
    }
  }
`;
