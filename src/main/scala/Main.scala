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

    // Test original entry
    assert(entry.tags == Set("Scala", "Git"))

    // Test adding a tag
    assert(updatedEntry.tags == Set("Scala", "Git", "GitHub"))

    // Test removing a tag
    assert(updated.tags == Set("Scala"))

    // Test removing all tags
    assert(empty.tags == Set.empty[String])

    // Test removing a non-existing tag
    assert(unchanged == entry)

    // Test original entry remains unchanged
    assert(entry.tags == Set("Scala", "Git"))

    println("All assertions passed!")
  }
}
