package api.proxy

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController()
@RequestMapping("/proxy")
class ProxyController(val kafkaTemplate: KafkaTemplate<String, String>) {

    @PostMapping("")
    fun forwardToKafka(@RequestBody message: Message) {
        println(message.toString())
        this.kafkaTemplate.send(message.topic, message.id, message.message)
    }

    data class Message(val topic: String, val id: String, val message: String)
}