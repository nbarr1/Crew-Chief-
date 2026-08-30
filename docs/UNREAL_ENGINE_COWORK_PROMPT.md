# Claude Cowork Prompt — Crew Chief: Unreal Engine 5 PC Edition

Copy everything below the line into Claude Cowork as the build prompt.

---

## The Ask

Build **Crew Chief**, a first-person football *officiating* career simulation game for **PC (Windows)** using **Unreal Engine 5.4+**, with lifelike graphics and full **gamepad/controller** gameplay. This is a ground-up Unreal port of an existing, fully-designed Android game — the complete game design is specified below and must be followed. You are not designing a new game; you are re-realizing a proven 2D top-down design as a AAA-style 3D experience.

The player is **not** a football player. The player is the **referee**. Every mechanic is about seeing the play, judging it correctly, throwing the flag at the right moment (and only the right moment), enforcing the correct penalty, and spotting the ball accurately — graded snap-by-snap by a supervisor, across a career ladder from Youth League to the Professional Championship Tier.

## Core Fantasy & Loop

1. **Assignment Briefing** — walk the stadium tunnel, review down/distance situation and your position's duty checklist.
2. **Pre-Snap** — take your position's mandated field location, lock onto your "keys" (the players your position is responsible for watching), check formation legality.
3. **Live Ball** — the play runs in real time. Watch your keys. A probabilistic "true foul" may or may not occur. Throw your flag (or don't).
4. **Dead Ball / Spotting** — mark forward progress, spot the ball precisely.
5. **Penalty Report** — if you flagged, identify the foul, the offending player number, and accept/decline enforcement; the enforcement engine computes the result.
6. **Review** — the supervisor grades the snap (Correct Call +5, Correct Non-Call +3, Marginal +1, Unnecessary −4, Missed −6, Ghost Flag −7). Ratings gate career progression.
7. Repeat per snap → post-game grade → career rating → promotion up the tier ladder.

## Fictionalization Constraint (non-negotiable)

Zero real-world IP. 100% original fictional teams (e.g., Ironwood Forge, Cascades Osprey, Metro Rail), generic tier names, original logos/uniforms, plain-language original rule text. No NFL/NCAA names, likenesses, rulebook text, or broadcast trade dress.

## Career Structure (port exactly)

Six tiers, each with crew size, minimum career rating to unlock, and spotting tolerance (accuracy required when spotting the ball):

| Tier | Crew | Min Rating | Spotting Tolerance |
|---|---|---|---|
| Youth League | 3 | 0 | 1.5 yd |
| High School Prep | 5 | 65 | 1.0 yd |
| Small College | 7 | 75 | 0.75 yd |
| Major College | 8 | 85 | 0.5 yd |
| Semi-Pro | 8 | 90 | 0.35 yd |
| Professional Championship | 8 | 95 | 0.25 yd |

Higher tiers = faster play speed, tighter reaction windows, up-tempo snaps, more subtle fouls, larger stadiums and crowds.

**Nine playable/assignable positions**, each with a distinct field location, sightlines, and responsibilities: Referee (passer protection, roughing), Umpire (interior line, holding), Down Judge (neutral zone, forward progress, chains), Line Judge (far LOS, illegal motion), Field Judge (deep coverage, DPI, pylon), Side Judge (deep opposite, kick coverage), Back Judge (11-man count, play clock, deep middle), Center Judge (rapid spotting, up-tempo), Replay Official (booth video review minigame). Ship the **Down Judge as the vertical slice**, with the architecture cleanly supporting the rest.

## Rules & Enforcement Engine (port exactly, in C++)

Foul catalog with phase (pre-snap / at-snap / live-ball / dead-ball), yardage, auto-first-down, loss-of-down, and enforcement spot (previous spot, spot of foul, dead-ball spot, all-but-one principle): False Start (5, dead-ball spot), Offside/Encroachment (5), Offensive Holding (10, all-but-one), Defensive Holding (5, auto 1st), DPI (15, auto 1st), OPI (10), Face Mask (15, auto 1st), Horse Collar (15, auto 1st), Unnecessary Roughness (15, auto 1st), Roughing the Passer (15, auto 1st), Illegal Block in the Back (10), Intentional Grounding (5, loss of down, spot of foul).

The enforcement engine must correctly handle: half-the-distance-to-the-goal, automatic first downs, loss of down, accept/decline decisions, and down/distance recomputation. **This engine must be pure, deterministic, headless C++ with a full automated test suite** (Unreal Automation Framework) — it is the heart of the game and was fully unit-tested in the original.

## Play Simulation (port the model)

A headless simulator generates ground truth before animation: play type (inside/outside run, short/deep pass, punt, field goal, kickoff), yardage outcome from tuned probability tables, incompletions, turnovers, touchdowns, and probabilistic **TrueFoulEvent** injection (foul type, committing player jersey number, foul yard line) weighted by real-world foul frequency. The 3D presentation layer then *performs* that scripted outcome with animation — judgment gameplay stays fair and gradeable because truth exists independently of rendering.

## Unreal Engine 5 Direction — Lifelike Graphics

- **Rendering**: Lumen GI + reflections, Nanite for stadium/environment geometry, Virtual Shadow Maps. Night games under floodlights are the signature look: volumetric light shafts, wet-turf specular variant, atmospheric haze, bloom on stadium lights.
- **Characters**: MetaHuman (or equivalently realistic rigs) for officials; modular player characters with team-colored uniforms, helmets, cloth simulation on jerseys and the flag. Realistic body diversity across linemen/backs/receivers.
- **Animation**: Motion-matched or high-quality animation sets for blocking, routes, tackles, QB drops, catches; contextual contact animations so holds, facemasks, and late hits are *visually readable* — the player must be able to genuinely see the foul to call it. Foul animations must be distinct but subtle in proportion to tier difficulty.
- **Field**: Photoreal turf with 5-yard mow strips, painted numbers/hashes, degradation/divots over the game, chain crew and pylons, dynamic TV-style broadcast overlays (glowing line of scrimmage, yellow line-to-gain, down & distance bug) togglable as an assist option.
- **Crowd & Presentation**: Niagara/instanced crowd scaling with tier (bleachers at Youth → packed 70k bowl at Professional), crowd audio reacting to calls (booing bad flags against the home team), stadium PA, whistle/flag/pad-collision audio.
- **Cameras**: First-person ref POV (primary), third-person follow, sideline broadcast cam, endzone cam, All-22 overhead tactical cam — switchable at any time, mirroring the original's five perspectives.
- Target 60 FPS at 1440p on an RTX 3070-class GPU; include scalability presets.

## Controller Gameplay (primary input; keyboard/mouse as secondary)

Design for an Xbox-style gamepad via **Enhanced Input**:

- **Left stick** — move official (positioning matters: bad angle = missed call, getting into the play's path = collision risk).
- **Right stick** — head/gaze look (first person) with focus/zoom on **LT** to read jersey numbers and hand-fighting.
- **RB (hold + release with flick)** — throw the penalty flag: a physical, flick-timed gesture replacing the mobile flick; flag arcs with cloth physics to the spot the player aims.
- **X** — blow whistle (kill the play dead; an inadvertent whistle is itself a graded mistake).
- **A** — confirm / interact (lock keys pre-snap via a radial target selector on the right stick, confirm spots, advance briefing).
- **Y** — signal mechanic: after flagging, pick the foul signal from a radial menu (chop, hold grasp, push, facemask twist) — correct signaling contributes to the grade.
- **B** — wave off / decline / cancel.
- **D-pad** — camera perspective switching; **RT** — sprint to cover the play downfield.
- **Ball spotting** — walk to the spot in first person and place the ball with a precision micro-adjust on the left stick, with controller **haptics ticking at each half-yard** (port of the original's magnetic yardline snap + haptic ticks); tolerance per tier as tabled above.
- Full rebinding, plus rumble cues for snap, collisions, and whistle.

## Modes & Meta (port all)

- **Career Mode** — persistent profile, per-game records, per-snap evaluations with supervisor feedback text, tier promotions/demotions. Persist with SQLite or SaveGame objects mirroring the original schema (CareerProfile, GameRecord, SnapEvaluation).
- **Film Study Mode** — post-game review room: rewatch any snap from any camera with the grade, the truth (what actually happened), and supervisor commentary side by side. In UE this becomes a proper replay system (record actor transforms per snap for scrub/rewind).
- **Assignment Briefing / tunnel walkout** — first-person tunnel walk with floodlight bloom reveal, duty checklist, and countdown (port of Milestone 11's transition set piece).
- **Rules Reference** — in-game plain-language rulebook built from the foul catalog.

## Architecture Requirements

- UE 5.4+ C++ project, Blueprints only for UI glue and content wiring. Keep the original's strict separation: `RulesEngine` (pure C++, tested), `PlaySim` (pure C++, tested), `Grading`, `Career` (persistence), `Presentation` (all rendering/animation) — sim and rules must never depend on presentation, exactly as the source project isolated its canvas behind domain interfaces.
- Unreal Automation tests porting the original suites: enforcement edge cases (half-distance, auto first down, loss of down, accept/decline) and career grading math.
- Common UI or UMG for HUD/menus, fully gamepad-navigable.
- Ship criteria for the vertical slice: one full quarter as Down Judge at the Prep tier, night stadium, full loop (briefing → snaps → grades → film study), 60 FPS target, controller-only playable start to finish.

## Milestone Order

1. Project scaffold, Enhanced Input gamepad map, ref locomotion + cameras in a blockout stadium.
2. Port PlaySim + RulesEngine + grading in C++ with automation tests (headless, no rendering).
3. Play performance layer: 22 animated players executing simulated outcomes; foul animations.
4. Officiating verbs: key-lock, flag throw, whistle, signals, penalty report flow, ball spotting with haptics.
5. Career persistence, tier ladder, supervisor grading UI, film study replay.
6. Lifelike pass: MetaHumans, Lumen night lighting, crowds, audio, broadcast overlays, tunnel walkout, polish to 60 FPS.

Work milestone by milestone; keep the rules/sim tests green at every step.
