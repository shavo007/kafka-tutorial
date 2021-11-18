# kafa streams


## Exercise Descriptions

Here's a brief description of each example in this repository.  For detailed step-by-step descriptions follow the Kafka Streams
course videos.  Note that for the purposes of facilitating the learning process, each exercise uses a utility class `TopicLoader` that will create
the required topics and populate them with some sample records for the Kafka Streams application. As a result when you run each exercise, the first output you'll
see on the console is from the `Callback` interface, and it will look similar to this:

```text
Record produced - offset - 0 timestamp - 1622133855705
Record produced - offset - 1 timestamp - 1622133855717
Record produced - offset - 2 timestamp - 1622133855717
```

### Basic Operations

The basic operations exercise demonstrates using Kafka Streams stateless operations like `filter` and `mapValues`.
You run the basic operations example with this command ` ./gradlew runStreams -Pargs=basic` and your output on the console should resemble this:

```text
Incoming record - key order-key value orderNumber-1001
Outgoing record - key order-key value 1001
Incoming record - key order-key value orderNumber-5000
Outgoing record - key order-key value 5000
Incoming record - key order-key value orderNumber-999
Incoming record - key order-key value orderNumber-3330
Outgoing record - key order-key value 3330
Incoming record - key order-key value bogus-1
Incoming record - key order-key value bogus-2
Incoming record - key order-key value orderNumber-8400
Outgoing record - key order-key value 8400
```
Take note that it's expected to not have a corresponding output record for each input record due to the filters applied by the Kafka Steams application.

Verify topics created
`kcat -b localhost:9092 -L | grep streams`

```bash
#consume messages
kcat -b localhost:9092 -t basic-input-streams
```

#### KTable

This exercise is a gentle introduction to the Kafka Streams `KTable` abstraction.  This example uses the same topology as the `basic` example, but your expected output
is different due to fact that a `KTable` is an update-stream, and records with the same key are considered updates to previous records.  The default behavior
of a `KTable` then is to emit only the latest update per key. The sample data for this exercise has the same key,
so in this case your output will consist of one record:
```text
Outgoing record - key order-key value 8400
```

NOTE: Since the default behavior for materialized `KTable`s is to emit changes on commit or when the cache is full, you'll need
to let this application run for roughly 40 seconds to see a result.

```bash
./gradlew runStreams -Pargs=ktable
kcat -b localhost:9092 -t ktable-output
```
### Serialisation (Serde)

```bash
./gradlew runStreams -Pargs=serdes

#view schema
curl --silent -X GET http://localhost:8081/subjects/sr-input-value/versions/latest | jq .
curl --silent -X GET http://localhost:8081/schemas/ids/1 | jq .

kcat -b localhost:9092 -t sr-input -s value=avro -r http://localhost:8081
```

### Conversations

> Give me the unread conversations for a subscriber

This exercise demonstrates an aggregated count of conversations (both sent and read) of a simulated stream of messages. You'll see the incoming records
on the console along with the aggregation results:

```bash
docker-compose exec broker kafka-topics --delete --bootstrap-server broker:9092 --topic dummy
./gradlew runStreams -Pargs=interactive
```

```text
#filtered into two grouped streams by event type


Outgoing record - key 1 value {"id":1,"timeSent":1637213704235,"eventType":"MESSAGE_SENT","orderTotal":200,"productName":"tayto"}
Outgoing record - key 1 value {"id":1,"timeSent":1637214604235,"eventType":"MESSAGE_SENT","orderTotal":200,"productName":"moro"}
Outgoing record - key 1 value {"id":1,"timeSent":1637215504235,"eventType":"MESSAGE_SENT","orderTotal":200,"productName":"tesla"}
Outgoing record - key 1 value {"id":1,"timeSent":1637216404235,"eventType":"MESSAGE_READ","orderTotal":200,"productName":"tesla"}
Outgoing record - key 1 value {"id":1,"timeSent":1637217304235,"eventType":"MESSAGE_READ","orderTotal":200,"productName":"moro"}
```

```text
#Interactive query
message sent count for subscriber 1: 3
message read count for subscriber 1: 2
message unread count for subscriber 1 is: 1
```

[Interactive queries](https://www.youtube.com/watch?v=fVDdY36Wk3w&t=4s)
[Interactive queries demo](https://github.com/confluentinc/kafka-streams-examples/tree/7.0.0-post/src/main/java/io/confluent/examples/streams/interactivequeries)
