import java.time.LocalDate

case class DevLogEntry(
                        title: String,
                        description: String,
                        date: LocalDate,
                        tags: Set[String]
                      ):

  def addTag(newTag: String): DevLogEntry = this.copy(tags = this.tags + newTag)

  def removeTag(tag: String): DevLogEntry = this.copy(tags = this.tags - tag)

object DevLogEntry:
  def create(title: String, description: String, date: LocalDate): DevLogEntry =
    DevLogEntry(title, description, date, Set.empty[String])