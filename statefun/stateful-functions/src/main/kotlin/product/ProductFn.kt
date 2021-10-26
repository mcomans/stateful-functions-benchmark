package product

import LoggedStatefulFunction
import createJsonType
import mu.KotlinLogging
import mu.withLoggingContext
import org.apache.flink.statefun.sdk.java.*
import org.apache.flink.statefun.sdk.java.message.Message
import org.apache.flink.statefun.sdk.java.message.MessageBuilder
import types.product.RetractStockResponse
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

    override fun invoke(context: Context, message: Message): CompletableFuture<Void> {
            if (message.`is`(ProductMessages.ADD_STOCK)) {
                val addStockMessage = message.`as`(ProductMessages.ADD_STOCK)

                logger.info { "Product ${context.self().id()} - Adding ${addStockMessage.amount} of stock"}

                val storage = context.storage()
                val product = storage.get(PRODUCT).orElse(Product(0, 0))
                product.stock += addStockMessage.amount

                storage.set(PRODUCT, product)

                logger.info { "Product ${context.self().id()} - New amount of stock: ${product.stock}" }

                return context.done()
            }

            if (message.`is`(ProductMessages.RETRACT_STOCK)) {
                val retractStockMessage = message.`as`(ProductMessages.RETRACT_STOCK)

                logger.info { "Product ${context.self().id()} - Retracting ${retractStockMessage.amount} of stock"}

                val storage = context.storage()
                val product = storage.get(PRODUCT).orElse(Product(0, 0))
                var success = false

                if (product.stock - retractStockMessage.amount >= 0) {
                    product.stock -= retractStockMessage.amount
                    success = true
                }

                storage.set(PRODUCT, product)

                logger.info { "Product ${context.self().id()} - New amount of stock: ${product.stock}" }

                if (context.caller().isPresent) {
                    val caller = context.caller().get()
                    val responseMessage = MessageBuilder
                        .forAddress(caller.type(), caller.id())
                        .withCustomType(
                            ProductMessages.RETRACT_STOCK_RESPONSE,
                            RetractStockResponse(
                                success,
                                retractStockMessage.amount,
                                product.price,
                                retractStockMessage.requestId
                            )
                        )
                        .build()
                    logger.info { "Product ${context.self().id()} - Sending ${if (success) "successful" else "failed"} response to caller ${caller.type().asTypeNameString()}/${caller.id()}" }
                    context.send(responseMessage)
                }

                return context.done()
            }

            if (message.`is`(ProductMessages.SET_PRICE)) {
                val setPriceMessage = message.`as`(ProductMessages.SET_PRICE)

                val storage = context.storage()
                val product = storage.get(PRODUCT).orElse(Product(0, 0))
                product.price = setPriceMessage.price
                storage.set(PRODUCT, product)

                logger.info { "Product ${context.self().id()} - Price set to: ${setPriceMessage.price}" }

                return context.done()
            }

            return context.done()
    }


    class Product(var price: Int, var stock: Int) {
        companion object {
            val TYPE = createJsonType("product", Product::class)
        }
    }
}