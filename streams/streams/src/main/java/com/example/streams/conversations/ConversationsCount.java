package com.example.streams.conversations;

import static org.apache.kafka.streams.StoreQueryParameters.fromNameAndType;
import java.io.IOException;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;

public class ConversationsCount {

  public static Long count(KafkaStreams kafkaStreams) throws IOException, InterruptedException {
     // Get the key-value store CountsKeyValueStore
    Thread.sleep(5000);
    ReadOnlyKeyValueStore<String, Long> messageSentStore = kafkaStreams
        .store(fromNameAndType("message-sent-count", QueryableStoreTypes.keyValueStore()));

		// ReadOnlyKeyValueStore<String, Long> messageUnReadStore = kafkaStreams
		// 		.store(fromNameAndType("message-unread-count", QueryableStoreTypes.keyValueStore()));

		ReadOnlyKeyValueStore<String, Long> messageReadStore = kafkaStreams
				.store(fromNameAndType("message-read-count", QueryableStoreTypes.keyValueStore()));

    // Get value by key
    System.out.println("message sent count for subscriber 1: " + messageSentStore.get("1"));
    // System.out.println("message unread count for subscriber 1: " + messageUnReadStore.get("1"));
    System.out.println("message read count for subscriber 1: " + messageReadStore.get("1"));
    System.out.println("message unread count for subscriber 1 is: " + (messageSentStore.get("1") - messageReadStore.get("1")));
    return messageSentStore.approximateNumEntries();
  }
}
