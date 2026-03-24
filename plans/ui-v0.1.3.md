# Monux UI v0.1.3 Improvement Plan

## Goal
Raise the UI quality bar to match HyperOS / Pixel system app standards.

## Scope
Android Compose UI only. No backend changes.

## Steps

- [ ] Step 1: Fix hardcoded colors
  - ConnectionCard.kt:69-71: Replace Color(0xFF0E8F63/3DBB82/79E5B1) with MaterialTheme.colorScheme.tertiary/tertiaryContainer/onTertiaryContainer
  - MainActivity.kt:575-576: Replace Color(0xFF5AA9FF) with MaterialTheme.colorScheme.secondaryContainer
  - MainActivity.kt:592: Replace Color(0xFF317AF7) with colorScheme token

- [ ] Step 2: Feature grid layout
  - Replace linear FeatureToggleCard list with LazyVerticalGrid(columns = GridCells.Fixed(2))
  - Each cell: icon + label + Switch, compact card style

- [ ] Step 3: Page transition animations
  - Wrap tab content in AnimatedContent with slideInHorizontally + fadeIn / slideOutHorizontally + fadeOut
  - Duration: 300ms, easing: EaseInOutCubic

- [ ] Step 4: Split MainActivity.kt
  - Extract HomeScreen composable to ui/screens/HomeScreen.kt
  - Extract FileScreen composable to ui/screens/FileScreen.kt
  - MainActivity.kt should only contain NavHost + scaffold wrapper

- [ ] Step 5: Build and release
  - Run ./gradlew assembleDebug to verify build
  - Run ./gradlew assembleRelease
  - versionCode = 5, versionName = "0.1.3"
  - git commit -m "feat: UI v0.1.3 - grid layout, page transitions, M3 color tokens"
  - git push origin main
  - gh release create v0.1.3 with APK asset

## Quality Bar
- No hardcoded Color() values outside Theme.kt
- Build must succeed with 0 errors
- All screens render without crash
