import React, { useEffect, useRef } from 'react';
import { Animated, StyleSheet, View, Easing, ViewStyle } from 'react-native';

interface Props {
  progress: number;          // 0..1
  color?: string;
  trackColor?: string;
  height?: number;
  duration?: number;
  style?: ViewStyle;
}

/**
 * Bar whose width tweens whenever `progress` changes.
 * On a level-up (progress drops from ~1 to lower value) we fill to 1 first
 * so the user visually sees "the bar filled" before it wraps to the new level.
 */
export function AnimatedBar({
  progress,
  color = '#3B82F6',
  trackColor = 'rgba(255,255,255,0.25)',
  height = 8,
  duration = 700,
  style,
}: Props) {
  const value = useRef(new Animated.Value(progress)).current;
  const prev  = useRef(progress);

  useEffect(() => {
    const from = prev.current;
    const to   = Math.max(0, Math.min(1, progress));

    // Level-up: prior >= 0.9 and new < prior → animate up to 1, snap, then to new.
    if (from > to && from >= 0.8) {
      Animated.sequence([
        Animated.timing(value, {
          toValue: 1, duration: Math.round(duration * 0.5),
          easing: Easing.out(Easing.cubic), useNativeDriver: false,
        }),
        Animated.timing(value, {
          toValue: 0, duration: 120,
          easing: Easing.in(Easing.quad), useNativeDriver: false,
        }),
        Animated.timing(value, {
          toValue: to, duration: Math.round(duration * 0.6),
          easing: Easing.out(Easing.cubic), useNativeDriver: false,
        }),
      ]).start();
    } else {
      Animated.timing(value, {
        toValue: to, duration,
        easing: Easing.out(Easing.cubic), useNativeDriver: false,
      }).start();
    }

    prev.current = to;
  }, [progress, value, duration]);

  const width = value.interpolate({
    inputRange: [0, 1], outputRange: ['0%', '100%'],
  });

  return (
    <View style={[styles.track, { height, backgroundColor: trackColor }, style]}>
      <Animated.View style={[styles.fill, { width, height, backgroundColor: color }]} />
    </View>
  );
}

const styles = StyleSheet.create({
  track: { borderRadius: 999, overflow: 'hidden', width: '100%' },
  fill:  { borderRadius: 999 },
});
