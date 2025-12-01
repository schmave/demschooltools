# AGENTS.md

## Role & Scope

- You are an automated coding assistant contributing to https://github.com/schmave/demschooltools.
- This document defines the rules you must follow when manipulating this codebase. Treat every instruction below as mandatory guidance for how to design, code, test, and safeguard changes.

## Technology Directives

| Area     | Rule                                                                                                    | Implementation hints |
|----------|---------------------------------------------------------------------------------------------------------|----------------------|
| Backend  | You must implement all backend work with Django under `django/`. New apps live beside `custodia` and `dst`, are wired into `django/demschooltools/settings.py`, and exposed through `django/demschooltools/urls.py`. | Use `uv run manage.py startapp`, register the app, add URLs/migrations/tests in the same subtree. |
| Frontend | You must implement all frontend work with React under `react/` using the existing Vite build (see `react/vite.config.js`). Update the manifest entries and Django templates (`django/dst/templates/*.html`) to load new bundles via `django-vite`. | Run `npm run watch` during development and `vite build` only via CI/deploy scripts. |
| Legacy   | The historical Play/Scala + JVM stack under `app/`, `conf/`, `project/`, `target/`, `authLibrary/`, `modelsLibrary/`, `lib/`, `node_modules/`, and related webpack config in the repo root (`/package.json`) is frozen. Do **not** modify or expand it without explicit human approval. |

## Current Django Architecture

- **Project layout (`django/demschooltools/`)**: central settings (`settings.py`), URLs (`urls.py`), middleware (`middleware.py`), custom auth backend/middleware (`auth.py`), templates (`templates/`), and storage logic coordinate the Django runtime. Settings load secrets from `.env` via `dotenv`, target PostgreSQL (`school_crm`), enable `django_vite`, and configure DRF to use ORJSON renderers/parsers plus Rollbar exception handling.
- **Apps**:
  - `django/dst/` contains the modern school-management surface: models for organizations, people, roles, manual chapters, attendance, etc. (`models.py`), multiple views (`attendance_views.py`, `manual_views.py`), reusable HTTP helpers (`utils.py` with `DstHttpRequest` and `render_main_template`), PDF generators (`pdf_utils.py` using Playwright), organization-level configuration (`org_config.py`), templates (`templates/`), and management commands (`management/commands/*.py`) used for seeding, reporting, and manual copying.
  - `django/custodia/` bridges legacy attendance tooling: DRF-style API views (`views.py`) expose `/custodia-api/*`, while server-rendered templates plus React bundles (see `django/dst/templates/sign_in_sheet.html`) back the UI. Models live in `custodia/models.py`, with migrations tracking schema history.
- **Views & templates**:
  - Django class-based views rely on `LoginRequiredMixin` and `request.org` (set by `PlaySessionMiddleware`) to scope queries to the current school. Manual-management views under `dst/manual_views.py` re-use `render_main_template` to inject menus and `org_config`.
  - DRF `APIView`s in `custodia/views.py` enforce permissions via `RequireAdmin` and raise built-in exceptions so the Rollbar handler captures issues.
  - Templates live beside each app (`django/dst/templates/`, `django/custodia/templates/`) and expect React entry points to hydrate a `<div id="react-root">` while `window.initialData` prepopulates JSON payloads.
- **Utilities & services**: Keep cross-cutting helpers centralized (`dst/utils.py`, `dst/org_config.py`, `dst/pdf_utils.py`, `demschooltools/form_renderer.py`). Extend those modules rather than scattering ad-hoc helpers.
- **Serializers**: The current code manually shapes dicts in the views; when adding new APIs prefer creating explicit serializers (e.g., `django/dst/serializers.py`) so data contracts are testable and ORJSON-friendly.
- **Migrations/tests**: App-specific migrations live under `django/dst/migrations/` and `django/custodia/migrations/`. Even though `django/dst/tests.py` is minimal, all new backend logic must be backed by Django `TestCase`s in each app’s `tests.py` or a dedicated `tests/` package.

## Current React Architecture

- **Bundles & routing**:
  - The modern bundle is rooted at `react/index.jsx` and `react/App.jsx`. It uses `createBrowserRouter` to map Django routes (e.g., `/attendance/signInSheet`) to React pages. Templates such as `django/dst/templates/sign_in_sheet.html` mount this bundle and pass bootstrapped data through `window.initialData`.
  - `react/custodia/js/*` houses the legacy HashRouter-based React app loaded inside `/custodia/*` pages; extend it only when you are explicitly working on Custodia screens.
  - The Vite config (`react/vite.config.js`) defines three named entry points (`custodia`, `custodia_css`, `reactapp`) that emit hashed assets under `django/static-vite/`. Whenever you add a new bundle, add it to `rollupOptions.input`, regenerate the manifest, and update the corresponding Django template.
- **Components & state**:
  - Shared UI components wrap MUI primitives (`react/components/*.jsx`) and are re-exported through `react/components/index.js`. Use these wrappers instead of raw `@mui/material` imports so styling stays consistent.
  - Containers in `react/containers/` (e.g., `DeleteDialog`) encapsulate modal flows, while `react/contexts/` exposes React Contexts such as `SnackbarContext` used throughout `react/pages/SignInSheetPage/SignInSheetPage.jsx`.
  - Pages (currently `SignInSheetPage`) live under `react/pages/*`, own their state via hooks (`useState`, `useMemo`, `useCallback`, `useEffect`), and depend on utilities from `react/utils/` (safe parsing, option normalization, formatting) plus `@react-pdf/renderer` to generate PDFs.
- **Styling & theming**: Global SCSS imports live in `react/index.scss`. Theme definitions (`react/theme/theme.js`) create responsive light/dark palettes with MUI. When introducing new surfaces, wrap them with `ThemeProvider` and `PageWrapper` so typography and padding stay uniform.
- **Testing**: No automated frontend tests currently exist; you must introduce them alongside new features (prefer Vitest + React Testing Library) and wire the runner into `package.json` before relying on it. Until then, document manual verification steps.
- **Lint/format**: ESLint is configured via `react/eslint.config.js` (React + Hooks + import rules) and Prettier (with `@trivago/prettier-plugin-sort-imports`) enforces formatting. Run `npm run lint` from `react/` and fix all warnings before submission.

## Database Rules

- **Canonical schema**: The authoritative ORM models are `django/dst/models.py` (core DST tables mapped onto existing `public.*` tables via `db_table` overrides) and `django/custodia/models.py` for swipe/attendance supplements. These models intentionally mirror legacy Play-era schemas; do not “clean up” column names or relationships.
- **Migrations workflow**:
  - Generate changes with `uv run manage.py makemigrations <app>` and review the generated files under `django/<app>/migrations/`.
  - Apply them locally with `uv run manage.py migrate`. The helper scripts `django/test_initial_migrate.sh` and `django/run_django.sh` show the expected bootstrap flow (provision `school_crm`, apply migrations, run `setup_initial_data` via `uv`).
  - When comparing against production data, dump schemas with `pg_dump -O --schema-only school_crm > <file>` as demonstrated in `django/test_initial_migrate.sh`.
- **High-risk tables**: Many models map to shared legacy tables (`public"."users`, `public"."user_role`, `public"."linked_account`, `tag`, etc.). Custodia tables rely on uniqueness constraints (e.g., `custodia/migrations/0003_swipe_person_swipe_day_empty_out_unique.py`).
- **Schema DO / DO NOT**:

| DO | DO NOT |
|----|--------|
| Use Django ORM field definitions and migrations to evolve the schema. | Do **not** touch SQL files such as `fix_case_numbers.sql` or run ad-hoc DDL unless a human explicitly instructs you. |
| Coordinate cross-app migrations when tables span both `custodia` and `dst`. | Do **not** rename or drop columns/constraints on legacy tables (`public.users`, `user_role`, etc.); they are still consumed by the Play/Scala stack. |
| Respect partial indexes and unique constraints already defined in migrations (e.g., swipe uniqueness, overrides). | Do **not** perform schema migrations that drop or truncate data without written human approval. |
| Seed or adjust reference data through management commands (`django/dst/management/commands/*.py`) when possible. | Do **not** bypass Django migrations with raw SQL or external tools. |

## Auth, Permissions & API Expectations

- Authentication flows are defined in `django/demschooltools/auth.py`. `PlaySessionMiddleware` inspects the `PLAY_SESSION` JWT cookie (or falls back to allowed IP addresses via `AllowedIp`) to populate `request.user` and `request.org`. You must preserve this flow—never implement alternative login states.
- Every view must trust but verify `request.org`: filter ORM queries by `organization=request.org` to prevent cross-school data leaks. Example: `custodia/views.get_person` enforces this before mutating person data.
- Authorization hinges on `dst.models.UserRole` and `User.hasRole`. Use explicit permission checks (`LoginRequiredMixin`, `UserRole` assertions, or DRF `BasePermission` subclasses like `RequireAdmin` in `custodia/views.py`) before touching attendance/manual data.
- API endpoints live in Django apps:
  - HTML views render through `render_main_template` (see `dst/utils.py`) with the React mountpoint and `window.initialData` payloads.
  - JSON endpoints use DRF `APIView`s or `ViewSet`s, returning `Response` objects compatible with ORJSON. Register them under `/custodia-api/*` or other namespaces via `django/demschooltools/urls.py`.
- Example pattern for a new endpoint:
  1. Add a view to the relevant app (e.g., `django/dst/views.py`) that subclasses `APIView`, reads `request.org`, enforces `UserRole` granularity, and serializes data with a serializer or helper.
  2. Wire the URL in `django/demschooltools/urls.py`.
  3. Provide tests plus a front-end consumer (typically via `window.initialData` or a fetch call scoped to `/custodia-api/...`).
- Never expose raw Django models directly—shape payloads similar to `custodia/views.student_to_dict`.

## Behavior Contract for AI Agents

- ❌ Do **not** modify or extend legacy Play/Scala/Java code under `app/`, `project/`, `conf/`, `target/`, `authLibrary/`, `modelsLibrary/`, or legacy webpack configs without human direction.
- ❌ Do **not** introduce new Python dependencies, npm packages, or system-level tools without approval, except when a React/Vite change makes a new npm dependency unavoidable. In that case, update `react/package.json` and document the requirement.
- ❌ Do **not** rename database columns, change `db_table` mappings, or drop constraints without human review.
- ❌ Do **not** generate, commit, or log secrets (`APPLICATION_SECRET`, Rollbar tokens, etc.).
- ❌ Do **not** run destructive migrations or data backfills that can drop or rewrite production data without explicit sign-off.
- ✔️ Do follow the closest existing pattern in the area you are modifying (views mimic `dst/manual_views.py`, React screens follow `react/pages/SignInSheetPage` structure, etc.).
- ✔️ Do scope every backend query by `request.org` and enforce `UserRole`s before exposing data.
- ✔️ Do ask for clarification (or fail fast) when requirements are ambiguous or would violate any rule above.

## Testing & Linting Enforcement

- **Backend**:
  - Run `uv run manage.py test` from `django/` to execute Django’s test runner. Tests should live in each app (`django/dst/tests.py`, etc.) and cover new models, views, serializers, and management commands.
  - Run `uv run ruff check django` (per `django/pyproject.toml`) before submission. Fix import order/lint errors rather than silencing them. For HTML templates, format with `django/format-djhtml.sh` when touched.
- **Frontend**:
  - Run `npm run lint` inside `react/`. The ESLint config enforces React, Hooks, and import rules; no warnings may remain.
  - Format JS/JSX/SCSS via Prettier (`npx prettier --check .` / `--write`) leveraging the configured import-sort plugin.
  - When you add a frontend test suite, document and run it (e.g., `npm run test`) before committing.
- **Build checks**:
  - Use `npm run watch` (Vite dev server on port 8082 per `DJANGO_VITE`) during development; reserve `npm run build` for release workflows so hashed assets match `django-vite`.
  - For backend processes, use the Procfile in `django/Procfile` (`uv run manage.py migrate && uv run manage.py runserver`) to mirror the expected startup order.

Compliance with these steps is not optional; deliverables without passing lint/tests will be rejected.

## Security & Privacy Expectations

- Secrets and credentials live in environment variables loaded via `dotenv` in `django/demschooltools/settings.py`. Never hard-code sensitive values or commit `.env` files.
- User data is sensitive (see `dst/models.py` for PII fields like addresses, DOB, attendance, swipes). Do not log PII, expose it to unauthorized roles, or ship debugging endpoints that bypass auth.
- Custodia swipe data (`custodia/models.py`) and attendance overrides are regulatory records. Respect uniqueness constraints and never fabricate entries for testing in production contexts.
- Rollbar is wired through middleware; let exceptions propagate so Rollbar can capture them, and do not intercept errors without re-raising.
- Playwright-generated PDFs (`dst/pdf_utils.py`) render Django HTML; sanitize inputs and avoid embedding secrets or unescaped content.
- Treat Allowlisted IP logic (`AllowedIp`, `get_ip_user` in `demschooltools/auth.py`) as a convenience, not a security boundary—still enforce roles and CSRF protections.

## Operational Checklist

- **When creating backend code** → scaffold a Django app under `django/`, define models/migrations/tests, implement class-based or DRF views that call `render_main_template` or return `Response`, wire URLs in `django/demschooltools/urls.py`, and gate everything with `request.org` + `UserRole`.
- **When creating frontend code** → add pages/components under `react/pages`, hook them into `react/App.jsx` or `react/custodia/js/app.jsx`, leverage shared components/context/hooks, and expose data through `window.initialData` from the matching Django template plus `django_vite` asset tags.
- **When modifying the database** → touch only Django ORM models, generate migrations via `uv run manage.py makemigrations`, apply with `uv run manage.py migrate`, and never issue raw SQL unless a human signs off.
- **Before committing** → run `uv run manage.py test`, `uv run ruff check django`, and `npm run lint` (plus any frontend tests you add), ensure Vite builds cleanly if assets changed, and document manual verification steps (e.g., PDF generation, attendance sign-in flows).

Failure to follow this AGENTS.md invalidates your contribution.
