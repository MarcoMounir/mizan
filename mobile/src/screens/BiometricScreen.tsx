import React, { useEffect, useState } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, SafeAreaView, ActivityIndicator } from 'react-native';
import { colors, spacing } from '../theme';
import { authService } from '../services/authService';
import { useAuthStore } from '../store/authStore';

export default function BiometricScreen({ navigation }: any) {
  const [loading, setLoading] = useState(false);
  const { user } = useAuthStore();

  useEffect(() => {
    attemptBiometric();
  }, []);

  const attemptBiometric = async () => {
    setLoading(true);
    try {
      const { available } = await authService.isBiometricAvailable();
      if (available) {
        const success = await authService.authenticateWithBiometric();
        if (success) {
          const refreshed = await authService.refreshSession();
          if (refreshed) return; // Auth store update triggers nav change
        }
      }
    } catch (e) {
      console.log('Biometric failed:', e);
    }
    setLoading(false);
  };

  const handleFallback = () => {
    navigation.replace('Welcome');
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.center}>
        <View style={styles.iconCircle}>
          <Text style={styles.icon}>🔐</Text>
        </View>

        {user && (
          <Text style={styles.greeting}>
            Welcome back, {user.displayName.split(' ')[0]}
          </Text>
        )}

        <Text style={styles.subtitle}>Unlock your portfolio securely</Text>

        {loading ? (
          <ActivityIndicator color="#fff" style={{ marginTop: spacing.xl }} />
        ) : (
          <>
            <TouchableOpacity style={styles.biometricBtn} onPress={attemptBiometric}>
              <Text style={styles.biometricBtnText}>Unlock with Biometrics</Text>
            </TouchableOpacity>

            <TouchableOpacity onPress={handleFallback}>
              <Text style={styles.fallback}>Sign in with another method</Text>
            </TouchableOpacity>
          </>
        )}
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.navy,
  },
  center: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: spacing.xl,
  },
  iconCircle: {
    width: 88,
    height: 88,
    borderRadius: 44,
    backgroundColor: 'rgba(29,78,216,0.12)',
    borderWidth: 2,
    borderColor: 'rgba(29,78,216,0.25)',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: spacing.lg,
  },
  icon: { fontSize: 36 },
  greeting: {
    fontFamily: 'Georgia',
    fontSize: 22,
    color: '#fff',
    marginBottom: spacing.sm,
  },
  subtitle: {
    fontSize: 13,
    color: 'rgba(255,255,255,0.45)',
    marginBottom: spacing.xl,
  },
  biometricBtn: {
    width: '100%',
    paddingVertical: 14,
    borderRadius: 14,
    backgroundColor: 'rgba(255,255,255,0.1)',
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.2)',
    alignItems: 'center',
    marginBottom: spacing.md,
  },
  biometricBtnText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
  },
  fallback: {
    color: 'rgba(255,255,255,0.4)',
    fontSize: 13,
    paddingVertical: spacing.sm,
  },
});
