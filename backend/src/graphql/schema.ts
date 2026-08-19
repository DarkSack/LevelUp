import { createSchema } from 'graphql-yoga';
import { authResolvers } from './resolvers/auth';
import { challengeResolvers } from './resolvers/challenges';
import { achievementResolvers } from './resolvers/achievements';
import { proposalResolvers } from './resolvers/proposals';
import { progressResolvers } from './resolvers/progress';
import type { GQLContext } from './context';

const typeDefs = /* GraphQL */ `
  scalar DateTime
  scalar JSON

  enum Category {
    sociability
    physical
    mental
    organization
    creativity
    finance
  }
  enum Difficulty { easy normal hard epic }
  enum ChallengeStatus { pending completed skipped expired }
  enum ProposalStatus { pending approved rejected promoted }

  type Profile {
    id: ID!
    username: String!
    displayName: String!
    pronouns: String
    age: Int
    avatarEmoji: String
    avatarColor: String
    timezone: String!
    categories: [Category!]!
  }

  type Stats {
    userId: ID!
    level: Int!
    xpTotal: Int!
    xpCurrentLevel: Int!
    streakDays: Int!
    streakLastDay: String
    challengesCompleted: Int!
  }

  type Me {
    id: ID!
    email: String!
    profile: Profile
    stats: Stats
  }

  type Challenge {
    id: ID!
    templateId: ID!
    assignedDate: String!
    title: String!
    description: String!
    xpReward: Int!
    targetValue: Float
    progressValue: Float!
    status: ChallengeStatus!
    completedAt: DateTime
    category: Category
  }

  type Achievement {
    id: ID!
    slug: String!
    title: String!
    description: String!
    emoji: String
    tier: Int!
    hidden: Boolean!
    unlocked: Boolean!
    unlockedAt: DateTime
  }

  type Proposal {
    id: ID!
    authorId: ID!
    authorName: String!
    authorEmoji: String
    title: String!
    description: String!
    category: Category!
    difficulty: Difficulty!
    status: ProposalStatus!
    votes: Int!
    votedByMe: Boolean!
    createdAt: DateTime!
  }

  type ActivityEntry {
    id: ID!
    when: DateTime!
    kind: String!
    title: String!
    detail: String
    xp: Int
    category: Category
  }

  type WeeklyProgress {
    xpByDay: [Int!]!
    categoryTotals: JSON!
    streakGrid: JSON!
    recent: [ActivityEntry!]!
  }

  type AuthPayload {
    accessToken: String
    requiresVerification: Boolean!
    me: Me
  }

  type CompleteChallengeResult {
    ok: Boolean!
    already: Boolean
    xpGained: Int
    xpTotal: Int
    level: Int
    streakDays: Int
    unlocks: [UnlockedAchievement!]!
  }

  type UnlockedAchievement {
    slug: String!
    title: String!
    description: String!
    emoji: String!
    tier: Int!
  }

  input OnboardingInput {
    displayName: String!
    username: String!
    pronouns: String
    age: Int
    avatarEmoji: String!
    avatarColor: String!
    categories: [Category!]!
  }

  input ProposalInput {
    title: String!
    description: String!
    category: Category!
    difficulty: Difficulty!
  }

  enum ProposalSort { popular recent mine }

  type Query {
    me: Me
    todayChallenges: [Challenge!]!
    achievements: [Achievement!]!
    proposals(sort: ProposalSort = popular): [Proposal!]!
    weeklyProgress: WeeklyProgress!
  }

  type Mutation {
    signUp(email: String!, password: String!, displayName: String): AuthPayload!
    signIn(email: String!, password: String!): AuthPayload!
    signOut: Boolean!
    completeOnboarding(input: OnboardingInput!): Me!

    generateDailyChallenges: Int!
    completeChallenge(userChallengeId: ID!, clientId: ID!, progressValue: Float): CompleteChallengeResult!

    submitProposal(input: ProposalInput!): Proposal!
    toggleVote(proposalId: ID!): Proposal!
  }
`;

export const schema = createSchema<GQLContext>({
  typeDefs,
  resolvers: [
    authResolvers,
    challengeResolvers,
    achievementResolvers,
    proposalResolvers,
    progressResolvers,
  ] as never,
});
