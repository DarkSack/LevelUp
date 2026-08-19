import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  View, StyleSheet, ScrollView, Pressable, Modal, Animated, KeyboardAvoidingView, Platform,
} from 'react-native';
import {
  Text, Card, Chip, ActivityIndicator, useTheme, TextInput,
  Button, Snackbar, HelperText, FAB, IconButton,
} from 'react-native-paper';
import { useAuth } from '../store/auth';
import {
  listProposals, submitProposal, toggleVote,
  type ProposalSort, type NewProposal,
} from '../api/proposals';
import { categoryStyle } from '../theme';
import { AVAILABLE_CATEGORIES } from '../mocks/data';
import type { ChallengeProposal, Category, Difficulty } from '../types';

const DIFFICULTIES: { key: Difficulty; label: string }[] = [
  { key: 'easy',   label: 'Fácil'  },
  { key: 'normal', label: 'Normal' },
  { key: 'hard',   label: 'Difícil' },
  { key: 'epic',   label: 'Épico'  },
];
const DIFF_COLOR: Record<Difficulty, string> = {
  easy: '#4ADE80', normal: '#3B82F6', hard: '#F97056', epic: '#8B5CF6',
};

export function ProposalsScreen() {
  const { user, profile } = useAuth();
  const theme = useTheme();

  const [sort, setSort]     = useState<ProposalSort>('popular');
  const [items, setItems]   = useState<ChallengeProposal[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitOpen, setSubmitOpen] = useState(false);
  const [snack, setSnack]   = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try { setItems(await listProposals(sort, user?.id)); }
    finally { setLoading(false); }
  }, [sort, user?.id]);

  useEffect(() => { load(); }, [load]);

  const onVote = async (p: ChallengeProposal) => {
    // Optimistic
    setItems((list) => list.map((x) =>
      x.id === p.id
        ? { ...x, voted_by_me: !x.voted_by_me, votes: x.votes + (x.voted_by_me ? -1 : 1) }
        : x));
    try { await toggleVote(p.id, user!.id); }
    catch {
      setSnack('No se pudo registrar el voto.');
      load();
    }
  };

  const onSubmit = async (draft: NewProposal) => {
    if (!user) return;
    try {
      await submitProposal(draft, user.id);
      setSubmitOpen(false);
      setSnack('¡Propuesta enviada! Se revisará pronto.');
      setSort('recent');
      load();
    } catch {
      setSnack('Error al enviar. Intenta de nuevo.');
    }
  };

  return (
    <View style={{ flex: 1 }}>
      <ScrollView contentContainerStyle={styles.root}>
        <Text variant="headlineMedium" style={styles.title}>Ideas de la comunidad</Text>
        <Text style={styles.subtitle}>
          Propón nuevos retos y vota por los que quieres ver en el juego.
        </Text>

        <View style={styles.filters}>
          {(['popular', 'recent', 'mine'] as ProposalSort[]).map((s) => (
            <Chip key={s}
              selected={sort === s}
              onPress={() => setSort(s)}
              style={styles.chip}>
              {s === 'popular' ? '🔥 Populares'
                : s === 'recent' ? '🆕 Recientes'
                : '👤 Mías'}
            </Chip>
          ))}
        </View>

        {loading && items.length === 0 ? (
          <View style={styles.center}><ActivityIndicator size="large" /></View>
        ) : items.length === 0 ? (
          <View style={styles.emptyWrap}>
            <Text style={styles.emptyEmoji}>💡</Text>
            <Text style={styles.emptyTitle}>
              {sort === 'mine' ? 'Aún no has propuesto nada.' : 'Sé el primero en proponer.'}
            </Text>
            <Text style={styles.emptyDesc}>
              Toca el botón de abajo para enviar tu idea.
            </Text>
          </View>
        ) : items.map((p) => (
          <ProposalCard key={p.id} proposal={p} onVote={() => onVote(p)} />
        ))}
      </ScrollView>

      <FAB
        icon="plus"
        label="Proponer"
        onPress={() => setSubmitOpen(true)}
        style={[styles.fab, { backgroundColor: theme.colors.primary }]}
        color="#fff"
      />

      <SubmitSheet
        visible={submitOpen}
        onClose={() => setSubmitOpen(false)}
        onSubmit={onSubmit}
        defaultCategory={profile?.categories?.[0] ?? 'physical'}
      />

      <Snackbar visible={!!snack} onDismiss={() => setSnack(null)} duration={2500}>
        {snack ?? ''}
      </Snackbar>
    </View>
  );
}

// ---------------------------------------------------------------------------

function ProposalCard({
  proposal: p, onVote,
}: { proposal: ChallengeProposal; onVote: () => void }) {
  const cat = categoryStyle[p.category];
  const scale = useRef(new Animated.Value(1)).current;

  const handleVote = () => {
    Animated.sequence([
      Animated.spring(scale, { toValue: 1.15, useNativeDriver: true, friction: 4, tension: 200 }),
      Animated.spring(scale, { toValue: 1,    useNativeDriver: true, friction: 5, tension: 120 }),
    ]).start();
    onVote();
  };

  return (
    <Card style={styles.card}>
      <View style={[styles.stripe, { backgroundColor: cat.color }]} />
      <View style={styles.cardRow}>
        <View style={{ flex: 1, padding: 14 }}>
          <View style={styles.cardHeader}>
            <View style={[styles.catPill, { backgroundColor: cat.color + '22' }]}>
              <Text style={[styles.catPillText, { color: cat.color }]}>
                {cat.emoji}  {cat.label}
              </Text>
            </View>
            <View style={[styles.diffPill, { backgroundColor: DIFF_COLOR[p.difficulty] + '22' }]}>
              <Text style={[styles.diffPillText, { color: DIFF_COLOR[p.difficulty] }]}>
                {DIFFICULTIES.find((d) => d.key === p.difficulty)?.label}
              </Text>
            </View>
            {p.status === 'promoted' && (
              <View style={styles.promotedPill}>
                <Text style={styles.promotedText}>✨ EN EL JUEGO</Text>
              </View>
            )}
            {p.status === 'approved' && (
              <View style={styles.approvedPill}>
                <Text style={styles.approvedText}>✅ APROBADO</Text>
              </View>
            )}
          </View>

          <Text variant="titleMedium" style={styles.cardTitle}>{p.title}</Text>
          <Text style={styles.cardDesc} numberOfLines={3}>{p.description}</Text>

          <Text style={styles.author}>
            {p.author_emoji ?? '👤'}  {p.author_name}  ·  {fmtDate(p.created_at)}
          </Text>
        </View>

        <Pressable onPress={handleVote} style={styles.voteBox}>
          <Animated.View style={{ transform: [{ scale }] }}>
            <View style={[
              styles.voteCircle,
              p.voted_by_me
                ? { backgroundColor: cat.color, borderColor: cat.color }
                : { borderColor: cat.color + '55' },
            ]}>
              <Text style={[styles.voteArrow, p.voted_by_me && { color: '#fff' }]}>▲</Text>
            </View>
          </Animated.View>
          <Text style={[styles.voteCount, p.voted_by_me && { color: cat.color, fontWeight: '700' }]}>
            {p.votes}
          </Text>
        </Pressable>
      </View>
    </Card>
  );
}

// ---------------------------------------------------------------------------

function SubmitSheet({
  visible, onClose, onSubmit, defaultCategory,
}: {
  visible: boolean;
  onClose: () => void;
  onSubmit: (p: NewProposal) => void;
  defaultCategory: Category;
}) {
  const theme = useTheme();
  const [title, setTitle]           = useState('');
  const [description, setDesc]      = useState('');
  const [category, setCategory]     = useState<Category>(defaultCategory);
  const [difficulty, setDifficulty] = useState<Difficulty>('normal');
  const [busy, setBusy]             = useState(false);

  const valid = title.trim().length >= 6 && description.trim().length >= 12;

  const submit = async () => {
    if (!valid) return;
    setBusy(true);
    try {
      await onSubmit({
        title: title.trim(),
        description: description.trim(),
        category, difficulty,
      });
      setTitle(''); setDesc(''); setDifficulty('normal');
    } finally { setBusy(false); }
  };

  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
      <KeyboardAvoidingView style={{ flex: 1 }}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <Pressable style={styles.sheetBg} onPress={onClose}>
          <Pressable style={[styles.sheet, { backgroundColor: theme.colors.surface }]}>
            <View style={styles.sheetHandle} />
            <View style={styles.sheetHeader}>
              <Text variant="titleLarge" style={{ fontWeight: '700' }}>Proponer un reto</Text>
              <IconButton icon="close" onPress={onClose} />
            </View>

            <ScrollView keyboardShouldPersistTaps="handled">
              <TextInput mode="outlined" label="Título" value={title} onChangeText={setTitle}
                maxLength={60} style={styles.input} />
              <HelperText type="info" visible>{title.length}/60 · mínimo 6</HelperText>

              <TextInput mode="outlined" label="Descripción" value={description}
                onChangeText={setDesc} multiline numberOfLines={4}
                maxLength={200} style={styles.input} />
              <HelperText type="info" visible>{description.length}/200 · mínimo 12</HelperText>

              <Text style={styles.sheetLabel}>Categoría</Text>
              <View style={styles.chipWrap}>
                {AVAILABLE_CATEGORIES.map((c) => (
                  <Chip key={c.key}
                    selected={category === c.key}
                    onPress={() => setCategory(c.key)}
                    style={styles.chip}>
                    {c.emoji}  {c.label}
                  </Chip>
                ))}
              </View>

              <Text style={styles.sheetLabel}>Dificultad</Text>
              <View style={styles.chipWrap}>
                {DIFFICULTIES.map((d) => (
                  <Chip key={d.key}
                    selected={difficulty === d.key}
                    onPress={() => setDifficulty(d.key)}
                    style={[styles.chip, difficulty === d.key && { backgroundColor: DIFF_COLOR[d.key] + '33' }]}
                    textStyle={difficulty === d.key ? { color: DIFF_COLOR[d.key], fontWeight: '700' } : undefined}>
                    {d.label}
                  </Chip>
                ))}
              </View>

              <Button mode="contained"
                onPress={submit}
                loading={busy} disabled={!valid || busy}
                style={styles.sheetSubmit}>
                Enviar propuesta
              </Button>
            </ScrollView>
          </Pressable>
        </Pressable>
      </KeyboardAvoidingView>
    </Modal>
  );
}

// ---------------------------------------------------------------------------

function fmtDate(iso: string) {
  const d = new Date(iso);
  const days = Math.floor((Date.now() - d.getTime()) / 86400000);
  if (days === 0) return 'hoy';
  if (days === 1) return 'ayer';
  if (days < 7)   return `hace ${days} días`;
  if (days < 30)  return `hace ${Math.floor(days / 7)} sem.`;
  return `hace ${Math.floor(days / 30)} meses`;
}

const styles = StyleSheet.create({
  root: { padding: 16, paddingTop: 48, paddingBottom: 120 },
  center: { alignItems: 'center', paddingVertical: 40 },

  title: { fontWeight: '800', marginBottom: 6 },
  subtitle: { opacity: 0.7, marginBottom: 16 },

  filters: { flexDirection: 'row', flexWrap: 'wrap', marginBottom: 12 },
  chip: { marginRight: 8, marginBottom: 8 },

  card: { marginBottom: 12, overflow: 'hidden' },
  stripe: { position: 'absolute', left: 0, top: 0, bottom: 0, width: 4 },
  cardRow: { flexDirection: 'row', paddingLeft: 6 },
  cardHeader: { flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginBottom: 8 },

  catPill: { paddingHorizontal: 8, paddingVertical: 3, borderRadius: 999 },
  catPillText: { fontSize: 11, fontWeight: '700' },
  diffPill: { paddingHorizontal: 8, paddingVertical: 3, borderRadius: 999 },
  diffPillText: { fontSize: 11, fontWeight: '700' },
  promotedPill: { paddingHorizontal: 8, paddingVertical: 3, borderRadius: 999, backgroundColor: '#F59E0B22' },
  promotedText:  { fontSize: 10, fontWeight: '800', color: '#F59E0B', letterSpacing: 0.5 },
  approvedPill: { paddingHorizontal: 8, paddingVertical: 3, borderRadius: 999, backgroundColor: '#4ADE8022' },
  approvedText:  { fontSize: 10, fontWeight: '800', color: '#4ADE80', letterSpacing: 0.5 },

  cardTitle: { fontWeight: '700', marginBottom: 4 },
  cardDesc:  { opacity: 0.85, lineHeight: 20 },
  author:    { fontSize: 11, opacity: 0.55, marginTop: 10 },

  voteBox: {
    width: 64, alignItems: 'center', justifyContent: 'center',
    borderLeftWidth: StyleSheet.hairlineWidth, borderLeftColor: 'rgba(148,163,184,0.25)',
    paddingVertical: 12,
  },
  voteCircle: {
    width: 40, height: 40, borderRadius: 20,
    borderWidth: 1.5,
    alignItems: 'center', justifyContent: 'center', marginBottom: 4,
  },
  voteArrow: { fontSize: 16, color: '#94A3B8', fontWeight: '700' },
  voteCount: { fontSize: 13, opacity: 0.75 },

  emptyWrap: { alignItems: 'center', paddingVertical: 40 },
  emptyEmoji: { fontSize: 56, marginBottom: 12 },
  emptyTitle: { fontWeight: '700', fontSize: 18 },
  emptyDesc: { opacity: 0.7, marginTop: 8, textAlign: 'center' },

  fab: {
    position: 'absolute', right: 16, bottom: 24,
  },

  sheetBg: { flex: 1, backgroundColor: 'rgba(0,0,0,0.55)', justifyContent: 'flex-end' },
  sheet: {
    padding: 16, paddingTop: 8,
    borderTopLeftRadius: 24, borderTopRightRadius: 24,
    maxHeight: '90%',
  },
  sheetHandle: {
    width: 44, height: 4, borderRadius: 2,
    backgroundColor: 'rgba(148,163,184,0.4)',
    alignSelf: 'center', marginBottom: 8,
  },
  sheetHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 },
  sheetLabel: { fontWeight: '700', marginTop: 12, marginBottom: 8 },

  input: { marginBottom: 2 },
  chipWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  sheetSubmit: { marginTop: 20, marginBottom: 16 },
});
