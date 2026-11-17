# Dev Log — Project Progress & Learning Notes

**Project:** `dev-log`  
**Language:** Scala 3  
**Last updated:** 17 November 2026  
**Purpose:** Build a useful development log while learning Scala design and experimenting with human–AI pair programming.

## 1. Our development approach

This project is both an application and a learning experiment. I write and run the code; AI helps me reason about design, review decisions and explore alternatives. Initially, AI does not need terminal access.

For each small feature, follow this cycle:

1. Define the problem and its requirements.
2. Think about the design before coding.
3. Implement it myself.
4. Test and review it, comparing alternatives.
5. Commit a meaningful milestone and record what I learned.

We use four recurring design questions:

| Foundation | Question |
| --- | --- |
| **Case class vs class** | How should we model data? |
| **Object vs class** | Where should behaviour live? |
| **Trait vs inheritance/composition** | How do we design for change? |
| **Collections** | How should we organise data? |

We will return to these questions as the application develops rather than treating them as isolated lessons.

## 2. Project setup — completed

- Created the GitHub repository `dev-log` and opened the local directory with IntelliJ IDEA.
- Learned that `../.idea` is IntelliJ's project configuration directory, not application source code.
- Set up a simple **sbt + Scala 3** console application. Our original setup used `scalaVersion := "3.9.0"` in `../build.sbt` and `sbt.version=2.0.11` in `project/build.properties` (these were the versions used in the setup exercise; check the files for their current values).
- Created `../src/main/scala/Main.scala` and successfully ran `sbt run`, which printed `Dev Log started`.
- Learned that sbt / IntelliJ may create generated directories such as `../project/project`, `target/` and `.bsp/`. They are not where application code goes.
- Added `../.gitignore` to exclude local IDE settings and generated build outputs.
- Committed and pushed the initial setup:

```text
chore: initialise Scala dev log project
```

Key files:

```text
dev-log/
├── .gitignore
├── build.sbt
├── project/
│   └── build.properties
└── src/
    └── main/
        └── scala/
            ├── Main.scala
            └── DevLogEntry.scala
```

The `../src/test/scala` directory can be added or used when we begin unit testing. Empty directories do not appear in Git commits.

## 3. First data model — completed

Created `DevLogEntry.scala`:

```scala
import java.time.LocalDate

case class DevLogEntry(
    title: String,
    description: String,
    date: LocalDate,
    tags: Set[String]
)
```

This describes a development-log entry using four fields:

- `title` and `description`: `String`
- `date`: `LocalDate`, rather than an arbitrary date string
- `tags`: immutable `Set[String]`, which prevents duplicate values

We created entries in `Main.scala`, printed them, and used Git to push the initial model. The commit was:

```text
feat: add initial dev log entry model
```

We also tested working with the repository across two Macs.

### Why a case class?

A case class is a useful default for this value-oriented model because it provides:

1. Construction without `new`.
2. Equality based on constructor field values.
3. Immutable constructor fields (`val`) by default.
4. `copy()` to create modified instances.
5. Readable `toString` output.

A regular `class` can be appropriate for other kinds of objects, but these features are not all supplied automatically in the same way.

**Important future distinction:** Two entries with identical fields compare equal as case-class values, but they might eventually be two distinct saved records. We will revisit **value equality vs record identity** when introducing IDs and persistence.

## 4. Immutability and `copy()` — completed experiment

We experimented with:

```scala
val updatedEntry = entry.copy(
  tags = entry.tags + "GitHub"
)
```

Correct predictions:

- `entry.tags` stays unchanged.
- `updatedEntry.tags` includes the extra tag.
- `entry == updatedEntry` is `false` when their tags differ.

**Lesson:** Immutability does not mean the application cannot represent changes. Instead of changing an existing value, we create a new value with the updated state.

## 5. Moving behaviour into the model — implemented

I implemented `addTag` inside `DevLogEntry`:

```scala
import java.time.LocalDate

case class DevLogEntry(
    title: String,
    description: String,
    date: LocalDate,
    tags: Set[String]
):
  def addTag(newTag: String): DevLogEntry =
    this.copy(tags = this.tags + newTag)
```

The explicit `: DevLogEntry` return type is a clarity improvement suggested during review; my original implementation relied on Scala's return-type inference.

Usage:

```scala
val withGitHub = entry.addTag("GitHub")
val stillOneGitHub = withGitHub.addTag("GitHub")
```

Because `tags` is a `Set`, adding the same string twice does not create a duplicate. The original `entry` is unchanged.

### Case-class method vs service object

We considered two designs:

```scala
// Approach A: behaviour on an instance
entry.addTag("GitHub")

// Approach B: behaviour in a separate singleton object
DevLogService.addTag(entry, "GitHub")
```

An example implementation of Approach B is:

```scala
object DevLogService {
  def addTag(entry: DevLogEntry, newTag: String): DevLogEntry =
    entry.addTag(newTag)
}
```

**Decision for now:** Keep simple behaviour that only transforms the entry's own data in `DevLogEntry`. A service becomes more useful when an operation coordinates external dependencies or other responsibilities, for example saving an entry to a database. Just needing a `DevLogEntry` argument does *not*, by itself, prove that the method must be on the class.

**Status clarification:** We discussed `DevLogService` as an alternative and showed how to write it. We have not established that it was added to the repository, and we do not need it yet.

## 6. What to learn now vs later

### Must know

- What a `case class` provides automatically.
- How value equality (`==`) works for case classes.
- Why `copy()` plus immutable collections leaves the original unchanged.
- The difference between an **instance method** (`entry.addTag`) and a method on an **object** (`DevLogService.addTag`).
- What `this` means inside an instance method.

### Good to know

- Why `Set` is useful when duplicate tags are not wanted.
- Why explicit return types improve method readability.
- How methods can delegate to one another without duplicating logic.
- Why behaviour working solely on an entry's data may belong on the entry itself.

### Revisit later

- Value equality vs persistent record identity and unique IDs.
- Companion objects and factory methods.
- Services, repositories and dependency injection.
- Traits, composition and abstraction boundaries.
- Automated testing and validation rules (for example, empty tags).

## 7. Next exercise — not yet completed

Implement a `removeTag` instance method on `DevLogEntry`:

```scala
def removeTag(tag: String): DevLogEntry = ???
```

Requirements:

- Return a new `DevLogEntry` with the tag removed.
- Keep the original entry unchanged.
- Decide what should happen when the tag is not present.

After reviewing that implementation, investigate **companion objects versus instance methods**. Then we can continue building toward storing and retrieving multiple entries.

No new commit is required for each tiny experiment; commit after a coherent, reviewed milestone.

## 8. Official Scala documentation

- [Scala 3 Book — Case Classes](https://docs.scala-lang.org/scala3/book/domain-modeling-tools.html#case-classes)
- [Scala 3 Book — Methods](https://docs.scala-lang.org/scala3/book/taste-methods.html)
- [Scala 3 Book — Singleton Objects](https://docs.scala-lang.org/scala3/book/taste-objects.html)
- [Scala 3 Book — Collections](https://docs.scala-lang.org/scala3/book/collections-classes.html)

**How to use these docs:** Start from the part relevant to the feature we're building. Read enough to answer a concrete question; come back later for advanced details.

---

**Current position:** Scala project set up and pushed; initial model committed; `addTag` implemented and reviewed in chat; `removeTag` is the next exercise.
