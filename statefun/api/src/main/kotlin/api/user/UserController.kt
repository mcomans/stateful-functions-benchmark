package api.user

import api.logging.RequestInfo
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.*
import types.MessageWrapper
import types.user.AddCredit
import java.util.*

@RestController
@RequestMapping("/users")
class UserController(val kafkaTemplate: KafkaTemplate<String, Any>, val requestInfo: RequestInfo) {

    @PostMapping("")
    fun createUser(@RequestBody user: User?): String {
        val userId = UUID.randomUUID().toString()

        if (user?.credits != null) {
            kafkaTemplate.send("add-credit", userId, MessageWrapper(requestInfo.requestId, AddCredit(user.credits)))
        }

        return userId
    }

    @PatchMapping("/{userId}/credits/add")
    fun addCredit(@PathVariable userId: String, @RequestBody user: User) {
        if (user.credits != null) {
            kafkaTemplate.send("add-credit", userId, MessageWrapper(requestInfo.requestId, AddCredit(user.credits)))
        }
    }

    data class User(val credits: Int?)
}