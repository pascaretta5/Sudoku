# Sudoku Android App Implementation Plan

**Repository:** [pascaretta5/Sudoku](https://github.com/pascaretta5/Sudoku)  
**Planning basis:** prioritized review findings from the current repository state on `master` at commit `b9ec459be094fdb02ca4e5cbe9c09b377a98b0f4` [1] [2] [3] [4] [5] [6]

## Implementation objective

The objective of this plan is to turn the app from a **visually promising prerelease** into a **stable, trustworthy, and feature-coherent Sudoku product**. The implementation order deliberately favors reliability and feature truthfulness over surface expansion. In practical terms, that means the first milestone is not new content, but ensuring that game sessions persist correctly, settings have real effect, and visible features on the home screen do what they claim. [1] [2] [3] [4] [5]

The recommended delivery sequence is organized into four execution tracks. The first two tracks should be treated as release-critical because they directly affect user trust and retention. The latter two tracks improve long-term maintainability and product depth.

| Track | Focus | Priority | Release impact |
|---|---|---|---|
| A | Save/load, lifecycle resilience, and resume flow | Critical | Release-blocking |
| B | Settings integration and audio completion | Critical | Release-blocking |
| C | Daily challenge, local records, achievements, and leaderboard foundations | High | Major feature milestone |
| D | Architecture cleanup, build hygiene, and testing | High | Long-term acceleration |

## Delivery strategy

The recommended strategy is to work in **vertical slices** rather than isolated technical components. Each milestone should produce something testable in the running app. This reduces the risk of accumulating partially connected subsystems such as the current settings screen and unfinished feature buttons. [2] [3] [5] [6]

A practical cadence would be to complete the work in five milestones. Milestones 1 and 2 focus on user trust. Milestone 3 turns placeholder product surface into real gameplay value. Milestones 4 and 5 reduce technical debt and create a foundation for future expansion.

| Milestone | Theme | Suggested duration | Primary outcome |
|---|---|---|---|
| 1 | Persistent active game sessions | 4–6 days | Resume-last-game works reliably |
| 2 | Settings and audio become real | 3–5 days | Preferences visibly affect gameplay |
| 3 | Daily challenge and local progression | 5–8 days | Core retention loop becomes real |
| 4 | Architecture refactor | 5–7 days | Game logic moves out of activities |
| 5 | Testing, CI, and release hardening | 2–4 days | Safer future iteration |

## Milestone 1: Persistent active game sessions

This milestone addresses the largest credibility gap in the current app. The strings promise that progress will be saved automatically, while `GameActivity` currently creates a new `GameState` on each entry and does not persist the active session in a recoverable way. `GameState` already contains a `SaveGame` data model, so the implementation should build on that instead of inventing a second persistence shape. [2] [4] [7]

### Scope

The purpose of this milestone is to ensure that a Sudoku session survives app backgrounding, navigation away from the game, process death, and deliberate exit from the board screen.

### Technical design

The cleanest short-term implementation is to create an **ActiveGameRepository** backed by a single local persistence source. For this app, there are two reasonable options. The first option is **Room**, which is more scalable if you plan multiple save slots, history, achievements, and records. The second option is **DataStore or SharedPreferences-backed JSON serialization**, which is faster to implement for a single active session. Because your roadmap already includes challenges and records, Room is the stronger medium-term choice.

| Decision point | Recommendation | Reason |
|---|---|---|
| Storage layer | Room | Better for future save slots, challenges, and stats |
| State owner | `GameViewModel` | Survives configuration changes and separates UI from logic |
| Save trigger | `onStop` plus explicit exit/save events | Safer than waiting for manual save only |
| Restore policy | Detect unfinished game on launch and offer resume | Makes persistence visible and user-friendly |

### Implementation tasks

| ID | Task | Details | Dependency |
|---|---|---|---|
| A1 | Define persistence entities | Create `ActiveGameEntity`, `CellEntity` or a serialized board blob, and metadata fields for difficulty, elapsed time, hints used, notes mode, selected cell, and daily-challenge flag. | None |
| A2 | Add repository layer | Implement `GameRepository` or `ActiveGameRepository` with `saveActiveGame`, `loadActiveGame`, `clearActiveGame`, and `hasActiveGame`. | A1 |
| A3 | Refactor `GameState` serialization | Reuse `GameState.SaveGame` concepts but move serialization responsibilities out of the UI layer. | A1 |
| A4 | Create `GameViewModel` | Move timer ownership, selected cell, notes mode, current board, and move actions into the ViewModel. | A2 |
| A5 | Restore flow in `GameActivity` | Load existing game when applicable instead of always constructing a new puzzle. | A4 |
| A6 | Add resume entry on home screen | Show **Resume Game** when unfinished progress exists, or intercept Play flow with a resume dialog. | A2 |
| A7 | Wire lifecycle saving | Save on `onStop`, on pause dialog exit, and before launching settings if needed. | A4 |
| A8 | Align strings and UX copy | Keep “saved automatically” only if implementation is reliable; otherwise change copy until complete. | A5 |

### UX changes

The user should encounter a visible and trustworthy resume path. The home screen should surface either a **Resume Last Game** button or a modal when the player taps Play and there is an unfinished session. If the unfinished game is a daily challenge, the resume copy should state that explicitly.

### Acceptance criteria

| Scenario | Expected result |
|---|---|
| User backgrounds app mid-game and returns | Board, notes, timer, hints used, and selected puzzle state are restored correctly |
| App process is killed and reopened | Unfinished game can be resumed without data loss |
| User exits from pause dialog | Game is still recoverable on next app open |
| User completes game | Active save is cleared and completion flow starts fresh |
| User starts a new game intentionally | Previous active session is replaced only after confirmation |

### Risks and mitigations

The main risk is state fragmentation if timer, board, and UI state are partly kept in the activity and partly in storage. That is why the ViewModel step should happen in the same milestone rather than later.

## Milestone 2: Settings and audio become real

The settings screen is already present and visually substantial, but several preferences are currently saved without being applied to gameplay. In parallel, `SoundManager` is called broadly across the app even though most of its functionality is still placeholder code. This milestone should make settings immediately observable and trustworthy. [2] [5] [6]

### Scope

This milestone covers sound effects, background music, volume, vibration, highlight-same-numbers, auto-check, and theme application.

### Technical design

Introduce a **PreferencesRepository** as the single source of truth. Activities and views should stop reading raw preference keys directly. Instead, they should consume a settings model such as `GamePreferences(soundEffectsEnabled, backgroundMusicEnabled, vibrationEnabled, highlightSameNumbers, autoCheck, volume, theme)`. [5] [6]

| Component | Change | Reason |
|---|---|---|
| `SettingsActivity` | Read/write through repository | Avoid raw string-key duplication |
| `SudokuBoardView` | Accept render settings from the ViewModel/UI state | Lets highlight behavior reflect preferences |
| `GameActivity` | Apply auto-check and vibration settings to interactions | Makes settings meaningful |
| `SoundManager` | Become fully implemented and preference-aware | Removes placeholder behavior |

### Implementation tasks

| ID | Task | Details | Dependency |
|---|---|---|---|
| B1 | Create `PreferencesRepository` | Wrap all current settings keys and expose typed getters/setters or reactive flows. | None |
| B2 | Apply highlight-same-numbers toggle | Update `SudokuBoardView.highlightCells()` to conditionally render same-number emphasis based on user preference. | B1 |
| B3 | Implement auto-check | After each user move, validate row/column/box and mark conflicts when auto-check is enabled. | B1 |
| B4 | Add board conflict state | Extend `Cell` or derived UI state to represent invalid/conflicting cells. | B3 |
| B5 | Implement theme switching | Map saved theme choice to app-wide colors or Material theme overlays. | B1 |
| B6 | Implement vibration/haptics | Add haptic feedback for invalid moves, completion, and optionally selection. Respect preference. | B1 |
| B7 | Complete `SoundManager` | Implement music lifecycle, volume handling, and missing sound methods such as error, hint, number placement, and completion. | B1 |
| B8 | Apply settings on launch | Ensure all screens initialize from persisted preferences consistently. | B1, B5, B7 |

### Audio architecture recommendation

Short sound effects should stay in `SoundPool`. Background music should move to `MediaPlayer` or another looping audio mechanism. The manager should initialize once, load preferences immediately, and expose explicit methods for gameplay events rather than generic string-based calls.

### Acceptance criteria

| Scenario | Expected result |
|---|---|
| User disables highlight-same-numbers | Board no longer highlights matching values beyond the selected-cell rule |
| User enables auto-check and places conflicting number | Conflict is shown immediately |
| User disables background music | Music stops and stays off across app restarts |
| User changes volume | Effects and music respond without requiring app restart |
| User changes theme | All major screens reflect the chosen visual theme consistently |
| User disables vibration | No haptic feedback occurs during play |

## Milestone 3: Daily challenge and local progression

This milestone converts the app’s visible but incomplete feature surface into a real retention loop. Right now the Daily Challenge button routes to a normal game, Achievements and Leaderboards do not open implemented screens, and the completion screen uses a hardcoded threshold for “new record.” This milestone should create a fully local first version, even if online competition comes later. [1] [3] [8]

### Scope

The goal is to implement a deterministic daily challenge, persistent local records, a first-pass achievements system, and basic leaderboard-style stats pages.

### Product rules

The daily challenge should generate the same puzzle for all users on the same date. The simplest approach is to seed puzzle generation using a stable value derived from the calendar date and a defined challenge difficulty. Completed daily challenges should be recorded once per date to prevent repeat farming.

| Feature | First version recommendation |
|---|---|
| Daily challenge | One puzzle per day, seeded by date, fixed difficulty or rotating pattern |
| Achievements | Local-only badges such as first win, no-hint win, streak milestones, fast solve, notes usage |
| Leaderboards | Local personal bests by difficulty, best time, highest score, daily streak |
| Completion screen | Real “new record” check based on saved stats |

### Implementation tasks

| ID | Task | Details | Dependency |
|---|---|---|---|
| C1 | Add challenge metadata to generator flow | Support deterministic generation from a seed and challenge type. | A2 or generator refactor |
| C2 | Define daily challenge record storage | Persist date, completion state, score, time, and streak contribution. | A2 |
| C3 | Implement daily challenge launcher | Replace placeholder in `MainActivity` with real challenge creation/resume behavior. | C1, C2 |
| C4 | Create local records repository | Store best times, best scores, total completed games, hintless wins, and streaks. | A2 |
| C5 | Build Achievements screen | Start with local computed achievements, not network-backed achievements. | C4 |
| C6 | Build Leaderboards/Stats screen | Show personal best tables and challenge history. | C4 |
| C7 | Replace fake new-record logic | Compare current result to actual persisted records in `GameCompleteActivity`. | C4 |
| C8 | Add progress reset integration | Ensure reset clears daily progress, achievements, records, and active game consistently. | C2, C4 |

### UX notes

The daily challenge should feel distinct from standard games. Use a badge or color accent on the challenge card and clearly label the game screen when a daily challenge is active. Because the user prefers a colorful design, this is also a good place to introduce celebratory visual identity without changing core gameplay. 

### Acceptance criteria

| Scenario | Expected result |
|---|---|
| User taps Daily Challenge on the same day twice | Same puzzle is presented or resumed |
| User completes the challenge | Result is recorded once, streak updates, and completion appears in stats |
| User opens Achievements | Screen shows earned and locked achievements based on actual data |
| User opens Leaderboards/Stats | Personal bests and aggregate stats are shown from stored records |
| User earns a new personal best | Completion screen shows the record state based on real comparison |

## Milestone 4: Architecture refactor

This milestone is about preventing future slowdown. The current codebase is still small enough that architecture improvements will be inexpensive. If delayed until after more feature growth, refactoring will become much more painful. The goal is to move from activity-led orchestration to a cleaner separation between UI, domain logic, and persistence. [2] [4] [5] [6]

### Target architecture

A pragmatic target is **MVVM plus repository layer**.

| Layer | Responsibility |
|---|---|
| UI (Activities/Views) | Render state, forward user input, show dialogs/navigation |
| ViewModels | Own screen state, timers, feature toggles, validation results, and one-shot events |
| Repositories | Persistence, preferences, records, achievements, and active-session loading |
| Domain/Game logic | Board rules, scoring, generator behavior, conflict detection |

### Implementation tasks

| ID | Task | Details | Dependency |
|---|---|---|---|
| D1 | Introduce `GameUiState` | Represent board, timer text, hint counts, notes mode, selected cell, and conflict markers. | A4 |
| D2 | Move timer logic to ViewModel | Remove manual `Handler` ownership from activity where possible. | D1 |
| D3 | Introduce event model | Navigation and dialogs should be emitted as UI events instead of being scattered across mutation methods. | D1 |
| D4 | Centralize intent extras | Replace raw string keys like `"DIFFICULTY"` with constants or typed navigation helpers. | None |
| D5 | Centralize preference keys | Eliminate duplicated raw strings in `SettingsActivity`. | B1 |
| D6 | Separate renderer data from mutable board model | Prevent direct view mutation risks by exposing immutable UI snapshots to `SudokuBoardView`. | D1 |

### Acceptance criteria

| Metric | Target state |
|---|---|
| `GameActivity` responsibilities | Mostly rendering and click handling, not state ownership |
| Raw preference keys | Encapsulated in repository/constants |
| Intent extra strings | Centralized and typed |
| Board rendering state | Derived from immutable UI state rather than direct mutable model access |

## Milestone 5: Testing, CI, and release hardening

Once the prior milestones are implemented, the app will finally justify lightweight automation. The current project would benefit from tests around generator correctness, board validation, persistence, and daily challenge determinism. Build hygiene also matters because the current setup uses AGP `8.8.1`, which expects Java 17 at build time, while not all environments may be configured accordingly. [6] [9]

### Implementation tasks

| ID | Task | Details | Dependency |
|---|---|---|---|
| E1 | Document local build requirements | Update README with Android Studio version guidance and JDK 17 requirement. | None |
| E2 | Add unit tests for Sudoku rules | Cover row/column/box validation, undo/redo, note behavior, and scoring. | D-layer stabilization |
| E3 | Add persistence tests | Verify save/restore of active sessions and reset flows. | A2 |
| E4 | Add deterministic challenge tests | Same seed/date should produce same challenge. | C1 |
| E5 | Add CI workflow | Run assemble, unit tests, and lint on each push/PR. | E1 |
| E6 | Decide on Compose direction | Remove unused Compose dependencies for now or begin intentional migration. | None |

### Acceptance criteria

| Area | Expected result |
|---|---|
| Build documentation | New contributors can build without guessing JDK requirements |
| Automated tests | Core game rules and persistence paths are covered |
| CI | Repository reports build/test status automatically |
| Dependency hygiene | Unused UI stacks are removed or justified |

## Recommended work breakdown by week

This schedule assumes one active developer and aims for realistic sequencing.

| Week | Focus | Deliverable |
|---|---|---|
| Week 1 | Active game persistence and resume flow | Users can resume interrupted games |
| Week 2 | Settings repository, board integration, audio completion | Preferences visibly affect play |
| Week 3 | Daily challenge and local stats repository | Real challenge loop and records |
| Week 4 | Achievements/stats screens and completion-screen accuracy | Feature surface matches implementation |
| Week 5 | Architecture cleanup and test coverage | Easier future development |
| Week 6 | CI, bug fixing, release polish | Stable prerelease or beta candidate |

## Suggested code structure changes

A more maintainable package layout would make future work easier. The current structure is serviceable, but it is still mostly screen-by-screen and utility-driven. A domain-oriented package structure will scale better.

| Current concern | Suggested package |
|---|---|
| Activities and screen logic | `ui/main`, `ui/game`, `ui/settings`, `ui/stats` |
| Persistent preferences | `data/preferences` |
| Active game storage | `data/game` |
| Records and achievements | `data/stats` |
| Sudoku rules and generation | `domain/sudoku` |
| Audio and haptics | `core/audio`, `core/feedback` |

## Definition of done for the full recommendation set

The recommendations should be considered fully implemented only when the app satisfies the following conditions: an interrupted game can always be resumed; every visible setting changes real behavior; the daily challenge is distinct and persistent; achievements and records are data-driven; no primary home-screen button leads to a dead end; the completion screen reflects actual stored stats; and the codebase can be built, tested, and extended without forcing activities to own business logic. [2] [3] [4] [5] [6] [8] [9]

## Immediate next sprint recommendation

If you want the highest return with the least strategic risk, the first sprint should include **A1 through A8** and **B1 through B3/B7**. In plain terms, that means: build active-game persistence, add a resume flow, introduce a preferences repository, make board highlighting respect settings, implement auto-check, and finish the audio backend enough that settings are no longer cosmetic. That sprint would already make the app feel dramatically more complete, even before daily challenges and records are finished.

## References

[1]: https://github.com/pascaretta5/Sudoku/blob/master/README.md "Sudoku README"
[2]: https://github.com/pascaretta5/Sudoku/blob/master/app/src/main/java/com/example/sudokumain/GameActivity.kt "GameActivity.kt"
[3]: https://github.com/pascaretta5/Sudoku/blob/master/app/src/main/java/com/example/sudokumain/MainActivity.kt "MainActivity.kt"
[4]: https://github.com/pascaretta5/Sudoku/blob/master/app/src/main/java/com/example/sudokumain/model/GameState.kt "GameState.kt"
[5]: https://github.com/pascaretta5/Sudoku/blob/master/app/src/main/java/com/example/sudokumain/SettingsActivity.kt "SettingsActivity.kt"
[6]: https://github.com/pascaretta5/Sudoku/blob/master/app/src/main/java/com/example/sudokumain/util/SoundManager.kt "SoundManager.kt"
[7]: https://github.com/pascaretta5/Sudoku/blob/master/app/src/main/res/values/strings.xml "strings.xml"
[8]: https://github.com/pascaretta5/Sudoku/blob/master/app/src/main/java/com/example/sudokumain/GameCompleteActivity.kt "GameCompleteActivity.kt"
[9]: https://github.com/pascaretta5/Sudoku/blob/master/gradle/libs.versions.toml "libs.versions.toml"
