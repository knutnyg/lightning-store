import org.amshove.kluent.shouldNotBeNullOrBlank
import org.amshove.kluent.shouldNotEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import xyz.nygaard.store.login.LoginService

object LoginServiceSpek : Spek({

    describe("LoginService") {
        val loginService = LoginService()
        it("Creates new user token") {
            val userToken = loginService.createPrivateKey()
            val userToken1 = loginService.createPrivateKey()
            userToken.shouldNotBeNullOrBlank()
            userToken.shouldNotEqual(userToken1)
        }
    }
})
