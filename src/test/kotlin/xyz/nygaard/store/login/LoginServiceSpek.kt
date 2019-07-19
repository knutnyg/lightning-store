import com.opentable.db.postgres.embedded.EmbeddedPostgres
import org.amshove.kluent.shouldNotBeNullOrBlank
import org.amshove.kluent.shouldNotEqual
import org.flywaydb.core.Flyway
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import xyz.nygaard.TestDatabase
import xyz.nygaard.store.login.LoginService

object LoginServiceSpek : Spek({

    val embeddedPostgres = EmbeddedPostgres.start()
    val testDatabase = TestDatabase(embeddedPostgres.postgresDatabase)

    Flyway.configure().run {
        dataSource(embeddedPostgres.postgresDatabase).load().migrate()
    }

    describe("LoginService") {
        val loginService = LoginService(testDatabase)
        it("Creates new user token") {
            val userToken = loginService.createAndSavePrivateKey()
            val userToken1 = loginService.createAndSavePrivateKey()
            userToken.shouldNotBeNullOrBlank()
            userToken.shouldNotEqual(userToken1)
        }
    }
})
