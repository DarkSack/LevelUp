import { Platform } from 'react-native';
import { configureFonts, MD3DarkTheme } from 'react-native-paper';

/**
 * On web, inject a Google Fonts stylesheet. On native, `expo-font` would be
 * needed to actually load a TTF; we fall back to a good system stack there.
 */
export function injectWebFonts() {
  if (Platform.OS !== 'web' || typeof document === 'undefined') return;
  if (!document.getElementById('lu-fonts')) {
    const link = document.createElement('link');
    link.id = 'lu-fonts';
    link.rel = 'stylesheet';
    link.href =
      'https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600;700&family=Rubik:wght@400;500;700;800;900&display=swap';
    document.head.appendChild(link);
  }
  // Global override: RN Web renders Text as spans with class css-textHasAncestor-*;
  // Paper's Roboto default wins over defaultProps. This forces our fonts on all
  // text and inputs regardless of nested style order.
  if (!document.getElementById('lu-font-css')) {
    const style = document.createElement('style');
    style.id = 'lu-font-css';
    style.textContent = `
      body, [class*="css-textHasAncestor"], [class*="css-text-"], [class*="css-textinput"],
      input, textarea, button {
        font-family: "Space Grotesk", system-ui, -apple-system, "Segoe UI", sans-serif !important;
      }
      [class*="css-textHasAncestor"][style*="font-weight: 700"],
      [class*="css-textHasAncestor"][style*="font-weight: 800"],
      [class*="css-textHasAncestor"][style*="font-weight: 900"],
      h1, h2, h3, h4 {
        font-family: "Rubik", system-ui, -apple-system, "Segoe UI", sans-serif !important;
      }
    `;
    document.head.appendChild(style);
  }
}

const displayFamily = Platform.select({
  web:    '"Rubik", system-ui, -apple-system, "Segoe UI", sans-serif',
  ios:    'System',
  android:'sans-serif-medium',
  default: 'System',
});

const bodyFamily = Platform.select({
  web:    '"Space Grotesk", system-ui, -apple-system, "Segoe UI", sans-serif',
  ios:    'System',
  android:'sans-serif',
  default: 'System',
});

// Paper MD3 fonts config: display/headline get the display font; the rest use body.
export const paperFonts = configureFonts({
  config: {
    displayLarge:   { ...MD3DarkTheme.fonts.displayLarge,   fontFamily: displayFamily, fontWeight: '800' as const },
    displayMedium:  { ...MD3DarkTheme.fonts.displayMedium,  fontFamily: displayFamily, fontWeight: '800' as const },
    displaySmall:   { ...MD3DarkTheme.fonts.displaySmall,   fontFamily: displayFamily, fontWeight: '800' as const },
    headlineLarge:  { ...MD3DarkTheme.fonts.headlineLarge,  fontFamily: displayFamily, fontWeight: '700' as const },
    headlineMedium: { ...MD3DarkTheme.fonts.headlineMedium, fontFamily: displayFamily, fontWeight: '700' as const },
    headlineSmall:  { ...MD3DarkTheme.fonts.headlineSmall,  fontFamily: displayFamily, fontWeight: '700' as const },
    titleLarge:     { ...MD3DarkTheme.fonts.titleLarge,     fontFamily: displayFamily, fontWeight: '700' as const },
    titleMedium:    { ...MD3DarkTheme.fonts.titleMedium,    fontFamily: displayFamily, fontWeight: '600' as const },
    titleSmall:     { ...MD3DarkTheme.fonts.titleSmall,     fontFamily: displayFamily, fontWeight: '600' as const },
    labelLarge:     { ...MD3DarkTheme.fonts.labelLarge,     fontFamily: bodyFamily,    fontWeight: '600' as const },
    labelMedium:    { ...MD3DarkTheme.fonts.labelMedium,    fontFamily: bodyFamily,    fontWeight: '600' as const },
    labelSmall:     { ...MD3DarkTheme.fonts.labelSmall,     fontFamily: bodyFamily,    fontWeight: '600' as const },
    bodyLarge:      { ...MD3DarkTheme.fonts.bodyLarge,      fontFamily: bodyFamily },
    bodyMedium:     { ...MD3DarkTheme.fonts.bodyMedium,     fontFamily: bodyFamily },
    bodySmall:      { ...MD3DarkTheme.fonts.bodySmall,      fontFamily: bodyFamily },
  },
});

export const defaultFontFamily = bodyFamily;
export const displayFontFamily = displayFamily;
