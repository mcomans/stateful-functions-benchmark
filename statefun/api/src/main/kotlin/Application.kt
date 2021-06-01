package nl.tudelft

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*

fun main(args: Array<String>) {
    val producer = getProducer()

    embeddedServer(Netty, port = 8000) {
        install(ContentNegotiation) {
            jackson()
        }

        routing {
            post("/proxy") {
                val message = call.receive<KafkaMessage>()

                producer.send(ProducerRecord(message.topic, message.id, message.message))

                call.respond(200)
            }
        }
    }.start(wait = true)
}

fun getProducer(): KafkaProducer<String,String> {
    val props = Properties()
    props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = "kafka:9092"
    props[ProducerConfig.CLIENT_ID_CONFIG] = "proxy"
    props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
    props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
    return KafkaProducer<String, String>(props)
}

class KafkaMessage(val topic: String, val id: String, val message: String)
