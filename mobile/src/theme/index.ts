import { MD3DarkTheme, MD3LightTheme } from 'react-native-paper';
import { paperFonts } from './fonts';

const palette = {
  electricBlue: '#3B82F6',
  mint: '#4ADE80',
  gold: '#F59E0B',
  coral: '#F97056',
  darkBg: '#0B0F1A',
  darkSurface: '#141B2D',
  lightBg: '#F7F8FB',
  lightSurface: '#FFFFFF',
};

export const darkTheme = {
  ...MD3DarkTheme,
  fonts: paperFonts,
  colors: {
    ...MD3DarkTheme.colors,
    primary: palette.electricBlue,
    secondary: palette.mint,
    tertiary: palette.gold,
    error: palette.coral,
    background: palette.darkBg,
    surface: palette.darkSurface,
    surfaceVariant: '#1B2338',
  },
};

export const lightTheme = {
  ...MD3LightTheme,
  fonts: paperFonts,
  colors: {
    ...MD3LightTheme.colors,
    primary: palette.electricBlue,
    secondary: palette.mint,
    tertiary: palette.gold,
    error: palette.coral,
    background: palette.lightBg,
    surface: palette.lightSurface,
  },
};

export { palette };

export const categoryStyle: Record<string, { color: string; emoji: string; label: string }> = {
  sociability:  { color: '#F97056', emoji: '🗣️', label: 'Social'      },
  physical:     { color: '#4ADE80', emoji: '🏃', label: 'Físico'      },
  mental:       { color: '#8B5CF6', emoji: '🧘', label: 'Mental'      },
  organization: { color: '#3B82F6', emoji: '📋', label: 'Organización' },
  creativity:   { color: '#F59E0B', emoji: '🎨', label: 'Creatividad' },
  finance:      { color: '#14B8A6', emoji: '💰', label: 'Finanzas'    },
};

