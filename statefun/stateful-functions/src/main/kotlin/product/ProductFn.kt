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

                logger.info { "Product ${addStockMessage.productId} - Adding ${addStockMessage.amount} of stock"}

                val storage = context.storage()
                val product = storage.get(PRODUCT).orElse(Product(0, 0))
                val newStock = product.stock + addStockMessage.amount

                storage.set(PRODUCT, Product(product.price, newStock))

                logger.info { "Product ${addStockMessage.productId} - New amount of stock: ${newStock}" }

                return context.done()
            }

            if (message.`is`(ProductMessages.RETRACT_STOCK)) {
                val retractStockMessage = message.`as`(ProductMessages.RETRACT_STOCK)

                logger.info { "Product ${retractStockMessage.productId} - Retracting ${retractStockMessage.amount} of stock"}

                val storage = context.storage()
                val product = storage.get(PRODUCT).orElse(Product(0, 0))
                val newStock = max(product.stock - retractStockMessage.amount, 0)

                // TODO: Check if enough stock is available?

                storage.set(PRODUCT, Product(product.price, newStock))

                logger.info { "Product ${retractStockMessage.productId} - New amount of stock: ${newStock}" }

                if (context.caller().isPresent) {
                    val caller = context.caller().get()
                    val responseMessage = MessageBuilder
                        .forAddress(caller.type(), caller.id())
                        .withCustomType(
                            ProductMessages.RETRACT_STOCK_RESPONSE,
                            RetractStockResponse(
                                retractStockMessage.productId,
                                true,
                                retractStockMessage.amount,
                                product.price
                            )
                        )
                        .build()
                    logger.info { "Product ${retractStockMessage.productId} - Sending successful response to caller ${caller.type().asTypeNameString()}/${caller.id()}" }
                    context.send(responseMessage)
                }

                return context.done()
            }

            if (message.`is`(ProductMessages.SET_PRICE)) {
                val setPriceMessage = message.`as`(ProductMessages.SET_PRICE)

                val storage = context.storage()
                val product = storage.get(PRODUCT).orElse(Product(0, 0))
                storage.set(PRODUCT, Product(setPriceMessage.price, product.stock))

                logger.info { "Product ${setPriceMessage.productId} - Price set to: ${setPriceMessage.price}" }

                return context.done()
            }

            return context.done()
    }


    class Product(val price: Int, val stock: Int) {
        companion object {
            val TYPE = createJsonType("product", Product::class)
        }
    }
}