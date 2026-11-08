# Progress

Checkpoint for resuming work (e.g., after a usage limit). Update at the end of every step.

## How to resume
1. Read this file, then `docs/SPEC.md`.
2. `git status` / `git log --oneline -10` to see where things stand.
3. Continue with the first unchecked item below.

## Milestones
- [x] M0 Repo, docs, GitHub — GitHub organization `ehligible` (moved there 2026-10-30 so no public address shows
      the owner's username): app repo https://github.com/ehligible/ehligible, website repo
      `ehligible/ehligible.github.io` served at https://ehligible.github.io/ (privacy policy: `/privacy.html`).
      App ID `ca.ehligible` (iOS bundle ID the same). Release secrets (upload keystore) not set yet:
      they wait for the Play account (`docs/publishing/google-play.md`).
- [x] M1 Gradle KMP/CMP skeleton + Android CI
- [x] M2 Rules engine (TDD) — IRCC worked examples, mutation-checked
- [x] M3 Presence model, inference, trips, export, backup codec, DataStore repository, border index
- [x] M4 Shared Compose UI (EN/FR) + fun touches (maple-leaf progress, confetti, one-liners, fun facts)
- [x] M5 Android detection (foreground fix + network country + 4-hourly worker) + notifications
- [x] M6 Android Glance widget (small / wide / large), pin-from-app button, midnight refresh
- [x] M7 Play release prep: signed v1.0 AAB verified on the emulator, EN/FR listing + graphics + screenshots,
      privacy/support site, `docs/publishing/google-play.md`, tag-triggered release workflow
- [x] M8 Background location (v1.1) behind `ehligible.backgroundLocation` — verified on the emulator: the fix
      is delivered while the app is closed and recorded; shipping steps in `docs/publishing/background-location.md`
- [x] M9 iOS app + WidgetKit — verified in CI (macOS 26.6, Xcode 26.6): Kotlin tests on an iPhone simulator,
      Swift widget tests, app build, launch (onboarding) and the seeded home screen, same numbers as Android.
      The widget's look in every size, state and language is rendered by a test. All of it is in the
      `ios-screenshots` artifact of the `iOS` workflow (`widget/widget-en.png`, `widget-fr.png`).
- [x] M10 Real-device test — the owner installed the closed-test build from Google Play on their Android phone
      (reported 2026-11-08): no issues besides the two updates below.

## Current step
Google Play closed test is **live** on "Closed testing - Alpha" (Canada): 1.0.0 (version code 1) approved and
published 2026-11-03; **1.0.1 (version code 2) submitted 2026-11-04**, in review. 1.0.1 pins androidx.fragment
1.9.1 because Play flagged the 1.0.0 one (1.1.0, pulled in by Play services) as an outdated SDK; signed files in
`C:\DevProjects\ehligible-release\1.0.1\`. Play Console was set up 2026-10-31 by Claude in the owner's Chrome
(package `ca.ehligible`), the owner approving each legal step. Opt-in link:
https://play.google.com/apps/testing/ca.ehligible. Testers: since 2026-11-08 the Google Group
`ehligible-testers@googlegroups.com` (anyone can join; the owner's 2 addresses are in) and all countries, sent for
review that day. Recruiting texts: `docs/publishing/tester-invite.md`. After 14 days with at least 12 opted in,
apply for production.
Account: trader status declared **non-trader** by the owner on 2026-11-04. The owner changed the public developer
name to "mirhamedjz" on 2026-11-04 (it was "Hamed JZ"). Play's remaining warning: no
native debug symbols (DataStore's small native library; harmless).

## Next updates (requested by the owner 2026-11-08; not started)
1. **Widget opacity** (main ask), maybe a few background colours. Today `androidApp/.../widget/CountdownWidget.kt`
   hard-codes `BACKGROUND = ColorProvider(day 0xFFFFF8F5, night 0xFF1A1112)` (white in light mode, dark in dark
   mode, which the owner likes), with INK/ACCENT and the leaf colours (`LeafBitmap.kt`) following day/night.
   Plan: a "Widget" section in Settings with a style (Match phone / Light / Dark, maybe a maple or blue tint)
   and an opacity slider (0–100%), saved with the app's settings and applied with `updateAll`; text and leaf
   colours follow the style so they stay readable when the background is see-through. iOS widgets can't be
   transparent, so on iOS only the colour applies (hide the slider there).
2. **Back animation is inconsistent.** Settings → back animates differently from Trips → back, and afterwards
   Trips does it too. `composeApp/.../ui/Navigation.kt` uses NavHost's default transitions (700 ms crossfade).
   On Android 14+ (targetSdk 36) the back gesture drives the pop transition (predictive back), while the top-bar
   arrow plays the timed fade, so how you go back changes the animation. Plan: reproduce on the emulator with
   gesture navigation (slow it down with `adb shell settings put global animator_duration_scale 10`), then set
   explicit, matching enter/exit/popEnter/popExit transitions (e.g., a short horizontal slide + fade, ~300 ms)
   so the arrow and the gesture look the same.
3. Ship both as 1.0.2 (version code 3) on the closed track; ask the owner before sending it for review.
4. ~~Testers setup~~ done 2026-11-08 (Google Group, all countries, recruiting posts in `tester-invite.md`); the
   owner recruits.

## Usage limits (owner asked to resume right after a reset)
- Built-in `autoContinueAtUsageLimit` is on (~/.claude/settings.json); it resumes after a reset if this window
  stays open and the laptop stays awake.
- The status line (`~/.claude/usage/statusline.py`) writes `~/.claude/usage/limits.json` (5h/7d used % and
  `resets_at`) and appends reset times to `limits.log`; a StopFailure/Notification hook logs limit hits and
  auto-resume events to `limit-events.log`.
- Auto-resume cron: removed on 2026-10-30 when the build work was done (owner's request). While unattended work
  remains, schedule one one-shot CronCreate ~2 min after `five_hour.resets_at` as a backup, and delete it when the
  work is done. Don't use frequent recurring crons: one firing mid-wait likely cancelled the auto-wait.

## Conventions
- Branch per milestone (`feat/...`), logical commits, merge `--no-ff` into main; run
  `./gradlew spotlessApply check :androidApp:assembleDebug` before merging (and with
  `-Pehligible.backgroundLocation=true` when touching detection).
- Two repos: the app here, and the website in `C:\DevProjects\ehligible.github.io` (plain HTML; pushing to `main`
  republishes it). Keep `https://ehligible.github.io/privacy.html` stable: the app and the Play listing link to it.
- Don't `cd` into subfolders in Bash (it moves the session's working directory); use absolute paths.
- Emulator helpers: `$LOCALAPPDATA/Temp/ehligible-ref/adbui.sh` (AVD `ehligible_pixel8`; debug app
  `ca.ehligible.debug`).
- When the owner's input or help is needed, send a phone notification (PushNotification).

## Waiting on the owner
1. ~~Back up the upload key~~ (done) and ~~create the Play account and app~~ (done, verified 2026-10-31).
2. Recruit testers with the texts in `docs/publishing/tester-invite.md` (friends, tester-exchange communities,
   newcomer groups) and test back in the exchanges; 14 days with at least 12 opted in, then apply for production.
3. Add the three signing secrets to `ehligible/ehligible` so tagged releases build signed bundles.
4. When v1.1 ships: upload the declaration video to YouTube as Unlisted (script in `background-location.md`).
5. Apple Developer account and a custom domain: later, if the owner decides.

## Notes
- Toolchain: JDK 17 (`C:\Users\mirha\dev\jdk17`), Android SDK `C:\Users\mirha\dev\android-sdk` (platform 36,
  build-tools 36, emulator + `system-images;android-36;google_apis_playstore;x86_64`, WHPX works).
- Library versions checked 2026-10-29: Kotlin 2.4.21, AGP 9.5.0, CMP 1.12.2, Gradle 9.8.2, Glance 1.2.0,
  WorkManager 2.12.0, DataStore 1.3.0, kotlinx-datetime 0.8.0, serialization 1.12.0, coroutines 1.11.0.
- Upload keystore lives outside the repo: `~/.ehligible/upload-keystore.jks` + `keystore.properties`.
- Emulator quirks: its network country is "us" (use airplane mode for screenshots); it has no Wi-Fi/cell
  positioning, so balanced-power background updates only arrive while another app (Maps) uses GPS.
- Kotlin package names are still `io.github.mirhamedjz.ehligible` (internal, not shown to users).
