# TrustMRR

Chat rooms for founders, gated by verified monthly recurring revenue. Everyone starts in the public room; verifying your MRR unlocks the private room that matches your tier.

## What's inside

| Module | What it is |
|---|---|
| `composeApp/` | Android app (Compose Multiplatform): sign up / log in, tier list, real-time room chat, revenue verification flow |
| `shared/` | Kotlin Multiplatform module: domain models, repositories and the API client |
| `server/` | Ktor backend: JWT auth with rotating refresh tokens, tier-gated chat over REST + WebSocket, Postgres with Flyway migrations |

## Run the backend

```bash
cp .env.example .env    # set DATABASE_* and JWT_SECRET
./gradlew :server:installDist
server/build/install/server/bin/server
```

Or `docker compose up -d` for the bundled Postgres + server + Caddy stack.

On first start against an empty database the server seeds the six revenue tiers and a handful of demo founders (their generated passwords are printed once in the server log).

## Build the Android app

```bash
./gradlew :composeApp:assembleDebug
```

The server base URL the app talks to is set in `shared/src/androidMain/kotlin/com/freeapp/founderrooms/core/config/NetworkConfig.kt`.

## How the gating works

Every account starts unverified, with access to the public room only. `POST /verify/start` opens a revenue verification (a review stub in this version). Posting into a tier above your verified MRR is rejected by the server with a 403, so the gate holds even outside the app.

## License

Apache 2.0, see [LICENSE](LICENSE).
