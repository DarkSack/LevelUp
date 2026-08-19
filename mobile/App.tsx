import React, { useEffect } from 'react';
import { useColorScheme, Text, TextInput as RNTextInput } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { PaperProvider } from 'react-native-paper';
import { StatusBar } from 'expo-status-bar';
import { darkTheme, lightTheme } from './src/theme';
import { injectWebFonts, defaultFontFamily } from './src/theme/fonts';
import { RootNavigator } from './src/navigation/RootNavigator';
import { useAuth } from './src/store/auth';
import { useThemeMode } from './src/store/theme';
import { MobileFrame } from './src/components/MobileFrame';

// Apply the body font as the global default for every <Text> / <TextInput>.
// Paper Text uses its theme fonts; this catches raw RN Text elsewhere.
{
  const rnText = Text as unknown as { defaultProps?: Record<string, unknown> };
  rnText.defaultProps = rnText.defaultProps ?? {};
  rnText.defaultProps.style = [{ fontFamily: defaultFontFamily }, rnText.defaultProps.style];

  const rnInput = RNTextInput as unknown as { defaultProps?: Record<string, unknown> };
  rnInput.defaultProps = rnInput.defaultProps ?? {};
  rnInput.defaultProps.style = [{ fontFamily: defaultFontFamily }, rnInput.defaultProps.style];
}

injectWebFonts();

export default function App() {
  const systemScheme = useColorScheme();
  const themeMode = useThemeMode((s) => s.mode);
  const hydrateTheme = useThemeMode((s) => s.hydrate);
  const hydrateAuth = useAuth((s) => s.hydrate);

  const effectiveScheme = themeMode === 'auto' ? systemScheme : themeMode;
  const theme = effectiveScheme === 'dark' ? darkTheme : lightTheme;

  useEffect(() => { hydrateAuth(); hydrateTheme(); }, [hydrateAuth, hydrateTheme]);

  return (
    <SafeAreaProvider>
      <PaperProvider theme={theme}>
        <StatusBar style={effectiveScheme === 'dark' ? 'light' : 'dark'} />
        <MobileFrame>
          <RootNavigator />
        </MobileFrame>
      </PaperProvider>
    </SafeAreaProvider>
  );
}
