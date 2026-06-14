import java.nio.file.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.jdk.CollectionConverters.*

/** Creates a .md file to report the files that were moved during download folder cleanup.
@param sourceDir The directory that was cleaned up.
@param files All files that were sent to trash.
*/
def writeReport(sourceDir: String, files: List[String]): Unit = {
    // Build a timestamp string
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
    
    // Build the path to place the report in
    val reportPath = Paths.get(sourceDir, s"cleanup_report_$timestamp.md")

    // Create the files content in markdown formatation
    val markdownContent = 
        s"""# Cleanup Report - $timestamp
           |
           |**Folder cleaned up:** '$sourceDir'
           |
           |### Moved files:
           |${if files.isEmpty then "* no files found." else files.map((f) => s"- $f").mkString("\n")}
        """.stripMargin

    // Write to file
    Files.writeString(reportPath, markdownContent)
    println(s"Report created: $reportPath")
}

@main def cleanupDownloads(): Unit = {
    // Define paths
    val homeDir = System.getProperty("user.home")
    val downloadPath = Paths.get(homeDir, "Downloads")

    // Temporary trashbin until real trashbin may be used
    val trashPath = Paths.get(homeDir, ".local_trash")

    // Create trashbin if it does not exist
    if !Files.exists(trashPath) then Files.createDirectories(trashPath)

    println(s"Getting all elements of $downloadPath")

    // Builds a list of all files in downloadPath
    val filesInDownload = Files.list(downloadPath)
        .iterator()
        .asScala
        .filter((file) => Files.isRegularFile(file))
        .toList


    // ListBuffer object used to store the filenme of the files moved
    val movedFiles = scala.collection.mutable.ListBuffer[String]()

    // Move the files
    filesInDownload.foreach { (file) => 
        // Filename of file
        val filename = file.getFileName

        // Builds the target path for the moved file
        val target = trashPath.resolve(filename)

        try
            // StandardCopyOption.REPLACE_EXISTING used replace for duplicates
            Files.move(file, target, StandardCopyOption.REPLACE_EXISTING)
            println(s"Moved: ${filename}")

            // Keep track of the files that get moved
            movedFiles.addOne(filename.toString())
        catch
            // Catch and print error if a file cannot be moved
            case e: Exception => println(s"Error at file ${filename}: ${e.getMessage}")
    }

    // Write report after all files were moved
    writeReport(downloadPath.toString, movedFiles.toList)
}