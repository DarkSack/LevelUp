import React, { useMemo, useState } from 'react';
import { View, StyleSheet, ScrollView, Pressable, Modal } from 'react-native';
import { Text, Button, Divider, useTheme, Chip } from 'react-native-paper';
import { LinearGradient } from 'expo-linear-gradient';
import { AnimatedBar } from '../components/AnimatedBar';
import { useAuth } from '../store/auth';
import { MOCK_MODE } from '../config';
import { mockActivity, mockTitles, mockState } from '../mocks/data';
import { categoryStyle, palette } from '../theme';
import type { Category } from '../types';

const xpForLevel = (l: number) => 50 * l * l + 50 * l;

export function ProfileScreen() {
  const { profile, stats, signOut } = useAuth();
  const theme = useTheme();
  const [titlesOpen, setTitlesOpen] = useState(false);
  const [titles, setTitles] = useState(MOCK_MODE ? mockTitles : []);

  const activeTitle = titles.find((t) => t.active);

  const level = stats?.level ?? 1;
  const nextThr = xpForLevel(level + 1) - xpForLevel(level);
  const xpPct   = stats ? Math.min(1, stats.xp_current_level / nextThr) : 0;

  const initials = (profile?.display_name ?? '?')
    .split(' ').map((s) => s[0]).slice(0, 2).join('').toUpperCase();
  const avatarEmoji = profile?.avatar_emoji ?? null;
  const avatarColor = profile?.avatar_color ?? palette.electricBlue;

  const activity = MOCK_MODE ? mockActivity : [];

  const setActive = (slug: string) => {
    setTitles((ts) => ts.map((t) => ({ ...t, active: t.slug === slug })));
  };

  return (
    <>
      <ScrollView contentContainerStyle={styles.root}>

        {/* Hero ---------------------------------------------------------- */}
        <View style={styles.heroWrap}>
          <LinearGradient
            colors={[avatarColor, '#8B5CF6']}
            start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }}
            style={styles.hero}>
            <View style={styles.avatarWrap}>
              <View style={styles.avatar}>
                {avatarEmoji
                  ? <Text style={{ fontSize: 44 }}>{avatarEmoji}</Text>
                  : <Text style={styles.avatarText}>{initials}</Text>}
              </View>
              <View style={styles.levelBadge}>
                <Text style={styles.levelBadgeText}>{level}</Text>
              </View>
            </View>
            <Text style={styles.heroName}>{profile?.display_name ?? 'Player'}</Text>
            <Text style={styles.heroUser}>@{profile?.username ?? 'user'}</Text>

            <Pressable style={styles.titlePill} onPress={() => setTitlesOpen(true)}>
              <Text style={styles.titlePillText}>
                {activeTitle ? activeTitle.label : 'Elegir título'}
              </Text>
              <Text style={styles.titlePillHint}>· cambiar</Text>
            </Pressable>

            <View style={styles.heroBarWrap}>
              <AnimatedBar
                progress={xpPct}
                color="#fff"
                trackColor="rgba(255,255,255,0.25)"
                height={8} />
              <Text style={styles.heroBarLabel}>
                {stats?.xp_current_level ?? 0} / {nextThr} XP
              </Text>
            </View>
          </LinearGradient>
        </View>

        {/* Stat grid ----------------------------------------------------- */}
        <View style={styles.statGrid}>
          <StatCard label="XP total"        value={stats?.xp_total ?? 0} accent={palette.electricBlue} />
          <StatCard label="Racha"           value={`🔥 ${stats?.streak_days ?? 0}`} accent="#F59E0B" />
          <StatCard label="Retos hechos"    value={stats?.challenges_completed ?? 0} accent={palette.mint} />
          <StatCard label="Categorías"      value={profile?.categories?.length ?? 0} accent="#8B5CF6" />
        </View>

        {/* Categories ---------------------------------------------------- */}
        <SectionTitle>Categorías activas</SectionTitle>
        <View style={styles.catRow}>
          {(profile?.categories ?? []).map((c: Category) => {
            const s = categoryStyle[c];
            return (
              <Chip key={c} style={[styles.catChip, { borderColor: s.color }]}
                textStyle={{ color: s.color, fontWeight: '600' }}
                mode="outlined">
                {s.emoji}  {s.label}
              </Chip>
            );
          })}
          {(profile?.categories ?? []).length === 0 && (
            <Text style={styles.dim}>Ninguna categoría todavía.</Text>
          )}
        </View>

        {/* Recent activity ---------------------------------------------- */}
        <SectionTitle>Actividad reciente</SectionTitle>
        <View style={styles.timeline}>
          {activity.map((e, i) => {
            const cat = e.category ? categoryStyle[e.category] : undefined;
            const dot = cat?.color ??
              (e.kind === 'level_up' ? '#F59E0B' :
               e.kind === 'achievement' ? '#8B5CF6' :
               e.kind === 'streak' ? '#F97056' : '#94A3B8');
            return (
              <View key={e.id} style={styles.timelineRow}>
                <View style={styles.timelineLeft}>
                  <View style={[styles.timelineDot, { backgroundColor: dot }]} />
                  {i < activity.length - 1 && <View style={styles.timelineLine} />}
                </View>
                <View style={styles.timelineContent}>
                  <View style={styles.timelineHeader}>
                    <Text variant="titleSmall">{e.title}</Text>
                    {e.xp ? <Text style={[styles.timelineXp, { color: dot }]}>+{e.xp} XP</Text> : null}
                  </View>
                  {e.detail && <Text style={styles.dim}>{e.detail}</Text>}
                  <Text style={styles.timelineWhen}>{fmt(e.when)}</Text>
                </View>
              </View>
            );
          })}
          {activity.length === 0 && (
            <Text style={styles.dim}>Aún no hay actividad.</Text>
          )}
        </View>

        <Divider style={{ marginVertical: 20 }} />
        <Button mode="outlined" onPress={signOut}>Cerrar sesión</Button>
      </ScrollView>

      {/* Title picker ---------------------------------------------------- */}
      <Modal visible={titlesOpen} transparent animationType="slide"
        onRequestClose={() => setTitlesOpen(false)}>
        <Pressable style={styles.modalBg} onPress={() => setTitlesOpen(false)}>
          <Pressable style={[styles.sheet, { backgroundColor: theme.colors.surface }]}>
            <Text variant="titleLarge" style={styles.sheetTitle}>Elegir título</Text>
            {titles.map((t) => (
              <Pressable key={t.slug}
                disabled={!t.unlocked}
                onPress={() => { setActive(t.slug); setTitlesOpen(false); }}
                style={[
                  styles.titleRow,
                  t.active && { backgroundColor: theme.colors.primary + '18' },
                  !t.unlocked && styles.titleRowLocked,
                ]}>
                <Text variant="titleMedium" style={t.active && { color: theme.colors.primary }}>
                  {t.label}
                </Text>
                <Text style={styles.dim}>
                  {t.active ? 'Activo' : t.unlocked ? 'Disponible' : '🔒 Bloqueado'}
                </Text>
              </Pressable>
            ))}
            <Button mode="text" onPress={() => setTitlesOpen(false)} style={{ marginTop: 8 }}>
              Cerrar
            </Button>
          </Pressable>
        </Pressable>
      </Modal>
    </>
  );
}

function SectionTitle({ children }: { children: React.ReactNode }) {
  return <Text variant="titleMedium" style={styles.section}>{children}</Text>;
}

function StatCard({ label, value, accent }: { label: string; value: string | number; accent: string }) {
  return (
    <View style={[styles.statCard, { borderColor: accent + '55' }]}>
      <Text style={[styles.statValue, { color: accent }]}>{value}</Text>
      <Text style={styles.statLabel}>{label}</Text>
    </View>
  );
}

function fmt(iso: string) {
  const d = new Date(iso);
  const now = new Date();
  const days = Math.floor((now.getTime() - d.getTime()) / 86400000);
  const hh = d.getHours().toString().padStart(2, '0');
  const mm = d.getMinutes().toString().padStart(2, '0');
  if (days === 0) return `hoy · ${hh}:${mm}`;
  if (days === 1) return `ayer · ${hh}:${mm}`;
  return `hace ${days} días`;
}

const styles = StyleSheet.create({
  root: { padding: 16, paddingTop: 48, paddingBottom: 40 },

  heroWrap: { borderRadius: 20, overflow: 'hidden', marginBottom: 20 },
  hero: { padding: 20, alignItems: 'center' },
  avatarWrap: { marginTop: 4 },
  avatar: {
    width: 88, height: 88, borderRadius: 44,
    backgroundColor: 'rgba(255,255,255,0.18)',
    borderWidth: 3, borderColor: 'rgba(255,255,255,0.9)',
    alignItems: 'center', justifyContent: 'center',
  },
  avatarText: { fontSize: 34, fontWeight: '800', color: '#fff' },
  levelBadge: {
    position: 'absolute', bottom: -4, right: -4,
    minWidth: 34, height: 34, borderRadius: 17,
    backgroundColor: '#F59E0B',
    alignItems: 'center', justifyContent: 'center',
    borderWidth: 3, borderColor: '#8B5CF6',
    paddingHorizontal: 6,
  },
  levelBadgeText: { color: '#fff', fontWeight: '800', fontSize: 14 },

  heroName: { color: '#fff', fontSize: 22, fontWeight: '800', marginTop: 12 },
  heroUser: { color: 'rgba(255,255,255,0.7)', fontSize: 13, marginTop: 2 },

  titlePill: {
    flexDirection: 'row', alignItems: 'center', gap: 6,
    backgroundColor: 'rgba(0,0,0,0.25)',
    paddingHorizontal: 14, paddingVertical: 6, borderRadius: 999,
    marginTop: 12,
  },
  titlePillText: { color: '#fff', fontWeight: '600' },
  titlePillHint: { color: 'rgba(255,255,255,0.6)', fontSize: 11 },

  heroBarWrap: { width: '100%', marginTop: 20 },
  heroBarBg: { height: 8, borderRadius: 4, backgroundColor: 'rgba(255,255,255,0.25)', overflow: 'hidden' },
  heroBarFill: { height: 8, backgroundColor: '#fff' },
  heroBarLabel: { color: 'rgba(255,255,255,0.85)', fontSize: 11, marginTop: 6, textAlign: 'right' },

  statGrid: { flexDirection: 'row', flexWrap: 'wrap', marginHorizontal: -6, marginBottom: 8 },
  statCard: {
    width: '50%',
    padding: 12,
    borderRadius: 14,
    borderWidth: 1.5,
    marginHorizontal: 0,
    marginBottom: 12,
  },
  statValue: { fontSize: 22, fontWeight: '800' },
  statLabel: { opacity: 0.7, marginTop: 4, fontSize: 12 },

  section: { fontWeight: '700', marginTop: 12, marginBottom: 10 },

  catRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginBottom: 8 },
  catChip: { borderWidth: 1.5, marginRight: 6, marginBottom: 6 },

  timeline: {},
  timelineRow: { flexDirection: 'row' },
  timelineLeft: { width: 28, alignItems: 'center' },
  timelineDot: { width: 12, height: 12, borderRadius: 6, marginTop: 6 },
  timelineLine: { flex: 1, width: 2, backgroundColor: 'rgba(148,163,184,0.3)', marginTop: 2 },
  timelineContent: { flex: 1, paddingBottom: 16 },
  timelineHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  timelineXp: { fontWeight: '700', fontSize: 12 },
  timelineWhen: { opacity: 0.5, fontSize: 11, marginTop: 4 },

  dim: { opacity: 0.65 },

  modalBg: { flex: 1, backgroundColor: 'rgba(0,0,0,0.55)', justifyContent: 'flex-end' },
  sheet: { padding: 20, borderTopLeftRadius: 20, borderTopRightRadius: 20 },
  sheetTitle: { fontWeight: '700', marginBottom: 12 },
  titleRow: {
    flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
    paddingHorizontal: 14, paddingVertical: 12, borderRadius: 12, marginBottom: 6,
  },
  titleRowLocked: { opacity: 0.5 },
});
