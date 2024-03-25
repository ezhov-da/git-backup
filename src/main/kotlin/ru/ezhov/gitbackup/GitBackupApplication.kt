package ru.ezhov.gitbackup

import mu.KotlinLogging
import org.eclipse.jgit.api.Git
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Service
import ru.ezhov.gitbackup.repository.GitHubRepository
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private val logger = KotlinLogging.logger {}

@SpringBootApplication
class GitBackupApplication

fun main(args: Array<String>) {
    runApplication<GitBackupApplication>(*args)
}

@Service
class Run(
    private val gitHubRepository: GitHubRepository
) : CommandLineRunner {
    override fun run(vararg args: String?) {
        val archiveDirectory = File("./archive")

        val repositories = gitHubRepository.repositories()

        gitHubRepository.repositories().forEachIndexed { index, repo ->
            logger.info { "Started '${index + 1}' from '${repositories.size}'. '${repo.name}'..." }

            val localRepoDirectory = File("./repos", repo.name)

            if (localRepoDirectory.exists()) {
                logger.info { "Repository '${repo.name}' already exists." }
            } else {
                val git = Git
                    .cloneRepository()
                    .setURI(repo.httpTransportUrl)
                    .setDirectory(localRepoDirectory)
                    .setCloneAllBranches(true)
                    .call()
            }

            archiveDirectory.mkdirs()
            val archiveFile = File(archiveDirectory, "${repo.name}.zip")

            if (archiveFile.exists()) {
                logger.info { "Archive '${repo.name}' already exists." }
            } else {
                val fos = FileOutputStream(archiveFile)
                val zipOut = ZipOutputStream(fos)

                zipFile(localRepoDirectory, localRepoDirectory.name, zipOut)
                zipOut.close()
                fos.close()
            }

            logger.info { "Completed '${repo.name}'" }
        }
    }

    // https://www.baeldung.com/java-compress-and-uncompress#zip_directory
    private fun zipFile(fileToZip: File, fileName: String, zipOut: ZipOutputStream) {
        if (fileToZip.isDirectory) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(ZipEntry(fileName))
                zipOut.closeEntry()
            } else {
                zipOut.putNextEntry(ZipEntry("$fileName/"))
                zipOut.closeEntry()
            }
            val children = fileToZip.listFiles()
            for (childFile in children) {
                zipFile(childFile, fileName + "/" + childFile.name, zipOut)
            }
            return
        }
        val fis = FileInputStream(fileToZip)
        val zipEntry = ZipEntry(fileName)
        zipOut.putNextEntry(zipEntry)
        val bytes = ByteArray(1024)
        var length: Int
        while ((fis.read(bytes).also { length = it }) >= 0) {
            zipOut.write(bytes, 0, length)
        }
        fis.close()
    }
}