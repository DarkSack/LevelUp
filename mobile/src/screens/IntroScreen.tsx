import React, { useEffect, useRef, useState } from 'react';
import { View, StyleSheet, Animated, Pressable } from 'react-native';
import { Text, Button, useTheme } from 'react-native-paper';

const LINES = [
  'Hola...',
  'Mmm...',
  'Parece que aún no nos conocemos.',
  '¿Por qué no nos presentamos?',
  'Soy LevelUp,',
  'una app creada por Sackito.',
  'Tu vida es el juego.',
  'Tú eres el personaje.',
  'Cada día tienes nuevas misiones.',
];

const FADE_IN = 700;
const HOLD = 1100;
const FADE_OUT = 500;

interface Props { onFinish: () => void }

export function IntroScreen({ onFinish }: Props) {
  const [idx, setIdx] = useState(0);
  const opacity = useRef(new Animated.Value(0)).current;
  const theme = useTheme();

  useEffect(() => {
    if (idx >= LINES.length) return;
    const seq = Animated.sequence([
      Animated.timing(opacity, { toValue: 1, duration: FADE_IN, useNativeDriver: true }),
      Animated.delay(HOLD),
      Animated.timing(opacity, { toValue: 0, duration: FADE_OUT, useNativeDriver: true }),
    ]);
    seq.start(({ finished }) => { if (finished) setIdx((i) => i + 1); });
    return () => seq.stop();
  }, [idx, opacity]);

  const done = idx >= LINES.length;

  return (
    <Pressable style={[styles.root, { backgroundColor: theme.colors.background }]}
      onPress={done ? undefined : () => setIdx((i) => Math.min(LINES.length, i + 1))}>
      <View style={styles.content}>
        {!done ? (
          <Animated.Text
            style={[styles.line, { opacity, color: theme.colors.onBackground }]}>
            {LINES[idx]}
          </Animated.Text>
        ) : (
          <View style={styles.cta}>
            <Text variant="displaySmall" style={[styles.brand, { color: theme.colors.primary }]}>
              LevelUp
            </Text>
            <Text variant="bodyLarge" style={styles.tag}>
              Convierte tu progreso en aventura.
            </Text>
            <Button mode="contained" onPress={onFinish} style={styles.btn}>
              Comenzar
            </Button>
          </View>
        )}
      </View>
      {!done && (
        <Text style={styles.skip} onPress={() => setIdx(LINES.length)}>
          Saltar
        </Text>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: 32 },
  content: { alignItems: 'center', justifyContent: 'center', minHeight: 120 },
  line: { fontSize: 26, fontWeight: '500', textAlign: 'center' },
  cta: { alignItems: 'center', gap: 16 },
  brand: { fontWeight: '800', letterSpacing: 1 },
  tag: { opacity: 0.75, textAlign: 'center', marginBottom: 12 },
  btn: { minWidth: 200 },
  skip: { position: 'absolute', bottom: 40, opacity: 0.5, padding: 12 },
});
