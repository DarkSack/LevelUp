import React, { useState } from 'react';
import { View, StyleSheet } from 'react-native';
import { Text, TextInput, Button, SegmentedButtons, HelperText } from 'react-native-paper';
import { MOCK_MODE } from '../config';
import { useAuth } from '../store/auth';

export function AuthScreen() {
  const [mode, setMode] = useState<'signin' | 'signup'>('signup');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const signIn = useAuth((s) => s.signIn);
  const signUp = useAuth((s) => s.signUp);

  const submit = async () => {
    setBusy(true); setError(null);
    try {
      if (MOCK_MODE) {
        await signIn('', '');
        return;
      }
      if (mode === 'signup') {
        await signUp(email, password, displayName || 'Player');
      } else {
        await signIn(email, password);
      }
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Error inesperado');
    } finally { setBusy(false); }
  };

  return (
    <View style={styles.root}>
      <Text variant="displaySmall" style={styles.title}>LevelUp</Text>
      <Text variant="bodyMedium" style={styles.subtitle}>Crea tu personaje.</Text>

      {!MOCK_MODE && (
        <SegmentedButtons
          value={mode}
          onValueChange={(v) => setMode(v as 'signin' | 'signup')}
          buttons={[
            { value: 'signup', label: 'Crear cuenta' },
            { value: 'signin', label: 'Entrar' },
          ]}
          style={styles.seg}
        />
      )}

      {mode === 'signup' && (
        <TextInput label="Cómo quieres que te llame" value={displayName}
          onChangeText={setDisplayName} style={styles.input} mode="outlined" />
      )}
      {!MOCK_MODE && (
        <>
          <TextInput label="Correo" value={email} onChangeText={setEmail}
            autoCapitalize="none" keyboardType="email-address"
            style={styles.input} mode="outlined" />
          <TextInput label="Contraseña" value={password} onChangeText={setPassword}
            secureTextEntry style={styles.input} mode="outlined" />
        </>
      )}

      {MOCK_MODE && (
        <HelperText type="info" visible>
          Modo demo: cualquier dato te deja entrar.
        </HelperText>
      )}
      {error && <HelperText type="error" visible>{error}</HelperText>}

      <Button mode="contained" onPress={submit} loading={busy} disabled={busy}
        style={styles.btn}>
        {mode === 'signup' ? 'Crear personaje' : 'Entrar'}
      </Button>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, padding: 24, justifyContent: 'center' },
  title: { textAlign: 'center', fontWeight: '700' },
  subtitle: { textAlign: 'center', opacity: 0.7, marginBottom: 32 },
  seg: { marginBottom: 16 },
  input: { marginBottom: 12 },
  btn: { marginTop: 12 },
});
