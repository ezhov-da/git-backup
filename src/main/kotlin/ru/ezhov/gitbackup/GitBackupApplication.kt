package ru.ezhov.gitbackup

import mu.KotlinLogging
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.kohsuke.github.GHRepository
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
    private val gitHubRepository: GitHubRepository,
    private val githubConfig: GithubConfig,
    private val directoryConfig: DirectoryConfig,
) : CommandLineRunner {
    override fun run(vararg args: String?) {
        val repositories = gitHubRepository.repositories()

        logger.info { "Repositories: ${repositories.size}" }

        val loadRepositories = loadRepositories(repositories = repositories)
        val createOrUpdatedArchives = createOrUpdateArchives(loadedRepositories = loadRepositories)

        if (createOrUpdatedArchives.isNotEmpty()) {
            logger.info { "Backup completed.\n${createOrUpdatedArchives.joinToString(separator = "\n") { "- ${it.name}" }}" }
        } else {
            logger.info { "Backup completed. No updated archives" }
        }
    }

    private fun loadRepositories(repositories: List<GHRepository>): List<LoadedRepository> {
        logger.info { "Repositories directory: ${directoryConfig.repositories().absolutePath}" }
        directoryConfig.repositories().mkdirs()

        val loadedRepositoryList = mutableListOf<LoadedRepository>()
        repositories.forEachIndexed { index, repo ->
            logger.info { "Started '${index + 1}' from '${repositories.size}'. '${repo.name}'..." }

            val localRepoDirectory = File(directoryConfig.repositories(), repo.name)

            if (localRepoDirectory.exists()) {
                logger.info { "Repository '${repo.name}' already exists." }

                val sizeBeforePull = getFolderSize(localRepoDirectory)

                Git
                    .open(localRepoDirectory)
                    .pull()
                    .setCredentialsProvider(UsernamePasswordCredentialsProvider(githubConfig.token, ""))
                    .call()

                logger.info { "Repository '${repo.name}' updated" }

                val sizeAfterPull = getFolderSize(localRepoDirectory)

                if (sizeBeforePull != sizeAfterPull) {
                    logger.info { "Size before '$sizeBeforePull' is not equals after '$sizeAfterPull'" }

                    loadedRepositoryList.add(LoadedRepository(repoFolder = localRepoDirectory, repo = repo, isChangeSize = true))
                } else {
                    logger.info { "'${repo.name}' size before '$sizeBeforePull' is equals after '$sizeAfterPull'" }

                    loadedRepositoryList.add(LoadedRepository(repoFolder = localRepoDirectory, repo = repo, isChangeSize = false))
                }
            } else {
                Git
                    .cloneRepository()
                    .setURI(repo.httpTransportUrl)
                    .setDirectory(localRepoDirectory)
                    .setCredentialsProvider(UsernamePasswordCredentialsProvider(githubConfig.token, ""))
                    .setCloneAllBranches(true)
                    .call()

                loadedRepositoryList.add(LoadedRepository(repoFolder = localRepoDirectory, repo = repo, isChangeSize = true))

                logger.info { "Repository ${repo.name} is new. Added to create archive" }
            }
        }

        return loadedRepositoryList
    }

    private fun createOrUpdateArchives(loadedRepositories: List<LoadedRepository>): List<File> {
        val archives = mutableListOf<File>()

        logger.info { "Archives directory directory: ${directoryConfig.archives().absolutePath}" }
        directoryConfig.archives().mkdirs()

        logger.info { "Archives for create/update: ${loadedRepositories.size}" }

        loadedRepositories.forEach { repo ->
            val archiveFile = File(directoryConfig.archives(), "${repo.repo.name}.zip")

            // Skip if archive already exists and repository is not changed
            if (archiveFile.exists() && !repo.isChangeSize) {
                logger.info { "Archive '${archiveFile.absolutePath}' already exists and repository is not changed" }

                return@forEach
            }

            logger.info { "Archive '${archiveFile.absolutePath}' create started..." }

            FileOutputStream(archiveFile).use { fos ->
                ZipOutputStream(fos).use { zipOut ->
                    zipFile(repo.repoFolder, repo.repoFolder.name, zipOut)
                }
            }

            archives.add(archiveFile)

            logger.info { "Archive '$archiveFile' is created/updated" }
        }

        return archives
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

    fun getFolderSize(folder: File): Long {
        var length: Long = 0
        val files = folder.listFiles() ?: return 0

        for (file in files) {
            length += if (file.isFile) {
                file.length()
            } else {
                getFolderSize(file)
            }
        }

        return length
    }
}

data class LoadedRepository(
    val repoFolder: File,
    val repo: GHRepository,
    val isChangeSize: Boolean,
)