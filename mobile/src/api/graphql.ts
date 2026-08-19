import { Client, cacheExchange, fetchExchange } from "urql";
import AsyncStorage from "@react-native-async-storage/async-storage";

const url =
  process.env.EXPO_PUBLIC_GRAPHQL_URL ?? "http://localhost:4010/api/graphql";

const TOKEN_KEY = "lu:accessToken";

let currentToken: string | null = null;

export async function loadToken(): Promise<string | null> {
  if (currentToken) return currentToken;
  try {
    currentToken = await AsyncStorage.getItem(TOKEN_KEY);
  } catch {
    currentToken = null;
  }
  return currentToken;
}

export async function saveToken(token: string | null): Promise<void> {
  currentToken = token;
  try {
    if (token) await AsyncStorage.setItem(TOKEN_KEY, token);
    else await AsyncStorage.removeItem(TOKEN_KEY);
  } catch {}
}

export const gqlClient = new Client({
  url,
  exchanges: [cacheExchange, fetchExchange],
  requestPolicy: "cache-and-network",
  fetchOptions: () => ({
    headers: currentToken
      ? { authorization: `Bearer ${currentToken}` }
      : ({} as Record<string, string>),
  }),
});
