import io.cloudstate.kotlinsupport.cloudstate
import product.Product
import product.ProductEntity
import product.persistence.Domain

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
            additionalDescriptors = mutableListOf(Product.getDescriptor(), Domain.getDescriptor() )
            snapshotEvery = 1
            persistenceId = "product"
        }
    }.start()
        .toCompletableFuture()
        .get()
}