package ru.ezhov.gitbackup.repository

import mu.KotlinLogging
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHubBuilder
import org.springframework.stereotype.Repository
import ru.ezhov.gitbackup.GithubConfig

private val logger = KotlinLogging.logger {}

@Repository
class GitHubRepository(
    private val githubConfig: GithubConfig
) {
    fun repositories(): List<GHRepository> {
        logger.info { "Getting GitHub repositories..." }

        val github = GitHubBuilder()
            .withOAuthToken(githubConfig.token, githubConfig.user)
            .build()

        val repo = github.myself.allRepositories
        return repo.values.toList()
    }
}