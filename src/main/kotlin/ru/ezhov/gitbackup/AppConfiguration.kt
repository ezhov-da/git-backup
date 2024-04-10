package ru.ezhov.gitbackup

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.io.File

@Configuration
@EnableConfigurationProperties(GithubConfig::class, DirectoryConfig::class)
class AppConfiguration

@ConfigurationProperties("github")
data class GithubConfig(
    val user: String,
    val token: String,
)

@ConfigurationProperties("directory")
data class DirectoryConfig(
    private val repositories: String,
    private val archives: String,
) {
    fun repositories(): File = File(repositories)
    fun archives(): File = File(archives)
}
