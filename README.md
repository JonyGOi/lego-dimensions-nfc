# LEGO Dimensions NFC Writer

A native Android application built with Jetpack Compose and Kotlin for reading, generating, and writing NFC tags (NTAG213 and NTAG215) for LEGO Dimensions.

Developed by João Pedro Favoni Fernandes (Brazil).

---

## Features

- Complete Catalog:
  - All 75+ characters with detailed abilities and franchises.
  - All vehicles and gadgets across all 3 rebuild tiers (Level 1 - Default, Level 2, and Level 3), totaling over 260 variations.
  - Category and rebuild level filter chips.
- Offline Ring Token Icons:
  - Over 310 circular icons corresponding to the official token labels embedded directly in the application.
- Ultra-Fast Tag Writing (< 1 second):
  - Direct 3-command writing sequence:
    1. Page 0x24: Character ID (encrypted with tag UID) or Vehicle ID.
    2. Page 0x25 / 0x26: Encryption key or vehicle flag (00 01 00 00).
    3. Page 0x2B / 0x85: NTAG password protection (PWD).
- Multi-Language Support:
  - Comprehensive internationalization supporting dozens of languages (English, Portuguese, Spanish, French, German, Italian, Japanese, Chinese, and more).
  - In-app language selector.
- Advanced Mode:
  - Direct tag creation by numeric ID.
  - Support for standard NTAG213 and NTAG215 tags (including Gen2 / magic UID tags).
- Local History:
  - Detailed log of written tags with timestamp, UID, and status.
- Credits:
  - Author and developer recognition, along with open-source community acknowledgments.

---

## Building from Source

### Prerequisites
- Android Studio (Iguana, Jellyfish, or newer)
- JDK 17
- Android SDK 34
- Android device running Android 8.0 or higher with NFC hardware enabled

### Command Line Build (Gradle)

```bash
git clone https://github.com/JonyGOi/lego-dimensions-nfc.git
cd lego-dimensions-nfc

./gradlew assembleDebug
```

The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## Technical Stack

- Language: Kotlin 1.9.22
- UI Toolkit: Jetpack Compose with Material Design 3
- NFC: Android NFC ReaderMode (FLAG_READER_NFC_A)
- Cryptography: Custom Tiny Encryption Algorithm (TEA) implementation for LEGO Dimensions
- Architecture: MVVM with StateFlow and Coroutines

---

## Credits and Acknowledgments

- Developer: João Pedro Favoni Fernandes (Brazil)
- Special Acknowledgments:
  - LEGO Dimensions preservation and reverse engineering community.
  - Laurent Ellerbach for the open-source research and documentation on tag encryption algorithms.

---

## License

This project is created for educational and preservation purposes. LEGO and LEGO Dimensions are registered trademarks of the LEGO Group.
