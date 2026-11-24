# 3D Terrain Physics Sim

Project name: Terrain Physics Simulator

## Current implementation

This document describes the target design. Proposed classes and flows below are not all implemented yet.

| Area | Implemented | Remaining work |
|---|---|---|
| Terrain | `Terrain`, `TerrainFeature`, `TerrainBuilder`, offline hills/city samples | External terrain source, asynchronous loading UI, validation and error handling for source responses |
| Object dropping | `BallFactory`, `drop_object` use case and interface adapters | Preset list, drop-speed slider, mini-map and application wiring |
| Simulation | `SimplePhysicsEngine`, play/step/stop interactors, pause through `ScenePresenter`, starting snapshot for stop | Speed UI and rewind history/use case |
| Persistence and rendering | JSON save/load, file validation, Swing 3D renderer, camera movement | Integration with terrain generation and dropping UI |

The running app opens on `SampleTerrains.hills()` (48 × 48, two-metre spacing) with one bowling ball. The flat 16 × 16 preview remains a debugging helper. The current drop package is named `drop_object`, and play/step/stop share the `simulation` package; names in the target layout below describe future structure.

## 1. What we are building

A Java Swing app where the user describes a place in words, like "rolling hills with a valley"
or "a simple city". An LLM turns the words into a terrain description.
The app builds 3D terrain from it. The user picks objects from a list, drops them on the terrain,
and watches them roll, bounce, and settle.

We keep 3D simple:
- The terrain is a **heightmap**: a grid of heights, about 48 x 48. Not real 3D models.
- Every object is a **sphere**. Different objects are presets with a different size, weight, bounce, and friction.
- The LLM never writes heights. It writes a short JSON list of shapes (hills, valleys, ramps, blocks).
  Our code turns that list into the grid.
- We draw the 3D view ourselves with Swing `Graphics2D`. No 3D library.
- The user drops objects by clicking a **top-down mini-map**, not the 3D view.

## 2. Project domain and details

Domain: Education or Science.

Short spec for the README:
The user types a prompt describing a landscape. The app asks an LLM for a terrain description
and builds a 3D terrain from it. The user picks an object from a scrolling list, sets a drop speed,
and clicks the mini-map to drop it. The objects roll under gravity.
The user can play, pause, stop (reset), rewind, and change speed, and save the scene to load it later.

## 3. APIs

**LLM API**
- Turns the user's prompt into a terrain description in JSON (see section 13).
- The system prompt limits it to our shape types and sizes.
  Example: when asked for a city, it only uses flat ground and blocks.
- Our code checks the JSON. If it is wrong, we show an error and let the user try again.
- The API key lives in an environment variable `LLM_API_KEY`. **Never commit a key.**
- The same prompt can give different terrain each time. That is fine. Save the scene to keep one.

The LLM is the only planned external terrain source. Real-world place search, elevation import, and weather APIs are outside scope. The provider and model still need to be selected; keep provider-specific HTTP and response parsing inside `LlmTerrainSource`.

## 4. Requirements Check

- Java Swing GUI: yes. Draw the 3D terrain, mini-map, and objects on JPanels.
- External API: yes. LLM API for terrain descriptions.
- Data persistence: yes. Save and load scenes as JSON files.
- One user story per person: yes. Section 6.
- Clean Architecture + SOLID: yes. Section 8.
- Pull requests: every change goes through a PR. See section 10.

## 5. Entities

**Terrain**
- cols, rows (grid size, about 48 x 48)
- cellSize (metres between grid points)
- heights (2D array, metres)
- name (the prompt or place name)
- methods: heightAt(x, y), slopeAt(x, y), width(), depth(), isInside(x, y)
- heightAt blends the four nearest grid points (bilinear), so the ground is smooth between points.
- slopeAt returns a Vector3D pointing uphill. Steeper means longer.
- x and y outside the grid are clamped to the edge.
- Immutable: a terrain never changes after it is built.

**TerrainFeature** (one shape from the LLM)
- kind: HILL, VALLEY, RAMP, BLOCK
- x, y (centre), size (radius or width/depth), height
- method: heightAt(x, y), the shape's height at that point

**TerrainBuilder**
- Turns a ground level plus a list of TerrainFeatures into a Terrain.
- This is a core rule, so it lives in the entities and has its own tests.

**ObjectPreset**
- name ("Bowling ball", "Beach ball", "Marble", ...)
- radius, mass, bounciness (0 to 1), friction
- colour (as an RGB number, so entities don't import Swing)

**Ball**
- position (Vector3D), velocity (Vector3D)
- preset (ObjectPreset)
- methods: getters, setters, isOnGround(terrain)

**Scene**
- one Terrain
- list of Balls
- gravity (default 9.81)
- simSpeed
- methods: addBall(), removeBall(), getBalls(), copy()

**SceneSnapshot** (for stop and rewind)
- the position and velocity of every ball at one moment
- methods: capture(scene), restore(scene)

**Vector3D** (helper)
- x, y, z
- methods: add, subtract, scale, dot, length, normalize

## 6. User stories

| Person | Story |
|---|---|
| A | As a user, I want to describe a landscape in words and get 3D terrain, so that I can play on any land I imagine. |
| B | As a user, I want to pick an object from a list, set its drop speed, and drop it anywhere, so that I can see how different objects roll. |
| C | As a user, I want to play, pause, stop, rewind, and change speed, so that I can watch at my own pace and replay what happened. |
| D | As a user, I want to save a scene and load it later, so that I don't lose a terrain I liked. |

## 7. Use cases (MVP)

Each lead writes: the interactor, its input/output interfaces, the controller,
the presenter, the UI part that triggers it, and the UI update after it.

**UC1: Generate terrain (A)**
1. User types a prompt and clicks Generate.
2. App shows a loading indicator. The UI does not freeze.
3. App sends the prompt to the LLM and gets back a terrain description (JSON).
4. App checks the description and builds a Terrain with TerrainBuilder.
5. App clears the old balls, pauses the sim, and draws the new terrain.
- Error case: LLM fails, times out, or returns bad JSON. Show a message, keep the old terrain.

**UC2: Drop object (B)**
1. User scrolls the object list and picks a preset.
2. User sets a drop speed with a slider.
3. User clicks a spot on the top-down mini-map.
4. App creates a Ball above that spot, moving down at the drop speed, and adds it to the Scene.
5. App draws the ball in the 3D view and on the mini-map.
- Error case: click is outside the terrain. Ignore it.

**UC3: Simulation controls (C)**
1. User clicks Play. A Swing Timer starts. App saves a starting snapshot.
2. Each tick calls the Step use case. Step moves every ball by a small time step
   and records a snapshot for rewind.
3. Presenter tells the view to redraw.
4. Pause: the timer stops.
5. Stop: the timer stops and the balls go back to the starting snapshot.
6. Rewind: while held, the app plays the saved snapshots backwards.
7. Speed slider: changes how much sim time each tick covers.

**UC4: Save / load scene (D)**
1. User clicks Save and picks a file name.
2. App writes the Scene (terrain heights, prompt, balls, presets, settings) to a JSON file.
3. User clicks Load and picks a file.
4. App reads it, builds the Scene, and draws it. No LLM call needed.
- Error case: bad or missing file. Show a message.

Draw a sequence diagram for each in PlantUML. Keep the `.puml` files in the repo.

## 8. Architecture

We follow *Clean Architecture* by Robert C. Martin.
The main idea: the app is built in rings. Inner rings hold the rules.
Outer rings hold the details (Swing, HTTP, files, the LLM). Details can change. Rules should not.

### 8.1 The four layers (inside out)

| Layer | Book name | What goes here | Our classes |
|---|---|---|---|
| 1 | Entities | Core rules that would be true in any app | `Terrain`, `TerrainFeature`, `TerrainBuilder`, `Ball`, `ObjectPreset`, `Scene`, `SceneSnapshot`, `Vector3D`, `PhysicsEngine` |
| 2 | Use cases | What the app does, one class per action | `GenerateTerrainInteractor`, `DropBallInteractor`, `StepInteractor`, `ResetInteractor`, `RewindInteractor`, `SaveSceneInteractor`, ... |
| 3 | Interface adapters | Convert data between use cases and the outside | controllers, presenters, view models |
| 4 | Frameworks and drivers | Tools we don't own | Swing views, LLM HTTP calls, JSON files, `Main` |

### 8.2 The Dependency Rule (the one rule that matters most)

**Source code dependencies only point inward.**

- An inner class never names an outer class. No `import` of anything outward.
- Entities import nothing from our other layers.
- Use cases import entities only. No `javax.swing`, no `java.net.http`, no `org.json`.
- Interface adapters import use cases and entities. No `javax.swing` here either.
- Only layer 4 imports Swing, HTTP, or JSON libraries.

When an inner layer needs something from outside (get a terrain description, show a result),
it **declares an interface it owns**, and the outer layer implements it.
This is the Dependency Inversion Principle and it is how we cross every boundary.

Quick check in code review: open the `import` lines. If any arrow points outward, reject the PR.
This matters more now that we use AI to write code: AI often puts logic or imports in the wrong layer.

### 8.3 Package layout

```
src/main/java/
  entity/                      layer 1
    Terrain.java  TerrainFeature.java  TerrainBuilder.java
    Ball.java  ObjectPreset.java  Scene.java  SceneSnapshot.java  Vector3D.java
    PhysicsEngine.java         interface: step(scene, dt)
    SimplePhysicsEngine.java   our section 9 rules
    SampleTerrains.java        shared hardcoded terrains for development and tests
  use_case/                    layer 2, one sub-package per use case
    generate_terrain/
      GenerateTerrainInputBoundary.java    interface, called by the controller
      GenerateTerrainInputData.java        plain data in (the prompt)
      GenerateTerrainInteractor.java       implements the input boundary
      GenerateTerrainOutputBoundary.java   interface, implemented by the presenter
      GenerateTerrainOutputData.java       plain data out
      TerrainSource.java                   interface: prompt -> list of TerrainFeatures
      TerrainUnavailableException.java
    drop_ball/   step/   reset/   rewind/   save_scene/   load_scene/   (same shape)
  interface_adapter/           layer 3
    ViewManagerModel.java
    generate_terrain/
      GenerateTerrainController.java
      GenerateTerrainPresenter.java
      GenerateTerrainState.java    what the screen shows (prompt, loading, error)
    simulation/
      SimulationViewModel.java
      SimulationState.java         heights, balls, playing, speed
    ...
  data_access/                 layer 4
    LlmTerrainSource.java             LLM HTTP call + JSON parsing
    FileSceneDataAccess.java          JSON save/load
    InMemorySceneDataAccess.java      holds the current Scene and snapshots while the app runs
  view/                        layer 4
    PromptPanel.java           prompt box, Generate button, loading indicator
    TerrainView3D.java         3D drawing with Graphics2D, drag to rotate
    MiniMapPanel.java          top-down view, click to drop
    ObjectListPanel.java       scrolling list of presets + drop speed slider
    ControlsPanel.java         play, pause, stop, rewind, speed
    Camera.java                3D to screen math (view layer only)
  app/                         layer 4, the "Main component"
    AppBuilder.java  Main.java
src/test/java/
  use_case/...                 interactor tests with fake data access
  entity/...                   terrain, builder, and physics math tests
```

### 8.4 How one use case flows (example: UC1 Generate terrain)

```
PromptPanel (view)                  runs the call in a SwingWorker so the UI doesn't freeze
  -> GenerateTerrainController      builds GenerateTerrainInputData from the text field
  -> GenerateTerrainInputBoundary   (interface)
  -> GenerateTerrainInteractor      the rules: check the prompt, ask the source for features,
                                    check the features, build Terrain, put it in a new Scene
       -> TerrainSource             (interface, owned by the use case)
            <- LlmTerrainSource     (layer 4 implements it)
  -> GenerateTerrainOutputBoundary  (interface)
  -> GenerateTerrainPresenter       turns OutputData into state
  -> SimulationViewModel            fires a property change
  -> TerrainView3D, MiniMapPanel    repaint
```

Notice the arrows of control go out and back, but every **source code** arrow points inward.
The interactor calls the presenter only through `OutputBoundary`, so it never knows Swing exists.
And it never knows which LLM we use, or whether the terrain came from an LLM at all.

Every use case must have all of these pieces:
- `InputData` and `OutputData`: plain Java objects. No entities passed out to the view.
  The presenter gets numbers and strings, not a `Ball`.
- `InputBoundary` and `OutputBoundary`: interfaces.
- One `Interactor`: the only place business logic lives.
- One data access interface: only the methods this use case needs.
- One `Controller`, one `Presenter`, and a `State` in a `ViewModel`.
- A success path and a fail path in the presenter (`prepareSuccessView`, `prepareFailView`).

### 8.5 Rules for each layer

**Entities**
- Pure Java. Can be tested with nothing but JUnit.
- Hold the physics and terrain math (`heightAt`, `slopeAt`, `TerrainBuilder`). That is our core rule set.
- `PhysicsEngine` is an interface here; `SimplePhysicsEngine` is the default.
  We can swap in a better one later (Strategy pattern, Open/Closed Principle).

**Use cases**
- One interactor per user action. It does one job (Single Responsibility).
- Gets everything through its constructor: data access, presenter, physics engine.
  It never calls `new` on an outer class.
- No Swing types, no `Exception` from HTTP leaking out. Catch at the data access
  layer and throw our own exception (for example `TerrainUnavailableException`, declared in the use case package).

**Interface adapters**
- Controllers are thin. They only copy UI input into `InputData` and call the interactor.
- Presenters are thin. They only format `OutputData` into `State` (round numbers, build messages).
- View models hold state and notify listeners. No logic.

**Frameworks and drivers**
- Views are humble: they read the view model and draw, and forward clicks to a controller.
  No physics, no API calls, no `if` logic about the rules.
- 3D drawing math (`Camera`) is a view detail. It turns world points into screen points. It is not physics.
- Data access classes implement the use case interfaces. All HTTP, LLM, and JSON parsing stays here.
- If we swap LLM providers or replace JSON with a database, only this layer changes.

### 8.6 The Main component (wiring)

The book calls `Main` the dirtiest component. It is the only place that knows every concrete class.
`AppBuilder` creates everything and plugs it together:

```java
new AppBuilder()
    .addSimulationView()
    .addGenerateTerrainUseCase()
    .addDropBallUseCase()
    .addSimulationControlUseCases()
    .addSaveSceneUseCase()
    .build();
```

Each lead adds their own `addXxx()` method. That keeps merge conflicts small.

### 8.7 The simulation loop

- A Swing `Timer` lives in the view layer (it is a framework detail).
- Each tick it calls `StepController.execute(dt)`, about 60 times a second.
- `StepInteractor` asks `PhysicsEngine` to move the balls, records a snapshot, then calls the presenter.
- Rewind and stop go through their own interactors, which read snapshots from data access.
- So moving balls goes through the same layers as every other action. No shortcuts from the timer to the entities.

### 8.8 Shared state between use cases

Several use cases need the current `Scene` (drop ball, step, reset, rewind, save).
We keep it in one `InMemorySceneDataAccess` object that implements each use case's small interface
(`DropBallDataAccessInterface`, `StepDataAccessInterface`, `RewindDataAccessInterface`, ...).
Each interface only lists the methods that use case needs (Interface Segregation Principle).
Whoever needs it first creates it. Others add their methods in their own PR.

### 8.9 SOLID and patterns, mapped to our code

| Principle / pattern | Where we show it |
|---|---|
| Single Responsibility | One interactor per use case; controller, presenter, view each do one job |
| Open/Closed | New LLM provider or physics engine = new class, no edits to interactors |
| Liskov Substitution | Fake and real terrain sources are fully interchangeable in tests |
| Interface Segregation | One small data access interface per use case, not one big one |
| Dependency Inversion | Interactors depend on interfaces they own; outer layers implement them |
| Strategy | `PhysicsEngine`, `TerrainSource` |
| Memento | `SceneSnapshot` for stop and rewind |
| Builder | `TerrainBuilder`, `AppBuilder` |

### 8.10 Testing

- Interactor tests use a fake data access class and a fake presenter that records what it got.
  No network, no LLM, no Swing, no files.
- `GenerateTerrainInteractor` tests use a fake `TerrainSource` that returns fixed features,
  and one that throws, to cover the fail path.
- Aim for every line in each interactor covered, including the fail path.
- Entity tests check the math: `heightAt` matches the grid, slope points uphill,
  TerrainBuilder puts a hill where asked, a ball stops in a valley, a ball never goes under the ground,
  rewind gives back the exact earlier positions.

### 8.11 PR review checklist (architecture)

- [ ] No outward imports (check `import` lines against 8.2).
- [ ] No Swing, HTTP, LLM, or JSON code outside `view/`, `data_access/`, `app/`.
- [ ] Entities are not passed to presenters or views. Only `OutputData` crosses.
- [ ] Logic lives in the interactor or entity, not the controller, presenter, or view.
- [ ] New use case has all the pieces in 8.4 and an interactor test.
- [ ] Wiring added only in `AppBuilder`.
- [ ] No API keys in the code or commits.

## 9. Physics (we keep it simple)

- Every object is a sphere. No boxes, no spinning, no ball-to-ball crashes in the MVP.
- In the air: gravity pulls it down.
- On the ground: it speeds up along the slope under it. Steeper slope means faster.
- Friction (from the preset) slows it down so it stops in valleys.
- Landing: when it hits the ground, flip the downward speed and multiply by the preset's bounciness.
- Walls (city blocks): if the next step would climb more than a fixed amount, bounce the ball back
  instead of letting it go up. This is a cheap trick, not real wall collision.
- Use a small fixed time step. After each step, if a ball is under the ground, put it back on top.
  This stops balls sinking or flying off.
- At the grid edge, clamp the ball inside the terrain and zero the velocity component pointing outward. Keep the ball in the scene, as implemented by `SimplePhysicsEngine`.

Rule for the team: good-enough physics is fine.
The grade is mostly about design. Don't spend weeks on accuracy.

## 10. How we work

- Nobody pushes to main. Every change is a PR.
- Every PR needs one review from a teammate before merging.
- Every PR also updates the README if it changes stories, use cases, or status.
- Each person only writes code for their own use case.
  Shared parts (like the main view) are agreed with the other lead first.
- Write unit tests for each interactor using fake data access.
- AI help is allowed. You still own and must understand every line in your PR.
  Check AI code against the 8.11 checklist before asking for review.

### 10.1 Who owns what

| Person | Builds | Uses while teammates work |
|---|---|---|
| A: terrain | `Terrain`, `TerrainFeature`, `TerrainBuilder`, `SampleTerrains`, UC1 with `LlmTerrainSource`, `PromptPanel` + loading UI | A fake terrain source and a flat preview |
| B: dropping objects | `ObjectPreset` list, `ObjectListPanel`, drop speed, `MiniMapPanel` click to drop, UC2 | `SampleTerrains.hills()` and a paused scene |
| C: simulation | `PhysicsEngine`, step, play/pause/stop, speed, `SceneSnapshot` + rewind, UC3 | `SampleTerrains.hills()` with a ball already on it |
| D: persistence + 3D view | UC4 save/load, `TerrainView3D` and `Camera` (3D drawing) | A sample scene with terrain, balls, and settings |

D owns the 3D view because persistence is the smallest use case.

## 11. Timeline (3 weeks)

**Week 1: Nov 16 to Nov 22. Shared pieces first.**
- Day 1 to 2: agree on and merge `Terrain`, `Ball`, `ObjectPreset`, `Scene`, `Vector3D`,
  `SampleTerrains`, and the terrain JSON format (section 13). **This blocks everyone. A leads it.**
- Everyone builds against `SampleTerrains.hills()`, not the LLM.
- End of week goal: hardcoded terrain shows in 3D with one ball rolling on it.

**Week 2: Nov 23 to Nov 29. Real features.**
- LLM terrain generation works end to end.
- All object presets, drop speed, mini-map dropping.
- Play, pause, stop, speed.
- Save and load.

**Week 3: Nov 30 to Dec 6. Extras, polish, presentation.**
- Rewind.
- City prompts with the wall bounce.
- README, screenshots, sequence diagrams, presentation.
- If we are behind, cut from the list in 11.1.

### 11.1 Cut list (cut from the top if we are behind)

1. City wall bounce (cities still show, balls just roll over block edges)
2. Rewind (stop/reset stays)
3. Drag to rotate the camera (use one fixed angle)

Never cut: generate terrain, drop objects, play/pause/stop, save/load. Those are our four user stories.

## 12. Risks

- **Physics is unstable.** Balls fly off or sink. Fix: small fixed time step, clamp to the ground every tick.
- **The LLM fails or returns junk.** Fix: strict system prompt, check the JSON, show an error, keep the old terrain.
- **The LLM or network is down during the demo.** Fix: load a saved scene. Persistence covers us.
- **3D drawing eats time.** Fix: flat-shaded triangles, painter's algorithm, no textures, no lighting tricks.
- **The `Terrain` agreement slips.** Everyone is blocked. Fix: settle it in the first two days.
- **AI writes code in the wrong layer.** Fix: the 8.11 checklist on every PR.
- **API key gets committed.** Fix: environment variable only, never in code.
- **One person writes too much.** Fix: stick to the 10.1 table. Graders check git logs.

## 13. Terrain JSON format (what the LLM returns)

The LLM returns only this. Our code checks it and builds the Terrain.

```json
{
  "name": "Rolling hills with a valley",
  "ground": 0,
  "features": [
    { "kind": "HILL",   "x": 12, "y": 30, "size": 8,  "height": 20 },
    { "kind": "VALLEY", "x": 30, "y": 20, "size": 10, "height": 12 },
    { "kind": "RAMP",   "x": 5,  "y": 5,  "size": 6,  "height": 10, "direction": "EAST" },
    { "kind": "BLOCK",  "x": 40, "y": 40, "size": 4,  "height": 25 }
  ]
}
```

- `x`, `y`, `size` are in grid cells (0 to 47). `height` is in metres.
- `kind` must be one of HILL, VALLEY, RAMP, BLOCK. Anything else is an error.
- Limits: at most 20 features, heights from 0 to 50.

System prompt rules (draft):
- Only output JSON in the format above. No other text.
- Use only the four kinds.
- When the user asks for a city, use flat ground and BLOCKs in a simple grid of streets. No hills.
- When the user asks for nature (hills, mountains, valleys), use HILL, VALLEY, and RAMP. No BLOCKs.
- Keep everything inside the grid.

First test (A, this week): send three prompts ("hills", "a simple city", "a ski slope")
from a small Java main and print the JSON. If it follows the format every time, the idea works.

## 14. Future extensions

- Objects that aren't spheres (boxes, cars) with rotation.
- Ball-to-ball collisions.
- Real wall collision for cities.
- Bigger grids and smoother shading.
- Share scenes with other users through an online store.
