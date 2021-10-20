package api.product

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController()
@RequestMapping("/products")
class ProductController() {

    @PostMapping()
    fun createProduct(): UUID {
        return UUID.randomUUID()
    }
}