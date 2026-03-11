import React, { useEffect } from 'react';
import { StatusBar } from 'react-native';
import Navigation from './src/navigation';
import { useAuthStore } from './src/store/authStore';

export default function App() {
  useEffect(() => {
    // On app launch, attempt to restore session from Keychain
    useAuthStore.getState().restoreSession();
  }, []);

  return (
    <>
      <StatusBar barStyle="light-content" backgroundColor="#0C1B3A" />
      <Navigation />
    </>
  );
}
