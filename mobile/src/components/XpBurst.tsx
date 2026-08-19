import React, { useEffect, useRef } from 'react';
import { Animated, StyleSheet, Easing, View } from 'react-native';
import { Text } from 'react-native-paper';

interface Props {
  xp: number;
  color?: string;
  onDone?: () => void;
}

export function XpBurst({ xp, color = '#3B82F6', onDone }: Props) {
  const anim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    Animated.timing(anim, {
      toValue: 1, duration: 1400, easing: Easing.out(Easing.cubic),
      useNativeDriver: true,
    }).start(({ finished }) => finished && onDone?.());
  }, [anim, onDone]);

  const translateY = anim.interpolate({ inputRange: [0, 1], outputRange: [0, -120] });
  const opacity    = anim.interpolate({ inputRange: [0, 0.15, 0.7, 1], outputRange: [0, 1, 1, 0] });
  const scale      = anim.interpolate({ inputRange: [0, 0.2, 1], outputRange: [0.6, 1.1, 1] });

  return (
    <View pointerEvents="none" style={styles.root}>
      <Animated.View style={[
        styles.pill,
        { backgroundColor: color, opacity, transform: [{ translateY }, { scale }] },
      ]}>
        <Text style={styles.text}>+{xp} XP</Text>
      </Animated.View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { ...StyleSheet.absoluteFillObject, alignItems: 'center', justifyContent: 'center', zIndex: 15 },
  pill: {
    paddingHorizontal: 24, paddingVertical: 12, borderRadius: 999,
    shadowColor: '#000', shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.25, shadowRadius: 8, elevation: 6,
  },
  text: { color: '#fff', fontSize: 22, fontWeight: '800' },
});
