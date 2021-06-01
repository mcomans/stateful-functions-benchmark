package product

import createJsonType
import org.apache.flink.statefun.sdk.java.types.Type
import types.product.*

object ProductMessages {
    val GET_PRODUCT: Type<GetProduct> = createJsonType("product", GetProduct::class)
    val GET_PRODUCT_RESPONSE = createJsonType("product", GetProductResponse::class)
    val SET_PRICE: Type<SetPrice> = createJsonType("product", SetPrice::class)
    val ADD_STOCK: Type<AddStock> = createJsonType("product", AddStock::class)
    val RETRACT_STOCK: Type<RetractStock> = createJsonType("product", RetractStock::class)
    val RETRACT_STOCK_RESPONSE = createJsonType("order", RetractStockResponse::class)
}