package api.user

import api.logging.RequestInfo
import api.logging.sendLogged
import api.shoppingcart.ShoppingCartController
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.*
import types.MessageWrapper
import types.user.AddCredit
import java.util.*

@RestController
@RequestMapping("/users")
class UserController(val kafkaTemplate: KafkaTemplate<String, Any>, val requestInfo: RequestInfo) {

    private val logger: Logger = LoggerFactory.getLogger(UserController::class.java)

    @PostMapping("")
    fun createUser(@RequestBody user: User?): String {
        val userId = UUID.randomUUID().toString()

        if (user?.credits != null) {
            kafkaTemplate.sendLogged("add-credit", userId, MessageWrapper(requestInfo.requestId, AddCredit(user.credits)), logger)
        }

        return userId
    }

    @PatchMapping("/{userId}/credits/add")
    fun addCredit(@PathVariable userId: String, @RequestBody user: User) {
        if (user.credits != null) {
            kafkaTemplate.sendLogged("add-credit", userId, MessageWrapper(requestInfo.requestId, AddCredit(user.credits)), logger)
        }
    }

    data class User(val credits: Int?)
}