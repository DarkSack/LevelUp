import React, { useEffect, useMemo, useRef, useState } from 'react';
import { View, StyleSheet, ScrollView, Pressable, Modal, Animated, Easing } from 'react-native';
import { Text, ActivityIndicator, ProgressBar, useTheme, Chip, Button } from 'react-native-paper';
import { useAuth } from '../store/auth';
import { MOCK_MODE } from '../config';
import { mockState } from '../mocks/data';
import { gqlClient } from '../api/graphql';
import { ACHIEVEMENTS_QUERY } from '../api/operations';

interface Achievement {
  id: string; slug: string; title: string; description: string;
  emoji: string | null; tier: number; hidden: boolean;
  unlocked?: boolean;
}

const TIER_COLOR: Record<number, string> = {
  1: '#94A3B8',   // silver-ish
  2: '#F59E0B',   // gold
  3: '#8B5CF6',   // epic purple
};
const TIER_LABEL: Record<number, string> = {
  1: 'Común',
  2: 'Raro',
  3: 'Épico',
};

type Filter = 'all' | 'unlocked' | 'locked';

export function AchievementsScreen() {
  const { user } = useAuth();
  const theme = useTheme();
  const [all, setAll] = useState<Achievement[]>([]);
  const [unlocked, setUnlocked] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<Filter>('all');
  const [detail, setDetail] = useState<Achievement | null>(null);
  const seenRef = useRef<Set<string>>(new Set());
  const [freshUnlocks, setFreshUnlocks] = useState<Set<string>>(new Set());

  useEffect(() => {
    (async () => {
      if (MOCK_MODE) {
        setAll(mockState.achievements as Achievement[]);
        const nowUnlocked = new Set(mockState.achievements.filter((a) => a.unlocked).map((a) => a.id));
        const fresh = new Set<string>();
        for (const id of nowUnlocked) {
          if (!seenRef.current.has(id)) fresh.add(id);
        }
        setUnlocked(nowUnlocked);
        setFreshUnlocks(fresh);
        for (const id of nowUnlocked) seenRef.current.add(id);
        setLoading(false);
        return;
      }
      try {
        const res = await gqlClient.query(ACHIEVEMENTS_QUERY, {}, { requestPolicy: 'network-only' }).toPromise();
        const list = (res.data?.achievements ?? []) as (Achievement & { unlocked: boolean })[];
        setAll(list);
        const nowUnlocked = new Set(list.filter((a) => a.unlocked).map((a) => a.id));
        const fresh = new Set<string>();
        for (const id of nowUnlocked) if (!seenRef.current.has(id)) fresh.add(id);
        setUnlocked(nowUnlocked);
        setFreshUnlocks(fresh);
        for (const id of nowUnlocked) seenRef.current.add(id);
      } finally { setLoading(false); }
    })();
  }, [user]);

  const totalVisible  = all.filter((a) => !a.hidden || unlocked.has(a.id)).length;
  const unlockedCount = all.filter((a) => unlocked.has(a.id)).length;
  const progress      = totalVisible === 0 ? 0 : unlockedCount / all.length;

  const filtered = useMemo(() => {
    return all.filter((a) => {
      if (filter === 'unlocked') return unlocked.has(a.id);
      if (filter === 'locked')   return !unlocked.has(a.id);
      return true;
    });
  }, [all, unlocked, filter]);

  if (loading) return <View style={styles.center}><ActivityIndicator size="large" /></View>;

  return (
    <>
      <ScrollView contentContainerStyle={styles.root}>
        <Text variant="headlineMedium" style={styles.title}>Logros</Text>

        <View style={styles.summary}>
          <Text variant="titleMedium" style={{ color: theme.colors.primary }}>
            {unlockedCount} <Text style={styles.summaryTotal}>/ {all.length}</Text>
          </Text>
          <Text style={styles.summaryLabel}>desbloqueados</Text>
          <ProgressBar progress={progress} style={styles.summaryBar} />
        </View>

        <View style={styles.filters}>
          {(['all', 'unlocked', 'locked'] as Filter[]).map((f) => (
            <Chip key={f}
              selected={filter === f}
              onPress={() => setFilter(f)}
              style={styles.chip}>
              {f === 'all' ? 'Todos' : f === 'unlocked' ? 'Desbloqueados' : 'Bloqueados'}
            </Chip>
          ))}
        </View>

        <View style={styles.grid}>
          {filtered.map((a) => (
            <AchievementCell key={a.id}
              achievement={a}
              unlocked={unlocked.has(a.id)}
              fresh={freshUnlocks.has(a.id)}
              onPress={() => setDetail(a)} />
          ))}
        </View>
      </ScrollView>

      <Modal visible={!!detail} transparent animationType="fade"
        onRequestClose={() => setDetail(null)}>
        <Pressable style={styles.modalBg} onPress={() => setDetail(null)}>
          <Pressable style={[styles.modalCard, { backgroundColor: theme.colors.surface }]}>
            {detail && (() => {
              const got = unlocked.has(detail.id);
              const isSecret = detail.hidden && !got;
              const tierColor = TIER_COLOR[detail.tier] ?? '#64748B';
              return (
                <>
                  <View style={[styles.modalEmblem, {
                    backgroundColor: got ? tierColor + '22' : 'rgba(148,163,184,0.2)',
                    borderColor: got ? tierColor : 'rgba(148,163,184,0.35)',
                  }]}>
                    <Text style={styles.modalEmoji}>
                      {isSecret ? '?' : (detail.emoji ?? '🏅')}
                    </Text>
                  </View>
                  <Text variant="titleLarge" style={styles.modalTitle}>
                    {isSecret ? '???' : detail.title}
                  </Text>
                  <View style={[styles.tierPill, { backgroundColor: tierColor + '33', marginTop: 6 }]}>
                    <Text style={[styles.tierText, { color: tierColor }]}>
                      {TIER_LABEL[detail.tier] ?? '—'}
                    </Text>
                  </View>
                  <Text style={styles.modalDesc}>
                    {isSecret ? 'Sigue jugando para descubrirlo.' : detail.description}
                  </Text>
                  <Text style={styles.modalStatus}>
                    {got ? '✅ Desbloqueado' : '🔒 Bloqueado'}
                  </Text>
                  <Button mode="contained" onPress={() => setDetail(null)} style={styles.modalBtn}>
                    Cerrar
                  </Button>
                </>
              );
            })()}
          </Pressable>
        </Pressable>
      </Modal>
    </>
  );
}

interface CellProps {
  achievement: Achievement;
  unlocked: boolean;
  fresh: boolean;
  onPress: () => void;
}

function AchievementCell({ achievement: a, unlocked, fresh, onPress }: CellProps) {
  const scale = useRef(new Animated.Value(1)).current;
  const glow  = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    if (!fresh) return;
    scale.setValue(0.6);
    glow.setValue(0);
    Animated.sequence([
      Animated.spring(scale, { toValue: 1.15, friction: 4, tension: 120, useNativeDriver: true }),
      Animated.spring(scale, { toValue: 1,    friction: 5, tension: 100, useNativeDriver: true }),
    ]).start();
    Animated.sequence([
      Animated.timing(glow, { toValue: 1, duration: 500, easing: Easing.out(Easing.quad), useNativeDriver: false }),
      Animated.timing(glow, { toValue: 0, duration: 900, delay: 400, easing: Easing.in(Easing.quad), useNativeDriver: false }),
    ]).start();
  }, [fresh, scale, glow]);

  const isSecret  = a.hidden && !unlocked;
  const tierColor = TIER_COLOR[a.tier] ?? '#64748B';
  const shadowOpacity = glow.interpolate({ inputRange: [0, 1], outputRange: [0, 0.9] });

  return (
    <Animated.View
      style={{
        width: '33.333%',
        transform: [{ scale }],
        shadowColor: tierColor, shadowOffset: { width: 0, height: 0 },
        shadowRadius: 16, shadowOpacity,
        elevation: fresh ? 8 : 0,
      }}>
      <Pressable onPress={onPress}
        style={[
          styles.cell,
          { borderColor: unlocked ? tierColor : 'rgba(148,163,184,0.25)' },
          !unlocked && styles.locked,
        ]}>
        <View style={[styles.emblem, { backgroundColor: unlocked ? tierColor + '22' : 'rgba(148,163,184,0.15)' }]}>
          <Text style={styles.emblemEmoji}>
            {isSecret ? '?' : (a.emoji ?? '🏅')}
          </Text>
        </View>
        <Text numberOfLines={1} style={styles.cellTitle}>
          {isSecret ? '???' : a.title}
        </Text>
        <View style={[styles.tierPill, { backgroundColor: tierColor + '33' }]}>
          <Text style={[styles.tierText, { color: tierColor }]}>
            {TIER_LABEL[a.tier] ?? '—'}
          </Text>
        </View>
        {fresh && <View style={[styles.newRibbon, { backgroundColor: tierColor }]}>
          <Text style={styles.newRibbonText}>NUEVO</Text>
        </View>}
      </Pressable>
    </Animated.View>
  );
}

const GAP = 12;

const styles = StyleSheet.create({
  root: { padding: 16, paddingTop: 56, paddingBottom: 32 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  title: { fontWeight: '700', marginBottom: 12 },

  summary: { marginBottom: 16 },
  summaryTotal: { opacity: 0.5, fontWeight: '400' },
  summaryLabel: { opacity: 0.6, fontSize: 11, textTransform: 'uppercase', letterSpacing: 1 },
  summaryBar: { marginTop: 8, height: 6, borderRadius: 3 },

  filters: { flexDirection: 'row', marginBottom: 12 },
  chip: { marginRight: 8 },

  grid: { flexDirection: 'row', flexWrap: 'wrap', marginHorizontal: -GAP / 2 },
  cell: {
    padding: 8,
    marginBottom: GAP,
    borderWidth: 1.5,
    borderRadius: 14,
    alignItems: 'center',
    marginHorizontal: 0,
    overflow: 'hidden',
  },
  newRibbon: {
    position: 'absolute', top: 6, right: -18,
    paddingHorizontal: 22, paddingVertical: 2,
    transform: [{ rotate: '30deg' }],
  },
  newRibbonText: {
    color: '#fff', fontSize: 9, fontWeight: '800', letterSpacing: 1,
  },
  locked: { opacity: 0.55 },
  emblem: {
    width: 56, height: 56, borderRadius: 28,
    alignItems: 'center', justifyContent: 'center',
    marginTop: 4, marginBottom: 8,
  },
  emblemEmoji: { fontSize: 28 },
  cellTitle: { fontSize: 12, fontWeight: '600', textAlign: 'center' },
  tierPill: { paddingHorizontal: 8, paddingVertical: 2, borderRadius: 999, marginTop: 6 },
  tierText: { fontSize: 9, fontWeight: '700', letterSpacing: 1, textTransform: 'uppercase' },

  modalBg: { flex: 1, backgroundColor: 'rgba(0,0,0,0.55)', alignItems: 'center', justifyContent: 'center', padding: 24 },
  modalCard: { padding: 24, borderRadius: 20, alignItems: 'center', width: '100%', maxWidth: 340 },
  modalEmblem: { width: 96, height: 96, borderRadius: 48, borderWidth: 2, alignItems: 'center', justifyContent: 'center', marginBottom: 12 },
  modalEmoji: { fontSize: 48 },
  modalTitle: { fontWeight: '700', textAlign: 'center' },
  modalDesc: { marginTop: 12, textAlign: 'center', opacity: 0.85 },
  modalStatus: { marginTop: 12, fontWeight: '600' },
  modalBtn: { marginTop: 16, minWidth: 160 },
});
