package api.product

import api.logging.RequestInfo
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.*
import types.product.AddStock
import types.product.SetPrice
import java.util.*

@RestController()
@RequestMapping("/products")
class ProductController(val kafkaTemplate: KafkaTemplate<String, Any>, val requestInfo: RequestInfo) {

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

    private fun handleProductChange(productId: String, product: Product?) {
        if (product?.price != null) {
            kafkaTemplate.send("set-price", productId, SetPrice(product.price, requestInfo.requestId))
        }
        if (product?.stock != null) {
            kafkaTemplate.send("add-stock", productId, AddStock(product.stock, requestInfo.requestId))
        }

    }

    data class Product(val price: Int?, val stock: Int?)
}