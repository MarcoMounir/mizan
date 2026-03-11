import React, { useState } from 'react';
import {
  View, Text, TouchableOpacity, StyleSheet, Platform,
  ActivityIndicator, SafeAreaView,
} from 'react-native';
import { colors, spacing } from '../theme';
import { authService } from '../services/authService';

export default function WelcomeScreen() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleGoogle = async () => {
    setLoading(true);
    setError(null);
    try {
      await authService.signInWithGoogle();
    } catch (e: any) {
      setError(e.message || 'Google sign-in failed');
    }
    setLoading(false);
  };

  const handleApple = async () => {
    setLoading(true);
    setError(null);
    try {
      await authService.signInWithApple();
    } catch (e: any) {
      setError(e.message || 'Apple sign-in failed');
    }
    setLoading(false);
  };

  return (
    <SafeAreaView style={styles.container}>
      {/* Branding */}
      <View style={styles.brandSection}>
        <Text style={styles.logoText}>📊</Text>
        <Text style={styles.appName}>Mizan</Text>
        <Text style={styles.subtitle}>PORTFOLIO INTELLIGENCE</Text>
        <Text style={styles.tagline}>
          Track your EGX portfolio.{'\n'}Understand your profits.{'\n'}Know your numbers.
        </Text>
      </View>

      {/* Auth buttons */}
      <View style={styles.authSection}>
        {error && <Text style={styles.error}>{error}</Text>}

        <TouchableOpacity style={styles.googleBtn} onPress={handleGoogle} disabled={loading}>
          {loading ? <ActivityIndicator color={colors.navy} /> : (
            <Text style={styles.googleBtnText}>Continue with Google</Text>
          )}
        </TouchableOpacity>

        {Platform.OS === 'ios' && (
          <TouchableOpacity style={styles.appleBtn} onPress={handleApple} disabled={loading}>
            <Text style={styles.appleBtnText}>Continue with Apple</Text>
          </TouchableOpacity>
        )}

        <Text style={styles.terms}>
          By continuing, you agree to Mizan's Terms of Service and Privacy Policy
        </Text>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.navy,
    justifyContent: 'space-between',
    paddingHorizontal: spacing.xl,
  },
  brandSection: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  logoText: { fontSize: 48, marginBottom: spacing.md },
  appName: {
    fontFamily: 'Georgia',
    fontSize: 36,
    color: '#fff',
    letterSpacing: -0.5,
  },
  subtitle: {
    fontFamily: 'Courier',
    fontSize: 10,
    letterSpacing: 3,
    color: 'rgba(255,255,255,0.4)',
    marginTop: spacing.sm,
  },
  tagline: {
    fontSize: 14,
    color: 'rgba(255,255,255,0.5)',
    textAlign: 'center',
    marginTop: spacing.lg,
    lineHeight: 22,
  },
  authSection: {
    paddingBottom: spacing.xxl,
  },
  error: {
    color: colors.loss,
    fontSize: 12,
    textAlign: 'center',
    marginBottom: spacing.md,
  },
  googleBtn: {
    backgroundColor: '#fff',
    paddingVertical: 15,
    borderRadius: 14,
    alignItems: 'center',
    marginBottom: spacing.md,
  },
  googleBtnText: {
    color: colors.navy,
    fontSize: 15,
    fontWeight: '600',
  },
  appleBtn: {
    backgroundColor: '#000',
    paddingVertical: 15,
    borderRadius: 14,
    alignItems: 'center',
    marginBottom: spacing.md,
  },
  appleBtnText: {
    color: '#fff',
    fontSize: 15,
    fontWeight: '600',
  },
  terms: {
    fontSize: 11,
    color: 'rgba(255,255,255,0.25)',
    textAlign: 'center',
    marginTop: spacing.sm,
    lineHeight: 16,
  },
});
