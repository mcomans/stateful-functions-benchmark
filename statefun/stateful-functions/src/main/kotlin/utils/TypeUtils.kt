import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.flink.statefun.sdk.java.TypeName
import org.apache.flink.statefun.sdk.java.types.SimpleType
import org.apache.flink.statefun.sdk.java.types.Type
import kotlin.reflect.KClass

private val JSON_OBJ_MAPPER = ObjectMapper().registerKotlinModule()

fun <T: Any> createJsonType(typeNamespace: String, jsonClass: KClass<T>): Type<T> {
    return SimpleType.simpleImmutableTypeFrom(
        TypeName.typeNameOf(typeNamespace, jsonClass.simpleName),
        JSON_OBJ_MAPPER::writeValueAsBytes
    ) { bytes: ByteArray? ->
        JSON_OBJ_MAPPER.readValue(
            bytes,
            jsonClass.java
        )
    }
}
