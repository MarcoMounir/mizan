import React from 'react';
import { View, Text, StyleSheet } from 'react-native';

// TODO: Port UI from the React artifact (egx-portfolio-tracker.jsx) to React Native components
export default function SplashScreen() {
  return (
    <View style={styles.container}>
      <Text style={styles.text}>SplashScreen</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#F3F5F9' },
  text: { fontSize: 18, color: '#0C1B3A' },
});
