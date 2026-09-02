## Linked issue

<!-- Replace N with the approved issue number. Keep the closing keyword. -->
Closes #N

## PR type

<!-- Check exactly one option and add the matching type:* label to the PR. -->
- [ ] Bug fix (`type:bug`)
- [ ] New feature (`type:feature`)
- [ ] Documentation only (`type:docs`)
- [ ] Code refactoring (`type:refactor`)
- [ ] Maintenance or tooling (`type:chore`)
- [ ] Breaking change (`type:breaking-change`)

## Summary

<!-- Explain the outcome and why it is needed in 1-3 bullets. -->
-

## Changes

| File or area | Change |
|---|---|
| `path/to/file` | Describe the change. |

## Test plan

<!-- Check the relevant verification. Explain any unchecked item below. -->
- [ ] JVM unit tests pass: `./gradlew testDebugUnitTest`
- [ ] Android lint passes: `./gradlew lintDebug`
- [ ] Debug build succeeds: `./gradlew assembleDebug`
- [ ] Instrumented/Compose tests pass when affected: `./gradlew connectedDebugAndroidTest`
- [ ] Affected behavior was verified on an Android device or emulator when applicable.

Unchecked test explanation:

## Contributor checklist

- [ ] The PR links an issue approved with `status:approved`.
- [ ] Exactly one `type:*` label is applied to the PR.
- [ ] User-visible behavior and documentation are consistent.
- [ ] Commits follow the Conventional Commits format.
- [ ] Commits and PR content contain no AI attribution or `Co-Authored-By` trailers.
- [ ] The PR contains no secrets, credentials, or unrelated changes.
