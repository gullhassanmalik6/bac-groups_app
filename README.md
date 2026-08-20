# CryptoPOS Android

Enterprise Android POS for Sunmi V3 and compatible terminals.

## Architecture

Clean Architecture + MVVM + Jetpack Compose

```
UI (Compose) → ViewModels → Domain repositories → Data (Retrofit/Room/DataStore)
                                      ↓
                         Hardware abstractions (Printer / CardReader)
```

Payment acquiring is performed by the FastAPI backend gateway adapters. The app never talks to Stripe/HyperPay directly.

## Stack

Kotlin · Compose · Hilt · Navigation · Retrofit · Room · DataStore · WorkManager · Timber

## Screens

Splash · Login · Dashboard · Payment · History · Transaction detail · Receipt · Wallet · Settings

## Run

1. Start backend on port 8000  
2. Open `App/CryptoPOS` in Android Studio  
3. Run on emulator (API uses `http://10.0.2.2:8000/api/v1/`)  
4. For a physical device, change `API_BASE_URL` in `app/build.gradle.kts` to your LAN IP  

Seeded merchant login requires a merchant user from the backend (register + create merchant + wallet via API/docs).

## Hardware seams

- `PosPrinter` → `SunmiPosPrinter` (reflective/SDK-ready) with `LogPosPrinter` fallback  
- `CardReaderGateway` → `SimulatedCardReaderGateway` for tap simulation until NFC SDK is wired  
