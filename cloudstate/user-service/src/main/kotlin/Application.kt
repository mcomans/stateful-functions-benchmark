import io.cloudstate.kotlinsupport.cloudstate
import user.User
import user.UserEntity

fun main() {
    cloudstate {
        config {
            host = "0.0.0.0"
            port = 8080
            loglevel = "INFO"
        }

        eventsourced {
            entityService = UserEntity::class
            descriptor = User.getDescriptor().findServiceByName("UserService")
            additionalDescriptors = mutableListOf(User.getDescriptor(), user.persistence.Domain.getDescriptor() )
            snapshotEvery = 1
            persistenceId = "user"
        }
    }.start()
        .toCompletableFuture()
        .get()
}