# Code analysis review

Reviewed the 209 findings in the 34 XML reports supplied on October 26, 2026. The reports are snapshots; they are preserved as supplied.

## Corrections

- Give `MotionChunkEntity` content-based byte-array equality and matching hashes, with a regression test for separately allocated payloads and hash collections.
- Use the detector version constant when creating detected events, and remove the unused storage error enum value.
- Remove unused imports and redundant null assertions, conditions, and qualifiers; simplify collection and null/empty checks.
- Use Kotlin math functions, duration overloads, immutable array bindings, and trailing lambdas.
- Put Compose modifier parameters first among optional parameters, update their callers, and use primitive float state for drag offsets.
- Remove unreachable analog dial color branches, clarify trip duration precedence, and simplify movement classification without changing its thresholds or hold periods.
- Remove the four unused Android cat drawable copies. Retain the original SVG artwork and the canvas-based Cat speedometer.
- Remove unused SVG namespaces and external DTD declarations; quote Mermaid labels and align the Markdown table.
- Correct actual spelling and grammar mistakes, including the local, ignored `ai-docs/setup.md` reference.
- Update the dependencies identified by the reports: Android Gradle plugin 9.4.1, Gradle 9.8.2, Kotlin Compose plugin 2.4.21, Core 1.19.1, Compose BOM 2026.09.00, Room 2.8.5, Navigation 2.10.2, and Robolectric 4.17.

Release references: [Android Gradle plugin compatibility](https://developer.android.com/build/releases/agp-9-4-0-release-notes), [Gradle 9.8.2](https://docs.gradle.org/9.8.2/release-notes.html), [Kotlin releases](https://kotlinlang.org/docs/releases.html), [Core](https://developer.android.com/jetpack/androidx/releases/core), [Room](https://developer.android.com/jetpack/androidx/releases/room), [Navigation](https://developer.android.com/jetpack/androidx/releases/navigation), and [Robolectric 4.17](https://github.com/robolectric/robolectric/releases/tag/robolectric-4.17).

## Findings retained intentionally

- `app/src/test/resources/robolectric.properties`: Robolectric reads `sdk=34` at runtime. The unused property inspection does not recognize this consumer; deleting it would change the tests' default Android version.
- GPL license wording: grammar, verbosity, and spelling warnings in the supplied license are false positives. Both license copies remain unchanged.
- Correct project names, personal names, Android acronyms, SVG/CSS terms, architecture names, and generated Gradle script terms: spellchecker suggestions do not justify renaming identifiers, preferences, or attribution. No broad inspection suppressions were added.

## Validation

- Sensor test fixtures now use `SensorBuilder`, replacing the helper deprecated by Robolectric 4.17.
- Full unit suite: 112 tests, 0 failures, 0 errors, 0 skipped.
- Sensor tests rerun after that fixture update: 2 passed.
- Android lint: no issues found.
- Debug APK: assembled successfully.
- All four cleaned SVG assets parse and have identical artwork to the original XML trees.
- IDE-only spelling and grammar inspections were reviewed from the supplied snapshots; Android lint does not rerun those inspections.

Gradle also reports a `Configuration.setVisible` deprecation during plugin configuration. There are no calls to this API in the project sources; it does not fail the build.
