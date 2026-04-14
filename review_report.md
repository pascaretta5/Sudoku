# Sudoku Android App Review

**Repository reviewed:** [pascaretta5/Sudoku](https://github.com/pascaretta5/Sudoku)  
**Branch:** `master`  
**Commit reviewed:** `b9ec459be094fdb02ca4e5cbe9c09b377a98b0f4`

## Executive summary

Your app already has a strong visual direction and a playable core loop. The repository shows a clear intention to become a polished Sudoku product with settings, daily challenges, achievements, leaderboards, hints, notes, undo, audio, and a completion screen. However, the code currently has a noticeable gap between the **feature surface presented in the UI** and the **features that are actually implemented**. The most valuable improvements are therefore not cosmetic first; they are about making core promises reliable, persistent, and internally consistent. [1] [2] [3]

The highest-impact next step is to strengthen the app around four pillars: **state persistence**, **feature completion**, **settings architecture**, and **Android maintainability**. Once those are in place, the app will feel much more trustworthy and ready for wider release. [2] [3] [4] [5]

## Highest-priority improvement areas

| Priority | Area | Why it matters | Evidence |
|---|---|---|---|
| 1 | Real save/load and lifecycle resilience | The app promises automatic progress saving, and the code even models saved games, but gameplay state is not actually persisted when the activity closes or is recreated. This is the biggest trust issue for users. | [2] [4] [6] |
| 2 | Finish the features already visible on the home screen | Daily Challenge, Achievements, and Leaderboards are visible product promises, but two buttons are dead ends and Daily Challenge currently launches a normal game. | [1] [3] |
| 3 | Make Settings affect gameplay for real | Several preferences are saved, but important ones are not applied to the board or game logic, which makes the settings screen feel disconnected. | [5] [7] |
| 4 | Complete audio implementation | SoundManager is called throughout the app, but most audio methods are placeholders. This weakens polish and creates misleading settings behavior. | [2] [3] [5] [8] |
| 5 | Improve architecture and build readiness | The app mixes UI, game state, persistence intent, and feature toggles directly inside activities. It also uses a modern Android plugin setup that expects a newer JDK than some environments provide. | [4] [5] [8] [9] |

## Detailed findings

### 1. Save/load is the most important missing capability

The strings file tells users that their progress will be saved automatically when they exit a game, but the gameplay activity does not persist the board, timer, selected difficulty, or note state in `onPause`, `onStop`, or `onSaveInstanceState`. Instead, `GameActivity` creates a fresh `GameState` in `onCreate`, and `GameState` only exposes an in-memory `createSaveGame()` model with no repository or storage layer connected to it. [2] [4] [6]

This is the single largest product-risk area because Sudoku sessions are often interrupted by calls, app switching, or process death. A puzzle game feels unreliable very quickly if the user cannot trust that their board will still be there later. I would treat this as the first release-blocking improvement.

| Recommendation | Practical implementation |
|---|---|
| Add real persistence for active games | Store board values, notes, elapsed time, hints used, difficulty, and challenge metadata in a single persistence layer, preferably using Room or DataStore-backed JSON if you want something lighter at first. |
| Restore game state automatically | On app launch or when entering the game screen, detect an unfinished session and offer **Resume** instead of always starting a new puzzle. |
| Support configuration changes properly | Save transient UI state such as selected cell and notes mode, or move the whole game session into a `ViewModel` with `SavedStateHandle`. |
| Align product copy with reality | Until autosave is implemented, remove or reword the exit message to avoid overpromising. |

### 2. The app advertises more features than it currently delivers

The README and home screen position the app as a fuller Sudoku experience, but the current implementation still routes the Daily Challenge button to a normal game and leaves Achievements and Leaderboards without navigation targets. On the completion screen, the “new record” banner is also simulated with a hardcoded `score > 5000` rule instead of real leaderboard logic. [1] [3] [10]

This matters because users judge app quality not only by what is present, but by whether buttons and labels mean what they say. Right now the product surface is slightly ahead of the implementation. That is normal in a prerelease, but tightening this gap will improve perceived quality immediately.

| Recommendation | Practical implementation |
|---|---|
| Implement a deterministic daily challenge | Seed puzzle generation by UTC date plus difficulty so every player gets the same board that day. Store completion status and streaks. |
| Either build or temporarily hide unfinished sections | If Achievements and Leaderboards are not ready, disable the buttons with explanatory text instead of leaving them as silent no-ops. |
| Replace fake record logic with persisted stats | Save best times, highest scores, streaks, and challenge completions in structured storage. |
| Add lightweight progression | Even a first version with local-only achievements and personal best tables would make the app feel substantially more complete. |

### 3. Settings are stored, but not fully connected to behavior

The settings screen persists options for sound effects, background music, vibration, highlight-same-numbers, auto-check, volume, and theme. However, the board view always highlights same-number patterns when a cell is selected, regardless of the setting, and there is no gameplay logic that uses the auto-check preference. Theme selection is also saved, but there is no visible app-wide theming mechanism applied across screens. [5] [7]

This is a strong opportunity because you already designed the feature set. The remaining work is mostly integration and architecture rather than invention.

| Setting | Current state | Improvement |
|---|---|---|
| Highlight same numbers | Always active in `SudokuBoardView` | Inject preference into the view and redraw conditionally. |
| Auto-check | Saved only | Validate the row, column, and box after each move and surface mistakes visually when enabled. |
| Theme | Saved only | Introduce a central theme manager and switch color resources or Material theme overlays consistently. |
| Vibration | Saved only | Trigger haptics for selection, errors, completion, or hints using the saved preference. |
| Volume/music toggles | UI exists, but backend is incomplete | Complete audio handling and keep a single source of truth for preferences. |

### 4. Audio polish is currently underimplemented

`SoundManager` initializes a `SoundPool` and loads click and completion sounds, but `updateSettings`, `resumeBackgroundMusic`, `pauseBackgroundMusic`, `playError`, `playNumberPlaced`, `playHint`, and `playGameComplete` are effectively empty. Despite that, these methods are already called by multiple activities and gameplay actions. [2] [3] [5] [8]

That means the app already pays the complexity cost of an audio subsystem without yet getting the user experience benefit. Completing this subsystem would improve responsiveness and make the settings screen feel meaningful.

| Recommendation | Practical implementation |
|---|---|
| Separate music and effects concerns | Use `MediaPlayer` or ExoPlayer for looping background music and `SoundPool` for short effects. |
| Apply saved preferences on init | Read one preference source during startup and update volumes immediately. |
| Cover all gameplay events | Add distinct sounds for number entry, invalid moves, hint use, pause/resume, and completion. |
| Handle lifecycle centrally | Move repeated pause/resume audio behavior into a lifecycle-aware component or base activity. |

### 5. Core game architecture will benefit from a refactor before feature growth

The app currently places most orchestration inside activities: starting games, wiring controls, managing timer updates, and invoking board mutations directly from UI code. `SudokuBoard` contains move history and validation, `GameState` contains timing and score logic, and `SettingsActivity` manages raw preference keys inline. This works for a small app, but it will become harder to maintain as you add persistence, daily challenges, achievements, analytics, and richer settings. [2] [4] [5]

A clean next step would be to introduce a more explicit architecture, such as **MVVM with a repository layer**. That would let the activity become a thin UI shell, while a `GameViewModel` owns game state, persistence coordination, and one-shot UI events.

| Refactor target | Why it helps |
|---|---|
| `GameViewModel` | Keeps timer, selected cell, notes mode, hint counts, and board mutations off the activity lifecycle. |
| `PreferencesRepository` | Replaces repeated string keys and makes settings observable across screens. |
| `GameRepository` | Unifies puzzle generation, active save restoration, and completed-game stats. |
| UI state model | Makes the board screen easier to test and safer to evolve. |

### 6. Gameplay UX can become much stronger with small rule-aware improvements

The current board supports notes, hints, undo, and same-number highlighting, which is a very good baseline. However, there is no visible invalid-entry feedback in the board renderer, no redo control exposed in the game UI, and no evidence of candidate auto-cleanup, duplicate highlighting, or contextual disable states in the number pad. `SudokuBoardView` already contains placeholder paint objects for error and hint states, which suggests this was planned but not completed. [2] [7] [11]

These are relatively small product upgrades compared with save/load, but they create a noticeably more premium Sudoku feel.

| Suggested UX upgrade | Why users notice it |
|---|---|
| Auto-check with optional error highlighting | Reduces frustration and makes settings useful. |
| Duplicate conflict highlighting | Helps players understand mistakes immediately. |
| Redo button in UI | Completes the undo/redo interaction model already present in `SudokuBoard`. |
| Smart number pad states | Dim completed numbers or highlight counts to improve scan speed. |
| Resume-last-game entry point | Makes the app feel polished and dependable. |

### 7. Android project hygiene needs some tightening

The project is configured with AGP `8.8.1` and Kotlin `2.0.0`, while the app module itself targets Java 11 language compatibility. In my validation attempt, the debug build did not complete under a Java 11 runtime because the Android Gradle Plugin expects Java 17 at build time. The dependency setup also includes Compose libraries and Compose build features even though the visible app implementation is classic XML/View-based, which adds complexity without obvious current benefit. [8] [9]

This is not the most urgent user-facing issue, but it is important for contributor experience and future maintenance.

| Recommendation | Practical implementation |
|---|---|
| Standardize build prerequisites | Document JDK 17+ in the README and optionally set `org.gradle.java.home` guidance for contributors. |
| Decide on UI direction | Either continue with XML/View and remove unused Compose dependencies for now, or start a deliberate migration plan. |
| Add CI checks | A simple GitHub Actions workflow for assemble, unit tests, and lint would prevent regressions. |
| Add lint/test coverage | Start with generator tests, board validation tests, and state persistence tests. |

## Suggested roadmap

A sensible roadmap would be to treat the next milestone as a **stability and credibility release** rather than a feature-sprawl release. First, implement real save/load and resume flow. Second, wire settings into actual gameplay behavior and complete the audio system. Third, either finish or hide unfinished home-screen features. Fourth, refactor state management into a `ViewModel` and repository structure before adding leaderboards or challenge streak systems at scale. [2] [3] [4] [5] [8]

If you follow that order, the app will feel much more complete even before adding cloud sync or competitive features. In other words, the fastest way to improve the app is not necessarily to add many new features, but to make the existing feature promises fully true.

## Best next three actions

| Order | Action | Outcome |
|---|---|---|
| 1 | Implement persistent active-game save/load and resume | Removes the largest trust gap in the current app. |
| 2 | Finish settings integration and audio backend | Makes the app feel coherent and polished. |
| 3 | Ship a real daily challenge plus local stats/records | Turns placeholder product surface into an actual retention feature. |

## References

[1]: https://github.com/pascaretta5/Sudoku/blob/master/README.md "Sudoku README"
[2]: https://github.com/pascaretta5/Sudoku/blob/master/app/src/main/java/com/example/sudokumain/GameActivity.kt "GameActivity.kt"
[3]: https://github.com/pascaretta5/Sudoku/blob/master/app/src/main/java/com/example/sudokumain/MainActivity.kt "MainActivity.kt"
[4]: https://github.com/pascaretta5/Sudoku/blob/master/app/src/main/java/com/example/sudokumain/model/GameState.kt "GameState.kt"
[5]: https://github.com/pascaretta5/Sudoku/blob/master/app/src/main/java/com/example/sudokumain/SettingsActivity.kt "SettingsActivity.kt"
[6]: https://github.com/pascaretta5/Sudoku/blob/master/app/src/main/res/values/strings.xml "strings.xml"
[7]: https://github.com/pascaretta5/Sudoku/blob/master/app/src/main/java/com/example/sudokumain/view/SudokuBoardView.kt "SudokuBoardView.kt"
[8]: https://github.com/pascaretta5/Sudoku/blob/master/app/src/main/java/com/example/sudokumain/util/SoundManager.kt "SoundManager.kt"
[9]: https://github.com/pascaretta5/Sudoku/blob/master/gradle/libs.versions.toml "libs.versions.toml"
[10]: https://github.com/pascaretta5/Sudoku/blob/master/app/src/main/java/com/example/sudokumain/GameCompleteActivity.kt "GameCompleteActivity.kt"
[11]: https://github.com/pascaretta5/Sudoku/blob/master/app/src/main/java/com/example/sudokumain/model/SudokuBoard.kt "SudokuBoard.kt"
