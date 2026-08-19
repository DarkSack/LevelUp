import React, { useEffect, useMemo, useRef } from 'react';
import { View, StyleSheet, Animated, useWindowDimensions, Easing } from 'react-native';

const COLORS = ['#3B82F6', '#4ADE80', '#F59E0B', '#F97056', '#8B5CF6', '#14B8A6'];

interface Props {
  count?: number;
  duration?: number;
  onDone?: () => void;
}

interface Piece {
  x: number; delay: number; color: string; size: number; rotStart: number;
  drift: number; sway: number;
  translate: Animated.Value;
  rotate: Animated.Value;
}

export function Confetti({ count = 60, duration = 2400, onDone }: Props) {
  const { width: W, height: H } = useWindowDimensions();
  const pieces = useMemo<Piece[]>(
    () => Array.from({ length: count }, () => ({
      x: Math.random() * W,
      delay: Math.random() * 400,
      color: COLORS[Math.floor(Math.random() * COLORS.length)],
      size: 6 + Math.random() * 8,
      rotStart: Math.random() * 360,
      drift: (Math.random() - 0.5) * 120,
      sway: 30 + Math.random() * 60,
      translate: new Animated.Value(0),
      rotate: new Animated.Value(0),
    })),
    [count],
  );

  const finished = useRef(0);

  useEffect(() => {
    const anims = pieces.map((p) =>
      Animated.parallel([
        Animated.timing(p.translate, {
          toValue: 1, duration, delay: p.delay,
          easing: Easing.out(Easing.quad),
          useNativeDriver: true,
        }),
        Animated.loop(
          Animated.timing(p.rotate, {
            toValue: 1, duration: 900 + Math.random() * 600,
            easing: Easing.linear, useNativeDriver: true,
          }),
        ),
      ]),
    );
    anims.forEach((a) => a.start(() => {
      finished.current += 1;
      if (finished.current >= pieces.length && onDone) onDone();
    }));
    // safety fallback if some anims never end (loop rot)
    const t = setTimeout(() => onDone?.(), duration + 500);
    return () => clearTimeout(t);
  }, [pieces, duration, onDone]);

  return (
    <View pointerEvents="none" style={styles.root}>
      {pieces.map((p, i) => {
        const translateY = p.translate.interpolate({
          inputRange: [0, 1], outputRange: [-40, H + 40],
        });
        const translateX = p.translate.interpolate({
          inputRange: [0, 0.5, 1],
          outputRange: [p.x, p.x + p.drift + p.sway, p.x + p.drift - p.sway],
        });
        const opacity = p.translate.interpolate({
          inputRange: [0, 0.85, 1], outputRange: [1, 1, 0],
        });
        const rotateZ = p.rotate.interpolate({
          inputRange: [0, 1],
          outputRange: [`${p.rotStart}deg`, `${p.rotStart + 360}deg`],
        });
        return (
          <Animated.View
            key={i}
            style={{
              position: 'absolute',
              width: p.size, height: p.size * 0.6,
              backgroundColor: p.color, borderRadius: 2,
              opacity,
              transform: [{ translateX }, { translateY }, { rotateZ }],
            }}
          />
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  root: { ...StyleSheet.absoluteFillObject, zIndex: 20 },
});
