package product

import com.google.protobuf.Empty
import io.cloudstate.javasupport.EntityId
import io.cloudstate.javasupport.eventsourced.CommandContext
import io.cloudstate.kotlinsupport.annotations.eventsourced.Snapshot
import io.cloudstate.kotlinsupport.annotations.eventsourced.CommandHandler
import io.cloudstate.kotlinsupport.annotations.eventsourced.EventHandler
import io.cloudstate.kotlinsupport.annotations.eventsourced.EventSourcedEntity
import io.cloudstate.kotlinsupport.annotations.eventsourced.SnapshotHandler
import product.persistence.Domain


@EventSourcedEntity
class ProductEntity(@EntityId private val entityId: String) {
    private var price: Int = 0
    private var stock: Int = 0

    @Snapshot
    fun snapshot(): Domain.Product {
        return Domain.Product.newBuilder().setPrice(price).setStock(stock).build()
    }

    @SnapshotHandler
    fun snapshotHandler(product: Domain.Product) {
        price = product.price
        stock = product.stock
    }


    @EventHandler
    fun priceChanged(priceChanged: Domain.PriceChanged) {
        this.price = priceChanged.price
    }

    @CommandHandler
    fun setPrice(price: Product.ProductPrice, ctx: CommandContext): Empty {
        ctx.emit(Domain.PriceChanged.newBuilder().setPrice(price.price).build())
        return Empty.getDefaultInstance()
    }

    @CommandHandler
    fun getProduct(getProductMessage: Product.GetProductMessage, ctx: CommandContext): Product.ProductResponse =
        Product.ProductResponse.newBuilder().setPrice(price).setStock(stock).build() 
}