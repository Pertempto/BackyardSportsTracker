# BackyardSportsTracker

## Debug APK signing

Debug builds use the repository's fixed `app/debug.keystore` (alias `androiddebugkey`, password `android`). This keeps debug APK signatures consistent across local builds and CI so an APK can update an existing installation. The key is intentionally public and must never be used to sign a release build.

## Data backups

Use **Settings → Export Data** to save a versioned JSON backup through Android's file picker. **Import Data** validates a backup before atomically replacing the current database. Keep a backup before switching signing keys or uninstalling the app.

## Game history

Use **Edit** on a game to correct its score or roster. The app replays that sport's game history so later rating snapshots and player ratings remain consistent. Deleted games are omitted from player history.
