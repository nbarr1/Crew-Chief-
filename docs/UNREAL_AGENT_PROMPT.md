# Crew Chief — Unreal Engine 5 Build Prompt (Editor-Driving Agent)

> **Which prompt is this?** This repo contains two. `UNREAL_ENGINE_COWORK_PROMPT.md` is a shorter brief for a
> code-writing assistant. **This** file is the deeper one, written for an agent with **live Unreal Editor
> control** (Unreal MCP, Python Remote Control, or equivalent) that can create Blueprints, spawn actors, build
> levels, and configure project settings itself. Use this one if your agent can drive the editor.

Copy everything below the line into your agent as the build brief.

---

# MISSION

Build **Crew Chief** — a first-person American football **officiating** career simulation — for **PC (Windows)**
in **Unreal Engine 5.4+**, with lifelike graphics and full gamepad support.

The player is **not** an athlete. The player is the **referee**. Every verb is judgment: see the play from your
assigned position, decide whether a foul occurred, throw the flag only when it did, identify it correctly,
enforce the right penalty, spot the ball accurately — and get graded on all of it, snap by snap, across a career
ladder from Youth League to the Professional Championship Tier.

This is a **port with fixes**, not a greenfield design. An Android version of this game exists in this
repository, fully designed and partially implemented. Its design data is authoritative and given below. Its
implementation has substantial gaps, also catalogued below. **Port Section A verbatim. Fix Section B — do not
reproduce it. Hit the fidelity bar in Section C.**

## Your posture as an agent

- You drive the editor. Create the assets, don't just describe them.
- Work milestone by milestone (Section G). Do not start a milestone until the previous one's acceptance
  criteria pass.
- After each milestone, run the automation test suite and a Play-In-Editor session, and confirm the stated
  acceptance criteria yourself before moving on.
- When source and this brief disagree, this brief wins — it already accounts for the source's known defects.
- When something here is ambiguous, prefer the reading that makes officiating judgment harder and more
  interesting, not the one that's easier to build.

# HARD CONSTRAINTS

### 1. Zero real-world intellectual property. Non-negotiable.

The project's own `DECISIONS.md` (D2) mandates "100% original, procedurally generated fictional teams… strict
adherence to zero IP infringement constraints." The existing Android build **violates this in two places**, and
you must not carry either one forward:

- `app/src/main/res/drawable/img_hero_stadium_1788041077376.jpg` is a photograph of a real NFL stadium showing
  **Tennessee Titans and Las Vegas Raiders** logos, wordmarks, and a "TITANS vs. RAIDERS" broadcast bar. It is
  used as the Career Hub hero (`CareerScreen.kt:299`) and the tunnel overlay (`GameScreen.kt:452`).
  **Do not import it. Do not derive from it.** Build an original fictional night-stadium hero instead.
- `FieldCanvas.kt:438` and `:470` paint the endzone wordmarks **"WILDCATS"** and **"TITANS"**. Replace with
  fictional team names from the roster below.

Everything you author must be original: team names, logos, uniforms, stadium names, signage, and rule text.
Write rules in your own plain language — never reproduce a real league's rulebook wording, and never imitate a
real broadcaster's on-screen trade dress. Photoreal rendering makes this *more* important, not less: lifelike
graphics plus real team marks is precisely the combination that draws a takedown. If a realism goal ever
conflicts with this constraint, the constraint wins.

Use the fictional teams the project already established: **Ironwood Forge, Cascades Osprey, Metro Rail, Coastal
Gators, Summit Raptors, Highland Stags, Pinecrest Badgers, Valley Vipers.** Extend that list in the same
naming register.

This constraint governs your reference material too. Section C has you study commercial football games for
fidelity — study them, match their quality bar, and author everything yourself. Never extract an asset from
them and never carry a licensed team, stadium, or likeness across.

### 2. The simulation and rules core must be pure, headless C++.

`FCrewChiefPlaySim`, `FEnforcementEngine`, and the grading logic must compile and run with no dependency on
rendering, actors, or the editor. Presentation consumes them; they never reach back. This is how the Android
version was architected (`DECISIONS.md` D1) and it is what makes the rules testable. Keep it.

### 3. All randomness must be seeded and reproducible.

The Kotlin simulator uses unseeded `kotlin.random.Random`, which makes replay and Film Study impossible to
reproduce faithfully. Every sim call in your port takes an explicit seed; a snap's seed is stored with its
record so any snap can be replayed exactly.

### 4. Tests stay green.

Port the existing unit tests as Unreal Automation tests (Section A.8) and keep them passing at every milestone.

# THE GAME IN ONE PAGE

A snap of football is a small, fast information problem. Twenty-two players move at once; you are one official
with one assigned position, one set of responsibilities, and one viewing angle. Somewhere in that mess a foul
may or may not have occurred. Your job is to know which.

The loop, per snap:

1. **Assignment briefing** — walk the tunnel, review the situation and your position's duty checklist.
2. **Pre-snap** — take your mandated field position, lock your *keys* (the specific players your position is
   responsible for watching), read the formation.
3. **Live ball** — the play runs, roughly a second and a half of real time. Watch your keys, not the ball.
4. **Dead ball** — mark forward progress and spot the ball to sub-yard precision.
5. **Penalty report** — if you flagged, identify the foul and the offending player, and the offended team
   accepts or declines; the enforcement engine computes the resulting down, distance, and spot.
6. **Review** — a supervisor grades the snap and cites the rule.

Then the career layer: ratings, promotions, film study, and the long climb from a three-official youth crew to
an eight-official championship crew where the spotting tolerance is a quarter of a yard.

**The fantasy is competence under scrutiny.** Everything that makes the player's job harder — a bad angle, a
screened view, a subtle hold, a fast tempo — is a feature.

# SOURCE OF TRUTH

The Android sources are your design reference. Read them as you work:

| System | File |
|---|---|
| Tiers, positions, grades | `app/src/main/java/com/example/core/model/OfficiatingEnums.kt` |
| Foul catalog | `app/src/main/java/com/example/rules/data/FoulType.kt` |
| Penalty enforcement | `app/src/main/java/com/example/rules/engine/EnforcementEngine.kt` |
| Play simulation | `app/src/main/java/com/example/sim/engine/PlaySimulator.kt`, `SimModels.kt` |
| Snap loop, grading, formations | `app/src/main/java/com/example/feature/game/GameViewModel.kt`, `GameUiState.kt` |
| Camera stations, field rendering | `app/src/main/java/com/example/core/ui/components/FieldCanvas.kt` |
| Career persistence | `core/data/repository/CareerRepository.kt`, `core/data/local/**` |
| Behavioral contract | `app/src/test/java/com/example/rules/engine/EnforcementEngineTest.kt` |
| Palette | `app/src/main/java/com/example/ui/theme/Color.kt` |

**Read `PROGRESS.md` with skepticism.** It marks milestones 1–4 and 7–11 "COMPLETED"; several of those systems
are declared but not wired up. Section B is the accurate account.

---

# SECTION A — CANONICAL DESIGN DATA (PORT VERBATIM)

These values are the game. Reproduce them exactly.

## A.1 Career tiers

| Tier | Display name | Crew | Min rating to unlock | Spotting tolerance |
|---|---|---|---|---|
| `YOUTH` | Youth League | 3 | 0 | ±1.5 yd |
| `PREP` | High School Prep | 5 | 65 | ±1.0 yd |
| `SMALL_COLLEGE` | Small College (D3/NAIA) | 7 | 75 | ±0.75 yd |
| `MAJOR_COLLEGE` | Major College (FBS) | 8 | 85 | ±0.5 yd |
| `SEMI_PRO` | Semi-Pro / Spring League | 8 | 90 | ±0.35 yd |
| `PROFESSIONAL` | Professional Championship Tier | 8 | 95 | ±0.25 yd |

Progression chain: YOUTH → PREP → SMALL_COLLEGE → MAJOR_COLLEGE → SEMI_PRO → PROFESSIONAL → (end).

Tier also scales presentation and difficulty: crowd density, stadium scale, broadcast production, play tempo,
and how *subtle* injected fouls are. Youth is a bleacher and a chain crew; Professional is a packed bowl under
television lights.

## A.2 The nine officiating positions

Field coordinates below are the Android camera stations (`FieldCanvas.kt:163-204`), in yards:
**X = 0–53.3 across the field** (26.65 = center), **Y = yard line**, **Z = height**. Yaw 0° faces downfield
(+Y), 90° faces +X. `los` = line of scrimmage.

| Position | Abbrev | Station X | Station Y | Base yaw | Primary responsibility |
|---|---|---|---|---|---|
| Referee (Crew Chief) | R | 26.65 | los − 8.5 | 0° | Passer protection, roughing, grounding, forward pass vs. fumble, signal announcements, crew conferences |
| Umpire | U | 26.65 | los + 7.5 | 180° | Interior line play, offensive holding, illegal blocks, false starts, ball spotting between downs |
| Down Judge | DJ | −2.5 | los | 90° | Neutral zone, offside, encroachment, forward progress, out of bounds, chain crew supervision |
| Line Judge | LJ | 55.8 | los | 270° | Far line of scrimmage, illegal motion, passer crossing the line, near-side forward progress |
| Field Judge | FJ | −2.5 | los + 16 | 75° | Deep receiver coverage, defensive pass interference, catch/no-catch at the boundary, goal-line pylon |
| Side Judge | SJ | 55.8 | los + 16 | 285° | Deep coverage opposite, secondary collisions, punt/kick sideline coverage, game clock backup |
| Back Judge | BJ | 26.65 | los + 20 | 180° | Defensive player count (11 men), tight end release, deep middle passes, kick uprights, play clock |
| Center Judge | CJ | 26.65 | los − 8.5, opposite R | 0° | Rapid ball spotting for up-tempo offenses, interior tackle/guard blocks, backfield mechanics |
| Replay Official | RO | Press box booth | — | — | Indisputable video review, catch/fumble verification, targeting confirmation under a review timer |

> Center Judge and Replay Official have **no station of their own** in the source — both fall through to the
> Down Judge's sideline default at (−2.5, los, 90°). The Center Judge values above are the intended placement
> (offensive backfield, opposite the Referee); the Replay Official needs a press-box booth scene entirely.

Eye height in first person is **1.85 yd**; third-person camera sits **3.8 yd behind** the station at **2.9 yd**
height. The responsibility strings above are player-facing copy — use them verbatim in assignment briefings.

Per-position **sightline windows** (from `GameViewModel.kt:31-35`) define what that official is accountable for:

- Referee: `los − 15` to `los + 10`
- Back Judge: `los − 5` to `los + 30`
- All others: `los − 10` to `los + 15`

In the Android build these windows only affect the 2D overhead camera. **In your port they must be real** — see
Section B.6.

## A.3 The foul catalog

Twelve fouls plus `NONE`. Columns: enforcement phase, yardage, automatic first down, loss of down, enforcement
spot, and which side commits it.

| Foul | Phase | Yds | Auto 1st | Loss of down | Enforcement spot | Side |
|---|---|---|---|---|---|---|
| False Start | Pre-snap | 5 | — | — | Dead-ball spot | Offense |
| Offside / Encroachment | At snap | 5 | — | — | Previous spot | Defense |
| Offensive Holding | Live ball | 10 | — | — | All-but-one | Offense |
| Defensive Holding | Live ball | 5 | **Yes** | — | Previous spot | Defense |
| Defensive Pass Interference | Live ball | 15 | **Yes** | — | Previous spot | Defense |
| Offensive Pass Interference | Live ball | 10 | — | — | Previous spot | Offense |
| Face Mask | Live ball | 15 | **Yes** | — | Previous spot | Either* |
| Horse Collar Tackle | Live ball | 15 | **Yes** | — | Previous spot | Defense |
| Unnecessary Roughness | Live ball | 15 | **Yes** | — | Previous spot | Either* |
| Roughing the Passer | Live ball | 15 | **Yes** | — | Previous spot | Defense |
| Illegal Block in the Back | Live ball | 10 | — | — | Previous spot | Offense |
| Intentional Grounding | Live ball | 5 | — | **Yes** | Spot of foul | Offense |

\* Face Mask and Unnecessary Roughness are modeled as defensive-only in the source because the `isOffensive`
flag defaults false. **Your port must let either team commit them** — see Section B.11.

Plain-language rule text (player-facing, use verbatim — it is the game's teaching content):

- **False Start** — "Interior lineman flinched or moved forward prior to the snap."
- **Offside / Encroachment** — "Defender in the neutral zone at the snap."
- **Offensive Holding** — "Grasping an opponent outside the frame of the jersey and restricting movement."
- **Defensive Holding** — "Grasping an eligible receiver beyond 5 yards restricting their route."
- **Defensive Pass Interference** — "Early contact significantly hindering a receiver's attempt to catch a forward pass."
- **Offensive Pass Interference** — "Creating separation via push-off beyond 1 yard while pass is in flight."
- **Face Mask** — "Grasping and twisting the face mask or helmet opening of an opponent."
- **Horse Collar Tackle** — "Grabbing inside collar or jersey opening and pulling runner down immediately."
- **Unnecessary Roughness** — "Blow to the head/neck area or contact out of bounds."
- **Roughing the Passer** — "Illegal forcible hit to quarterback after release of the football."
- **Illegal Block in the Back** — "Block directed above the waist into the back of an opponent."
- **Intentional Grounding** — "Pass thrown without realistic chance of completion by a passer facing imminent loss of yardage."
- **No Foul** — "Clean play."

## A.4 Grading

Six grades with score deltas:

| Grade | Label | Delta |
|---|---|---|
| `CORRECT_CALL` | Correct Call | **+5.0** |
| `CORRECT_NON_CALL` | Correct Non-Call | **+3.0** |
| `MARGINAL_CALL` | Marginal Decision | **+1.0** |
| `UNNECESSARY_CALL` | Over-Officiated / Unnecessary | **−4.0** |
| `MISSED_CALL` | Missed Infraction | **−6.0** |
| `INCORRECT_CALL` | Incorrect Call (Ghost Flag) | **−7.0** |

The call truth table (`GameViewModel.confirmSpotAndEvaluate`, lines 371-395):

| A foul occurred? | What the player did | Grade | Supervisor feedback |
|---|---|---|---|
| Yes | Flagged, identified the **correct** foul | `CORRECT_CALL` | "MASTERCLASS CALL! You accurately identified {foul} on #{num}." |
| Yes | Flagged, but named the **wrong** foul (or none) | `MARGINAL_CALL` | "Flag thrown, but official foul was {foul} on #{num} (You selected {selection})." |
| Yes | **No flag** | `MISSED_CALL` | "MISSED INFRACTION! A {foul} occurred on #{num} at the {yard} yd line." |
| No | Flagged anyway | `INCORRECT_CALL` | "PHANTOM FLAG! Clean play by both squads; no foul occurred." |
| No | No flag | `CORRECT_NON_CALL` | "GREAT DISCIPLINE! Clean block and coverage; correct non-call." |

`UNNECESSARY_CALL` has no producer in the source. **Give it one** — see Section B.7.

Every graded snap cites the offending foul's plain-language rule text.

**Spot grading**, from the same function:

| Error | Grade band |
|---|---|
| ≤ 0.3 yd | EXACT SPOT |
| ≤ 1.0 yd | GOOD SPOT |
| > 1.0 yd | OFF SPOT |

These are hardcoded in the source. **Replace them with the tier's `spottingToleranceYards`** — see Section B.4.

## A.5 The snap state machine

`PRE_SNAP → LIVE_BALL → DEAD_BALL_SPOTTING → PENALTY_REPORT → REVIEW → (next snap)`

| Phase | Player can | Exits when |
|---|---|---|
| `PRE_SNAP` | Move to position, look around, lock keys, read formation, switch camera | Snap |
| `LIVE_BALL` | Track keys, throw flag, blow whistle | Play ends naturally, or a flag/whistle kills it |
| `DEAD_BALL_SPOTTING` | Walk to the spot, place the ball, scrub the replay | Spot locked in |
| `PENALTY_REPORT` | Identify foul + offending number, signal it; offended team accepts/declines | Report confirmed |
| `REVIEW` | Read the grade, rule citation, and supervisor note; rewatch | Advance to next snap |

`PENALTY_REPORT` is **never entered** in the Android build — it exists only as a banner string. **Build it
properly**: it is where the enforcement engine finally gets used.

Status banner copy per phase, for reference: pre-snap "SCAN PRE-SNAP FORMATION", live "LIVE ACTION", spotting
"ALIGN FORWARD PROGRESS SPOT", report "CONFERRING WITH CREW CHIEF", review "OFFICIAL CALL EVALUATION".

## A.6 Play simulation model

Ground truth is generated **before** any animation plays; the presentation layer then performs that scripted
outcome. This is what makes judgment gradeable — truth exists independently of rendering. Keep this ordering.

**Play selection** (`PlaySimulator.kt:15-19`) — driven purely by distance:

| Condition | Outcome |
|---|---|
| `distance > 7` | 50% `PASS_DEEP`, 50% `PASS_SHORT` |
| `distance ≤ 7` | 50% `RUN_INSIDE`, 50% `RUN_OUTSIDE` |

**Yardage** (uniform integer ranges, inclusive lower / exclusive upper):

| Play type | Incompletion | Yardage on a completion/carry | Mean |
|---|---|---|---|
| `RUN_INSIDE` | — | −2 to +7 | +2.5 |
| `RUN_OUTSIDE` | — | −4 to +14 | +5.0 |
| `PASS_SHORT` | **30%** | +2 to +11 | +6.5 |
| `PASS_DEEP` | **55%** | +15 to +44 | +29.5 |

Yard-line convention: **1 = offense's own 1, 99 = opponent's 1, 100 = the goal line.** A touchdown is purely
geometric — it happens when `yardLine + yards ≥ 100`, clamped to 100.

**Foul injection** (`PlaySimulator.kt:47-63`): **15% flat chance per play**, then a foul chosen **uniformly from
all 12** — so ≈1.25% per foul per play, unconditioned on play type. This is a realism defect; see Section B.10.

**Foul spot placement**, by the foul's enforcement rule:

| Enforcement rule | Spot |
|---|---|
| Previous spot | Exactly the line of scrimmage |
| Spot of foul | LOS − 5 to LOS + 4, uniform |
| Dead-ball spot | The final dead-ball yard line |
| All-but-one | LOS − 5 to LOS + max(1, yards) − 1 |

All spots clamped to 1–99.

**Offending jersey number**: offense 50–78, defense 20–58. In the source these are drawn independently of the
actual 22 players on the field, so the reported number often belongs to nobody visible. **Draw from the actual
players in your port** — see Section B.12.

## A.7 The enforcement algorithm

Signature: `(PlayResult, FoulType, bool accepted) → (bool accepted, GameState newState, FString explanation)`.

**Declined, or no foul:** the play stands. `nextDown = (yardsGained ≥ distance) ? 1 : down + 1`;
`nextDistance = (nextDown == 1) ? 10 : distance − yardsGained`; yard line = the dead-ball spot.

**Accepted** — resolve the enforcement spot first:

| Rule | Enforce from |
|---|---|
| Previous spot | Line of scrimmage |
| Spot of foul | The foul's yard line |
| Dead-ball spot | Final dead-ball yard line |
| All-but-one | The foul spot **if it is behind** the previous spot, else the previous spot |

Then apply yardage with **half-the-distance to the goal**: for an offensive foul the reference is the distance
to the offense's own goal; for a defensive foul it is the distance to the defending goal. If the penalty is at
least half that distance, it becomes half that distance instead.

> **Port note:** the Kotlin implementation compares against a floating-point half but then assigns using
> **integer division**, so from the 7-yard line a 10-yard foul moves the ball 3 yards, not 3.5. It also uses
> `>=` rather than `>`, so a penalty exactly equal to half the distance gets *labeled* half-the-distance while
> moving the full amount. Decide deliberately: either reproduce this exactly for parity, or clean it up and
> update the ported tests. **Recommended: clean it up**, and keep sub-yard precision throughout since your
> spotting tolerances go down to 0.25 yd.

Then recompute down and distance:

**Offensive foul**
1. Loss of down (Intentional Grounding only): `down + 1`, distance grows by the net loss from the original LOS.
2. Pre-snap (False Start): replay the down, distance += penalty yards.
3. Otherwise: replay the down, distance grows by the net loss from the original LOS.

**Defensive foul**
1. Automatic first down, **or** the penalty alone reaches the line to gain → 1st & 10.
2. Pre-snap: replay the down, distance −= penalty yards.
3. Otherwise: replay the down, distance −= the actual yard-line movement.

Note Offside is phased "at snap", not "pre-snap", so it takes branch 3.

Explanation string format:
`"{Foul name}. {N} yard penalty enforced from {the previous spot|the spot of the foul}{ (Half the distance to the goal)}."`

## A.8 The behavioral contract — port these four tests

These are the existing unit tests (`EnforcementEngineTest.kt`). Port them as Unreal Automation tests; they must
pass before any presentation work begins.

| # | Setup | Expected |
|---|---|---|
| 1 | 1st & 10 at own 6; inside run +2 to the 8; Offensive Holding at the 6 | Ball at **3**, **1st & 13**, explanation contains "Half the distance" |
| 2 | 2nd & 8 at the 92; short pass incomplete; Offside at the 92 | Ball at **96**, **2nd & 4**, explanation contains "Half the distance" |
| 3 | 3rd & 20 at the 30; deep pass incomplete; Defensive Pass Interference | Ball at **45**, **1st & 10** |
| 4 | 2nd & 10 at the 40; pass for −8, dead at 32; Intentional Grounding at the 32 | Ball at **27**, **3rd & 23** |

Add tests of your own for every case Section B tells you to fix.

## A.9 Formations

Three, chosen at random per snap. Coordinates in yards, X across / Y relative to `los`.

**Offensive line, present in all three:** LT #72 (21.0, los−0.2), LG #65 (23.8, los−0.2), C #50 (26.65, los),
RG #66 (29.5, los−0.2), RT #78 (32.3, los−0.2), TE #87 (35.0, los−0.3).

| Formation | Backfield & receivers |
|---|---|
| **Shotgun Spread** | QB #12 (26.65, los−4.8), RB #22 (23.5, los−4.8), WR #88 (46.5, los−0.5), WR #81 (7.0, los−0.5), slot WR #17 (39.5, los−1.2) |
| **Pro Set** | QB #12 under center (26.65, los−1.5), FB #44 (26.65, los−4.2), TB #22 (26.65, los−6.8), WR #88 (47.0, los−0.5), WR #81 (6.5, los−0.5) |
| **Trips Right** | QB #12 (26.65, los−4.5), RB #22 (24.0, los−4.5), WR #88 (48.0, los−0.5), WR #81 (41.5, los−1.0), WR #17 (37.0, los−1.5) |

Each formation totals 11 offensive players (5 linemen + tight end + 5 backs/receivers).

**Defense (11, fixed):** DL #94 (20.8, los+1.2), #99 (24.5, los+1.1), #91 (28.8, los+1.1), #97 (33.5, los+1.2);
LB #55 (21.5, los+4.5), #54 (26.65, los+4.8), #52 (33.0, los+4.5); CB #21 (7.0, los+5.5), CB #24 (46.5, los+6.0),
FS #32 (20.0, los+14.5), SS #33 (34.0, los+11.5).

**Crew officials** are placed at their stations and omitted when the player occupies that position — the player
never sees a duplicate of themselves.

**Pre-snap motion:** 45% chance per snap; begins ~700 ms after the formation sets; a receiver crosses the
formation laterally over ~1 second. In the source this is decorative. **Make it matter** — illegal motion and
illegal shift are real calls for the Line Judge.

**Live ball duration** in the source is ~1.5 seconds (45 frames at 33 ms). That is a placeholder, not a target:
with real animation, plays should run to their natural length. Take actual play length, snap-to-snap timing,
and tempo from the reference footage in Section C.2. What matters is that the reaction window stays tight
enough to demand attention.

## A.10 Visual identity

Palette (`Color.kt`) — carry these into your UI and lighting as the brand:

| Token | Hex | Use |
|---|---|---|
| Flag Gold | `#FFC72C` | Penalty flag, line to gain, primary accent |
| Turf Green | `#2E7D32` | Field, confirm actions |
| Turf Green Dark | `#1B5E20` | Alternating mow strips |
| Down Marker Orange | `#FF6D00` | Chain gang, spot reticle, down & distance |
| Review Booth Blue | `#00B0FF` | Line of scrimmage, review UI |
| Spot Progress Blue | `#40C4FF` | Forward progress |
| Grade Correct | `#00E676` | Positive grades |
| Grade Warning | `#FFD600` | Marginal |
| Grade Incorrect | `#FF1744` | Missed / ghost flag |
| Stadium Night | `#0B100E` | Background |
| Stripe White | `#F5F7F6` | Chalk, officials' stripes |
| Whistle Chrome | `#CFD8DC` | Hardware |

The HUD reads as **monospace** — every label style in the Android theme is `FontFamily.Monospace` with wide
letter tracking. Keep that broadcast-instrument feel. The app is **dark-only** by design.

Field details worth preserving: alternating 5-yard mow strips, collegiate hash marks at X 20.0–20.8 and
32.5–33.3, 1-yard ticks along both sidelines, a dashed coaching box at X −2.0 from the 25 to the 75, orange
pylons at all four corners, a gooseneck goalpost with the crossbar at 3.3 yd and uprights to 10 yd, and a chain
crew with an orange down box and a gold line-to-gain stake connected by a dashed chain.

Broadcast overlays (assist options, toggleable): glowing blue line of scrimmage, glowing gold line to gain,
down & distance bug, and a pulsing orange spot reticle during spotting.

---

# SECTION B — KNOWN DEFECTS: FIX, DO NOT REPLICATE

Everything below is a gap between the Android build's design intent and its actual behavior. Each item states
the intended behavior. **These fixes are in scope.** They are most of what turns a faithful port into a good
game.

### B.1 The enforcement engine is never called

`EnforcementEngine.applyPenalty` is invoked only from its own unit test. Live gameplay does its own ad-hoc
down/distance update and **never enforces a penalty on the game state**. A flag changes the grade and nothing
else — the ball doesn't move.

**Intended:** the `PENALTY_REPORT` phase calls the engine, the offended team accepts or declines, and the
resulting spot, down, and distance are applied and shown to the player. Wire it up. This is the single most
important fix on this list.

### B.2 Three inconsistent down/distance implementations

The engine's decline branch, the engine's accept branches, and `GameViewModel.confirmSpotAndEvaluate` each
compute down and distance differently. The gameplay one does `(down % 4) + 1`, which **wraps 4th down back to
1st** — turnover on downs simply doesn't exist.

**Intended:** one authoritative implementation in the C++ core, used by every path, with a real turnover on
downs and a possession model.

### B.3 Tier promotion is not implemented

`minRatingRequired` and `nextTier` are referenced nowhere outside the enum declaration. Every tier chip in the
career UI is freely clickable, so a player can jump straight to the Professional tier at rating 75.

**Intended:** promotion is earned. A tier unlocks when career rating reaches its `minRatingRequired`; sustained
poor grades can send an official back down. Assignments are offered, not self-selected.

### B.4 Per-tier spotting tolerance is cosmetic

`spottingToleranceYards` (1.5 down to 0.25) appears only as text on the career screen. The grader uses hardcoded
0.3 / 1.0 yard thresholds regardless of tier.

**Intended:** spot grading is measured against the *current tier's* tolerance. A spot that earns "EXACT" in
Youth League is a miss in the Professional tier. This is a primary difficulty curve — use it.

### B.5 In-game snaps are never persisted

`GameViewModel` never touches the database. Career statistics and Film Study are populated entirely by
`CareerViewModel.simulateGameSession()`, which **fabricates** 6–11 snaps using an 85% "competent officiating"
dice roll. **The player's actual officiating has no effect on their career.**

**Intended:** every snap the player officiates is written as a record — situation, position, keys, true foul,
action taken, grade, spot error, supervisor note — and career stats and Film Study read from those real records.
Delete the fabrication path entirely.

### B.6 No sightline or occlusion model in 3D

`visibleYardRange` is used only by the 2D overhead camera. In the four 3D cameras every player is drawn with no
occlusion, no zone culling, and no view restriction beyond the ±65° yaw / ±25° pitch look clamp.

**Intended — and this is the core of the fantasy:** you can only call what you can actually see. Bodies block
sightlines. A hold on the back side of the formation is invisible from the Down Judge's position. Being out of
position produces missed calls *because of geometry*, not because of a dice roll. Grading should weight fouls
inside your responsibility window far more heavily than ones outside it, and a foul genuinely screened from your
view should not be graded as a miss.

### B.7 `UNNECESSARY_CALL` is unreachable

The "Over-Officiated / Unnecessary" grade (−4.0) is defined and color-mapped but never produced.

**Intended:** it fires when the player flags a technically-true but trivial infraction that good officials
let go — incidental contact, a hold that didn't affect the play. This requires the simulator to mark injected
fouls with a **materiality** level (marginal vs. clear), which also gives you a natural difficulty dial: higher
tiers inject more marginal fouls.

### B.8 `CallGrade.scoreDelta` is never summed

The +5/+3/+1/−4/−6/−7 values exist on the enum and are used by nothing. The career rating instead comes from a
separate formula in the simulated-game path.

**Intended:** grade deltas accumulate into the game grade, which feeds the career rating. One scoring path.

### B.9 No timing pressure of any kind

There is no play clock, no game clock, no reaction window, no whistle-timing grade. `clockSeconds = 900` is
stored and never decremented or displayed.

**Intended:** a running game clock and play clock; delay-of-game as a callable situation; up-tempo offenses that
pressure the Center Judge's spotting speed at higher tiers; and grading on **whistle timing** — a quick whistle
that kills a play early is a real and serious officiating error.

### B.10 Foul injection ignores play type

15% flat, uniform across all 12 fouls, unconditioned on what actually happened. Roughing the Passer can occur on
an inside run; Defensive Pass Interference on a run play.

**Intended:** fouls are conditioned on play type and game situation. Pass plays draw pass interference, holding,
and roughing; runs draw holding, blocks in the back, and horse collars; pre-snap fouls occur before the snap.
Rates should reflect plausible officiating frequency — holding is common, horse collar is rare.

### B.11 Face Mask and Unnecessary Roughness are defensive-only

Both default `isOffensive = false`, so an offensive face mask cannot be represented.

**Intended:** either team can commit them; the committing side is part of the injected foul event.

### B.12 The offending jersey number belongs to nobody

Numbers are drawn from ranges (offense 50–78, defense 20–58) independent of the 22 players actually on the
field, so the reported number frequently matches no visible player.

**Intended:** the foul is committed by a *specific player actor*, and the player identifies that number. The
number the player reports should be graded, not just stored.

### B.13 Flag placement is not a mechanic

The flag always lands on the line of scrimmage regardless of where the player aimed or where the foul occurred.

**Intended:** the flag lands where thrown, and **flag placement accuracy is graded** — for spot fouls, where you
drop the flag determines enforcement.

### B.14 Keying players is impossible in 3D and never graded

The 3D tap handler is empty; keys can only be set in the overhead camera, and `isKey` has no grading effect
anywhere. Pre-snap keys are pure decoration.

**Intended:** keying is how you declare what you're watching. Keying correctly for your position and the
formation earns mechanics credit; fouls that occur on your keys are weighted heavily in grading.

### B.15 The signal mechanic was never built

`officialSignalGiven` and `setOfficialSignal()` are declared and wired into state with no consumer.

**Intended:** after a flag, the referee announces the foul with a hand signal. Correct signaling is graded.

### B.16 Missing game systems

None of the following exist: turnovers (`isTurnover` is never set true), punts, field goals, kickoffs, safeties,
possession changes, scoring, or quarters. `PlayType` declares `PUNT`, `FIELD_GOAL`, and `KICKOFF`; none are ever
generated.

**Intended:** a complete game model. Special teams in particular are officiating-rich — kick catch interference,
illegal formation, players downfield — and several positions (Side Judge, Back Judge) exist largely to work
them.

### B.17 Cloud sync is upload-only with no restore

`CloudSyncManager` writes the profile and every game record to Firestore on each trigger (O(n) writes, no delta
detection), swallows failures, and **has no download path** — a reinstall loses everything despite the cloud
copy. Snap evaluations are never synced at all.

**Intended:** two-way sync with conflict resolution, incremental writes, retry on failure, and snap-level
records included so Film Study survives a reinstall.

### B.18 Smaller corrections

- No yard-line clamping in enforcement — the ball can be pushed past 100 or to 0. Clamp, and add real goal-line
  handling (safety, touchback, 1st & goal).
- Distance is never clamped and can go negative. There is no "1st & Goal" concept anywhere.
- Room's enum converters **silently fall back** to defaults on an unparseable value (`YOUTH`, `DOWN_JUDGE`,
  `CORRECT_NON_CALL`) — a corrupt save quietly becomes a wrong one. Fail loudly instead.
- The database uses `fallbackToDestructiveMigration()` — a schema change wipes the player's career. Version
  your save format properly.
- The spot readout labels "EXACT SPOT (±0.2 YDS)" while the threshold is actually 0.3.
- The ball's pass arc peaks at ~5.3 yd though the code comment claims 4.2.
- "Snap to runner" instantly sets the spot to the exact ground truth, guaranteeing a perfect grade. Remove it,
  or make it an assist that caps the achievable spot grade.
- After a play ends the spot is **pre-seeded to the correct answer**, so doing nothing scores perfectly. The
  player must place the spot themselves.

---

# SECTION C — GAMEPLAY REFERENCE: COLLEGE FOOTBALL FOOTAGE

Use gameplay footage of **EA Sports College Football 26 and 27** as your fidelity benchmark. College football is
the correct reference for this project specifically: four of the six tiers are collegiate or below, the 7- and
8-official crew mechanics are collegiate, and the hash marks specified in A.10 are already collegiate spacing.

Your goal is that a Crew Chief play, watched with the HUD off, is indistinguishable in motion quality and
atmosphere from a play in those games — and then that everything about the *player's relationship* to that play
is inverted.

## C.1 What you take from it, and what you do not

Take **observation only**: how football looks, moves, sounds, and paces on screen. Watching a competitor and
matching its quality bar is ordinary practice. Characterize what you see, then author your own assets to that
standard.

Do not take anything transferable:

- **No asset extraction.** Do not rip or import models, animations, motion capture data, textures, audio, or
  shaders from the games or their files.
- **No licensed content.** No real schools, conferences, stadiums, logos, uniforms, helmets, mascots, fight
  songs, or player likenesses. Those games license them; Constraint 1 forbids them here regardless.
- **No interface trade dress.** Do not reproduce EA's HUD, score bug, menus, play-call screens, or replay
  wipes. Build your own broadcast language from the palette in A.10.

Match the **bar**, author the **content**. If a stadium's lighting is useful reference, build a fictional
stadium that lights like it.

## C.2 What to extract

| Area | What to characterize |
|---|---|
| **Player movement** | Locomotion weight, acceleration and deceleration, cut sharpness, momentum on contact, how a 300-lb lineman moves differently from a slot receiver |
| **Line play** | Blocking engagements, hand fighting, double teams, pass sets, how a hold actually *looks* when it happens in traffic — this is your foul-readability reference |
| **Ball skills** | QB drops and throwing motion, route breaks, catch and contested-catch animation, tackle variety, ball security and fumbles |
| **Tempo** | Time between snaps, huddle vs. no-huddle, play clock behavior, substitution flow, how long a play actually lasts from snap to whistle |
| **Play vocabulary** | Formation variety, motion and shifts, RPOs, option looks, tempo offenses, special teams alignments |
| **Presentation** | Crowd density and behavior, sideline population, lighting for day and night games, weather, turf wear, chain crew operation |
| **Audio** | Crowd beds and reactions, contact sounds, PA, sideline chatter, whistle |

Three of these directly correct defects in Section B:

- **Tempo** fixes B.9. The Android build's ~1.5-second live-ball window and total absence of a clock are
  placeholders. Real play length, real play-clock pressure, and real up-tempo sequences are what make the
  Center Judge's spotting speed and the Back Judge's play-clock duty into actual mechanics.
- **Play vocabulary** expands A.9. Three formations is a placeholder; college football's formation and motion
  variety is the target, and it is what makes pre-snap reading (illegal formation, illegal motion, illegal
  shift, numbering) a real officiating problem rather than a memorized one.
- **Line play** informs B.10. Watching which fouls plausibly arise from which play types is how you build the
  conditioned foul model that replaces the flat uniform 15%.

## C.3 Watch the officials

This is the part no other reference gives you, and the reason this section exists.

Those games render officials on the field. In footage, **track them rather than the ball.** For each play type,
note: where each official stands before the snap, how they move once the ball is live, who follows the runner
and who holds the line of scrimmage, who covers the sideline, when and how a flag is thrown, how the ball is
spotted and relayed, how the chain crew is operated, and how signals are given after a foul.

Use this to sanity-check the station coordinates in A.2 and to author the officials' own animation set — the
mechanics of officiating (the walk to the spot, the wind, the flag toss, the signal) are the animations your
game needs most and that no football game has ever needed to do well, because no football game has ever put the
camera there.

Broadcast footage of real college games is equally good reference for this, and better in some respects, since
the officials are real and the mechanics are correct.

## C.4 The inversion

Everything above is reference for **what happens in the world**. None of it is reference for **how the player
experiences it**. Invert deliberately:

| College Football 26/27 | Crew Chief |
|---|---|
| Camera behind the QB or a broadcast angle | First person, from the assigned official's station and eye height |
| You control an athlete and execute | You control an official and *judge* |
| The HUD tells you the play and the situation | You read the formation and situation yourself |
| Penalties are events that happen to you | Penalties are your decision, and your decision is graded |
| Success is yards, points, wins | Success is accuracy, positioning, and crew trust |
| The camera follows the ball | The ball is often the wrong thing to watch — your keys are elsewhere |

The last row is the design thesis. In a football game the ball is the point; in an officiating game, watching
the ball instead of your keys is exactly how you miss the hold that decides the drive. Build the cameras and
the animation so that the temptation to ball-watch exists and is punished.

## C.5 If you cannot process video directly

If your tooling cannot ingest video, do not skip this section — convert it into an asset the build can use.
Extract frames at a fixed interval, work from those; or ask the user to supply the footage as a structured
reference document covering the C.2 table. Then commit that characterization to
`docs/GAMEPLAY_REFERENCE.md` in this repo so the observations are reviewable, versioned, and cited by the
animation and tempo work rather than living only in your context.

# SECTION D — EDITOR BUILD INSTRUCTIONS

You have live editor control. Create these assets concretely.

## D.1 Project and modules

- UE **5.4+**, C++ project named `CrewChief`, Windows target, 64-bit.
- Modules (C++ only where marked pure — no engine rendering dependencies):
  - `CrewChiefCore` *(pure)* — `FGameSituation`, `FPlayResult`, `FTrueFoulEvent`, foul catalog data assets.
  - `CrewChiefSim` *(pure)* — `FCrewChiefPlaySim`, seeded, deterministic.
  - `CrewChiefRules` *(pure)* — `FEnforcementEngine`.
  - `CrewChiefGrading` *(pure)* — call grading, spot grading, mechanics scoring.
  - `CrewChiefCareer` — persistence, save game, tier progression, cloud sync.
  - `CrewChiefGame` — actors, pawns, controllers, animation, UI.
- Blueprints only for content wiring and UI. All logic lives in C++.

## D.2 Input — Enhanced Input

Create Input Mapping Context **`IMC_Official`** with these Input Actions:

| Action | Binding | Behavior |
|---|---|---|
| `IA_Move` | Left stick | Move the official. Position determines what you can see. |
| `IA_Look` | Right stick | Head/gaze. Clamp yaw ±65°, pitch ±25° in first person. |
| `IA_Focus` | LT (hold) | Narrow FOV to read jersey numbers and hand-fighting. |
| `IA_ThrowFlag` | RB (hold, release with a flick) | Flag arcs to the aimed point with cloth physics. |
| `IA_Whistle` | X | Kill the play. An inadvertent whistle is a graded error. |
| `IA_Confirm` | A | Confirm spot, advance briefing, select. |
| `IA_KeyLock` | A + right stick | Radial key selector, pre-snap only. |
| `IA_Signal` | Y | Radial foul-signal menu (chop, hold grasp, push-off, facemask twist). |
| `IA_Cancel` | B | Wave off, decline, cancel. |
| `IA_Sprint` | RT | Cover the play downfield. |
| `IA_CameraCycle` | D-pad | Switch among the five perspectives. |
| `IA_SpotAdjust` | Left stick (spotting phase) | Micro-adjust with **haptic detents every half yard**. |

Haptics carry real information: a tick per half yard while spotting, a thump on the snap, impact rumble on
collisions, a sharp pulse on the whistle. Support full rebinding. Keyboard and mouse are secondary but complete.

## D.3 Cameras

Five perspectives, switchable at any time, matching the Android build:

| Perspective | Placement |
|---|---|
| First person (ref's eyes) | At the station, eye height 1.85 yd, pitch +8° |
| Third person (follow) | 3.8 yd behind the station, height 2.9 yd, pitch +12° |
| Sideline broadcast | X −12, height 8.5 yd, pitch +22° |
| Endzone | Center, 28 yd beyond the LOS, height 5.2 yd, pitch +14° |
| All-22 overhead | Center, height 24 yd, pitch +70° |

First person is primary and is what grading assumes.

## D.4 Blueprints and actors to create

- `BP_CrewChiefGameMode`, `BP_OfficialController`, `BP_OfficialPawn` (nine position variants driven by data).
- `BP_PlayerCharacter` — modular, team-colored, with a jersey number decal driven from the sim.
- `BP_PenaltyFlag` — physics + cloth, weighted head, ribbon trail, gold `#FFC72C`.
- `BP_Football` — leather material, laces, spin.
- `BP_ChainCrew` — down box, line-to-gain stake, dashed chain.
- `BP_Pylon`, `BP_Goalpost`.
- `DA_FoulCatalog`, `DA_TierTable`, `DA_PositionTable` — data assets carrying Section A's tables so designers
  can tune without recompiling.
- `WBP_HUD`, `WBP_PenaltyReport`, `WBP_ReviewCard`, `WBP_CareerHub`, `WBP_FilmStudy`, `WBP_RulesReference` —
  all fully gamepad-navigable (Common UI recommended).

## D.5 Rendering and lighting

- Enable **Lumen** GI and reflections, **Nanite** for stadium geometry, **Virtual Shadow Maps**.
- The signature look is a **night game under floodlights**: volumetric light shafts, atmospheric haze, bloom on
  the towers, wet-turf specular variant. Place four light towers, matching the Android composition.
- Turf: photoreal, alternating 5-yard mow strips, painted numbers and hashes, divots and wear accumulating over
  the game.
- Crowds: Niagara or instanced meshes, density scaling by tier — bleachers at Youth, a packed bowl at
  Professional. Crowd audio reacts to calls, including booing a flag against the home team.
- Officials as MetaHumans (or equivalent), in black-and-white vertical stripes with a white cap.
- **Foul animations must be visually readable.** The player has to be able to genuinely see a hold happen. Make
  fouls distinct at their materiality level, and let higher tiers inject subtler ones.
- Target **60 FPS at 1440p on an RTX 3070-class GPU**; ship scalability presets.

## D.6 Audio

The Android build synthesizes three tones and has no audio assets. You need real sound: whistle (with variation),
flag throw, pad collisions with impact weight, crowd beds per tier, stadium PA, chain crew, and the referee's
announcement voice. Positional audio matters — hearing contact you didn't see is a legitimate officiating cue.

## D.7 Film Study replay

Record actor transforms and the full sim state per snap so any snap can be scrubbed, rewound, and rewatched from
any of the five cameras, with the grade, the ground truth, and the supervisor's note shown alongside. Because
snap seeds are stored (Constraint 3), replays are exact.

---

# SECTION E — FULL SCOPE

All of this is in scope:

- **Nine officiating positions**, each with real sightlines, responsibilities, and position-specific grading.
  Down Judge is the best starting point — it was the Android vertical slice — but all nine ship.
- **Six tiers** with enforced promotion gates, scaling tolerance, tempo, foul subtlety, and presentation.
- **Career mode** — persistent profile, per-game records, per-snap evaluations, promotions and demotions,
  assignments, and a season structure. The Android schema has unused hooks for appeals, crew reputation,
  playoff and championship assignments, and a week counter; build them out.
- **Film Study** — a review room fed by *real* snaps.
- **Assignment briefing** — the tunnel walkout with floodlight bloom, duty checklist, and countdown.
- **Rules Reference** — an in-game plain-language rulebook generated from the foul catalog.
- **Cloud sync** — two-way, incremental, with snap records included.

# SECTION F — CONTROLLER FEEL

The touch build's verbs translate as follows. Preserve the *intent*, not the touch implementation:

| Android verb | Gamepad equivalent |
|---|---|
| Upward flick on the turf | Hold RB, flick the right stick — flag arcs to where you aimed |
| Drag to look | Right stick, clamped ±65° / ±25° |
| Drag the spot laser | Walk to the spot, micro-adjust on the left stick, half-yard haptic detents |
| Tap a player to key | Radial selector on the right stick, pre-snap |
| Camera chips | D-pad |
| Penalty picker dialog | Radial menus: side → foul → jersey number |
| (never built) signal | Y opens the signal radial; correct signal is graded |

# SECTION G — MILESTONES

Each milestone ends with its acceptance criteria demonstrated **in the editor**.

**M1 — Scaffold, input, cameras.**
Project and modules created; `IMC_Official` with all Input Actions; official pawn moves and looks; all five
cameras cycle; blockout stadium with a correctly-proportioned field.
*Accept:* PIE session, gamepad only — walk the field, cycle all five cameras, look clamps hold.

**M2 — Pure core with tests.**
`FCrewChiefPlaySim`, `FEnforcementEngine`, grading, all seeded and headless. Section A.8's four tests ported,
plus new tests for the B-list fixes (turnover on downs, goal-line clamping, tier-scaled spotting, materiality).
*Accept:* automation suite green; a headless commandlet runs 10,000 seeded snaps with no invalid game state, and
the same seed reproduces identical results.

**M3 — Play performance layer.**
22 animated players executing sim outcomes; formation and motion vocabulary drawn from the Section C reference,
not just the three formations in A.9; readable foul animations at two materiality levels; real play length and
snap-to-snap tempo.
*Accept:* PIE — snap a play, watch a scripted outcome perform; injected holding is visibly identifiable from the
Umpire's position and *not* visible from the Field Judge's. Side-by-side against the Section C footage, the
motion quality and pacing read as the same sport.

**M4 — Officiating verbs.**
Key locking, flag throw with placement, whistle with timing grade, signal radial, penalty report with
accept/decline, ball spotting with haptic detents. **The enforcement engine drives the game state** (fixes B.1,
B.2, B.13, B.14, B.15).
*Accept:* PIE — flag a foul, identify it, the offended team declines, the correct down and distance result;
flag placement and spot accuracy both appear in the grade.

**M5 — Career, grading, Film Study.**
Persistence of real snaps, tier ladder with enforced promotion, supervisor grading UI, Film Study replay, rules
reference (fixes B.3, B.4, B.5, B.7, B.8).
*Accept:* officiate a full quarter; career rating moves based on actual performance; every snap is rewatchable
in Film Study from all five cameras; promotion is blocked below the rating gate.

**M6 — Complete game model.**
Clock, play clock, possession, turnovers, scoring, special teams; play-type-conditioned fouls (fixes B.9, B.10,
B.16).
*Accept:* a full simulated game runs end to end with valid state throughout, including special teams.

**M7 — Lifelike pass.**
MetaHumans, Lumen night lighting, crowds, audio, broadcast overlays, tunnel walkout, polish. Officials get their
own animation set — the walk to the spot, the flag toss, the wind, the signals — per Section C.3.
*Accept:* 60 FPS at 1440p on target hardware; a stranger watching the screen would take it for a broadcast, and
would not be able to tell your atmosphere from the Section C reference.

# DEFINITION OF DONE

- A player can start a career at Youth League and be promoted to the Professional tier through officiating
  performance alone.
- All nine positions are playable with genuinely different sightlines and responsibilities.
- Every flag actually enforces; every snap is graded, persisted, and rewatchable.
- The automation suite is green, and seeded replays reproduce exactly.
- Controller-only from launch to quit.
- 60 FPS at 1440p on an RTX 3070-class GPU.
- Not one real-world team, league, logo, stadium, or rulebook line anywhere in the project.
