package ru.ezhov.gitbackup.repository

import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHubBuilder
import org.springframework.stereotype.Repository

@Repository
class GitHubRepository {
    fun repositories(): List<GHRepository> {
        val github = GitHubBuilder().withOAuthToken("____", "___").build()

//        val ser = github.searchRepositories().user("___")

        val repo = github.myself.allRepositories

        return repo.values.toList()

//        val client = GitHubClient.create(URI.create("https://api.github.com/"), "___")

//        return client.createSearchClient().repositories(ImmutableSearchParameters.builder().q("___").page(1).build())
//            .join().items()?.toList().orEmpty()
    }
}