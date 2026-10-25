import java.time.LocalDate

object Main {
  def main(args: Array[String]): Unit = {
    val entry = DevLogEntry(
      title = "My First Dev Log",
      description = "Testing development across two Macs",
      date = LocalDate.now(),
      tags = Set("Scala", "Git")
    )

    println(entry)
  }
}
