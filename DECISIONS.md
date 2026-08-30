# Crew Chief — Architectural & Design Decisions (DECISIONS.md)

### D1: Package & Module Layout Strategy
- **Decision:** Use a clean, modular single-app multi-package structure with strict MVVM and unidirectional data flow (`core/model`, `core/data`, `core/ui`, `rules/engine`, `rules/data`, `sim/engine`, `feature/game`, `feature/career`, `feature/film`, `feature/crew`, `feature/collection`, `feature/events`).
- **Rationale:** Keeps compilation fast and deterministic within Android Gradle while isolating the rendering canvas behind domain interfaces for future 3D expansion without coupling game logic.

### D2: Legal & Procedural Fictionalization
- **Decision:** 100% original, procedurally generated fictional teams (e.g. Ironwood Forge, Cascades Osprey, Metro Rail), generic tier rulesets (Youth, Prep, Small College, Major College, Semi-Pro, Professional), and custom plain-language rule explanations.
- **Rationale:** Strict adherence to zero IP infringement constraints while modeling authentic officiating mechanics.

### D3: Officiating Perspectives & Field Canvas
- **Decision:** Custom Jetpack Compose `Canvas` rendering engine with dynamic viewport transformation tailored for each officiating position (e.g., Down Judge sideline plane with chain crew line-of-scrimmage tracker, Referee offensive backfield pocket view, Deep Judge endzone coverage).
- **Rationale:** Delivers 60 FPS fluid rendering on portrait and landscape orientations with one-handed gesture recognition (flick flag throws, magnetic down marker spotting, lasso key locking).
