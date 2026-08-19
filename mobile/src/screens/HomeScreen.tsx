import React, { useCallback, useEffect, useState } from 'react';
import { View, StyleSheet, ScrollView, RefreshControl } from 'react-native';
import {
  Text, Card, Button, ProgressBar, ActivityIndicator, Snackbar, useTheme,
} from 'react-native-paper';
import { LinearGradient } from 'expo-linear-gradient';
import { useAuth } from '../store/auth';
import {
  fetchTodayChallenges, completeChallenge, requestDailyGeneration,
} from '../api/challenges';
import { newUuid } from '../utils/uuid';
import { categoryStyle, palette } from '../theme';
import type { UserChallenge } from '../types';
import { Confetti } from '../components/Confetti';
import { XpBurst } from '../components/XpBurst';
import { LevelUpOverlay } from '../components/LevelUpOverlay';
import { AnimatedBar } from '../components/AnimatedBar';
import { AchievementUnlockOverlay } from '../components/AchievementUnlockOverlay';
import type { UnlockInfo } from '../api/challenges';

const xpForLevel = (l: number) => 50 * l * l + 50 * l;

export function HomeScreen() {
  const { user, profile, stats, refreshProfile } = useAuth();
  const theme = useTheme();
  const [challenges, setChallenges] = useState<UserChallenge[]>([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [snack, setSnack] = useState<string | null>(null);
  const [burst, setBurst] = useState<{ xp: number; color: string; key: number } | null>(null);
  const [showConfetti, setShowConfetti] = useState(false);
  const [levelUp, setLevelUp] = useState<number | null>(null);
  const [unlockQueue, setUnlockQueue] = useState<UnlockInfo[]>([]);

  const load = useCallback(async () => {
    if (!user) return;
    setLoading(true);
    try {
      let list = await fetchTodayChallenges(user.id);
      if (list.length === 0) {
        await requestDailyGeneration(user.id).catch(() => {});
        list = await fetchTodayChallenges(user.id);
      }
      setChallenges(list);
    } finally { setLoading(false); }
  }, [user]);

  useEffect(() => { load(); }, [load]);

  const complete = async (uc: UserChallenge) => {
    setBusyId(uc.id);
    const prevLevel = stats?.level ?? 1;
    try {
      const res = await completeChallenge(uc.id, newUuid());
      const color = uc.category ? categoryStyle[uc.category].color : theme.colors.primary;
      if (res.xp_gained && res.xp_gained > 0) {
        setBurst({ xp: res.xp_gained, color, key: Date.now() });
        setShowConfetti(true);
      }
      await Promise.all([load(), refreshProfile()]);
      if (res.level && res.level > prevLevel) {
        setTimeout(() => setLevelUp(res.level!), 900);
      }
      if (res.unlocks && res.unlocks.length > 0) {
        // Queue after level-up so it plays next.
        setTimeout(() => setUnlockQueue((q) => [...q, ...res.unlocks!]),
          res.level && res.level > prevLevel ? 1600 : 900);
      }
    } catch {
      setSnack('No se pudo completar. Reintenta.');
    } finally { setBusyId(null); }
  };

  if (loading && challenges.length === 0) {
    return <View style={styles.center}><ActivityIndicator size="large" /></View>;
  }

  const level = stats?.level ?? 1;
  const nextThreshold = xpForLevel(level + 1) - xpForLevel(level);
  const xpProgress = stats ? Math.min(1, stats.xp_current_level / nextThreshold) : 0;
  const doneToday = challenges.filter((c) => c.status === 'completed').length;

  return (
    <>
      <ScrollView contentContainerStyle={styles.root}
        refreshControl={<RefreshControl refreshing={loading} onRefresh={load} />}>

        <Text variant="labelLarge" style={styles.greet}>
          Hola, {profile?.display_name ?? 'Player'}
        </Text>

        {/* Hero card ------------------------------------------------------ */}
        <View style={styles.heroWrap}>
          <LinearGradient
            colors={[palette.electricBlue, '#6366F1']}
            start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }}
            style={styles.hero}>
            <View style={styles.rowBetween}>
              <View>
                <Text style={styles.heroLabel}>NIVEL</Text>
                <Text style={styles.heroLevel}>{level}</Text>
              </View>
              <View style={styles.streakBadge}>
                <Text style={styles.streakFlame}>🔥</Text>
                <Text style={styles.streakDays}>{stats?.streak_days ?? 0}</Text>
                <Text style={styles.streakUnit}>días</Text>
              </View>
            </View>
            <AnimatedBar
              progress={xpProgress}
              color="#FFFFFF"
              trackColor="rgba(255,255,255,0.25)"
              style={styles.heroBar} />
            <Text style={styles.heroXp}>
              {stats?.xp_current_level ?? 0} / {nextThreshold} XP al siguiente nivel
            </Text>
          </LinearGradient>
        </View>

        {/* Today summary --------------------------------------------------- */}
        <View style={styles.summary}>
          <SummaryStat value={String(doneToday)}                 label="hoy" />
          <SummaryStat value={String(challenges.length)}         label="misiones" />
          <SummaryStat value={String(stats?.challenges_completed ?? 0)} label="totales" />
        </View>

        <Text variant="titleMedium" style={styles.section}>🌅 Misiones de hoy</Text>

        {challenges.length === 0 && (
          <Text style={styles.empty}>Aún no hay misiones. Desliza para refrescar.</Text>
        )}

        {challenges.map((c) => {
          const cat = c.category ? categoryStyle[c.category] : undefined;
          const color = cat?.color ?? theme.colors.primary;
          const isDone = c.status === 'completed';
          const progress = c.target_value
            ? Math.min(1, (c.progress_value ?? 0) / c.target_value)
            : (isDone ? 1 : 0);

          return (
            <Card key={c.id} style={[styles.card, isDone && styles.cardDone]}>
              <View style={[styles.stripe, { backgroundColor: color }]} />
              <Card.Content style={styles.cardContent}>
                <View style={styles.rowBetween}>
                  <View style={styles.cardTitleWrap}>
                    <Text style={styles.cardEmoji}>{cat?.emoji ?? '⭐'}</Text>
                    <View style={{ flex: 1 }}>
                      <Text variant="titleMedium">{c.title}</Text>
                      {cat && <Text style={[styles.cardCat, { color }]}>{cat.label}</Text>}
                    </View>
                  </View>
                  <View style={[styles.xpPill, { backgroundColor: color + '22' }]}>
                    <Text style={[styles.xpPillText, { color }]}>+{c.xp_reward} XP</Text>
                  </View>
                </View>

                <Text style={styles.desc}>{c.description}</Text>

                {c.target_value != null && (
                  <View style={styles.progressWrap}>
                    <ProgressBar progress={progress} color={color} style={styles.progress} />
                    <Text style={styles.progressLabel}>
                      {c.progress_value} / {c.target_value}
                    </Text>
                  </View>
                )}

                <View style={styles.actions}>
                  {isDone
                    ? <Button icon="check" disabled>Completado</Button>
                    : <Button mode="contained" buttonColor={color}
                        loading={busyId === c.id} disabled={busyId === c.id}
                        onPress={() => complete(c)}>Completar</Button>}
                </View>
              </Card.Content>
            </Card>
          );
        })}
      </ScrollView>

      {showConfetti && (
        <Confetti count={70} duration={2400} onDone={() => setShowConfetti(false)} />
      )}
      {burst && (
        <XpBurst
          key={burst.key}
          xp={burst.xp}
          color={burst.color}
          onDone={() => setBurst(null)}
        />
      )}
      <LevelUpOverlay
        visible={levelUp != null}
        level={levelUp ?? 1}
        onDismiss={() => setLevelUp(null)}
      />
      <AchievementUnlockOverlay
        visible={unlockQueue.length > 0}
        title={unlockQueue[0]?.title ?? ''}
        description={unlockQueue[0]?.description}
        emoji={unlockQueue[0]?.emoji}
        tier={unlockQueue[0]?.tier}
        onDismiss={() => setUnlockQueue((q) => q.slice(1))}
      />

      <Snackbar visible={!!snack} onDismiss={() => setSnack(null)} duration={2500}>
        {snack ?? ''}
      </Snackbar>
    </>
  );
}

function SummaryStat({ value, label }: { value: string; label: string }) {
  return (
    <View style={styles.summaryCell}>
      <Text variant="headlineSmall" style={styles.summaryValue}>{value}</Text>
      <Text style={styles.summaryLabel}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { padding: 16, paddingTop: 56, paddingBottom: 32 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },

  greet: { opacity: 0.7, marginBottom: 8 },

  heroWrap: { borderRadius: 20, overflow: 'hidden', marginBottom: 16 },
  hero: { padding: 20 },
  rowBetween: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  heroLabel: { color: 'rgba(255,255,255,0.7)', fontSize: 11, letterSpacing: 2, fontWeight: '700' },
  heroLevel: { color: '#fff', fontSize: 48, fontWeight: '800', lineHeight: 52 },
  heroBar: { marginTop: 16, height: 8, borderRadius: 4, backgroundColor: 'rgba(255,255,255,0.25)' },
  heroXp: { color: 'rgba(255,255,255,0.85)', marginTop: 8, fontSize: 12 },

  streakBadge: {
    backgroundColor: 'rgba(0,0,0,0.25)',
    borderRadius: 16, paddingHorizontal: 14, paddingVertical: 8,
    alignItems: 'center',
  },
  streakFlame: { fontSize: 24 },
  streakDays: { color: '#fff', fontSize: 20, fontWeight: '700', marginTop: -2 },
  streakUnit: { color: 'rgba(255,255,255,0.7)', fontSize: 10, letterSpacing: 1 },

  summary: { flexDirection: 'row', marginBottom: 20 },
  summaryCell: { flex: 1, alignItems: 'center' },
  summaryValue: { fontWeight: '700' },
  summaryLabel: { opacity: 0.6, fontSize: 11, textTransform: 'uppercase', letterSpacing: 1 },

  section: { marginBottom: 12, marginTop: 4, fontWeight: '600' },

  card: { marginBottom: 12, overflow: 'hidden' },
  cardDone: { opacity: 0.65 },
  stripe: { position: 'absolute', left: 0, top: 0, bottom: 0, width: 4 },
  cardContent: { paddingLeft: 16 },
  cardTitleWrap: { flexDirection: 'row', alignItems: 'center', flex: 1, gap: 10 },
  cardEmoji: { fontSize: 22 },
  cardCat: { fontSize: 11, fontWeight: '700', letterSpacing: 1, textTransform: 'uppercase', marginTop: 2 },

  xpPill: { borderRadius: 999, paddingHorizontal: 10, paddingVertical: 4 },
  xpPillText: { fontSize: 12, fontWeight: '700' },

  desc: { marginTop: 10, opacity: 0.9 },

  progressWrap: { marginTop: 12 },
  progress: { height: 6, borderRadius: 3 },
  progressLabel: { fontSize: 11, opacity: 0.6, marginTop: 4, textAlign: 'right' },

  actions: { flexDirection: 'row', justifyContent: 'flex-end', marginTop: 14 },
  empty: { opacity: 0.6, marginTop: 24, textAlign: 'center' },
});
