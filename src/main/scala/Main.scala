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
