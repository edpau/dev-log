# Dev Log — Project Progress & Learning Notes

**Project:** `dev-log`  
**Language:** Scala 3  
**Last updated:** 18 November 2026  
**Purpose:** Build a useful development log while learning Scala design and experimenting with human–AI pair programming.

## 1. Our development approach

This project is both an application and a learning experiment. I write and run the code; AI helps me reason about design, review decisions and explore alternatives. Initially, AI does not need terminal access.

For each small feature, follow this cycle:

1. Define the problem and its requirements.
2. Think about the design before coding.
3. Implement it myself.
4. Test and review it, comparing alternatives.
5. Commit a meaningful milestone and record what I learned.

For **every exercise**, keep the relevant documentation links next to the exercise (not only in a reference list at the end). Include a brief **“What to look for”** note so I can return to the source and find the concept independently. Prefer the official Scala 3 Book and link to a specific section whenever possible; use official tool documentation for setup exercises. Only record an implementation as complete after I have written and checked it. Distinguish a code-review pass from a build or test run that has been verified locally.

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
            ├── DevLogEntry.scala
            └── DevLogService.scala
```

The `../src/test/scala` directory can be added or used when we begin unit testing. Empty directories do not appear in Git commits.

### Related documentation

- [The Book of sbt — Getting Started](https://www.scala-sbt.org/2.x/docs/en/guide/index.html) — sbt project structure, build definitions and commands.
- [Scala 3 Book — Main Methods](https://docs.scala-lang.org/scala3/book/methods-main-methods.html) — how a Scala program starts.

**What to look for:** How `build.sbt`, `project/build.properties` and the application's entry point have different jobs.

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

### Related documentation

- [Scala 3 Book — Case Classes](https://docs.scala-lang.org/scala3/book/domain-modeling-tools.html#case-classes) — generated methods, equality, `copy()` and constructor fields.
- [Scala 3 Book — Working with Sets](https://docs.scala-lang.org/scala3/book/collections-classes.html#working-with-sets) — why `Set` stores unique elements.

**What to look for:** Identify which case-class features Scala generates for us, and why `tags` is a `Set[String]` rather than a `List[String]`.

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

**`val` clarification (17 November 2026):** `val entry` prevents assigning a different value to the variable `entry`, but `val` alone does not make a referenced object deeply immutable. Here, case-class parameters are `val` by default, `tags` is an immutable `Set[String]`, and `copy()` creates a new entry rather than changing the existing one.

### Related documentation

- [Scala 3 Book — Immutable Values](https://docs.scala-lang.org/scala3/book/fp-immutable-values.html) — case classes and the “update as you copy” pattern.
- [Scala 3 Book — Adding elements to a Set](https://docs.scala-lang.org/scala3/book/collections-classes.html#adding-elements-to-a-set) — returning a new immutable collection with `+`.

**What to look for:** Which value is unchanged after `copy()`, and which operation produces a new `Set`?

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

**Status clarification:** `DevLogService.scala` is present in the uploaded project ZIP, and it delegates to `entry.addTag(newTag)`. We can keep it as an example of a separate singleton object, but it is not required for this simple operation.

### Related documentation

- [Scala 3 Book — Methods](https://docs.scala-lang.org/scala3/book/taste-methods.html) — methods inside case classes and other types.
- [Scala 3 Book — Fields and methods](https://docs.scala-lang.org/scala3/book/domain-modeling-tools.html#fields-and-methods) — instance behaviour in a class body.
- [Scala 3 Book — Singleton Objects](https://docs.scala-lang.org/scala3/book/taste-objects.html) — the alternative of placing methods in an `object`.

**What to look for:** Where a method can be declared and how an instance method differs from a method in a singleton object. Keeping `addTag` on `DevLogEntry` is **our design decision**, not a rule imposed by these docs.

## 6. What to learn now vs later

### Must know

- What a `case class` provides automatically.
- How value equality (`==`) works for case classes.
- Why `copy()` plus immutable collections leaves the original unchanged.
- How `Set` operators `+` (add) and `-` (remove) produce updated immutable collections.
- The distinction between `val` and an immutable object or collection.
- How simple `assert` checks verify expected behaviour and fail with `AssertionError` when a condition is false.
- The difference between an **instance method** (`entry.addTag`) and a method on an **object** (`DevLogService.addTag`).
- What `this` means inside an instance method.
- The distinction between an instance method, singleton object, and companion object.
- Why validation only inside a factory can be bypassed by another public construction path.

### Good to know

- Why `Set` is useful when duplicate tags are not wanted.
- Why explicit return types improve method readability.
- How methods can delegate to one another without duplicating logic.
- Why behaviour working solely on an entry's data may belong on the entry itself.
- Why a companion factory can supply default values and why constructor defaults may sometimes be simpler.
- How Scala 3 infers a return type, and why an explicit return type on a public method can help readers.

### Revisit later

- Value equality vs persistent record identity and unique IDs.
- Factories that enforce construction rules, and alternative ways of designing creation APIs.
- Services, repositories and dependency injection.
- Traits, composition and abstraction boundaries.
- A dedicated test suite (for example, using a Scala testing framework) and validation rules (for example, empty titles).

## 7. Exercise 2 — immutable `removeTag` and `assert` checks (completed and reviewed)

**Date reviewed:** 17 November 2026  
**Code review:** Passed; the following was supplied in chat after the ZIP upload.  
**Verification note:** The assertions are written and reviewed, but a successful local `sbt run` has not been confirmed in chat.  
**Commit:** Ready to make; not yet confirmed as pushed.

### Problem and solution

Remove a tag while preserving the original entry. A nonexistent tag should be harmless; it should leave the set's elements unchanged.

```scala
def removeTag(tag: String): DevLogEntry =
  this.copy(tags = this.tags - tag)
```

- `tags - tag` creates a Set without the specified element; it does not modify the original immutable Set.
- `copy(tags = ...)` creates a new `DevLogEntry` and preserves `title`, `description` and `date`.
- Removing a missing tag returns a Set with the same elements; no `if`, `Option` or exception handling is needed for this simple case.
- Removing the last remaining tag produces `Set.empty[String]`.

`addTag` remains:

```scala
def addTag(newTag: String): DevLogEntry =
  this.copy(tags = this.tags + newTag)
```

### Checking behaviour with `assert` in `Main.scala`

The following is the test example I wrote. It checks the expected results automatically rather than relying only on printed output:

```scala
import java.time.LocalDate

object Main {
  def main(args: Array[String]): Unit = {
    val entry = DevLogEntry(
      title = "My First Dev Log",
      description = "Testing development across two Macs",
      date = LocalDate.now(),
      tags = Set("Scala", "Git")
    )

    val updatedEntry = entry.addTag("GitHub")
    val updated = entry.removeTag("Git")
    val empty = updated.removeTag("Scala")
    val unchanged = entry.removeTag("new")

    // Original entry
    assert(entry.tags == Set("Scala", "Git"))

    // Add a new tag
    assert(updatedEntry.tags == Set("Scala", "Git", "GitHub"))

    // Remove an existing tag
    assert(updated.tags == Set("Scala"))

    // Remove the last tag
    assert(empty.tags == Set.empty[String])

    // Removing a missing tag preserves the value
    assert(unchanged == entry)

    // Original entry is still unchanged
    assert(entry.tags == Set("Scala", "Git"))

    println("All assertions passed!")
  }
}
```

### What is `assert`?

`assert(condition)` verifies that a Boolean expression is `true`:

```scala
assert(updated.tags == Set("Scala"))
```

- If the condition is `true`, the program continues.
- If it is `false`, `assert` throws an `AssertionError`; the success message at the end won't be reached.
- These are useful **manual application checks**, not yet a dedicated automated test suite.
- The two checks of `entry.tags` are intentionally redundant for learning: they reinforce that operations on copies have not changed the original.

**Local verification to do before committing:** run `sbt run` and confirm the message `All assertions passed!` appears without errors. The chat review alone does not prove the code was executed.

### Explanation in my own words

> `entry.removeTag("Git")` returns a new copy with one fewer tag. The original `entry` is not modified. `val` prevents reassignment, while the immutable Set and the `copy()` operation are what give this example its immutable update behaviour.

### Documentation

- [Scala 3 Book — Deleting elements from a Set](https://docs.scala-lang.org/scala3/book/collections-classes.html#deleting-elements-from-a-set) — the `-` operation and Set behaviour.
- [Scala 3 Book — Immutable Values](https://docs.scala-lang.org/scala3/book/fp-immutable-values.html) — creating updated copies without modifying the original.
- [Scala 3 Book — Building and Testing with sbt](https://docs.scala-lang.org/scala3/book/tools-sbt.html) — examples of assertions in a test workflow and the next step beyond checks in `Main`.

**What to look for:** Why `-` does not mutate an immutable Set; why `copy()` returns a new case-class instance; what `assert` does if a condition is false.

### Commit milestone

Planned commit message:

```text
feat: add immutable tag removal to DevLogEntry
```

Include the implementation, `Main.scala` checks and this progress note in the commit. Do not mark the commit as pushed until Git confirms it.

## 8. Exercise 3 — companion-object factory and assertions (implemented and reviewed)

**Date reviewed:** 18 November 2026  
**Code review:** Passed for the submitted implementation and updated assertions.  
**Local verification:** Not confirmed in chat; run `sbt run` and check for `All assertions passed!`.  
**Commit/push:** Proposed below; not yet confirmed.

### Problem

Create a new Dev Log entry with an empty tag set without making every caller supply `Set.empty[String]`. This lets us compare methods on an existing instance with a method on its companion object.

### Implementation — `DevLogEntry.scala`

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

  def removeTag(tag: String): DevLogEntry =
    this.copy(tags = this.tags - tag)

object DevLogEntry:
  def create(title: String, description: String, date: LocalDate): DevLogEntry =
    DevLogEntry(title, description, date, Set.empty[String])
```

The implementation has an explicit `: DevLogEntry` result type. `create` is on the **companion object** (`object DevLogEntry`), while `addTag` and `removeTag` remain **instance methods**. The factory adds a convenient creation path; it does not disable the case class's generated `apply` constructor.

### Checks — `Main.scala`

My latest exercise solution uses:

```scala
import java.time.LocalDate

object Main {
  def main(args: Array[String]): Unit = {
    val entry = DevLogEntry.create(
      "Learning Scala",
      "Understanding companion objects",
      LocalDate.now()
    )

    val updated = entry.addTag("Scala")

    assert(entry.title == "Learning Scala")
    assert(entry.description == "Understanding companion objects")
    assert(entry.tags.isEmpty)

    assert(updated.tags == Set("Scala"))
    assert(entry.tags.isEmpty)

    println("All assertions passed!")
  }
}
```

These checks cover the title, description, initial empty tags, successful addition of a tag, and the original entry remaining unchanged. **They do not yet explicitly assert the date**; if that becomes important, capture `LocalDate.now()` in a variable, pass it to `create`, and assert `entry.date == thatDate`. No local execution result has been reported.

### Design discussion — the trade-off I noticed

> If we know a new Dev Log should start with no tags, the factory saves us from repeatedly passing an empty Set. But our `create` method cannot accept initial tags, so it is less flexible for that use case.

That is correct **for the current `create` signature**. Callers may still use the generated case-class constructor to supply tags directly. An alternative would be to give `tags` a default value in the case-class constructor:

```scala
case class DevLogEntry(
    title: String,
    description: String,
    date: LocalDate,
    tags: Set[String] = Set.empty[String]
)
```

Default arguments can make the simple creation case more flexible, without a separate custom `create` method. **This was discussed as an alternative; it has not been implemented in the submitted solution.**

### Design discussion — where should validation live?

My answer to the follow-up design question was:

> Put a rule such as "the title must not be empty" in the case class, because a caller can bypass validation in `create` by using the generated case-class `apply` method directly.

This reasoning is sound. With a public case-class constructor, a check only in the factory does not protect every construction path. A possible **future exercise** is a class-body invariant using `require`, such as `require(title.trim.nonEmpty, "Title must not be empty")`, which would run when instances are constructed through the public constructor, generated `apply`, or `copy`. **No validation rule has been added yet.** We should also discuss when throwing an exception is appropriate versus returning a validation result.

### Must know / Good to know / Revisit later

- **Must know:** An instance method works on one entry; a companion object is a singleton with the same name as its class, declared alongside it; the case-class constructor remains available even after adding `create`.
- **Good to know:** A companion factory can apply standard initial values; constructor default parameters may be simpler; public methods can state their return type explicitly.
- **Revisit later:** Invariants with `require`, validation errors as `Either`, controlled constructors, proper automated unit tests, and record identity.

### Documentation

- [Scala 3 Book — Companion Objects](https://docs.scala-lang.org/scala3/book/domain-modeling-tools.html#companion-objects) — companion object naming and the relationship to the class.
- [Scala 3 Book — Singleton Objects](https://docs.scala-lang.org/scala3/book/taste-objects.html) — why `object` methods do not require a separate instance.
- [Scala 3 Book — Methods](https://docs.scala-lang.org/scala3/book/taste-methods.html) — methods on classes and objects.
- [Scala 3 Book — Default Parameter Values](https://docs.scala-lang.org/scala3/book/methods-most.html#default-parameter-values) — an alternative to a factory for an optional initial tag set.
- [Scala 3 Book — Case Classes](https://docs.scala-lang.org/scala3/book/domain-modeling-tools.html#case-classes) — generated construction and `copy()` behaviour.

**What to look for:** Identify which call uses an existing `entry`, which call creates a new entry using `DevLogEntry.create(...)`, and which call uses the case class's generated `DevLogEntry(...)`. Notice that a convenient factory does not automatically enforce a rule on every construction path.

### Suggested Git milestone

After verifying the code locally, commit the companion factory, updated `Main.scala` assertions, and the progress notes with:

```text
feat: add companion factory for dev log entries
```

Only record this as committed and pushed after Git confirms it. The earlier Exercise 2 commit is still unconfirmed in the supplied notes; verify Git history rather than assuming it was pushed.

## 9. Next exercise — validate a Dev Log title (not started)

**Candidate goal:** Prevent creation of a `DevLogEntry` with a blank title regardless of whether callers use `DevLogEntry(...)`, `DevLogEntry.create(...)`, or `entry.copy(...)`.

Before writing code, decide what counts as invalid (`""`, spaces, etc.), whether the API should **throw** (`require`) or **return a value describing failure** (`Either`), and where the invariant should be enforced. Keep this next exercise small and avoid adding rules that have not yet been discussed.

### Documentation

- [Scala 3 Book — Case Classes](https://docs.scala-lang.org/scala3/book/domain-modeling-tools.html#case-classes) — constructing and copying case-class values.
- [Scala 3 Book — Companion Objects](https://docs.scala-lang.org/scala3/book/domain-modeling-tools.html#companion-objects) — factory methods versus construction paths.
- [Scala Standard Library — Predef.require](https://www.scala-lang.org/api/3.x/scala/Predef$.html) — assertions for conditions that callers must meet; inspect the `require` methods.

**What to look for:** Ask whether a rule written only in `create` would still run when a caller writes `DevLogEntry(...)` or `entry.copy(...)`.

## 10. Official Scala documentation — quick reference

- [Scala 3 Book — Case Classes](https://docs.scala-lang.org/scala3/book/domain-modeling-tools.html#case-classes)
- [Scala 3 Book — Methods](https://docs.scala-lang.org/scala3/book/taste-methods.html)
- [Scala 3 Book — Singleton Objects](https://docs.scala-lang.org/scala3/book/taste-objects.html)
- [Scala 3 Book — Collections](https://docs.scala-lang.org/scala3/book/collections-classes.html)

**How to use these docs:** The links above each exercise are the starting point. Read only enough to answer that exercise's question, then use this list as a wider reference. When we add a future exercise, record its direct documentation links and “What to look for” note alongside its implementation.

---

**Current position (18 November 2026):** Scala project set up and initially pushed; `addTag`/`removeTag` implemented; Exercise 3 companion-object `create` and its expanded assertions reviewed. Local `sbt run` and Exercise 3 commit/push remain unconfirmed. Next candidate exercise: enforcing a nonblank title across all construction paths.
