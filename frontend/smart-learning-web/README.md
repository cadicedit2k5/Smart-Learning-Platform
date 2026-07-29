# Smart Learning Web

Frontend for the Smart Learning Platform, built with Vue 3, TypeScript, Vite,
Pinia, Vue Router, Tailwind CSS, Axios and Vitest.

## Requirements

- Node.js `22.18+` or `24.12+`
- npm (the lockfile is committed, so use `npm ci` in CI)

## Getting started

```sh
cp .env.example .env.local
npm install
npm run dev
```

On Windows PowerShell, copy the environment file with:

```powershell
Copy-Item .env.example .env.local
```

Only variables prefixed with `VITE_` are exposed to browser code. Do not store
secrets in frontend environment files.

## Scripts

```sh
npm run dev             # Start the development server
npm run build           # Type-check and create a production build
npm run lint            # Check code without modifying files
npm run lint:fix        # Fix supported lint issues
npm run format          # Format the project
npm run format:check    # Check formatting
npm run test:unit       # Run unit tests once
npm run test:unit:watch # Run unit tests in watch mode
```

## Project structure

```text
src/
├── assets/          # Global styles and static assets imported by code
├── layouts/         # Route-level page shells
├── pages/           # Route entry components
├── router/          # Route definitions and navigation concerns
├── shared/          # Reusable, domain-agnostic infrastructure
│   └── api/         # HTTP client and API primitives
├── App.vue          # Application root
└── main.ts          # Plugin registration and app mount
```

Create these directories only when the related code is introduced:

```text
src/
├── components/      # Truly reusable UI components
├── composables/     # Reusable Vue composition logic
├── features/        # Business modules grouped by feature
├── stores/          # Cross-feature Pinia stores
├── types/           # Shared TypeScript types
└── utils/           # Small pure utilities
```

A feature should keep its own components, API calls, store and types together
instead of spreading them across global folders:

```text
features/course/
├── api/
├── components/
├── stores/
├── types/
└── index.ts
```

## Conventions

- Use `PascalCase.vue` with multi-word component names.
- Pages end in `Page`, layouts end in `Layout`.
- Prefer the `@/` alias for imports across directories.
- Lazy-load route components.
- Keep API transport in `shared/api`; put domain endpoints inside their feature.
- Export a feature's public API from its `index.ts`.
- Do not add a global abstraction until at least two real use cases need it.
- Tests live beside the code in `__tests__` directories.

## Environment variables

| Variable            | Purpose              | Example                     |
| ------------------- | -------------------- | --------------------------- |
| `VITE_API_BASE_URL` | Backend API base URL | `http://localhost:8080/api` |

Add new variables to `.env.example` and type them in `env.d.ts`.
