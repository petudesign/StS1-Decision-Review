# Spire Coach

Spire Coach is a learning-first Slay the Spire 1 mod, currently supporting Ironclad only. It watches the player's own combat decisions, records the concrete state around them, and explains meaningful lessons after combat ends.

It is deliberately not an autoplayer, a best-move assistant, an LLM feature, or a general stats dashboard. The player acts first. Spire Coach stays silent when the evidence is weak or the difference is trivial.

## Why this exists

I did not want to spend 30 minutes searching for a YouTube tutorial that was both genuinely good and interesting enough to keep watching. I wanted to learn from my own decisions instead, so this project explores whether local gameplay data and well-timed explanations can make learning the game more useful.

The goal is not to tell the player what to do. Spire Coach looks at a completed decision from a coach's point of view: what was the situation, what did the player choose, what changed, and is there a concrete lesson worth explaining? It is intentionally not an autoplayer, card picker, best-move solver, or other cheat mod. The player still makes every decision.

## Requirements and scope

For the mod to work in-game, install:

- The original **Slay the Spire 1** (the Java-based game, for example through Steam).
- [ModTheSpire](https://steamcommunity.com/sharedfiles/filedetails/?id=1605060445).
- [BaseMod](https://steamcommunity.com/sharedfiles/filedetails/?id=1605833019).
- `SpireCoach.jar` in the game's `mods` directory, launched through ModTheSpire with BaseMod enabled.

This is a mod for **Slay the Spire 1 only**. It does not work with Slay the Spire 2. The current implementation also supports **Ironclad only**; other characters are intentionally ignored for now.

The game-side code is Java and does not depend on Windows-only APIs, so a macOS version should be possible when the original game, ModTheSpire, and BaseMod are available there. The current build helper and live verification are Windows-specific, so macOS packaging and runtime behavior still need to be verified separately.

## Data status and validation

This project is experimental. It has **not collected enough run data to claim that its card advice or coaching rules are generally correct**. The current advice is deterministic and hand-authored; it is not learned from the player's data, it is not a guaranteed best-play solver, and it can be wrong outside the supported Ironclad situations.

The mod writes raw, local NDJSON logs to the game's `logs/spirecoach/` directory. The logs are not uploaded to a server by this project. Run-level logs record card offers, selections, skips, combat outcomes, and the advice shown at the time, so the data can later be inspected and compared with outcomes. A single run is not proof of correctness; meaningful validation needs many runs and controlled comparisons.

## Current vertical slice

- Java 8, ModTheSpire, BaseMod, Maven
- Ironclad only
- Buffered local NDJSON combat and run logs under `logs/spirecoach/`
- Combat start, turn start, card plays, resolved state, and combat end capture
- Post-combat review overlay after every fight, including explicit "no high-confidence lesson" feedback
- Run summary after defeat or the final boss
- Card reward tracking: offers, chosen cards, skipped rewards, displayed advice, and deck-size changes are recorded
- One certain rule: a Strike played before Bash when applying Vulnerable first would have produced a meaningful damage increase
- Maximum two lessons per combat (only one rule currently exists)

## What it does now

During an Ironclad run, the mod observes the state around card plays and turn boundaries through BaseMod hooks. It compares the before and after snapshots, then applies small, deterministic coaching rules. When a combat ends, it shows either a concrete lesson or an explicit message that no high-confidence lesson was found.

At card rewards, it records the offered cards and explains the kind of problem a choice might solve. This is guidance, not an objectively correct answer: card rewards depend on the current deck, the route, upcoming threats, and the player's intended plan. The current card advice is hand-authored heuristics and has not yet been validated against enough runs.

The first combat lesson is deliberately narrow: when the logged state makes it unambiguous, the mod can explain that applying Vulnerable before a following Strike would have increased that attack's damage. Narrow rules are preferred while the data capture and validation process is still being established.

Example lesson:

> Card order mattered here.
>
> You played Strike before applying Vulnerable. Applying Vulnerable first would have increased the damage of the following attack.
>
> Lesson: Apply multipliers and enabling effects before the actions that benefit from them.

## Architecture

```text
Slay the Spire / BaseMod hooks
        -> GameStateCapture
        -> TurnSnapshot + ActionSnapshot
        -> CombatRecorder
        -> CoachingRule / CoachingEngine
        -> Lesson
        -> CombatReviewOverlay
        -> combat + run NDJSON loggers
```

The domain model and `SequencingRule` are in `src/main/java` and have no game imports. The live game adapter is in `src/game/java`, which keeps rule logic testable and leaves room for additional characters, rules, and offline analysis later.

## Local analysis

The raw NDJSON files are the source of truth. DuckDB is a good optional next layer for offline analysis: it is an embedded database that can read local files without a server or cloud account. It is not necessary for the game mod itself, and keeping it outside the Java 8 mod avoids an extra dependency and keeps Windows/macOS installs simpler.

The repository includes a dependency-free structural validator for the logs:

```powershell
python tools/validate_logs.py --log-dir "C:\Program Files (x86)\Steam\steamapps\common\SlayTheSpire\logs\spirecoach"
```

This checks that events are valid JSON and that recorded card-reward and run counters are internally consistent. It does **not** validate whether the coaching advice was strategically right. An eventual DuckDB importer can build queries and reports on top of these same raw logs without changing what the game records.

## Build

The game jar is intentionally not committed. Install Java 8, Maven, ModTheSpire, and BaseMod, then configure the jar paths with environment variables or Maven properties:

```powershell
$env:STS_JAR = 'C:\Path\to\SlayTheSpire\desktop-1.0.jar'
$env:BASEMOD_JAR = 'C:\Path\to\BaseMod.jar'
$env:MODTHESPIRE_JAR = 'C:\Path\to\ModTheSpire.jar'
mvn -Dgame-build=true package
```

Alternatively pass `-Dsts.jar=... -Dbasemod.jar=... -Dmodthespire.jar=...`. The package output is `target/SpireCoach.jar`; copy it into the game's `mods` directory and launch through ModTheSpire with BaseMod enabled.

On Windows, the checked-in helper uses the installed JDK, Maven, game jar,
BaseMod, and ModTheSpire paths automatically. It looks for the JDK, Maven, and
dependency jars under `%USERPROFILE%\Tools` by default; set `STS_COACH_TOOLS` if
your local tools live somewhere else:

```powershell
.\build.ps1
.\build.ps1 -Install
```

The `-Install` variant also copies `target/SpireCoach.jar` into the game's
`mods` directory. A new terminal may be needed before a plain `mvn` command sees
your own environment settings; the helper does not depend on that.

The default Maven build compiles the game-independent domain and runs the sequencing rule tests without requiring proprietary game jars:

```powershell
mvn test
```

## Roadmap

The roadmap is intentionally evidence-driven. Planned items are not claims that the current coach is already correct.

1. **Reliable instrumentation — current.** Capture combat decisions, card rewards, run outcomes, and the exact advice shown. Keep the raw local logs readable and structurally valid.
2. **Offline validation — next.** Collect enough varied runs to measure false positives, missed lessons, and whether the feedback is understandable. Use the raw NDJSON as the source of truth and optionally import it into local DuckDB for analysis.
3. **More trustworthy combat lessons.** Add narrowly defined rules for lethal opportunities, block and incoming damage, energy use, potion timing, and sequencing. Each rule should explain its evidence and stay silent when the state is ambiguous.
4. **Deck-building feedback.** Improve card-reward explanations with deck needs, scaling plans, consistency, and upcoming-act context without pretending there is one universally correct pick.
5. **Usability and platform verification.** Add a clearer run history, settings, export tools, and verify the installation/runtime flow on macOS as well as Windows.
6. **Broader character support.** Extend the data model and rules beyond Ironclad only after the first character's feedback loop is reliable.

An autoplayer, automated card selector, game-playing solver, cloud service, and AI that makes decisions for the player are outside the current scope.

## Current limitations

- The adapter currently captures the turn-start hook provided by BaseMod; exact post-draw timing should be tightened when the installed BaseMod version is verified in a live run.
- Card results are finalized on the next card input or at combat end. This avoids reading state before the action queue resolves, but it means actions that never resolve cleanly are logged with the best available terminal state.
- Target resolution uses state deltas and intentionally drops ambiguous multi-target actions instead of guessing. The first rule therefore stays silent unless the target is unambiguous.
- The first rule currently supports Ironclad's direct `Strike_R` only. This is deliberate: multi-hit and conditional attacks need richer card-effect metadata before they can be called certain.
- The run review is currently a lightweight summary, not a validated performance score or a complete lesson history.
- No missed-lethal, unused-energy, block, potion, repeated-pattern, solver, cloud, or AI coaching features are implemented yet.

## Development status

Milestone 1 (reliable combat and run logging): initial implementation complete, pending live-game verification against the local BaseMod/ModTheSpire versions.

Milestone 2 (first coaching rule): implemented end to end in the domain tests and game adapter, pending live-game verification of the success case: play Strike before Bash, finish combat, receive the review.
