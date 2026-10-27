# Real Terrain Physics Sim

Project name: TerrainSim? TerrainPhys? idk

## 1. What we are building

A Java Swing app where the user picks a real place on Earth.
The app loads the real shape of the land from an API.
The user drops balls on it and watches them roll down the real slopes.

We show the land from the side, like a mountain cut in half.
That keeps the physics simple and needs only one API call per scene.

## 2. Project domain and details

Domain: Education or Science.

Short spec for the README:
The user searches for a place by name. The app finds its coordinates.
The user picks a start and end point. The app gets 100 ground heights along that line.
The app draws the land from the side. The user drops balls on it.
The balls roll under gravity. The user can pause, change speed, and save the scene.

## 3. APIs

**Open-Meteo Elevation API**
- Docs: https://open-meteo.com/en/docs/elevation-api
- Endpoint: `https://api.open-meteo.com/v1/elevation`
- Send up to 100 points per call. Get back a JSON list called `elevation`, in metres.
- Data is 90 m resolution. Fine for hills and valleys, not small bumps.
- No API key needed for non-commercial use.
- We must credit Copernicus and Open-Meteo in the app and README.

**Open-Meteo Geocoding API**
- Turns a place name like "Whistler" into latitude and longitude.
- Lets the user type a name instead of numbers.


## 4. Requirements Check

- Java Swing GUI: yes. Draw the land and balls on a JPanel.
- External API: yes. Elevation + geocoding.
- Data persistence: yes. Save and load scenes as JSON files.
- One user story per person: yes. section 6.
- Clean Architecture + SOLID: yes. section 8.
- Pull requests: every change goes through a PR except this plan. See section 10.

## 5. Entities

**Terrain**
- startLat, startLon, endLat, endLon
- heights (list of 100 numbers, metres)
- spacing (metres between points)
- methods: heightAt(x), slopeAt(x), length()

**Ball**
- position (x along the land, plus height)
- velocity
- mass, radius
- methods: getters, setters, isOnGround()

**Scene**
- one Terrain
- list of Balls
- gravity (default 9.81)
- simSpeed
- methods: addBall(), removeBall(), getBalls()

**Vector2D** (helper)
- x, y
- methods: add, scale, length

## 6. User stories

- As a user, I want to search a place by name and load its real terrain, so that I can play on real land.
- As a user, I want to drop balls anywhere on the terrain, so that I can watch where they roll.
- As a user, I want to play, pause and change the speed, so that I can watch at my own pace.
- As a user, I want to save a scene and load it later, so that I don't lose my setup.

## 7. Use cases (MVP)

Each lead writes: the interactor, its input/output interfaces, the controller,
the presenter, the UI part that triggers it, and the UI update after it.

**UC1: Load terrain**
1. User types a place name and clicks Search.
2. App calls geocoding and shows the coordinates.
3. User sets the line length or end point and clicks Load.
4. App splits the line into 100 points and calls the elevation API.
5. App builds a Terrain and draws it.
- Error case: no place found, or API fails. Show a message.

**UC2: Drop ball**
1. User clicks a spot above the land.
2. App creates a Ball there and adds it to the Scene.
3. App draws the ball.
- Error case: click is outside the land. Ignore it.

**UC3: Play / pause / speed**
1. User clicks Play.
2. A Swing Timer starts. Each tick calls the Step use case.
3. Step moves every ball forward by a small time step.
4. Presenter tells the view to redraw.
5. User clicks Pause. Timer stops.
6. User moves the speed slider. Time step changes.

**UC4: Save / load scene**
1. User clicks Save and picks a file name.
2. App writes the Scene (place, heights, balls, settings) to a JSON file.
3. User clicks Load and picks a file.
4. App reads it, builds the Scene, and draws it. No API call needed.
- Error case: bad or missing file. Show a message.

Draw a sequence diagram for each in PlantUML. Keep the `.puml` files in the repo.

## 8. Architecture

Four layers, inside out:

1. **Entities**: Terrain, Ball, Scene, Vector2D. No Swing, no API code.
2. **Use cases**: one interactor per use case. Talks only to interfaces.
3. **Interface adapters**: controllers, presenters, view models.
4. **Frameworks**: Swing views, API calls, file reading and writing.

Key rules:
- Use cases never call the API or files directly. They call a data access interface.
  Example: `TerrainDataAccess` with `getHeights(points)`.
  The real version calls Open-Meteo. A fake version returns fixed numbers for tests.
- The physics sits behind a `PhysicsEngine` interface with `step(scene, dt)`.
  We can swap in a better engine later. This is the Strategy pattern.
- The frame loop is just a Swing Timer calling the Step controller about 60 times a second.
  So moving balls goes through the same layers as everything else.

## 9. Physics (we keep it very simple)

- Treat each ball as a point that sits on the land or falls through the air.
- In the air: gravity pulls it down.
- On the land: it speeds up based on how steep the slope is under it.
  Steeper slope means faster.
- Add a little friction so balls slow down and stop in valleys.
- Round balls only. No boxes, no spinning, no ball-to-ball crashes in the MVP.

Rule for the team: good-enough physics is fine.
The grade is mostly about design. Don't spend weeks on accuracy.

## 10. How we work

- Nobody pushes to main. Every change is a PR.
- Every PR needs one review from a teammate before merging.
- Every PR also updates the README if it changes stories, use cases, or status.
- Each person only writes code for their own use case.
  Shared parts (like the main view) are agreed with the other lead first.
- Write unit tests for each interactor using fake data access.

## 11. Timeline

**Now to user stories deadline (check Quercus)**
- Agree on the side view and the story list.
- Fill in the lead for each story.
- One person makes a real elevation API call from Java (see section 13).
- Put stories and spec in the README.

**Before the design lab after Reading Week**
- Sketch every screen.
- Write the use cases in the course template.
- Draw entity diagram and sequence diagrams in PlantUML.

**Weeks 7 to 8 (Clean Architecture lectures)**
- Set up the folder layout for the four layers.
- Build entities and the empty interfaces first, together.
- Each lead starts their use case.

**After that, until the presentation**
- Finish MVP use cases.
- Refactor based on code reviews and SOLID.
- Add later stories if time allows.
- Write down future ideas and patterns we'd add.

## 12. Risks

- **Physics eats our time.** Fix: keep the rules in section 9. Timebox it.
- **API is harder than it looks.** Fix: test a real call this week.
- **One person writes too much.** Fix: stick to the lead table. Graders check git logs.
- **Flat land looks boring.** Fix: test with real hilly places early (Whistler, Banff, Blue Mountain).
- **API is down during the demo.** Fix: load a saved scene instead. Persistence covers us.

## 13. First API test

Pick two points on a hill. Split the line into 100 points. Send them. Print the heights.

```java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ElevationTest {
    public static void main(String[] args) throws Exception {
        double lat1 = 50.115, lon1 = -122.950;  // Whistler, bottom
        double lat2 = 50.060, lon2 = -122.950;  // Whistler, higher up
        int n = 100;

        StringBuilder lats = new StringBuilder();
        StringBuilder lons = new StringBuilder();
        for (int i = 0; i < n; i++) {
            double t = i / (double) (n - 1);
            if (i > 0) { lats.append(","); lons.append(","); }
            lats.append(lat1 + t * (lat2 - lat1));
            lons.append(lon1 + t * (lon2 - lon1));
        }

        String url = "https://api.open-meteo.com/v1/elevation"
                + "?latitude=" + lats + "&longitude=" + lons;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).build();
        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response.body());
    }
}
```

If the numbers go up and down like a hill, the idea works.
The course examples use OkHttp. Either is fine.

## 14. Future extensions

- Top-down view: a grid of heights shown as a colour map. Balls roll in any direction.
- Ball-to-ball collisions.
- Compare two places side by side.
- Live weather from Open-Meteo, like wind pushing the balls.
- Share scenes with other users through an online store.
