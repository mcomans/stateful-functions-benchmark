package api.product

import api.logging.RequestInfo
import net.devh.boot.grpc.client.inject.GrpcClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import product.Product
import product.ProductServiceGrpc
import java.util.*

@RestController()
@RequestMapping("/products")
class ProductController(val requestInfo: RequestInfo) {

    @GrpcClient("product-service")
    private lateinit var productStub: ProductServiceGrpc.ProductServiceBlockingStub;

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

    private fun handleProductChange(productId: String, productBody: Product?) {
        if (productBody?.price != null) {
            productStub.setPrice(product.Product.ProductPrice.newBuilder().setProductId(productId).setPrice(productBody.price).build())
        }
        if (productBody?.stock != null) {
            productStub.addStock(product.Product.AddStockMessage.newBuilder().setProductId(productId).setAmount(productBody.stock).build())
        }

    }

    data class Product(val price: Int?, val stock: Int?)
}