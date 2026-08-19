import { createClient } from '@insforge/sdk';

const baseUrl = process.env.INSFORGE_BASE_URL!;
const anonKey = process.env.INSFORGE_ANON_KEY!;
const apiKey  = process.env.INSFORGE_API_KEY!;

if (!baseUrl || !anonKey || !apiKey) {
  throw new Error(
    '[backend] Missing INSFORGE_BASE_URL / INSFORGE_ANON_KEY / INSFORGE_API_KEY',
  );
}

/** Admin-scoped client. Bypasses RLS. Use for server-authoritative writes. */
export const admin = createClient({ baseUrl, anonKey: apiKey });

/** Public client (anon key). RLS still applies. */
export const anon = createClient({ baseUrl, anonKey });

/** Per-request client scoped to a caller's JWT. RLS enforced as that user. */
export function userClient(accessToken: string) {
  return createClient({ baseUrl, accessToken });
}
