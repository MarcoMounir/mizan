import { GoogleSignin } from '@react-native-google-signin/google-signin';
import { appleAuth } from '@invertase/react-native-apple-authentication';
import ReactNativeBiometrics from 'react-native-biometrics';
import { Platform } from 'react-native';
import api from '../api/client';
import { useAuthStore } from '../store/authStore';
import { AuthResponse } from '../types';
import { GOOGLE_WEB_CLIENT_ID, GOOGLE_IOS_CLIENT_ID } from '@env';

// ── Configure Google Sign-In ──
GoogleSignin.configure({
  webClientId: GOOGLE_WEB_CLIENT_ID,
  iosClientId: GOOGLE_IOS_CLIENT_ID,
  offlineAccess: false,
});

const rnBiometrics = new ReactNativeBiometrics({ allowDeviceCredentials: true });

export const authService = {
  // ── Google Sign-In ──
  async signInWithGoogle(): Promise<AuthResponse> {
    await GoogleSignin.hasPlayServices();
    const userInfo = await GoogleSignin.signIn();
    const idToken = userInfo.idToken;

    if (!idToken) throw new Error('No Google ID token received');

    const { data } = await api.post<AuthResponse>('/auth/google', {
      idToken,
      deviceId: await getDeviceId(),
      deviceInfo: `${Platform.OS} ${Platform.Version}`,
    });

    await useAuthStore.getState().setAuthFromResponse(data);
    return data;
  },

  // ── Apple Sign-In ──
  async signInWithApple(): Promise<AuthResponse> {
    const appleAuthResponse = await appleAuth.performRequest({
      requestedOperation: appleAuth.Operation.LOGIN,
      requestedScopes: [appleAuth.Scope.FULL_NAME, appleAuth.Scope.EMAIL],
    });

    const credentialState = await appleAuth.getCredentialStateForUser(appleAuthResponse.user);
    if (credentialState !== appleAuth.State.AUTHORIZED) {
      throw new Error('Apple auth not authorized');
    }

    const { data } = await api.post<AuthResponse>('/auth/apple', {
      idToken: appleAuthResponse.identityToken,
      authorizationCode: appleAuthResponse.authorizationCode,
      fullName: appleAuthResponse.fullName,
      deviceId: await getDeviceId(),
      deviceInfo: `${Platform.OS} ${Platform.Version}`,
    });

    await useAuthStore.getState().setAuthFromResponse(data);
    return data;
  },

  // ── Biometric check ──
  async isBiometricAvailable(): Promise<{ available: boolean; type: string }> {
    const { available, biometryType } = await rnBiometrics.isSensorAvailable();
    return { available, type: biometryType || 'unknown' };
  },

  async authenticateWithBiometric(): Promise<boolean> {
    const { success } = await rnBiometrics.simplePrompt({
      promptMessage: 'Unlock your portfolio',
      cancelButtonText: 'Use password',
    });
    return success;
  },

  // ── Token refresh ──
  async refreshSession(): Promise<boolean> {
    const { refreshToken, setAuthFromResponse, signOut } = useAuthStore.getState();
    if (!refreshToken) return false;

    try {
      const { data } = await api.post<AuthResponse>('/auth/refresh', { refreshToken });
      await setAuthFromResponse(data);
      return true;
    } catch {
      await signOut();
      return false;
    }
  },

  // ── Sign out ──
  async signOut(): Promise<void> {
    const { refreshToken } = useAuthStore.getState();
    try {
      await api.post('/auth/signout', { refreshToken });
    } catch {
      // Best-effort server-side logout
    }
    try {
      await GoogleSignin.signOut();
    } catch { /* ignore */ }
    await useAuthStore.getState().signOut();
  },
};

// Simple device ID (in production, use a persistent UUID stored in Keychain)
async function getDeviceId(): Promise<string> {
  return `${Platform.OS}-${Math.random().toString(36).slice(2, 10)}`;
}
