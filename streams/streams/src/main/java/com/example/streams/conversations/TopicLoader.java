package com.example.streams.conversations;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Properties;
import com.example.streams.basic.StreamsUtils;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import io.confluent.kafka.serializers.KafkaJsonSerializer;

public class TopicLoader {

    public static void main(String[] args) throws IOException {
        runProducer();
    }

    static void runProducer() throws IOException {
        Properties properties = StreamsUtils.loadProperties();
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaJsonSerializer.class);

        try (Admin adminClient = Admin.create(properties);
                Producer<String, DataRecord> producer = new KafkaProducer<>(properties)) {
            final String inputTopic = properties.getProperty("conversations.input.topic");
            var topics = List.of(StreamsUtils.createTopic(inputTopic));
            adminClient.createTopics(topics);

            Callback callback = (metadata, exception) -> {
                if (exception != null) {
                    System.out.printf("Producing records encountered error %s %n", exception);
                } else {
                    System.out.printf("Record produced - offset - %d timestamp - %d %n",
                            metadata.offset(), metadata.timestamp());
                }

            };
            Instant instant = Instant.now();
            DataRecord record1 = DataRecord.builder().eventType("MESSAGE_SENT").id(1L)
                    .orderTotal(200L).productName("tayto").timeSent(instant.toEpochMilli()).build();
            instant = instant.plus(15L, ChronoUnit.MINUTES);
            DataRecord record2 = DataRecord.builder().eventType("MESSAGE_SENT").id(1L)
                    .orderTotal(200L).productName("moro").timeSent(instant.toEpochMilli()).build();
            instant = instant.plus(15L, ChronoUnit.MINUTES);
            DataRecord record3 = DataRecord.builder().eventType("MESSAGE_SENT").id(1L)
                    .orderTotal(200L).productName("tesla").timeSent(instant.toEpochMilli()).build();
            instant = instant.plus(15L, ChronoUnit.MINUTES);
            DataRecord record4 = DataRecord.builder().eventType("MESSAGE_READ").id(1L)
                    .orderTotal(200L).productName("tesla").timeSent(instant.toEpochMilli()).build();
            instant = instant.plus(15L, ChronoUnit.MINUTES);
            DataRecord record5 = DataRecord.builder().eventType("MESSAGE_READ").id(1L)
                    .orderTotal(200L).productName("moro").timeSent(instant.toEpochMilli()).build();
            var messages = List.of(record1, record2, record3, record4, record5);

            messages.forEach((message -> {
                ProducerRecord<String, DataRecord> producerRecord =
                        new ProducerRecord<String, DataRecord>(inputTopic,
                                String.valueOf(message.getId()), message);
                producer.send(producerRecord, callback);
            }));
        }
    }
}
