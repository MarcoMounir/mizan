import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { useAuthStore } from '../store/authStore';

// Screens
import SplashScreen from '../screens/SplashScreen';
import WelcomeScreen from '../screens/WelcomeScreen';
import BiometricScreen from '../screens/BiometricScreen';
import DashboardScreen from '../screens/DashboardScreen';
import PortfolioScreen from '../screens/PortfolioScreen';
import RiskScreen from '../screens/RiskScreen';
import SettingsScreen from '../screens/SettingsScreen';
import StockDetailScreen from '../screens/StockDetailScreen';
import AddOrderScreen from '../screens/AddOrderScreen';

const AuthStack = createNativeStackNavigator();
const MainTab = createBottomTabNavigator();
const RootStack = createNativeStackNavigator();

function AuthNavigator() {
  return (
    <AuthStack.Navigator screenOptions={{ headerShown: false }}>
      <AuthStack.Screen name="Welcome" component={WelcomeScreen} />
      <AuthStack.Screen name="Biometric" component={BiometricScreen} />
    </AuthStack.Navigator>
  );
}

function MainTabs() {
  return (
    <MainTab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarStyle: { backgroundColor: '#0C1B3A', borderTopWidth: 0 },
        tabBarActiveTintColor: '#fff',
        tabBarInactiveTintColor: '#7A8FAE',
      }}
    >
      <MainTab.Screen name="Overview" component={DashboardScreen} />
      <MainTab.Screen name="Portfolio" component={PortfolioScreen} />
      <MainTab.Screen name="Risk" component={RiskScreen} />
      <MainTab.Screen name="Settings" component={SettingsScreen} />
    </MainTab.Navigator>
  );
}

function AppNavigator() {
  return (
    <RootStack.Navigator screenOptions={{ headerShown: false }}>
      <RootStack.Screen name="MainTabs" component={MainTabs} />
      <RootStack.Screen name="StockDetail" component={StockDetailScreen}
        options={{ presentation: 'modal' }} />
      <RootStack.Screen name="AddOrder" component={AddOrderScreen}
        options={{ presentation: 'modal' }} />
    </RootStack.Navigator>
  );
}

export default function Navigation() {
  const { isAuthenticated, isLoading } = useAuthStore();

  if (isLoading) {
    return <SplashScreen />;
  }

  return (
    <NavigationContainer>
      {isAuthenticated ? <AppNavigator /> : <AuthNavigator />}
    </NavigationContainer>
  );
}
