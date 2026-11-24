# Terrain Physics Simulator

A Java Swing simulator for dropping spheres onto 3D terrain. The plan is to use an LLM to turn a text prompt into a terrain description, then let users select and drop objects, control the simulation, and save or load scenes.

## User stories and owners

| Owner | User story |
| --- | --- |
| Person A | Describe a landscape and generate terrain. |
| Person B | Pick an object, set its drop speed, and drop it on the mini-map. |
| Person C | Play, pause, stop, rewind, and change simulation speed. |
| Nick Wang (Person D) | Save a scene and reopen it later. Person D also owns the 3D renderer. |

The planned terrain APIs and shared architecture are described in [plan.md](plan.md). The preview starts on a 48 × 48 rolling-hills heightmap with one bowling ball. Heights are in metres and adjacent samples are two metres apart. A flat 16 × 16 scene remains available in code for debugging.

## Terrain generation plan

The user describes an imagined landscape, such as "rolling hills with a valley" or "a simple city". The LLM returns a JSON list of hills, valleys, ramps, and blocks. Java validates the description and `TerrainBuilder` converts it into a heightmap; the LLM does not generate the height grid directly.

The LLM is the only planned external terrain source. Real-world elevation import and weather integration are outside scope. The provider and model are still to be selected. The planned integration reads its key from `LLM_API_KEY`; never commit a key. The current offline preview needs no API key.

Generation will run in the background. Invalid responses, timeouts, or network failures will show an error and keep the existing scene. Saved scenes include the heightmap and reopen without an LLM call.

## Current status

- Working in the app: 3D terrain rendering, camera movement, sphere physics, play/pause/stop, and JSON scene save/load.
- Implemented backend, awaiting UI integration: object dropping through `DropObjectInteractor`, its controller, presenter, and view model.
- Planned: LLM terrain integration, the prompt panel, object selection and drop-speed controls, mini-map dropping, a simulation-speed control, and rewind.
- `SampleTerrains.hills()` and `SampleTerrains.city()` provide offline terrain for development and tests.

## Run the preview

1. Open this Maven project in IntelliJ and select JDK 26 as the project SDK.
2. Run the `main` method in `app.Main`.
3. Drag on the 3D view to rotate. Use **W/S** to move forward/back along the ground, **A/D** to move left/right, and **Q/E** to move up/down. Opening a new terrain centres the camera on it.
4. Click **Play** to roll the ball down the hills, **Pause** to hold its current position, and **Stop** to restore the state captured on the first Play. Play after Pause resumes the same run.
5. Use **Save Scene** to export the current scene to JSON and **Load Scene** to reopen it. A successful load pauses the simulation.

## Model View Presenter

Swing calls a listener in the view when the user clicks Save or Load, presses a movement key, or drags the scene. The listener immediately calls `ScenePresenter`. For file actions, the presenter opens the chooser through the view and passes the selection to the appropriate controller. The model consists of the save/load interactors, the current scene, and the JSON storage gateway.

The interactor reports success, failure, or a file requiring replacement to the same presenter through its output boundary. The presenter updates `SimulationViewModel`. The view model calls `displayChanged` on the presenter, which calls `display` on the Swing view. Camera movement follows the same display update path and changes only the camera pose.

Person A owns LLM terrain generation, response validation, and the prompt/loading UI. Person D’s renderer and scene files accept a rectangular grid of heights in metres and its cell spacing through the existing terrain mapper.

For terminal builds, set `JAVA_HOME` to JDK 26. On macOS, run `export JAVA_HOME=$(/usr/libexec/java_home -v 26)`. Run `mvn test` for the suite or `mvn -Dtest=PlaneTerrainTest test` for the flat-terrain cases.
