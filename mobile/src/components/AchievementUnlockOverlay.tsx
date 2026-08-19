import React, { useEffect, useRef } from 'react';
import { View, StyleSheet, Animated, Easing, Modal, Pressable } from 'react-native';
import { Text, Button } from 'react-native-paper';
import { LinearGradient } from 'expo-linear-gradient';
import { Confetti } from './Confetti';

const TIER_COLOR: Record<number, [string, string]> = {
  1: ['#94A3B8', '#64748B'],
  2: ['#F59E0B', '#F97056'],
  3: ['#8B5CF6', '#EC4899'],
};

interface Props {
  visible: boolean;
  title: string;
  description?: string;
  emoji?: string;
  tier?: number;
  onDismiss: () => void;
}

export function AchievementUnlockOverlay({
  visible, title, description, emoji = '🏅', tier = 1, onDismiss,
}: Props) {
  const scale   = useRef(new Animated.Value(0)).current;
  const shake   = useRef(new Animated.Value(0)).current;
  const shine   = useRef(new Animated.Value(-1)).current;

  useEffect(() => {
    if (!visible) return;
    scale.setValue(0);
    shake.setValue(0);
    shine.setValue(-1);
    Animated.sequence([
      Animated.spring(scale, { toValue: 1, friction: 5, tension: 100, useNativeDriver: true }),
      Animated.parallel([
        Animated.sequence([
          Animated.timing(shake, { toValue: 1,  duration: 60, useNativeDriver: true }),
          Animated.timing(shake, { toValue: -1, duration: 60, useNativeDriver: true }),
          Animated.timing(shake, { toValue: 1,  duration: 60, useNativeDriver: true }),
          Animated.timing(shake, { toValue: 0,  duration: 60, useNativeDriver: true }),
        ]),
        Animated.timing(shine, {
          toValue: 1, duration: 1100, delay: 200,
          easing: Easing.inOut(Easing.ease), useNativeDriver: true,
        }),
      ]),
    ]).start();
  }, [visible, scale, shake, shine]);

  const [c1, c2] = TIER_COLOR[tier] ?? TIER_COLOR[1];
  const rotateShake = shake.interpolate({ inputRange: [-1, 1], outputRange: ['-6deg', '6deg'] });
  const shineX      = shine.interpolate({ inputRange: [-1, 1], outputRange: [-200, 200] });

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onDismiss}>
      <Pressable style={styles.bg} onPress={onDismiss}>
        <Confetti count={50} duration={2400} />

        <View style={styles.center}>
          <Text style={styles.overline}>LOGRO DESBLOQUEADO</Text>

          <Animated.View style={{ transform: [{ scale }, { rotate: rotateShake }] }}>
            <LinearGradient
              colors={[c1, c2]}
              style={styles.badge}
              start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }}>
              <Text style={styles.emoji}>{emoji}</Text>
              <Animated.View style={[styles.shine, { transform: [{ translateX: shineX }, { rotate: '20deg' }] }]} />
            </LinearGradient>
          </Animated.View>

          <Text style={styles.title}>{title}</Text>
          {description && <Text style={styles.desc}>{description}</Text>}

          <Button mode="contained" onPress={onDismiss} style={styles.btn} buttonColor={c1}>
            ¡Genial!
          </Button>
        </View>
      </Pressable>
    </Modal>
  );
}

const styles = StyleSheet.create({
  bg: { flex: 1, backgroundColor: 'rgba(11,15,26,0.94)', alignItems: 'center', justifyContent: 'center', padding: 32 },
  center: { alignItems: 'center' },

  overline: { color: 'rgba(255,255,255,0.75)', fontSize: 12, letterSpacing: 3, fontWeight: '800', marginBottom: 20 },

  badge: {
    width: 160, height: 160, borderRadius: 32,
    alignItems: 'center', justifyContent: 'center',
    borderWidth: 3, borderColor: 'rgba(255,255,255,0.35)',
    overflow: 'hidden',
  },
  emoji: { fontSize: 88 },
  shine: {
    position: 'absolute', top: 0, bottom: 0, width: 60,
    backgroundColor: 'rgba(255,255,255,0.35)',
  },

  title: { color: '#fff', fontSize: 24, fontWeight: '800', marginTop: 24, textAlign: 'center' },
  desc:  { color: 'rgba(255,255,255,0.75)', fontSize: 14, marginTop: 8, textAlign: 'center', paddingHorizontal: 12 },

  btn: { marginTop: 24, minWidth: 200 },
});
