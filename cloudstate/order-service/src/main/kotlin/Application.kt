import io.cloudstate.kotlinsupport.cloudstate
import order.Order
import order.OrderEntity

fun main() {
    cloudstate {
        config {
            host = "0.0.0.0"
            port = 8080
            loglevel = "INFO"
        }

        eventsourced {
            entityService = OrderEntity::class
            descriptor = Order.getDescriptor().findServiceByName("OrderService")
            additionalDescriptors = mutableListOf(Order.getDescriptor(), order.persistence.Domain.getDescriptor() )
            snapshotEvery = 1
            persistenceId = "order"
        }
    }.start()
        .toCompletableFuture()
        .get()
}