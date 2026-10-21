# User Guide frontend foundation

This module provides presentation-only building blocks. It does not fetch guides,
resolve audiences, persist progress, or change the current application UI.

## Application integration

Import `src/app/styles/user-guide.css` from the global stylesheet, then mount one
provider and host near the application root:

```tsx
<UserGuideProvider onLifecycleEvent={handleLifecycleEvent}>
  {children}
  <UserGuideHost />
</UserGuideProvider>
```

Start a resolved guide from a page or a future orchestration hook:

```tsx
const { startGuide } = useUserGuide();
startGuide(guidePresentation);
```

Mark a target element without coupling the renderer to a CSS selector:

```tsx
<button {...userGuideAnchorProps("parking.create-button")}>Create</button>
```

The `anchorKey` returned by the backend must match this stable key. When an
anchor-based display type cannot find its target, the host falls back to a modal.

## Deferred integration

- Fetch eligible guides by `contextKey` after route and account access are ready.
- Convert media object keys to short-lived URLs before rendering.
- Send opened, step changed, dismissed, and completed events to the backend.
- Apply replay policy and resume `currentStepId` using the server state.
- Add focus trapping and end-to-end accessibility tests before production rollout.
