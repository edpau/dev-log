import java.time.LocalDate

object Main {
  def main(args: Array[String]): Unit = {
    val entry = DevLogEntry(
      title = "My First Dev Log",
      description = "Testing development across two Macs",
      date = LocalDate.now(),
      tags = Set("Scala", "Git")
    )

    val updatedEntry = entry.copy(
      tags = entry.tags + "GitHub"
    )

    val updatedEntryWithFunction = entry.addTag("new")
    val updatedEntryWithFunction2 = updatedEntryWithFunction.addTag("new")

    val resultB = DevLogService.addTag(entry, "late")

    println(entry.tags)
    println(updatedEntryWithFunction.tags)
    println(updatedEntryWithFunction2.tags)
    println(resultB.tags)
    print(entry == updatedEntry)
  }
}
