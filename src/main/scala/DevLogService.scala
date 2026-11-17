object DevLogService {

  def addTag(
              entry: DevLogEntry,
              newTag: String
            ): DevLogEntry =
    entry.addTag(newTag)

}
