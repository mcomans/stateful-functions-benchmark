package api.logging

import org.slf4j.Logger
import org.slf4j.MDC
import org.springframework.kafka.core.KafkaTemplate
import types.MessageWrapper

fun KafkaTemplate<String, Any>.sendLogged(topic: String, id: String, message: MessageWrapper, logger: Logger) {
    MDC.put("status", "SENDING_KAFKA_MESSAGE")
    MDC.put("message_type", message.message.javaClass.name)
    logger.info("Sending kafka message")
    this.send(topic, id, message)
    MDC.remove("status")
    MDC.remove("message_type")
}