import io.cloudstate.kotlinsupport.cloudstate
import shoppingcart.ShoppingCartEntity
import shoppingcart.Shoppingcart

fun main() {
    cloudstate {
        config {
            host = "0.0.0.0"
            port = 8080
            loglevel = "INFO"
        }
        eventsourced {
            entityService = ShoppingCartEntity::class
            descriptor = Shoppingcart.getDescriptor().findServiceByName("ShoppingCartService")
            additionalDescriptors = mutableListOf(Shoppingcart.getDescriptor(), shoppingcart.persistence.Domain.getDescriptor() )
            snapshotEvery = 1
            persistenceId = "shoppingcart"
        }
    }.start()
        .toCompletableFuture()
        .get()
}