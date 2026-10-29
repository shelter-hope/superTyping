# Agent instructions

## Git

- Commit as **绘山雷霆** `<1464598950@qq.com>` (`git config user.name` / `user.email`). Do not use `Cursor Agent`.
- Commit messages (subject and body) must be in **Chinese**. Prefer clear, imperative summaries such as `文档：…` / `修复：…` / `功能：…` / `重构：…` / `部署：…` / `数据库：…`.
- Default branch is **master**. Keep local and remote in sync; prefer working on `master` unless a feature branch is explicitly required.
- Remotes: **gitee** (`painted-mountain-thunder/super-typing`) is the primary mirror, **github** (`shelter-hope/superTyping`) is the secondary. There is no `origin`; after finishing work push to both (`git push gitee master` / `git push github master`).
- Never commit `.idea/`. `.trae/rules/project-rules.md` is project rules, not local state: keep it in sync when conventions change.

## Backend structure (typingGame)

- Spec: `typingGame/docs/backend-structure-spec.md`; audit & roadmap: `docs/backend-structure-audit-and-plan.md`.
- Layout: `libs/kernel` + `libs/security` (libraries only) and `services` = one module per Spring Boot service. No `common` module may come back.
- Each service keeps the same 7 top-level packages: `controller / service(+service.impl) / repository / entity / dto / config / support`; domain sub-packages live under `service` or `support`.
- Entities belong to exactly one service. Cross-service reads go through `/api/v1/internal/**` APIs; the gateway returns 404 for any path containing `/internal/`.
- Database: one MySQL schema per service (`typing_game_<domain>`), schema created by Flyway `V1__init.sql` on startup, `ddl-auto=validate`. `typingGame/DB/typing_word_game.sql` is the single-file baseline for reference only.

## Gates

- Backend: `cd typingGame; mvn -o test` (includes per-service `ArchitectureTest`).
- Migrations/deploy changes: `typingGame/deploy/verify-migrations.ps1`; running stack smoke: `typingGame/deploy/smoke.ps1` (scripts are UTF-8 **with BOM**, required by Windows PowerShell 5.1).
- Frontend: `cd app-64; npm test && npm run build`.
