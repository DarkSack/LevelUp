# LevelUp

RPG-style personal development mobile app. Your life is the game.

## Stack

- **Mobile**: Expo (React Native) + TypeScript, React Navigation, Zustand, React Native Paper
- **Offline**: Event-log pattern with `client_id` UUIDs

## Structure

```
LevelUp/
└── mobile/                       Expo app
    └── src/
        ├── api/                  Typed API calls
        ├── screens/              Home, Challenges, Profile, ...
        ├── navigation/           Root navigator
        ├── store/                Zustand stores
        ├── theme/                Colors, typography
        └── types/                Shared TS types
```

## First-time setup

```bash
cd mobile
npm install
npx expo start
```

## Design principles

- **Offline-first**: pending completions queued locally with `client_id`.
- **Deterministic daily challenges**: `(user_id, date)` seeds the generator, so re-runs are idempotent.
