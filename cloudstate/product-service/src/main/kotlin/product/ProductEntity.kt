package product

import com.google.protobuf.Empty
import io.cloudstate.javasupport.EntityId
import io.cloudstate.javasupport.eventsourced.CommandContext
import io.cloudstate.kotlinsupport.annotations.eventsourced.Snapshot
import io.cloudstate.kotlinsupport.annotations.eventsourced.CommandHandler
import io.cloudstate.kotlinsupport.annotations.eventsourced.EventHandler
import io.cloudstate.kotlinsupport.annotations.eventsourced.EventSourcedEntity
import io.cloudstate.kotlinsupport.annotations.eventsourced.SnapshotHandler
import io.grpc.ManagedChannelBuilder
import product.persistence.Domain


@EventSourcedEntity
class ProductEntity(@EntityId private val entityId: String) {
    private var price: Int = 0
    private var stock: Int = 0
    private var frequentItems = mutableMapOf<String, Int>()

    private val asyncProductStub = ProductServiceGrpc.newFutureStub(
        ManagedChannelBuilder.forAddress("product-service", 80).usePlaintext().build()
    )

    @Snapshot
    fun snapshot(): Domain.Product = Domain.Product
        .newBuilder()
        .setPrice(price)
        .setStock(stock)
        .addAllFrequentItems(frequentItems.map {
            Domain.FrequentItem.newBuilder().setProductId(it.key).setAmount(it.value).build()
        })
        .build()

    @SnapshotHandler
    fun snapshotHandler(product: Domain.Product) {
        price = product.price
        stock = product.stock
        frequentItems.putAll(product.frequentItemsList.map { it.productId to it.amount })
    }

    @EventHandler
    fun priceChanged(priceChanged: Domain.PriceChanged) {
        this.price = priceChanged.price
    }

    @EventHandler
    fun stockChanged(stockChanged: Domain.StockChanged) {
        this.stock = stockChanged.stock
    }

    @EventHandler
    fun frequentItemsChanged(frequentItemsChanged: Domain.FrequentItemsChanged) {
        for (item in frequentItemsChanged.productsList) {
            val count = frequentItems[item]
            val new = count?.plus(1) ?: 1
            frequentItems[item] = new
        }
    }

    @CommandHandler
    fun setPrice(price: Product.ProductPrice, ctx: CommandContext): Empty {
        println("Product $entityId - new price: ${price.price}")
        ctx.emit(Domain.PriceChanged.newBuilder().setPrice(price.price).build())
        return Empty.getDefaultInstance()
    }

    @CommandHandler
    fun getProduct(getProductMessage: Product.GetProductMessage, ctx: CommandContext): Product.ProductResponse =
        Product.ProductResponse.newBuilder().setPrice(price).setStock(stock).build()

    @CommandHandler
    fun addStock(addStockMessage: Product.AddStockMessage, ctx: CommandContext): Empty {
        val newStock = stock + addStockMessage.amount;
        println("Product $entityId - Adding ${addStockMessage.amount} of stock. New stock: $newStock")
        ctx.emit(Domain.StockChanged.newBuilder().setStock(newStock).build())
        return Empty.getDefaultInstance()
    }

    @CommandHandler
    fun retractStock(retractStockMessage: Product.RetractStockMessage, ctx: CommandContext): Product.RetractStockResponse {
        val newStock = stock - retractStockMessage.amount;
        println("Product $entityId - Retracting ${retractStockMessage.amount} of stock. New stock: $newStock")
        if (newStock >= 0) {
            ctx.emit(Domain.StockChanged.newBuilder().setStock(newStock).build())
            return Product.RetractStockResponse.newBuilder().setPrice(price).setSuccess(true).build();
        }
        return Product.RetractStockResponse.newBuilder().setPrice(price).setSuccess(false).build();
    }

    @CommandHandler
    fun updateFrequentItems(updateFrequentItemsMessage: Product.UpdateFrequentItemsMessage, ctx: CommandContext): Empty {
        ctx.emit(Domain.FrequentItemsChanged.newBuilder().addAllProducts(updateFrequentItemsMessage.productsList).build())
        return Empty.getDefaultInstance()
    }

    @CommandHandler
    fun getFrequentItemsGraph(message: Product.GetFrequentItemsGraphMessage): Product.GetFrequentItemsGraphResponse {
        val topItems = frequentItems.toList().sortedBy { it.second }.take(message.top).map { it.first }.filterNot { message.visitedList.contains(it) };
        if (message.depth == 1) {
            return Product.GetFrequentItemsGraphResponse
                .newBuilder()
                .addAllItems(topItems)
                .setRequestId(message.requestId)
                .build();
        }

        val futures = topItems.map {
            asyncProductStub.getFrequentItemsGraph(
               Product.GetFrequentItemsGraphMessage
                   .newBuilder()
                   .setProductId(it)
                   .setDepth(message.depth - 1)
                   .setTop(message.top)
                   .addAllVisited(message.visitedList.union(topItems) + entityId)
                   .setRequestId(message.requestId)
                   .build()
            )
        }

        val items = futures.flatMap { it.get().itemsList }.toSet() + topItems

        return Product.GetFrequentItemsGraphResponse.newBuilder().addAllItems(items).build()
    }
}