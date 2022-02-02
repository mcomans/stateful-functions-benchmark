package product

import LoggedStatefulFunction
import createJsonType
import messages.BenchmarkMessages
import mu.KotlinLogging
import org.apache.flink.statefun.sdk.java.*
import org.apache.flink.statefun.sdk.java.message.MessageBuilder
import types.MessageWrapper
import types.WrappedMessage
import types.product.*
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

class ProductFn : LoggedStatefulFunction() {

    companion object {
        val TYPE = TypeName.typeNameFromString("benchmark/product")
        val PRODUCT: ValueSpec<Product> = ValueSpec.named("product").withCustomType(Product.TYPE)
        val FREQUENTLY_BOUGHT_TOGETHER: ValueSpec<FrequentlyBoughtTogether> = ValueSpec
            .named("frequently_bought_together")
            .withCustomType(FrequentlyBoughtTogether.TYPE)
        val QUERY_RESULTS: ValueSpec<QueryResults> = ValueSpec
            .named("query_results")
            .withCustomType(QueryResults.TYPE)
        val SPEC = StatefulFunctionSpec.builder(TYPE)
            .withValueSpec(PRODUCT)
            .withValueSpec(FREQUENTLY_BOUGHT_TOGETHER)
            .withValueSpec(QUERY_RESULTS)
            .withSupplier(::ProductFn)
    }

    override fun invoke(context: Context, requestId: String, message: WrappedMessage): CompletableFuture<Void> {
        when(message) {
            is AddStock -> handleAddStock(context, message)
            is RetractStock -> handleRetractStock(context, requestId, message)
            is SetPrice -> handleSetPrice(context, message)
            is UpdateFrequentlyBoughtTogether -> handleUpdateFrequentlyBoughtTogether(context, message)
            is GetFrequentlyBoughtTogetherGraph -> handleGetFrequentlyBoughtTogether(context, requestId, message)
            is GetFrequentlyBoughtTogetherGraphResponse -> handleGetFrequentlyBoughtTogetherResponse(context, requestId, message)
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
        val product = storage.get(PRODUCT).orElse(Product(0, 0))
        var success = false

        if (product.stock - message.amount >= 0) {
            product.stock -= message.amount
            success = true
        }

        storage.set(PRODUCT, product)

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

    private fun handleUpdateFrequentlyBoughtTogether(context: Context, message: UpdateFrequentlyBoughtTogether) {
        val storage = context.storage()
        val freq = storage.get(FREQUENTLY_BOUGHT_TOGETHER).orElse(FrequentlyBoughtTogether(mutableMapOf()))

        message.productIds.forEach { productId ->
            val count = freq.products[productId]
            val new = count?.plus(1) ?: 1

            freq.products[productId] = new
        }

        storage.set(FREQUENTLY_BOUGHT_TOGETHER, freq)

        logger.info { "Product ${context.self().id()} - Frequently bought together items updated" }
    }

    private fun handleGetFrequentlyBoughtTogether(context: Context, requestId: String, message: GetFrequentlyBoughtTogetherGraph) {
        val storage = context.storage()
        val queryResults = storage.get(QUERY_RESULTS).orElse(QueryResults(mutableMapOf()))
        val freq = storage.get(FREQUENTLY_BOUGHT_TOGETHER).orElse(FrequentlyBoughtTogether(mutableMapOf()))

        // If no frequently bought together products exist for this product, return empty set
        if (context.caller().isPresent && freq.products.isEmpty()) {
            logger.info { "Product ${context.self().id()} - Frequently bought together items set is empty, returning empty set" }
            sendFreqProductsResponse(setOf(), context.caller().get().id(), context, requestId)
            return
        }

        val topProducts = freq.products.toList().sortedBy { it.second }.map { it.first }.filterNot { message.visited.contains(it) }.take(message.top)

        if (context.caller().isPresent && message.depth == 1) {
            logger.info { "Product ${context.self().id()} - Depth reached, returning set of freq items" }
            sendFreqProductsResponse(message.visited + topProducts, context.caller().get().id(), context, requestId)
            return
        }

        val topProductsMap = topProducts.associateWith { RequestQueryResultsProduct() }

        queryResults.results[requestId] = RequestQueryResults(topProductsMap, context.caller().orElse(null)?.id())

        storage.set(QUERY_RESULTS, queryResults)

        topProducts.forEach { productId ->
            val query = MessageBuilder.forAddress(ProductFn.TYPE, productId).withCustomType(
                BenchmarkMessages.WRAPPER_MESSAGE,
                MessageWrapper(
                    requestId, GetFrequentlyBoughtTogetherGraph(
                        message.top,
                        message.depth - 1,
                        message.visited + topProducts + context.self().id()
                    )
                )
            ).build()
            context.send(query)
        }
    }

    private fun handleGetFrequentlyBoughtTogetherResponse(context: Context, requestId: String, message: GetFrequentlyBoughtTogetherGraphResponse) {
        val storage = context.storage()
        val queryResults = storage.get(QUERY_RESULTS).orElse(QueryResults(mutableMapOf()))
        val requestQueryResults = queryResults.results[requestId] ?: return

        val productId = context.caller().get().id()

        logger.debug { "Product ${context.self().id()} - Received ${message.products} from product $productId" }

        requestQueryResults.queriedProducts[productId]?.responded = true
        requestQueryResults.queriedProducts[productId]?.results?.addAll(message.products)

        if (requestQueryResults.queriedProducts.all { it.value.responded } && requestQueryResults.callerId != null) {
            logger.debug {"All products responded"}
            queryResults.results.remove(requestId)
            logger.debug {"Sending response to product ${requestQueryResults.callerId}"}
            sendFreqProductsResponse(
                requestQueryResults.queriedProducts.values.fold(setOf()) {
                        acc, productResults -> acc + productResults.results },
                requestQueryResults.callerId, context, requestId)
        }
        else {
            queryResults.results[requestId] = requestQueryResults
        }


        storage.set(QUERY_RESULTS, queryResults)
    }

    private fun sendFreqProductsResponse(products: Set<String>, callerId: String, context: Context, requestId: String) {
        val response = MessageBuilder.forAddress(TYPE, callerId).withCustomType(
            BenchmarkMessages.WRAPPER_MESSAGE,
            MessageWrapper(requestId, GetFrequentlyBoughtTogetherGraphResponse(products))
        ).build()
        context.send(response)
    }

    class Product(var price: Int, var stock: Int) {
        companion object {
            val TYPE = createJsonType("product", Product::class)
        }
    }

    data class FrequentlyBoughtTogether(val products: MutableMap<String, Int>) {
        companion object {
            val TYPE = createJsonType("product", FrequentlyBoughtTogether::class)
        }
    }

    data class QueryResults(val results: MutableMap<String, RequestQueryResults>)
    {
        companion object {
            val TYPE = createJsonType("product", QueryResults::class)
        }
    }

    data class RequestQueryResults(val queriedProducts: Map<String, RequestQueryResultsProduct>, val callerId: String?)

    data class RequestQueryResultsProduct(var responded: Boolean = false, var results: MutableSet<String> = mutableSetOf())
}