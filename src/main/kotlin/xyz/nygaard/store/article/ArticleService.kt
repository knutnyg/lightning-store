package xyz.nygaard.store.article

import org.slf4j.LoggerFactory
import xyz.nygaard.db.DatabaseInterface
import xyz.nygaard.db.toList
import java.time.Instant
import java.util.UUID

class ArticleService(private val database: DatabaseInterface) {

    private val log = LoggerFactory.getLogger("ArticleService")

    fun getLimitedArticles(): List<ArticleTeaser> {
        return database.connection.use { connection ->
            connection.prepareStatement("SELECT * FROM ARTICLE")
                .use { statement ->
                    statement.executeQuery()
                        .toList {
                            ArticleTeaser(
                                uuid = getString("id"),
                                title = getString("title"),
                                teaser = getString("teaser")
                            )
                        }
                }
        }
    }

    fun getFullArticle(uuid: String, key: String): Article? {
        // TODO: Validate key against paid invoices
        return database.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT * FROM ARTICLE WHERE ID = ?
            """
            ).use { statement ->
                statement.setString(1, uuid)
                statement.executeQuery()
                    .toList {
                        Article(
                            uuid = getString("id"),
                            title = getString("title"),
                            teaser = getString("teaser"),
                            content = getString("content"),
                            created = getLong("created"),
                            updated = getLong("updated")
                        )
                    }.first()
            }
        }
    }

    fun createArticle(article: NewArticle): String {
        val uuid = UUID.randomUUID().toString()

        database.connection.use { connection ->
            connection.prepareStatement(
                """
                    INSERT INTO ARTICLE(id, title, teaser, content, created, updated) VALUES(?, ?, ?, ?, ?, ?)
                """
            ).use { statement ->
                statement.setString(1, uuid)
                statement.setString(2, article.title)
                statement.setString(3, article.teaser)
                statement.setString(4, article.content)
                statement.setLong(5, Instant.now().epochSecond)
                statement.setLong(6, Instant.now().epochSecond)
                statement.executeUpdate()
            }
            connection.commit()
        }
        return uuid
    }

    fun updateArticle(article: NewArticle) {
        database.connection.use { connection ->
            connection.prepareStatement(
                """
                    UPDATE ARTICLE SET title = ?, teaser = ?, content = ?, updated = ? WHERE id = ?
                """
            ).use { statement ->
                statement.setString(1, article.title)
                statement.setString(2, article.teaser)
                statement.setString(3, article.content)
                statement.setLong(4, Instant.now().epochSecond)
                statement.setString(5, article.uuid)
                statement.executeUpdate()
            }
                connection.commit()
        }
    }
}

data class Article(
    val uuid: String,
    val title: String,
    val teaser: String,
    val content: String,
    val created: Long,
    val updated: Long
)

data class ArticleTeaser(
    val uuid: String,
    val title: String,
    val teaser: String
)


data class NewArticle(
    val uuid: String? = null,
    val title: String,
    val teaser: String,
    val content: String
)
