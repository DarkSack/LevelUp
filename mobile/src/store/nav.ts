import { create } from 'zustand';

export type ScreenKey = 'home' | 'achievements' | 'progress' | 'proposals' | 'profile';

interface NavState {
  screen: ScreenKey;
  menuOpen: boolean;
  setScreen: (s: ScreenKey) => void;
  openMenu: () => void;
  closeMenu: () => void;
  toggleMenu: () => void;
}

const store = create<NavState>((set) => ({
  screen: 'home',
  menuOpen: false,
  setScreen: (screen) => set({ screen, menuOpen: false }),
  openMenu:  () => set({ menuOpen: true  }),
  closeMenu: () => set({ menuOpen: false }),
  toggleMenu: () => set((s) => ({ menuOpen: !s.menuOpen })),
}));

export const useNav = store;

// Debug hook: expose the store on window for browser inspection.
if (typeof window !== 'undefined') {
  (window as unknown as { __nav?: typeof store }).__nav = store;
}

export const MENU_ITEMS: { key: ScreenKey; label: string; emoji: string }[] = [
  { key: 'home',         label: 'Inicio',   emoji: '🏠' },
  { key: 'achievements', label: 'Logros',   emoji: '🏆' },
  { key: 'progress',     label: 'Progreso', emoji: '📈' },
  { key: 'proposals',    label: 'Ideas',    emoji: '💡' },
  { key: 'profile',      label: 'Perfil',   emoji: '👤' },
];
