import React from 'react';
import { View, StyleSheet, Platform, useWindowDimensions } from 'react-native';
import { useTheme } from 'react-native-paper';

/**
 * Wraps the whole app so on wide viewports (web) it renders as a centered
 * phone-sized column instead of stretching edge-to-edge. On native the
 * component becomes a plain full-flex View.
 */
const PHONE_WIDTH = 440;
const PHONE_MIN_HEIGHT = 640;

export function MobileFrame({ children }: { children: React.ReactNode }) {
  const { width } = useWindowDimensions();
  const theme = useTheme();

  if (Platform.OS !== 'web' || width < PHONE_WIDTH + 40) {
    return <View style={{ flex: 1, backgroundColor: theme.colors.background }}>{children}</View>;
  }

  return (
    <View style={[styles.stage, { backgroundColor: theme.colors.surfaceVariant ?? '#0B0F1A' }]}>
      <View style={[
        styles.phone,
        {
          backgroundColor: theme.colors.background,
          borderColor: 'rgba(148,163,184,0.25)',
        },
      ]}>
        {children}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  stage: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 20,
  },
  phone: {
    width: PHONE_WIDTH,
    minHeight: PHONE_MIN_HEIGHT,
    maxHeight: '100%',
    borderRadius: 32,
    borderWidth: 1,
    overflow: 'hidden',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 12 },
    shadowOpacity: 0.35,
    shadowRadius: 32,
    elevation: 12,
  },
});
