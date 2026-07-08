# Frontend Rules

## Phase 1 - Foundation Rules

1. All imports inside `src` should use the `@/` alias instead of deep relative paths.
2. `src/main.tsx` stays as the Vite entry only and delegates bootstrapping to `src/app/main.tsx`.
3. `src/app/main.tsx` is responsible for composing `App`, `AppProviders`, and one global stylesheet entry.
4. Global styles must flow through `src/app/styles/globals.css`; feature files should not import vendor or app-wide CSS directly.
5. Runtime configuration must be declared in `.env.example` and read from `src/config/env.ts`.
6. Build tooling such as Vite, TypeScript, and React plugin packages belong in `devDependencies`, while runtime UI packages stay in `dependencies`.

## Phase 2 - Structure Rules

1. `src/app/App.tsx` should delegate navigation concerns to `src/app/router` instead of owning routing logic itself.
2. Application navigation uses `createBrowserRouter` with nested layout shells for `admin`, `client`, and `auth`.
3. Internal navigation must use `Link`, `NavLink`, `Navigate`, or router actions from `react-router-dom`; do not add `#/...` links or raw `window.location` routing.
4. Route metadata stays in `src/app/routes.tsx`, while browser-router assembly and layout orchestration live in `src/app/router`.
5. API path strings must be registered in `src/core/api/apiEndpoints.ts` before features start consuming them.
6. Theme primitives must be defined in `src/core/theme/tokens.ts`, and `src/core/theme/theme.ts` should only compose higher-level theme objects from those tokens.
7. Brand colors are standardized around `#2563EB` for primary actions and `#1D4ED8` for hover/strong emphasis.
8. Browser-router deployment requires SPA rewrite support so unknown paths resolve back to `index.html`.
9. Corner radius for UI surfaces must stay within `6px` to `10px`; only fully circular elements such as avatars, dots, or pills may use `9999px`.

## Phase 3 - Sidebar Rules

1. Admin sidebar state must derive the active route from `useLocation()` instead of receiving `currentPath` props manually.
2. Admin navigation data must use the structured `link | group | divider` format.
3. Divider rows belong in sidebar navigation data, not in component index-based conditions.
4. Group expansion should auto-follow the current route and still allow manual toggle by the user.
5. Sidebar icons must be rendered through a centralized icon map or icon component, not raw class strings embedded throughout the JSX tree.
6. Child menu items must declare explicit route match prefixes so list pages and nested form/detail routes stay active together.
7. Sidebar labels and menu metadata must remain UTF-8 clean Vietnamese text.

## Phase 4 - Sidebar Shell Rules

1. Admin sidebar visual layout must use project-owned `vm-*` classes, not depend on AdminLTE treeview classes such as `nav-treeview` or `menu-open`.
2. Sidebar width and content offset must be explicitly controlled by the shell so the fixed header and fixed sidebar stay aligned together.
3. Long sidebar navigation must scroll inside the sidebar column, not push the page shell height unpredictably.
4. Group parents and leaf items should have distinct active treatments: parent groups use a soft highlighted state, while active leaf routes use a stronger selected state.
5. Sidebar items must preserve icon alignment, text truncation, and consistent `6px-10px` corner radius across desktop breakpoints.
6. At tablet and mobile widths where no drawer behavior exists, the fixed sidebar should be hidden and admin content should reclaim the full width.

## Phase 5 - Card Management Rules

1. The admin card page redesign applies only to the `main content` canvas of `/admin/card`; it must not redefine the shared admin header or sidebar shell.
2. `Quản lý thẻ` should be composed from five clear sections: page header actions, KPI summary, status tabs, filter toolbar, and a table-plus-detail workspace.
3. Each table row represents one physical card from card inventory first; subscription state and lost-card state are related dimensions shown in dedicated columns.
4. Search on the card page must match at least `card number`, `UID`, `license plate`, and `customer name`.
5. Subscription badges must stay independent from inventory badges so a card can be `Đã gán` while the subscription is `Hết hạn` or `Chờ duyệt`.
6. Lost-card badges must stay independent from inventory badges so a card can be `Mất thẻ` while the report itself is still `Đang mở` or already `Đã xử lý`.
7. Selecting a row in the card list must update a contextual detail panel on the same page instead of forcing a navigation jump.
8. Card page surfaces, filters, tabs, and table controls must keep the project radius rule within `6px-10px`, except fully circular pills or dots.
9. Primary content section titles in admin feature pages, such as `Quản lý thẻ`, should use a standardized font size of `25px`.
