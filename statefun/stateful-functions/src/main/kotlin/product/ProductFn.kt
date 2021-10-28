package product

import LoggedStatefulFunction
import createJsonType
import messages.BenchmarkMessages
import mu.KotlinLogging
import mu.withLoggingContext
import org.apache.flink.statefun.sdk.java.*
import org.apache.flink.statefun.sdk.java.message.Message
import org.apache.flink.statefun.sdk.java.message.MessageBuilder
import types.MessageWrapper
import types.WrappedMessage
import types.product.AddStock
import types.product.RetractStock
import types.product.RetractStockResponse
import types.product.SetPrice
import java.lang.Integer.max
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

class ProductFn : LoggedStatefulFunction() {

    companion object {
        val TYPE = TypeName.typeNameFromString("benchmark/product")
        val PRODUCT: ValueSpec<Product> = ValueSpec.named("product").withCustomType(Product.TYPE)
        val SPEC = StatefulFunctionSpec.builder(TYPE)
            .withValueSpec(PRODUCT)
            .withSupplier(::ProductFn)
    }

    override fun invoke(context: Context, requestId: String, message: WrappedMessage): CompletableFuture<Void> {
        when(message) {
            is AddStock -> handleAddStock(context, message)
            is RetractStock -> handleRetractStock(context, requestId, message)
            is SetPrice -> handleSetPrice(context, message)
        }

        return context.done()
    }

    private fun handleAddStock(context: Context, message: AddStock) {
        logger.info { "Product ${context.self().id()} - Adding ${message.amount} of stock"}

        val storage = context.storage()
        val product = storage.get(PRODUCT).orElse(Product(0, 0))
        product.stock += message.amount

        storage.set(PRODUCT, product)

        logger.info { "Product ${context.self().id()} - New amount of stock: ${product.stock}" }
    }

    private fun handleRetractStock(context: Context, requestId: String, message: RetractStock) {
        logger.info { "Product ${context.self().id()} - Retracting ${message.amount} of stock" }

        val storage = context.storage()
        val product = storage.get(ProductFn.PRODUCT).orElse(Product(0, 0))
        var success = false

        if (product.stock - message.amount >= 0) {
            product.stock -= message.amount
            success = true
        }

        storage.set(ProductFn.PRODUCT, product)

        logger.info { "Product ${context.self().id()} - New amount of stock: ${product.stock}" }

        if (context.caller().isPresent) {
            val caller = context.caller().get()
            val responseMessage = MessageBuilder
                .forAddress(caller.type(), caller.id())
                .withCustomType(
                    BenchmarkMessages.WRAPPER_MESSAGE,
                    MessageWrapper(requestId, RetractStockResponse(
                        success,
                        message.amount,
                        product.price
                    ))
                )
                .build()
            logger.info {
                "Product ${
                    context.self().id()
                } - Sending ${if (success) "successful" else "failed"} response to caller ${
                    caller.type().asTypeNameString()
                }/${caller.id()}"
            }
            context.send(responseMessage)
        }
    }

    private fun handleSetPrice(context: Context, message: SetPrice) {
        val storage = context.storage()
        val product = storage.get(PRODUCT).orElse(Product(0, 0))
        product.price = message.price
        storage.set(PRODUCT, product)

        logger.info { "Product ${context.self().id()} - Price set to: ${message.price}" }

    }


    class Product(var price: Int, var stock: Int) {
        companion object {
            val TYPE = createJsonType("product", Product::class)
        }
    }
}