package api

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory


@SpringBootApplication
class ApiApplication(private val kafkaProperties: KafkaProperties) {

    @Bean
    fun producerConfigs(): Map<String, Any> {
        return HashMap<String, Any>(kafkaProperties.buildProducerProperties())
    }

    @Bean
    fun producerFactory(): ProducerFactory<String, String> {
        return DefaultKafkaProducerFactory(producerConfigs())
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, String> {
        return KafkaTemplate(producerFactory())
    }
}

fun main(args: Array<String>) {
    runApplication<ApiApplication>(*args)
}

