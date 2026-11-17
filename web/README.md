# BoardFlow Web

An offline-capable local web studio for planning storyboards and silent animatics. Spring Boot serves the editor and API from the same process. Once the server is running, users need only a browser.

## Start

From the repository root, run `build_web_windows.bat`, then `run_web_windows.bat`. Open **http://localhost:8080**. Building requires JDK 21+ and Maven 3.6.3+. Running the compiled JAR requires Java 21+; browser users need no Java installation or compiler. Node is used only for optional browser tests.

Alternatively, from `web/`:

```powershell
mvn package
java -jar target/boardflow-web-1.0.0.jar
```

The first visit offers a complete example storyboard. New projects can be empty or include example shots. Example images are original SVG storyboard sketches, clearly labeled; importing your own frame replaces the sketch. All editor assets are served locally, with no external font or image requests.

## What works

- Create, rename, describe, switch between, and delete storyboards.
- Add establishing shots, dialogue close-ups, and action tracking shots.
- Edit scene, title, action notes, dialogue, camera angle, movement, duration, and transition.
- Upload PNG, JPEG, or WebP frames. The browser scales them to at most 1280 pixels on the longest side and stores JPEGs with the project.
- Drag to reorder, or use the inspector's earlier/later buttons. Duplicate and remove shots.
- Search across titles, scenes, action notes, dialogue, and shot types; switch board/list views.
- See live shot/scene counts, total runtime, and a proportional sequence strip.
- Apply recommended durations and read camera checklists supplied by the domain subclasses.
- Preview a silent animatic with pause, restart, seek, cut, dissolve, and fade to black. Transitions occupy the last 0.6 seconds of a shot, without extending its runtime.
- Autosave after edits, with an explicit saving indicator and unsaved-work navigation protection. Stale-tab saves return a conflict rather than overwrite newer work.
- Export a complete `.boardflow.json` backup with images, import it as a new project, or export a CSV shot list.
- Use the responsive phone layout and keyboard shortcuts: N adds a shot, Space previews/pauses, Ctrl/Cmd+S saves, ? opens help.

## Architecture

```text
Browser HTML/CSS/JS
        |
Spring MVC ProjectController
        |
Application StoryboardService ---> Domain StoryboardShot
        |                            |-- EstablishingShot
ProjectRepository (port)              |-- DialogueCloseUp
        |                            `-- ActionTrackingShot
JpaProjectRepository (adapter)
        |
H2 database: projects + ordered shot records
```

`domain/` has no Spring, persistence, or UI dependency. `application/` owns use cases, transport-independent project records, and the repository contract. `persistence/` maps those records to JPA entities. `web/` handles HTTP, validation, and status codes. Derived camera advice and duration recommendations are recalculated by the backend, not trusted from incoming JSON. A future JavaFX client can consume the same API or call the application services inside a Spring context.

## Data and scope

The default H2 database lives in `web/data/boardflow.mv.db` when started through the launcher. Data survives server restarts. Export project backups to transfer work; copy the database file only while the server is stopped. Project backups use the web JSON format; the original Swing XML `.boardflow` format is separate.

This first web release is a single-user local workspace, bound to `127.0.0.1`. Authentication, shared accounts, cloud synchronization, recorded audio, and video export are not implemented. A remotely hosted release needs server hosting that supports Java, account access controls, and a managed database configuration. The JavaFX interface remains future work.

Limits: 250 shots per project; 1-3600 seconds per shot; titles 120 characters; notes/dialogue 20,000 characters per field. Export backups before clearing local application data.

## API

| Method | Path | Behavior |
|---|---|---|
| GET | `/api/projects` | List project summaries |
| POST | `/api/projects` | Create with `{ "title": "My film", "demo": false }` |
| GET | `/api/projects/{id}` | Read the complete project |
| PUT | `/api/projects/{id}` | Save the complete document, including its latest `version` |
| DELETE | `/api/projects/{id}` | Delete the project and its shots |

Invalid data returns 400, missing projects 404, and stale versions 409. Project image data is validated as a decodable PNG/JPEG header with dimensions up to 4096 pixels per side. Project and shot IDs are UUIDs. The backend computes total runtime, recommended durations, and checklists on responses.

## Verification

```powershell
mvn test
npm ci
npm run check
```

For browser tests, start a separate instance in one terminal:

```powershell
java -jar target/boardflow-web-1.0.0.jar --server.port=8081 --spring.datasource.url=jdbc:h2:file:./target/browser-data/boardflow
```

In another terminal in `web/`, run `npm run test:browser`. Tests use locally installed Google Chrome and their own projects; no Playwright browser download is needed. Screenshots and traces are written to ignored `test-results/`. To use another test server, set `BOARDFLOW_URL`.
