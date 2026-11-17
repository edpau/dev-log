import java.time.LocalDate

case class DevLogEntry(
                        title: String,
                        description: String,
                        date: LocalDate,
                        tags: Set[String]
                      ):

  def addTag(newTag: String): DevLogEntry = this.copy(tags = this.tags + newTag) 
