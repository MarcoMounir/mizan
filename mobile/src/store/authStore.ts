import { create } from 'zustand';
import * as Keychain from 'react-native-keychain';
import { User, AuthResponse } from '../types';

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;

  // Actions
  setAuthFromResponse: (response: AuthResponse) => Promise<void>;
  setTokens: (accessToken: string, refreshToken: string) => Promise<void>;
  restoreSession: () => Promise<boolean>;
  signOut: () => Promise<void>;
  updateBiometric: (enabled: boolean) => void;
}

const KEYCHAIN_SERVICE = 'com.mizan.auth';

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  accessToken: null,
  refreshToken: null,
  isAuthenticated: false,
  isLoading: true,

  setAuthFromResponse: async (response: AuthResponse) => {
    const user: User = {
      id: response.userId,
      email: response.email,
      displayName: response.displayName,
      authProvider: response.authProvider,
      profileImageUrl: response.profileImageUrl,
      biometricEnabled: response.biometricEnabled,
    };

    // Store refresh token in Keychain (encrypted, biometric-gated)
    await Keychain.setGenericPassword(
      'refresh_token',
      JSON.stringify({ refreshToken: response.refreshToken, user }),
      {
        service: KEYCHAIN_SERVICE,
        accessControl: Keychain.ACCESS_CONTROL.BIOMETRY_ANY_OR_DEVICE_PASSCODE,
        accessible: Keychain.ACCESSIBLE.WHEN_PASSCODE_SET_THIS_DEVICE_ONLY,
      }
    );

    set({
      user,
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      isAuthenticated: true,
      isLoading: false,
    });
  },

  setTokens: async (accessToken: string, refreshToken: string) => {
    const { user } = get();

    // Update Keychain
    await Keychain.setGenericPassword(
      'refresh_token',
      JSON.stringify({ refreshToken, user }),
      {
        service: KEYCHAIN_SERVICE,
        accessControl: Keychain.ACCESS_CONTROL.BIOMETRY_ANY_OR_DEVICE_PASSCODE,
        accessible: Keychain.ACCESSIBLE.WHEN_PASSCODE_SET_THIS_DEVICE_ONLY,
      }
    );

    set({ accessToken, refreshToken });
  },

  restoreSession: async (): Promise<boolean> => {
    try {
      const credentials = await Keychain.getGenericPassword({
        service: KEYCHAIN_SERVICE,
        // This triggers biometric prompt on iOS/Android
        authenticationPrompt: {
          title: 'Unlock Mizan',
          subtitle: 'Verify your identity to access your portfolio',
        },
      });

      if (credentials && credentials.password) {
        const { refreshToken, user } = JSON.parse(credentials.password);
        set({
          user,
          refreshToken,
          isAuthenticated: false, // Needs token refresh via API
          isLoading: false,
        });
        return true;
      }
    } catch (error) {
      console.log('No stored session or biometric failed:', error);
    }

    set({ isLoading: false });
    return false;
  },

  signOut: async () => {
    try {
      await Keychain.resetGenericPassword({ service: KEYCHAIN_SERVICE });
    } catch (e) {
      console.warn('Keychain reset failed:', e);
    }
    set({
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
      isLoading: false,
    });
  },

  updateBiometric: (enabled: boolean) => {
    set((state) => ({
      user: state.user ? { ...state.user, biometricEnabled: enabled } : null,
    }));
  },
}));
