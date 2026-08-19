import React, { useState } from 'react';
import { View, StyleSheet, ScrollView } from 'react-native';
import { Text, useTheme, SegmentedButtons, Card } from 'react-native-paper';
import { useAuth } from '../store/auth';
import { MOCK_MODE } from '../config';
import {
  mockWeeklyXp, mockCategoryTotals, mockStreakGrid, mockActivity,
} from '../mocks/data';
import { categoryStyle } from '../theme';
import type { Category } from '../types';

const DAY_LABELS = ['L', 'M', 'M', 'J', 'V', 'S', 'D'];

export function ProgressScreen() {
  const { stats } = useAuth();
  const theme = useTheme();
  const [range, setRange] = useState<'week' | 'month'>('week');

  const weekly = MOCK_MODE ? mockWeeklyXp : new Array(7).fill(0);
  const weekTotal = weekly.reduce((a, b) => a + b, 0);
  const weekMax   = Math.max(1, ...weekly);

  const catTotals = MOCK_MODE ? mockCategoryTotals : {} as Record<Category, number>;
  const catEntries = (Object.entries(catTotals) as [Category, number][])
    .filter(([, n]) => n > 0)
    .sort((a, b) => b[1] - a[1]);
  const catTotal = catEntries.reduce((a, [, n]) => a + n, 0) || 1;

  const streakGrid = MOCK_MODE ? mockStreakGrid : [];
  const activeDaysMonth = streakGrid.slice(-4).flat().filter(Boolean).length;

  const recent = MOCK_MODE ? mockActivity.slice(0, 5) : [];

  return (
    <ScrollView contentContainerStyle={styles.root}>
      <Text variant="headlineMedium" style={styles.title}>Progreso</Text>

      <SegmentedButtons
        value={range}
        onValueChange={(v) => setRange(v as 'week' | 'month')}
        buttons={[
          { value: 'week',  label: 'Semana' },
          { value: 'month', label: 'Mes'    },
        ]}
        style={styles.seg}
      />

      {/* Weekly XP bars ---------------------------------------------------- */}
      <Card style={styles.card}>
        <Card.Content>
          <View style={styles.rowBetween}>
            <Text variant="titleMedium">XP esta semana</Text>
            <Text variant="titleMedium" style={{ color: theme.colors.primary }}>
              {weekTotal}
            </Text>
          </View>
          <Text style={styles.dim}>{Math.round(weekTotal / 7)} XP por día en promedio</Text>

          <View style={styles.barsWrap}>
            {weekly.map((v, i) => {
              const isToday = i === weekly.length - 1;
              const h = (v / weekMax) * 100;
              return (
                <View key={i} style={styles.barCol}>
                  <View style={styles.barTrack}>
                    <View style={[
                      styles.barFill,
                      { height: `${h}%`, backgroundColor: isToday ? theme.colors.primary : theme.colors.primary + '88' },
                    ]} />
                  </View>
                  <Text style={[styles.barLabel, isToday && { fontWeight: '800' }]}>
                    {DAY_LABELS[i]}
                  </Text>
                  <Text style={styles.barValue}>{v}</Text>
                </View>
              );
            })}
          </View>
        </Card.Content>
      </Card>

      {/* Category breakdown ----------------------------------------------- */}
      <Card style={styles.card}>
        <Card.Content>
          <Text variant="titleMedium">Retos por categoría</Text>
          <Text style={styles.dim}>Últimos 30 días</Text>

          {/* stacked bar */}
          <View style={styles.stackWrap}>
            {catEntries.map(([cat, n]) => {
              const s = categoryStyle[cat];
              const pct = (n / catTotal) * 100;
              return (
                <View key={cat} style={{ width: `${pct}%`, backgroundColor: s.color, height: '100%' }} />
              );
            })}
          </View>

          <View style={styles.catList}>
            {catEntries.map(([cat, n]) => {
              const s = categoryStyle[cat];
              const pct = Math.round((n / catTotal) * 100);
              return (
                <View key={cat} style={styles.catRow}>
                  <View style={[styles.catDot, { backgroundColor: s.color }]} />
                  <Text style={styles.catLabel}>{s.emoji}  {s.label}</Text>
                  <Text style={styles.catCount}>{n}</Text>
                  <Text style={styles.catPct}>{pct}%</Text>
                </View>
              );
            })}
            {catEntries.length === 0 && (
              <Text style={styles.dim}>Aún no hay datos.</Text>
            )}
          </View>
        </Card.Content>
      </Card>

      {/* Streak calendar --------------------------------------------------- */}
      <Card style={styles.card}>
        <Card.Content>
          <View style={styles.rowBetween}>
            <Text variant="titleMedium">Constancia</Text>
            <Text variant="titleMedium" style={{ color: '#F97056' }}>
              🔥 {stats?.streak_days ?? 0}
            </Text>
          </View>
          <Text style={styles.dim}>
            {activeDaysMonth} días activos en las últimas 4 semanas
          </Text>

          <View style={styles.calWrap}>
            {streakGrid.map((week, wi) => (
              <View key={wi} style={styles.calWeek}>
                {week.map((cell, di) => (
                  <View key={di} style={[
                    styles.calCell,
                    { backgroundColor: cell
                      ? intensity(wi, streakGrid.length)
                      : 'rgba(148,163,184,0.15)' },
                  ]} />
                ))}
              </View>
            ))}
          </View>
          <View style={styles.legend}>
            <Text style={styles.legendLabel}>menos</Text>
            {[0.25, 0.5, 0.75, 1].map((v, i) => (
              <View key={i} style={[styles.legendCell, { backgroundColor: `rgba(74,222,128,${v})` }]} />
            ))}
            <Text style={styles.legendLabel}>más</Text>
          </View>
        </Card.Content>
      </Card>

      {/* Milestones ------------------------------------------------------- */}
      <Card style={styles.card}>
        <Card.Content>
          <Text variant="titleMedium">Hitos recientes</Text>
          <View style={{ marginTop: 8 }}>
            {recent.map((r) => (
              <View key={r.id} style={styles.milestone}>
                <Text style={styles.milestoneEmoji}>
                  {r.kind === 'level_up' ? '⬆️' :
                   r.kind === 'achievement' ? '🏆' :
                   r.kind === 'streak' ? '🔥' : '✅'}
                </Text>
                <View style={{ flex: 1 }}>
                  <Text style={styles.milestoneTitle}>{r.title}</Text>
                  {r.detail && <Text style={styles.dim}>{r.detail}</Text>}
                </View>
                {r.xp ? <Text style={styles.milestoneXp}>+{r.xp}</Text> : null}
              </View>
            ))}
          </View>
        </Card.Content>
      </Card>
    </ScrollView>
  );
}

function intensity(week: number, total: number) {
  // fresher weeks (higher index) render with stronger green
  const t = (week + 1) / total;   // 0..1
  const alpha = 0.25 + t * 0.75;
  return `rgba(74,222,128,${alpha.toFixed(2)})`;
}

const styles = StyleSheet.create({
  root: { padding: 16, paddingTop: 48, paddingBottom: 40 },
  title: { fontWeight: '700', marginBottom: 12 },
  seg: { marginBottom: 16 },
  card: { marginBottom: 14 },

  rowBetween: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  dim: { opacity: 0.65, marginTop: 4, fontSize: 12 },

  barsWrap: { flexDirection: 'row', height: 140, marginTop: 16, alignItems: 'flex-end' },
  barCol: { flex: 1, alignItems: 'center' },
  barTrack: {
    width: 22, height: 110,
    backgroundColor: 'rgba(148,163,184,0.15)',
    borderRadius: 6, overflow: 'hidden',
    justifyContent: 'flex-end',
  },
  barFill: { width: '100%', borderRadius: 6 },
  barLabel: { marginTop: 6, fontSize: 11, opacity: 0.8 },
  barValue: { fontSize: 10, opacity: 0.55 },

  stackWrap: {
    flexDirection: 'row',
    height: 12, borderRadius: 6, overflow: 'hidden',
    marginTop: 14,
    backgroundColor: 'rgba(148,163,184,0.15)',
  },
  catList: { marginTop: 12, gap: 6 },
  catRow: { flexDirection: 'row', alignItems: 'center', paddingVertical: 4 },
  catDot: { width: 10, height: 10, borderRadius: 5, marginRight: 8 },
  catLabel: { flex: 1, fontSize: 13 },
  catCount: { width: 30, textAlign: 'right', fontWeight: '600' },
  catPct: { width: 44, textAlign: 'right', opacity: 0.6, fontSize: 12 },

  calWrap: { flexDirection: 'row', marginTop: 14, gap: 4 },
  calWeek: { gap: 4, flex: 1 },
  calCell: { aspectRatio: 1, borderRadius: 3, width: '100%' },
  legend: { flexDirection: 'row', alignItems: 'center', marginTop: 10, gap: 4, justifyContent: 'flex-end' },
  legendLabel: { fontSize: 10, opacity: 0.6, marginHorizontal: 4 },
  legendCell: { width: 12, height: 12, borderRadius: 3 },

  milestone: { flexDirection: 'row', alignItems: 'center', paddingVertical: 8, gap: 12 },
  milestoneEmoji: { fontSize: 22 },
  milestoneTitle: { fontWeight: '600' },
  milestoneXp: { fontWeight: '700', opacity: 0.8 },
});
