## Summary

<!-- Briefly describe what this PR changes and why. -->

## Type of Change

- [ ] `feat` — New feature
- [ ] `fix` — Bug fix
- [ ] `refactor` — Code refactoring (no behavior change)
- [ ] `test` — Adding or updating tests
- [ ] `docs` — Documentation update
- [ ] `ci` / `build` / `chore` — Tooling, CI, or dependency maintenance

## Related Issue

<!-- Link related issue(s), e.g., Closes #123 -->

## How Was This Tested?

- [ ] `./gradlew testDebugUnitTest`
- [ ] `./gradlew assembleDebug`
- [ ] Manual verification on emulator / physical device

<!-- Describe any specific test cases or device scenarios verified (e.g. WhatsApp / WhatsApp Business). -->

## Screenshots / Recordings (if UI changed)

<!-- Attach before/after screenshots for Compose UI changes. -->

## Checklist

- [ ] Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `test:`, `refactor:`, etc.).
- [ ] PR is focused and reasonably sized.
- [ ] No unnecessary Android permissions were added (`READ_CONTACTS`, `QUERY_ALL_PACKAGES`, `INTERNET`, etc.).
- [ ] WhatsApp integration uses official `https://wa.me/...` Click to Chat URIs via `WhatsAppLauncher` / `LaunchWhatsAppUseCase` (no hardcoded internal activities, no auto-sending).
- [ ] Phone numbers are not logged in plaintext.
- [ ] New/updated logic includes unit tests.
