package api.user

import api.logging.RequestInfo
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.web.bind.annotation.*
import user.UserServiceGrpc
import java.util.*

@RestController
@RequestMapping("/users")
class UserController(val requestInfo: RequestInfo) {

    @GrpcClient("user")
    private lateinit var userStub: UserServiceGrpc.UserServiceBlockingStub;

    @PostMapping("")
    fun createUser(@RequestBody userBody: User?): String {
        val userId = UUID.randomUUID().toString()

        if (userBody?.credits != null) {
            userStub.addCredits(
                user.User.AddCreditsMessage.newBuilder().setUserId(userId).setAmount(userBody.credits).build()
            )
        }

        return userId
    }

    @PatchMapping("/{userId}/credits/add")
    fun addCredit(@PathVariable userId: String, @RequestBody userBody: User) {
        if (userBody.credits != null) {
            userStub.addCredits(
                user.User.AddCreditsMessage.newBuilder().setUserId(userId).setAmount(userBody.credits).build()
            )
        }
    }

    data class User(val credits: Int?)
}