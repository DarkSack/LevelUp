import React from 'react';
import { View, StyleSheet, Pressable, Platform } from 'react-native';
import { Text, useTheme } from 'react-native-paper';
import { LinearGradient } from 'expo-linear-gradient';
import { useAuth } from '../store/auth';
import { useNav, MENU_ITEMS, type ScreenKey } from '../store/nav';
import { useThemeMode, THEME_OPTIONS } from '../store/theme';
import { palette } from '../theme';

const MENU_WIDTH = 280;

// CSS transitions work on web via RN Web's style layer. On native these
// keys are ignored, so we fall back to a plain toggle (fine as a first pass).
const transitionStyle = Platform.OS === 'web'
  ? ({
      transitionProperty: 'left, opacity',
      transitionDuration: '260ms',
      transitionTimingFunction: 'cubic-bezier(0.16, 1, 0.3, 1)',
    } as const)
  : ({} as const);

export function SideMenu() {
  const { menuOpen, closeMenu, setScreen, screen } = useNav();
  const { profile, stats, signOut } = useAuth();
  const theme = useTheme();

  const avatarEmoji = profile?.avatar_emoji ?? '🎮';
  const avatarColor = profile?.avatar_color ?? palette.electricBlue;

  if (!menuOpen) return null;

  return (
    <>
      <View style={[styles.backdrop, { opacity: 0.55 }]}>
        <Pressable style={StyleSheet.absoluteFill} onPress={closeMenu} />
      </View>

      <View style={[
        styles.panel,
        { backgroundColor: theme.colors.surface },
      ]}>
        <LinearGradient
          colors={[avatarColor, '#8B5CF6']}
          start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }}
          style={styles.header}>
          <View style={styles.avatar}>
            <Text style={styles.avatarEmoji}>{avatarEmoji}</Text>
          </View>
          <View style={{ marginLeft: 12 }}>
            <Text style={styles.name}>{profile?.display_name ?? 'Player'}</Text>
            <Text style={styles.sub}>Nivel {stats?.level ?? 1} · 🔥 {stats?.streak_days ?? 0}</Text>
          </View>
        </LinearGradient>

        <View style={styles.list}>
          {MENU_ITEMS.map((item) => {
            const active = item.key === screen;
            return (
              <Pressable key={item.key}
                onPress={() => setScreen(item.key as ScreenKey)}
                style={[
                  styles.row,
                  active && { backgroundColor: theme.colors.primary + '18' },
                ]}>
                <Text style={styles.rowEmoji}>{item.emoji}</Text>
                <Text style={[
                  styles.rowLabel,
                  active && { color: theme.colors.primary, fontWeight: '700' },
                ]}>
                  {item.label}
                </Text>
                {active && <View style={[styles.activeDot, { backgroundColor: theme.colors.primary }]} />}
              </Pressable>
            );
          })}
        </View>

        <View style={styles.footer}>
          <ThemeSwitcher />
          <Pressable style={styles.row} onPress={() => { closeMenu(); signOut(); }}>
            <Text style={styles.rowEmoji}>🚪</Text>
            <Text style={[styles.rowLabel, { color: theme.colors.error }]}>Cerrar sesión</Text>
          </Pressable>
        </View>
      </View>
    </>
  );
}

function ThemeSwitcher() {
  const mode = useThemeMode((s) => s.mode);
  const setMode = useThemeMode((s) => s.setMode);
  const theme = useTheme();

  return (
    <View style={styles.themeWrap}>
      <Text style={styles.themeLabel}>Tema</Text>
      <View style={styles.themeRow}>
        {THEME_OPTIONS.map((o) => {
          const active = o.key === mode;
          return (
            <Pressable key={o.key}
              onPress={() => setMode(o.key)}
              style={[
                styles.themeChip,
                active
                  ? { backgroundColor: theme.colors.primary, borderColor: theme.colors.primary }
                  : { borderColor: 'rgba(148,163,184,0.35)' },
              ]}>
              <Text style={styles.themeChipEmoji}>{o.emoji}</Text>
              <Text style={[
                styles.themeChipLabel,
                { color: active ? '#fff' : theme.colors.onSurface },
              ]}>{o.label}</Text>
            </Pressable>
          );
        })}
      </View>
    </View>
  );
}

export function MenuButton() {
  const toggleMenu = useNav((s) => s.toggleMenu);
  const theme = useTheme();
  const screen = useNav((s) => s.screen);
  const label = MENU_ITEMS.find((i) => i.key === screen)?.label ?? 'LevelUp';

  return (
    <View style={styles.appBar}>
      <Pressable onPress={toggleMenu}
        style={({ pressed }) => [
          styles.menuBtn,
          { backgroundColor: theme.colors.surface },
          pressed && { opacity: 0.7 },
        ]}
        hitSlop={12}>
        <View style={styles.hamburger}>
          <View style={[styles.hamLine, { backgroundColor: theme.colors.onSurface }]} />
          <View style={[styles.hamLine, { backgroundColor: theme.colors.onSurface }]} />
          <View style={[styles.hamLine, { backgroundColor: theme.colors.onSurface }]} />
        </View>
      </Pressable>
      <Text style={[styles.appBarTitle, { color: theme.colors.onBackground }]} numberOfLines={1}>
        {label}
      </Text>
      <View style={styles.appBarSpacer} />
    </View>
  );
}

const styles = StyleSheet.create({
  appBar: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingTop: 12,
    paddingBottom: 4,
    zIndex: 5,
  },
  menuBtn: {
    width: 40, height: 40, borderRadius: 12,
    alignItems: 'center', justifyContent: 'center',
    shadowColor: '#000', shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.12, shadowRadius: 6, elevation: 3,
  },
  hamburger: { width: 20, gap: 4 },
  hamLine: { height: 2, width: '100%', borderRadius: 1 },
  appBarTitle: { flex: 1, textAlign: 'center', fontSize: 16, fontWeight: '700', marginHorizontal: 8 },
  appBarSpacer: { width: 40 },

  backdrop: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: '#000',
    zIndex: 40,
  },

  panel: {
    position: 'absolute',
    top: 0, bottom: 0, left: 0,
    width: MENU_WIDTH,
    zIndex: 50,
    shadowColor: '#000', shadowOffset: { width: 4, height: 0 },
    shadowOpacity: 0.25, shadowRadius: 24, elevation: 20,
  },
  header: { padding: 20, paddingTop: 32, flexDirection: 'row', alignItems: 'center' },
  avatar: {
    width: 56, height: 56, borderRadius: 28,
    backgroundColor: 'rgba(255,255,255,0.2)',
    borderWidth: 2, borderColor: 'rgba(255,255,255,0.85)',
    alignItems: 'center', justifyContent: 'center',
  },
  avatarEmoji: { fontSize: 28 },
  name: { color: '#fff', fontSize: 18, fontWeight: '800' },
  sub:  { color: 'rgba(255,255,255,0.85)', fontSize: 12, marginTop: 2 },

  list: { paddingVertical: 12 },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16, paddingVertical: 12,
    marginHorizontal: 8, borderRadius: 12,
  },
  rowEmoji: { fontSize: 22, width: 32, textAlign: 'center' },
  rowLabel: { fontSize: 16, marginLeft: 8, flex: 1 },
  activeDot: { width: 8, height: 8, borderRadius: 4 },

  footer: {
    marginTop: 'auto',
    paddingBottom: 24,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: 'rgba(148,163,184,0.2)',
    paddingTop: 8,
  },

  themeWrap: { paddingHorizontal: 16, paddingTop: 8, paddingBottom: 12 },
  themeLabel: {
    fontSize: 11, opacity: 0.6, letterSpacing: 1,
    textTransform: 'uppercase', marginBottom: 8, fontWeight: '700',
  },
  themeRow: { flexDirection: 'row', gap: 6 },
  themeChip: {
    flex: 1,
    flexDirection: 'row', alignItems: 'center', justifyContent: 'center',
    paddingVertical: 8, paddingHorizontal: 6,
    borderRadius: 10, borderWidth: 1.5, gap: 4,
  },
  themeChipEmoji: { fontSize: 14 },
  themeChipLabel: { fontSize: 12, fontWeight: '600' },
});
