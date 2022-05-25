package api.product

import api.logging.RequestInfo
import api.logging.sendLogged
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.*
import types.MessageWrapper
import types.product.AddStock
import types.product.GetFrequentlyBoughtTogetherGraph
import types.product.SetPrice
import java.util.*

@RestController()
@RequestMapping("/products")
class ProductController(val kafkaTemplate: KafkaTemplate<String, Any>, val requestInfo: RequestInfo) {

    private val logger: Logger = LoggerFactory.getLogger(ProductController::class.java)

    @PostMapping()
    fun createProduct(@RequestBody product: Product?): String {
        val productId = UUID.randomUUID().toString()

        handleProductChange(productId, product)

        return productId
    }

    @PatchMapping("/{productId}")
    fun patchProduct(@PathVariable productId: String, @RequestBody product: Product): String {
        handleProductChange(productId, product)
        return productId
    }

    @GetMapping("/{productId}/freq-items")
    fun getFrequentItems(@PathVariable productId: String, @RequestBody query: FrequentItemsQuery) {
        kafkaTemplate.sendLogged("freq-items-query", productId, MessageWrapper(requestInfo.requestId,
            GetFrequentlyBoughtTogetherGraph(top = query.top, depth = query.depth, visited = setOf())), logger)
    }

    private fun handleProductChange(productId: String, product: Product?) {
        if (product?.price != null) {
            kafkaTemplate.sendLogged("set-price", productId, MessageWrapper(requestInfo.requestId, SetPrice(product.price)), logger)
        }
        if (product?.stock != null) {
            kafkaTemplate.sendLogged("add-stock", productId, MessageWrapper(requestInfo.requestId, AddStock(product.stock)), logger)
        }

    }

    data class Product(val price: Int?, val stock: Int?)
    data class FrequentItemsQuery(val depth: Int = 3, val top: Int = 3)
}
