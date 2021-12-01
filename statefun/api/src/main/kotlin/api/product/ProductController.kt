package api.product

import api.logging.RequestInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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

        return productId;
    }

    @PatchMapping("/{productId}")
    fun patchProduct(@PathVariable productId: String, @RequestBody product: Product): String {
        handleProductChange(productId, product)
        return productId;
    }

    @GetMapping("/{productId}/freq-items")
    fun getFrequentlyBoughtTogetherItems(@PathVariable productId: String) {
        kafkaTemplate.send("freq-items-query", productId, MessageWrapper(requestInfo.requestId,
            GetFrequentlyBoughtTogetherGraph(visited = setOf())))
    }

    private fun handleProductChange(productId: String, product: Product?) {
        if (product?.price != null) {
            kafkaTemplate.send("set-price", productId, MessageWrapper(requestInfo.requestId, SetPrice(product.price)))
        }
        if (product?.stock != null) {
            kafkaTemplate.send("add-stock", productId, MessageWrapper(requestInfo.requestId, AddStock(product.stock)))
        }

    }

    data class Product(val price: Int?, val stock: Int?)
}