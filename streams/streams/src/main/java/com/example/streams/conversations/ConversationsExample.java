package com.example.streams.conversations;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KGroupedStream;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.common.utils.Bytes;
import static org.apache.kafka.streams.StoreQueryParameters.fromNameAndType;

import io.confluent.kafka.serializers.KafkaJsonSerializer;
import io.confluent.kafka.serializers.KafkaJsonDeserializer;

public class ConversationsExample {

  public static void main(String[] args) throws IOException, InterruptedException {
    Properties streamsProperties = new Properties();
    try (FileInputStream fis = new FileInputStream("src/main/resources/streams.properties")) {
      streamsProperties.load(fis);
    } catch (Exception e) {
      // TODO: handle exception
    }
    streamsProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, "conversations-streamzz");
    streamsProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    System.out.println("in here");
    StreamsBuilder builder = new StreamsBuilder();
    final String inputTopic = streamsProperties.getProperty("conversations.input.topic");
    KStream<String, DataRecord> conversationsStream =
        builder.stream(inputTopic, Consumed.with(Serdes.String(), getJsonSerde()));

    final KGroupedStream<String, DataRecord> sentMessages = conversationsStream
        .filter((key, value) -> value.getEventType().equals("MESSAGE_SENT"))
        .peek(
            (key, value) -> System.out.println("Outgoing record - key " + key + " value " + value))
        .groupByKey();

    final KGroupedStream<String, DataRecord> readMessages = conversationsStream
        .filter((key, value) -> value.getEventType().equals("MESSAGE_READ"))
        .peek(
            (key, value) -> System.out.println("Outgoing record - key " + key + " value " + value))
        .groupByKey();

    //TODO: WIP

    // KTable<String, Long> readCountBySubscriber = readMessages.count();
    // KTable<String, Long> sentCountBySubscriber = sentMessages.count();

    // sentCountBySubscriber.join(readCountBySubscriber, (leftValue, rightValue) -> {
    // 	if (leftValue != null) {
    // 		System.out.println("left value is " + leftValue);
    // 	}
    // 	if (rightValue != null) {
    // 		System.out.println("right value is " + rightValue);
    // 	}
    // 	return (leftValue - rightValue);
    // }, Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as("message-unread-count").withValueSerde(Serdes.Long()));
    // Create a State Store for with the message sent count
    sentMessages
        .count(Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as("message-sent-count")
            .withValueSerde(Serdes.Long()));
    // Create a State Store for with the message read count
    readMessages
        .count(Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as("message-read-count")
            .withValueSerde(Serdes.Long()));

    KafkaStreams kafkaStreams = new KafkaStreams(builder.build(), streamsProperties);
    kafkaStreams.cleanUp();
		TopicLoader.runProducer();
    kafkaStreams.start();
    ConversationsCount.count(kafkaStreams);
    // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        kafkaStreams.close();
      } catch (final Exception e) {
        // ignored
      }
    }));

  }

  private static Serde<DataRecord> getJsonSerde() {

    Map<String, Object> serdeProps = new HashMap<>();
    serdeProps.put("json.value.type", DataRecord.class);

    final Serializer<DataRecord> mySerializer = new KafkaJsonSerializer<>();
    mySerializer.configure(serdeProps, false);

    final Deserializer<DataRecord> myDeserializer = new KafkaJsonDeserializer<>();
    myDeserializer.configure(serdeProps, false);

    return Serdes.serdeFrom(mySerializer, myDeserializer);
  }
}
