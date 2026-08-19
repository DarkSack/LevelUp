import type { YogaInitialContext } from 'graphql-yoga';
import { admin, anon, userClient } from '@/lib/insforge';

export interface GQLContext {
  accessToken: string | null;
  userId: string | null;
  admin: typeof admin;
  anon: typeof anon;
  user: ReturnType<typeof userClient> | null;
}

export async function buildContext({ request }: YogaInitialContext): Promise<GQLContext> {
  const auth = request.headers.get('authorization');
  const accessToken = auth?.startsWith('Bearer ') ? auth.slice(7).trim() : null;

  let userId: string | null = null;
  let user: ReturnType<typeof userClient> | null = null;
  if (accessToken) {
    user = userClient(accessToken);
    const { data } = await user.auth.getCurrentUser();
    userId = data?.user?.id ?? null;
    if (!userId) user = null;
  }

  return { accessToken, userId, admin, anon, user };
}

export function requireUser(ctx: GQLContext): string {
  if (!ctx.userId) throw new Error('UNAUTHENTICATED');
  return ctx.userId;
}
