package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// Sleek Interface Design System Tokens
// ==========================================
// Surfaces & Backgrounds
val SleekBackground = Color(0xFFFDFBFF)          // Ultra-clean crisp off-white canvas
val SleekSurface = Color(0xFFFFFFFF)             // Pure white cards & dialogs
val SleekSurfaceSecondary = Color(0xFFF0F4F8)    // Soft grey-blue tint for inputs, chips & secondary surfaces
val SleekSurfaceDashed = Color(0xFFF8F9FA)       // Dashed card / auxiliary section background
val SleekDarkContainer = Color(0xFF1E1E1E)       // Sleek dark terminal container for telemetry, live stream & code blocks
val SleekDarkContainerInner = Color(0xFF141414)  // Inner deep terminal layer

// Borders & Dividers
val SleekBorder = Color(0xFFE1E2E5)              // Crisp modern border
val SleekBorderSubtle = Color(0xFFECEEF1)        // Soft secondary divider

// Primary Accents & Pills
val SleekBluePrimary = Color(0xFF0B57D0)         // Google Sleek accent blue
val SleekBlueActive = Color(0xFF0842A0)          // Pressed / active blue
val SleekBlueContainer = Color(0xFFD3E3FD)       // Soft ice-blue pill & active chip background
val SleekOnBlueContainer = Color(0xFF041E49)     // High-contrast deep navy ink text/icon on container

// Typography & Text Tones
val SleekTextPrimary = Color(0xFF1A1C1E)         // Deep slate black text
val SleekTextSecondary = Color(0xFF44474E)       // Refined cool grey text
val SleekTextMuted = Color(0xFF74777F)           // Muted tertiary text
val SleekTextWhite = Color(0xFFFFFFFF)           // White text for dark surfaces & primary buttons

// Status & Indicator Badges (from Sleek Interface HTML)
val SleekGreenContainer = Color(0xFFE6F4EA)      // Success / GET badge background
val SleekGreenText = Color(0xFF137333)           // Success / GET badge text
val SleekRedContainer = Color(0xFFFCE8E6)        // Emergency / Alert container
val SleekRedText = Color(0xFFC5221F)             // Emergency / Alert text
val SleekYellowContainer = Color(0xFFFEF7E0)     // Warning container
val SleekYellowText = Color(0xFFB06000)          // Warning text

// Terminal & Code Syntax Highlighting (from Sleek Interface HTML)
val SleekCodeKey = Color(0xFFCE9178)
val SleekCodeNumber = Color(0xFFB5CEA8)
val SleekCodeText = Color(0xFFD4D4D4)
val SleekCodeAccent = Color(0xFF0B57D0)

// ==========================================
// Semantic Aliases for Compatibility
// ==========================================
val TVMidnightNavy = SleekBackground
val TVSurfaceDark = SleekSurface
val TVCardBackground = SleekSurface
val TVCardBorder = SleekBorder

val HorizonCyan = SleekBluePrimary
val HorizonSky = Color(0xFF1A73E8)
val SolarGold = Color(0xFFE37400)
val SunsetOrange = Color(0xFFD9381E)
val SevereRed = SleekRedText
val RadarGreen = SleekGreenText
val StormPurple = Color(0xFF7B1FA2)

val TextPrimaryWhite = SleekTextPrimary
val TextSecondarySilver = SleekTextSecondary
val TextMuted = SleekTextMuted

val FocusCyanGlow = SleekBluePrimary

