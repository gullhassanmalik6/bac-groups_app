# Bonyan POS (Android)

Official Android POS for **Bonyan Advanced Contracting Establishment** on Sunmi V3.

- App name: **Bonyan POS** / بنيان POS
- Application ID: `com.bacgroupsa.pos`
- Support: `info@bacgroupsa.com`
- Release API: `https://api.bacgroupsa.com/api/v1/`

## Architecture

Clean Architecture + MVVM + Jetpack Compose

```
UI (Compose) → ViewModels → Domain repositories → Data (Retrofit/Room/DataStore)
                                      ↓
                         Hardware abstractions (Printer / CardReader)
```

Payment acquiring is performed by the FastAPI backend gateway adapters.

## Stack

Kotlin · Compose · Hilt · Navigation · Retrofit · Room · DataStore · WorkManager · Timber

## Run

1. Start backend on port 8000
2. Open `App/CryptoPOS` in Android Studio
3. Emulator API: `http://10.0.2.2:8000/api/v1/`
4. Release / Sunmi: `https://api.bacgroupsa.com/api/v1/`

## Hardware seams

- `PosPrinter` → `SunmiPosPrinter` with `LogPosPrinter` fallback
- `CardReaderGateway` → simulated until NFC SDK is wired
