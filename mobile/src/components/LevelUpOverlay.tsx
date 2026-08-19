import React, { useEffect, useRef } from 'react';
import { View, StyleSheet, Animated, Easing, Modal, Pressable } from 'react-native';
import { Text, Button } from 'react-native-paper';
import { LinearGradient } from 'expo-linear-gradient';
import { palette } from '../theme';
import { Confetti } from './Confetti';

interface Props {
  visible: boolean;
  level: number;
  onDismiss: () => void;
}

export function LevelUpOverlay({ visible, level, onDismiss }: Props) {
  const scale = useRef(new Animated.Value(0.3)).current;
  const rot   = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    if (!visible) return;
    scale.setValue(0.3);
    rot.setValue(0);
    Animated.parallel([
      Animated.spring(scale, { toValue: 1, friction: 5, tension: 90, useNativeDriver: true }),
      Animated.loop(
        Animated.timing(rot, {
          toValue: 1, duration: 8000,
          easing: Easing.linear, useNativeDriver: true,
        }),
      ),
    ]).start();
  }, [visible, scale, rot]);

  const rayRotate = rot.interpolate({ inputRange: [0, 1], outputRange: ['0deg', '360deg'] });

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onDismiss}>
      <Pressable style={styles.bg} onPress={onDismiss}>
        <Confetti count={80} duration={2600} />

        <View style={styles.center}>
          <Animated.View style={[styles.rays, { transform: [{ rotate: rayRotate }] }]}>
            <LinearGradient
              colors={['#F59E0B', 'transparent']}
              style={styles.ray}
              start={{ x: 0.5, y: 0 }} end={{ x: 0.5, y: 1 }} />
          </Animated.View>

          <Animated.View style={[styles.badgeWrap, { transform: [{ scale }] }]}>
            <LinearGradient
              colors={[palette.gold, '#F97056']}
              style={styles.badge}
              start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }}>
              <Text style={styles.badgeSmall}>NIVEL</Text>
              <Text style={styles.badgeLevel}>{level}</Text>
            </LinearGradient>
          </Animated.View>

          <Text style={styles.title}>¡Subiste de nivel!</Text>
          <Text style={styles.subtitle}>Nuevos retos se han desbloqueado.</Text>

          <Button mode="contained" onPress={onDismiss} style={styles.btn} buttonColor={palette.gold}>
            Continuar
          </Button>
        </View>
      </Pressable>
    </Modal>
  );
}

const styles = StyleSheet.create({
  bg: { flex: 1, backgroundColor: 'rgba(11,15,26,0.94)', alignItems: 'center', justifyContent: 'center', padding: 32 },
  center: { alignItems: 'center' },

  rays: {
    position: 'absolute',
    width: 420, height: 420,
    alignItems: 'center', justifyContent: 'center',
  },
  ray: { width: 6, height: 210, opacity: 0.6 },

  badgeWrap: {
    shadowColor: '#F59E0B', shadowOpacity: 0.6,
    shadowRadius: 20, shadowOffset: { width: 0, height: 0 }, elevation: 12,
  },
  badge: {
    width: 160, height: 160, borderRadius: 80,
    alignItems: 'center', justifyContent: 'center',
    borderWidth: 4, borderColor: 'rgba(255,255,255,0.35)',
  },
  badgeSmall: { color: 'rgba(255,255,255,0.9)', fontSize: 13, letterSpacing: 3, fontWeight: '700' },
  badgeLevel: { color: '#fff', fontSize: 72, fontWeight: '900', lineHeight: 78 },

  title:    { color: '#fff', fontSize: 26, fontWeight: '800', marginTop: 24 },
  subtitle: { color: 'rgba(255,255,255,0.75)', fontSize: 14, marginTop: 6, textAlign: 'center' },

  btn: { marginTop: 24, minWidth: 200 },
});
