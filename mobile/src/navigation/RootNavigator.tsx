import React, { useState } from 'react';
import { View, ActivityIndicator, StyleSheet } from 'react-native';
import { HomeScreen } from '../screens/HomeScreen';
import { AchievementsScreen } from '../screens/AchievementsScreen';
import { ProgressScreen } from '../screens/ProgressScreen';
import { ProposalsScreen } from '../screens/ProposalsScreen';
import { ProfileScreen } from '../screens/ProfileScreen';
import { AuthScreen } from '../screens/AuthScreen';
import { OnboardingScreen } from '../screens/OnboardingScreen';
import { IntroScreen } from '../screens/IntroScreen';
import { SideMenu, MenuButton } from '../components/SideMenu';
import { useAuth } from '../store/auth';
import { useNav } from '../store/nav';

export function RootNavigator() {
  const { user, profile, loading } = useAuth();
  const screen = useNav((s) => s.screen);
  const [seenIntro, setSeenIntro] = useState(false);

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  if (!seenIntro && !user) return <IntroScreen onFinish={() => setSeenIntro(true)} />;
  if (!user) return <AuthScreen />;
  if (!profile || profile.categories.length === 0) return <OnboardingScreen />;

  return (
    <View style={{ flex: 1 }}>
      <MenuButton />
      <View style={{ flex: 1 }}>
        {screen === 'home'         && <HomeScreen />}
        {screen === 'achievements' && <AchievementsScreen />}
        {screen === 'progress'     && <ProgressScreen />}
        {screen === 'proposals'    && <ProposalsScreen />}
        {screen === 'profile'      && <ProfileScreen />}
      </View>
      <SideMenu />
    </View>
  );
}

const styles = StyleSheet.create({
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
});
