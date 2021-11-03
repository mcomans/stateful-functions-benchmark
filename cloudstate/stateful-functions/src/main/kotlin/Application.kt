import io.cloudstate.kotlinsupport.cloudstate
import product.Product
import product.ProductEntity
import shoppingcart.ShoppingCartEntity
import shoppingcart.Shoppingcart
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
            entityService = ProductEntity::class
            descriptor = Product.getDescriptor().findServiceByName("ProductService")
            additionalDescriptors = mutableListOf(Product.getDescriptor(), product.persistence.Domain.getDescriptor() )
            snapshotEvery = 1
            persistenceId = "product"
        }
        eventsourced {
            entityService = UserEntity::class
            descriptor = User.getDescriptor().findServiceByName("UserService")
            additionalDescriptors = mutableListOf(User.getDescriptor(), user.persistence.Domain.getDescriptor() )
            snapshotEvery = 1
            persistenceId = "user"
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