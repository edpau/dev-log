# Scala 3 Study Notes: Instance Methods, Singleton Objects, and Companion Objects

**Study priority:** Must know → Good to know → Revisit later  
**Context:** Scala 3, with examples based on a small `DevLogEntry` project.  
**Goal:** Understand *where methods live*, *how they are called*, and *when to use each construct*.

---

## The 30-second overview

| Concept | What it means | How you call it | Main use |
|---|---|---|---|
| **Instance method** | A method (`def`) defined on a class or case class and called on one of its instances | `entry.addTag("Git")` | Behaviour belonging to a particular entry |
| **Singleton object** | An `object` declaration with one shared instance | `TagUtils.normalise(" Scala ")` | Shared utilities, constants, or application-level functionality |
| **Companion object** | A singleton `object` with the **same name** as a class/case class, defined in the **same source file** | `DevLogEntry.starter("My log")` | Factory methods and class-level behaviour related to that type |

**The relationship to remember:** Every companion object is a singleton object, but not every singleton object is a companion object.

---

# 1. Must know

## A. Instance method

A method declared **inside a class or case class** can be called on an **instance** of that class. It can read that instance's fields and express behaviour specific to that instance.

```scala
case class DevLogEntry(title: String, tags: Set[String]) {
  def addTag(tag: String): DevLogEntry =
    copy(tags = tags + tag)

  def removeTag(tag: String): DevLogEntry =
    copy(tags = tags - tag)
}

val entry = DevLogEntry("My first log", Set("Scala"))
val updated = entry.addTag("Git")

assert(entry.tags == Set("Scala"))
assert(updated.tags == Set("Scala", "Git"))
```

What is happening?

1. `entry` is an **instance** of `DevLogEntry`.
2. `entry.addTag("Git")` calls an **instance method** on `entry`.
3. Inside `addTag`, `tags` refers to **that entry's tags**.
4. `copy(...)` creates a **new** `DevLogEntry`, rather than changing `entry`.
5. The original `entry` remains unchanged because this design uses an immutable case class and immutable `Set`.

**Remember:** An instance method does **not** have to mutate an instance. In your Dev Log project, returning an updated copy is a good design.

## B. Singleton object

The keyword `object` declares a **single shared object**. You do **not** create it with `new`.

```scala
object TagUtils {
  def normalise(tag: String): String =
    tag.trim.toLowerCase
}

val result = TagUtils.normalise("  SCALA  ")
assert(result == "scala")
```

Here:

- `TagUtils` is the singleton object.
- `normalise` is a method **on that singleton object**, not an instance method of `DevLogEntry`.
- Call it with `TagUtils.normalise(...)`; you do not need `new TagUtils`.
- This is a useful place for functionality that does **not** depend on one particular Dev Log entry.

**Mental model:** It looks a little like calling a Java `static` utility method, but Scala's `object` is an actual singleton object rather than simply a set of static methods.

## C. Companion object

An `object` becomes a **companion object** when it shares the **same name** and **source file** as its class or case class.

```scala
case class DevLogEntry(title: String, tags: Set[String]) {
  def addTag(tag: String): DevLogEntry =
    copy(tags = tags + tag)
}

object DevLogEntry {
  def starter(title: String): DevLogEntry =
    DevLogEntry(title, Set("Scala"))
}

val entry = DevLogEntry.starter("Learning Scala")
val updated = entry.addTag("Git")
```

Look at the **two different uses of `DevLogEntry`**:

- `DevLogEntry.starter(...)` calls a method on the **companion object**.
- `entry.addTag(...)` calls an **instance method** on one `DevLogEntry` value.

The companion object is a natural home for a **factory method** such as `starter`, which creates a new entry with sensible default values.

### Very important: `apply` and case classes

```scala
val entry = DevLogEntry("Learning", Set("Scala"))
```

Why can you call `DevLogEntry(...)` without writing `new`?

- For a **case class**, Scala generates an `apply` method in its companion object.
- So `DevLogEntry(...)` is effectively a call to `DevLogEntry.apply(...)`.
- If you write your own `object DevLogEntry`, Scala can still supply the usual generated case-class `apply` (provided you have not defined a conflicting one).
- **Do not confuse `apply` with `copy`:** `apply` is used to create an entry through its companion; `copy` is called on an *existing entry* to create a modified one.

**Scala 3 nuance:** Regular concrete classes can often be constructed with `ClassName(...)` too, through *universal apply methods*. Therefore, the absence of `new` **alone** does not prove that a type is a case class or that you wrote a custom companion object.

---

## D. One working example containing all three concepts

Put the following code in a small Scala 3 project (you may split the definitions into files later). The class and its companion must remain in the same source file.

```scala
// 1. A case class with instance methods
case class DevLogEntry(title: String, tags: Set[String]) {
  def addTag(tag: String): DevLogEntry =
    copy(tags = tags + tag)

  def removeTag(tag: String): DevLogEntry =
    copy(tags = tags - tag)
}

// 2. Its companion object: same name, same file
object DevLogEntry {
  def starter(title: String): DevLogEntry =
    DevLogEntry(title, Set("Scala"))
}

// 3. An unrelated singleton object: no matching class
object TagUtils {
  def normalise(tag: String): String =
    tag.trim.toLowerCase
}

object Main {
  def main(args: Array[String]): Unit = {
    val first = DevLogEntry.starter("Learning Scala")
    val second = first.addTag("Git")
    val third = second.removeTag("Scala")

    assert(first.tags == Set("Scala"))
    assert(second.tags == Set("Scala", "Git"))
    assert(third.tags == Set("Git"))
    assert(TagUtils.normalise("  SCALA  ") == "scala")

    println("All assertions passed")
  }
}
```

### Identify each part

| Expression | Which concept? | Why? |
|---|---|---|
| `first.addTag("Git")` | Instance method | Uses `first`'s `tags` |
| `second.removeTag("Scala")` | Instance method | Returns a changed copy of `second` |
| `TagUtils.normalise("  SCALA  ")` | Singleton object method | Does not need an entry instance |
| `DevLogEntry.starter("Learning Scala")` | Companion object method | Creates an entry with defaults |
| `DevLogEntry("Learning", Set("Scala"))` | Generated companion `apply` | Constructs a case-class instance |
| `first.copy(tags = Set("Git"))` | Generated **instance** method | Creates a copy from `first` |

---

# 2. Good to know

## A. How to choose where a method belongs

Ask yourself: **What information does this behaviour need?**

- **It needs the current entry's fields** → instance method: `entry.removeTag("Git")`.
- **It works independently of any entry** → standalone singleton utility: `TagUtils.normalise("Git")` (or a top-level method in Scala 3).
- **It constructs or describes the `DevLogEntry` type as a whole** → companion object: `DevLogEntry.starter("Learning")`.

These are design guidelines, not hard compiler rules. For example, an `object` method *could* take an entry as an argument, but keeping entry-specific behaviour on the entry often makes code easier to discover.

## B. Companions can access each other's `private` members

```scala
class SecretBox(private val value: String)

object SecretBox {
  def reveal(box: SecretBox): String =
    box.value // Allowed: SecretBox is the companion object
}

val box = new SecretBox("hidden")
assert(SecretBox.reveal(box) == "hidden")
```

This private access is one reason companions are particularly useful for closely related construction and helper code. Ordinary unrelated objects do **not** gain this special access.

## C. `object` means shared, but not automatically immutable or thread-safe

```scala
object Counter {
  var total: Int = 0
}
```

This is **shared mutable state**. Callers are reading and changing the same `total`. That can become difficult to reason about, especially with concurrency. Prefer immutable data and pure methods unless shared mutation is genuinely required and properly managed.

An `object` is normally initialized when it is first accessed. This does not mean every method call constructs a new instance.

## D. Methods with or without `()`

The official Scala guidance is to express intent:

```scala
def tagCount: Int = tags.size       // Value-like: no side effect
def save(): Unit = println("Saved") // Action with a side effect
```

This is a **convention** that makes code easier to read. Public method return types are also worth writing explicitly.

## E. `object Main` and Scala 3 top-level methods

A Scala application can define `main` inside an object:

```scala
object Main {
  def main(args: Array[String]): Unit =
    println("Hello")
}
```

Scala 3 also supports top-level methods and a `@main` entry point:

```scala
@main def hello(): Unit =
  println("Hello")
```

So you do **not** need a singleton object for every function in Scala 3.

---

# 3. Revisit later

These ideas are useful, but you do **not** need to master them before continuing your Dev Log exercises.

| Topic | Why revisit it later? |
|---|---|
| `apply` / `unapply` | `apply` supports construction-like syntax; `unapply` supports extracting values in pattern matching. |
| `case object` | Useful for a single named value, often with sealed traits or algebraic data types. |
| Singleton initialization and concurrency | Matters when an `object` has effects or mutable shared state. |
| Companion factory validation | Learn how `apply`, `Option`, `Either`, or `Try` can help create only valid values. |
| Top-level definitions and packages | More options for organizing Scala 3 code without a utility object for everything. |

---

# 4. Quick self-check (answer before looking back)

1. If `entry` is a `DevLogEntry`, is `entry.removeTag("Git")` a singleton method or an instance method?
2. Why can you call `TagUtils.normalise(...)` without constructing `TagUtils`?
3. What **two conditions** make `object DevLogEntry` a companion object?
4. Does `entry.addTag("Git")` change `entry` in the immutable example?
5. Where would you put a method that makes a `DevLogEntry` with standard default tags?

<details>
<summary>Answers</summary>

1. Instance method.
2. `TagUtils` is already a singleton `object`.
3. It must have the same name as its class/case class and be defined in the same source file.
4. No. It returns a new copy.
5. In `object DevLogEntry`, its companion object.

</details>

---

# 5. Official Scala documentation (recommended reading order)

### Must read

1. **[Scala 3 Book — Singleton Objects](https://docs.scala-lang.org/scala3/book/taste-objects.html)** — Introduction to singleton objects, utility methods, and companion objects.
2. **[Scala 3 Book — Domain Modeling Tools](https://docs.scala-lang.org/scala3/book/domain-modeling-tools.html)** — Read the **Classes**, **Objects**, **Companion objects**, and **Case classes** sections.
3. **[Scala 3 Book — Methods](https://docs.scala-lang.org/scala3/book/methods-intro.html)** — Where methods can be defined.

### Good to read

4. **[Scala 3 Book — Method Features](https://docs.scala-lang.org/scala3/book/methods-most.html)** — Method calls, return types, `()` conventions, and visibility.
5. **[Tour of Scala — Singleton Objects](https://docs.scala-lang.org/tour/singleton-objects.html)** — More examples of companions and private access.

### Revisit later

6. **[Scala 3 Reference — Universal Apply Methods](https://docs.scala-lang.org/scala3/reference/other-new-features/creator-applications.html)** — Precise rules for `ClassName(...)` without `new` in Scala 3.
7. **[Scala 3 Book — Main Methods](https://docs.scala-lang.org/scala3/book/methods-main-methods.html)** — `@main` and executable programs.
8. **[Tour of Scala — Case Classes](https://docs.scala-lang.org/tour/case-classes.html)** — Generated case-class behaviour and pattern matching.

---

## What to remember for your next exercise

> **Instance method:** behaviour of *this entry*.  
> **Singleton object:** a shared object for functionality not tied to a particular instance.  
> **Companion object:** the singleton object associated with a class of the same name, commonly used for creating values and type-level helpers.

**Recommended next step:** In your Dev Log project, keep `addTag` and `removeTag` on `DevLogEntry`. Try adding a companion `starter(title)` method that creates an entry with default tags, and test it using `assert`.
