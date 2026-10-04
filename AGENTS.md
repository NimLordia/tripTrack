# TripTrack agent entry point

Android ride recorder. Load only the context needed for the assigned change.

- Read `docs/product.md`, then select one route in `docs/index.md`.
- `./scripts/context.ps1 -Task capture` prints a focused context packet. Use `-List` for tasks.
- Read relevant source files before editing; follow a linked specification only when it affects the task.
- Keep requirements in their topic file. Link instead of repeating them. Pages in `docs/sources.md` preserve history; do not preload the whole chat or backlog.
- When delegating, use a fresh agent context when supported. Send the task, owned paths, selected specification paths and relevant interface files. Avoid copying the entire project history.
- Keep source files focused. Split by responsibility, not an arbitrary line limit. Keep routine specifications near 200 words; place detailed references separately.
- User corrections override older documents. Preserve the distinction between requirements, scaffold choices and open product decisions.

Code: `app/src/main/java/com/triptrack/app/`. Architecture: `docs/architecture.md`. Build: `docs/build.md`.

Run checks relevant to changes; the Android baseline is `./scripts/build.ps1`. Do not claim live recording, cloud sync or device behavior is verified by a successful compile.
