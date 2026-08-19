import React, { useState } from 'react';
import { View, StyleSheet, ScrollView, Pressable, KeyboardAvoidingView, Platform } from 'react-native';
import { Text, TextInput, Chip, Button, useTheme } from 'react-native-paper';
import { LinearGradient } from 'expo-linear-gradient';
import { useAuth } from '../store/auth';
import type { OnboardingData } from '../store/auth';
import { AVAILABLE_CATEGORIES } from '../mocks/data';
import type { Category } from '../types';

const AVATAR_COLORS = [
  '#3B82F6', '#4ADE80', '#F59E0B', '#F97056',
  '#8B5CF6', '#14B8A6', '#EC4899', '#64748B',
];
const AVATAR_EMOJIS = [
  '🎮', '⚔️', '🛡️', '🔥', '⭐', '🌙', '🚀', '🎯',
  '🦊', '🐺', '🦁', '🐉', '🧙', '🥷', '🧝', '🧑‍🎤',
];

const STEPS = 5;

export function OnboardingScreen() {
  const completeOnboarding = useAuth((s) => s.completeOnboarding);
  const theme = useTheme();

  const [step, setStep] = useState(0);
  const [busy, setBusy] = useState(false);

  const [displayName, setDisplayName] = useState('');
  const [username, setUsername]       = useState('');
  const [pronouns, setPronouns]       = useState<string | null>(null);
  const [age, setAge]                 = useState<string>('');
  const [emoji, setEmoji]             = useState(AVATAR_EMOJIS[0]);
  const [color, setColor]             = useState(AVATAR_COLORS[0]);
  const [categories, setCategories]   = useState<Category[]>([]);

  const toggleCat = (c: Category) =>
    setCategories((s) => s.includes(c) ? s.filter((x) => x !== c) : [...s, c]);

  const canAdvance = () => {
    switch (step) {
      case 0: return displayName.trim().length >= 2 && username.trim().length >= 3;
      case 1: return true;                              // pronouns/age optional
      case 2: return !!emoji && !!color;
      case 3: return categories.length > 0;
      default: return true;
    }
  };

  const finish = async () => {
    setBusy(true);
    try {
      const data: OnboardingData = {
        display_name: displayName.trim(),
        username: username.trim().toLowerCase().replace(/\s+/g, '_'),
        pronouns: pronouns || null,
        age: age ? parseInt(age, 10) : null,
        avatar_emoji: emoji,
        avatar_color: color,
        categories,
      };
      await completeOnboarding(data);
    } finally { setBusy(false); }
  };

  return (
    <KeyboardAvoidingView style={{ flex: 1 }}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={styles.root}
        keyboardShouldPersistTaps="handled">

        <ProgressDots current={step} total={STEPS} accent={theme.colors.primary} />

        {step === 0 && (
          <StepIdentity
            displayName={displayName} setDisplayName={setDisplayName}
            username={username}       setUsername={setUsername} />
        )}
        {step === 1 && (
          <StepPronouns
            pronouns={pronouns} setPronouns={setPronouns}
            age={age} setAge={setAge} />
        )}
        {step === 2 && (
          <StepAvatar
            emoji={emoji} setEmoji={setEmoji}
            color={color} setColor={setColor}
            displayName={displayName || 'Tú'} />
        )}
        {step === 3 && (
          <StepCategories categories={categories} onToggle={toggleCat} />
        )}
        {step === 4 && (
          <StepWelcome displayName={displayName} emoji={emoji} color={color} />
        )}

        <View style={styles.nav}>
          {step > 0 && (
            <Button mode="text" onPress={() => setStep((s) => s - 1)} disabled={busy}>
              Atrás
            </Button>
          )}
          <View style={{ flex: 1 }} />
          {step < STEPS - 1 ? (
            <Button mode="contained"
              onPress={() => setStep((s) => s + 1)}
              disabled={!canAdvance() || busy}>
              Continuar
            </Button>
          ) : (
            <Button mode="contained" onPress={finish} loading={busy} disabled={busy}>
              Empezar aventura
            </Button>
          )}
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

// ---------------------------------------------------------------------------
// Steps
// ---------------------------------------------------------------------------

function StepIdentity({
  displayName, setDisplayName, username, setUsername,
}: {
  displayName: string; setDisplayName: (v: string) => void;
  username: string;    setUsername: (v: string) => void;
}) {
  return (
    <View>
      <Text variant="headlineMedium" style={styles.title}>Presentémonos</Text>
      <Text style={styles.hint}>¿Cómo quieres que te llamemos?</Text>
      <TextInput mode="outlined" label="Tu nombre"
        value={displayName} onChangeText={setDisplayName}
        style={styles.input} />
      <TextInput mode="outlined" label="Nombre de usuario"
        value={username} onChangeText={setUsername}
        autoCapitalize="none"
        left={<TextInput.Affix text="@" />}
        style={styles.input} />
    </View>
  );
}

function StepPronouns({
  pronouns, setPronouns, age, setAge,
}: {
  pronouns: string | null; setPronouns: (v: string | null) => void;
  age: string;             setAge: (v: string) => void;
}) {
  const opts = ['he/him', 'she/her', 'they/them', 'otro/prefiero no decir'];
  return (
    <View>
      <Text variant="headlineMedium" style={styles.title}>Cuéntanos un poco más</Text>
      <Text style={styles.hint}>Todo esto es opcional.</Text>

      <Text style={styles.label}>Pronombres</Text>
      <View style={styles.chipRow}>
        {opts.map((p) => (
          <Chip key={p}
            selected={pronouns === p}
            onPress={() => setPronouns(pronouns === p ? null : p)}
            style={styles.chip}>
            {p}
          </Chip>
        ))}
      </View>

      <Text style={[styles.label, { marginTop: 20 }]}>Edad</Text>
      <TextInput mode="outlined" label="Edad" keyboardType="number-pad"
        value={age} onChangeText={(t) => setAge(t.replace(/\D/g, '').slice(0, 3))}
        style={styles.input} />
    </View>
  );
}

function StepAvatar({
  emoji, setEmoji, color, setColor, displayName,
}: {
  emoji: string; setEmoji: (v: string) => void;
  color: string; setColor: (v: string) => void;
  displayName: string;
}) {
  return (
    <View>
      <Text variant="headlineMedium" style={styles.title}>Diseña tu personaje</Text>
      <Text style={styles.hint}>Elige un emoji y un color.</Text>

      <View style={styles.previewWrap}>
        <View style={[styles.previewAvatar, { backgroundColor: color + '33', borderColor: color }]}>
          <Text style={styles.previewEmoji}>{emoji}</Text>
        </View>
        <Text style={styles.previewName}>{displayName}</Text>
      </View>

      <Text style={styles.label}>Emoji</Text>
      <View style={styles.grid}>
        {AVATAR_EMOJIS.map((e) => (
          <Pressable key={e}
            onPress={() => setEmoji(e)}
            style={[styles.emojiCell, emoji === e && { borderColor: color, backgroundColor: color + '22' }]}>
            <Text style={styles.emojiCellText}>{e}</Text>
          </Pressable>
        ))}
      </View>

      <Text style={[styles.label, { marginTop: 20 }]}>Color</Text>
      <View style={styles.colorRow}>
        {AVATAR_COLORS.map((c) => (
          <Pressable key={c}
            onPress={() => setColor(c)}
            style={[styles.colorSwatch, { backgroundColor: c },
              color === c && styles.colorSwatchActive]} />
        ))}
      </View>
    </View>
  );
}

function StepCategories({
  categories, onToggle,
}: {
  categories: Category[]; onToggle: (c: Category) => void;
}) {
  return (
    <View>
      <Text variant="headlineMedium" style={styles.title}>¿Qué quieres mejorar?</Text>
      <Text style={styles.hint}>Selecciona una o varias.</Text>

      <View style={styles.catGrid}>
        {AVAILABLE_CATEGORIES.map((c) => {
          const active = categories.includes(c.key);
          return (
            <Pressable key={c.key}
              onPress={() => onToggle(c.key)}
              style={[styles.catCard, active && styles.catCardActive]}>
              <Text style={styles.catEmoji}>{c.emoji}</Text>
              <Text style={styles.catLabel}>{c.label}</Text>
            </Pressable>
          );
        })}
      </View>
    </View>
  );
}

function StepWelcome({
  displayName, emoji, color,
}: {
  displayName: string; emoji: string; color: string;
}) {
  return (
    <View style={styles.welcomeWrap}>
      <LinearGradient
        colors={[color, color + '77']}
        style={styles.welcomeAvatar}
        start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }}>
        <Text style={styles.welcomeEmoji}>{emoji}</Text>
      </LinearGradient>
      <Text variant="headlineMedium" style={styles.welcomeTitle}>
        ¡Bienvenido, {displayName}!
      </Text>
      <Text style={styles.welcomeText}>
        Tu personaje está listo.{'\n'}Cada día tendrás nuevas misiones.
      </Text>
    </View>
  );
}

// ---------------------------------------------------------------------------

function ProgressDots({ current, total, accent }: { current: number; total: number; accent: string }) {
  return (
    <View style={styles.dots}>
      {Array.from({ length: total }).map((_, i) => (
        <View key={i} style={[
          styles.dot,
          i <= current
            ? { backgroundColor: accent, width: i === current ? 24 : 8 }
            : { backgroundColor: 'rgba(148,163,184,0.35)' },
        ]} />
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  root: { padding: 24, paddingTop: 64, paddingBottom: 40 },

  dots: { flexDirection: 'row', justifyContent: 'center', gap: 6, marginBottom: 28 },
  dot: { height: 8, borderRadius: 4 },

  title: { fontWeight: '800', marginBottom: 6 },
  hint:  { opacity: 0.7, marginBottom: 20 },
  label: { fontWeight: '700', marginBottom: 8, marginTop: 4 },

  input: { marginBottom: 12 },
  chipRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  chip: { marginRight: 6, marginBottom: 6 },

  previewWrap: { alignItems: 'center', marginBottom: 24 },
  previewAvatar: {
    width: 96, height: 96, borderRadius: 48,
    borderWidth: 3,
    alignItems: 'center', justifyContent: 'center',
  },
  previewEmoji: { fontSize: 44 },
  previewName: { marginTop: 10, fontSize: 16, fontWeight: '700' },

  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  emojiCell: {
    width: 52, height: 52, borderRadius: 12,
    alignItems: 'center', justifyContent: 'center',
    borderWidth: 1.5, borderColor: 'rgba(148,163,184,0.3)',
    marginRight: 6, marginBottom: 6,
  },
  emojiCellText: { fontSize: 24 },

  colorRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 12 },
  colorSwatch: {
    width: 40, height: 40, borderRadius: 20,
    marginRight: 8, marginBottom: 8,
    borderWidth: 3, borderColor: 'transparent',
  },
  colorSwatchActive: { borderColor: '#fff', transform: [{ scale: 1.1 }] },

  catGrid: { flexDirection: 'row', flexWrap: 'wrap', marginHorizontal: -6 },
  catCard: {
    width: '50%',
    padding: 16, borderRadius: 14,
    borderWidth: 1.5, borderColor: 'rgba(148,163,184,0.3)',
    marginBottom: 10,
    alignItems: 'center',
  },
  catCardActive: { borderColor: '#3B82F6', backgroundColor: 'rgba(59,130,246,0.12)' },
  catEmoji: { fontSize: 32, marginBottom: 6 },
  catLabel: { fontWeight: '600' },

  welcomeWrap: { alignItems: 'center', paddingVertical: 24 },
  welcomeAvatar: {
    width: 140, height: 140, borderRadius: 70,
    alignItems: 'center', justifyContent: 'center',
    marginBottom: 20,
  },
  welcomeEmoji: { fontSize: 72 },
  welcomeTitle: { fontWeight: '800', textAlign: 'center' },
  welcomeText: { textAlign: 'center', opacity: 0.75, marginTop: 8, lineHeight: 22 },

  nav: { flexDirection: 'row', alignItems: 'center', marginTop: 28 },
});
