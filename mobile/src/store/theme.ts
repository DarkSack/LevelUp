import { create } from 'zustand';
import AsyncStorage from '@react-native-async-storage/async-storage';

export type ThemeMode = 'auto' | 'light' | 'dark';

interface ThemeState {
  mode: ThemeMode;
  hydrated: boolean;
  setMode: (m: ThemeMode) => void;
  hydrate: () => Promise<void>;
}

const KEY = 'lu:themeMode';

export const useThemeMode = create<ThemeState>((set) => ({
  mode: 'auto',
  hydrated: false,
  setMode: (m) => {
    set({ mode: m });
    AsyncStorage.setItem(KEY, m).catch(() => {});
  },
  hydrate: async () => {
    try {
      const v = await AsyncStorage.getItem(KEY);
      if (v === 'auto' || v === 'light' || v === 'dark') {
        set({ mode: v, hydrated: true });
        return;
      }
    } catch {}
    set({ hydrated: true });
  },
}));

export const THEME_OPTIONS: { key: ThemeMode; label: string; emoji: string }[] = [
  { key: 'auto',  label: 'Auto',   emoji: '🌓' },
  { key: 'light', label: 'Claro',  emoji: '☀️' },
  { key: 'dark',  label: 'Oscuro', emoji: '🌙' },
];
