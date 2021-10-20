# Steps

Access control center at `http://localhost:9021/`

```bash
#create a stream
CREATE STREAM MOVEMENTS (PERSON VARCHAR KEY, LOCATION VARCHAR)
  WITH (VALUE_FORMAT='JSON', PARTITIONS=1, KAFKA_TOPIC='movements');
# insert records
INSERT INTO MOVEMENTS VALUES ('Allison', 'Denver');
INSERT INTO MOVEMENTS VALUES ('Robin', 'Leeds');
INSERT INTO MOVEMENTS VALUES ('Robin', 'Ilkley');
INSERT INTO MOVEMENTS VALUES ('Allison', 'Boulder');
```

or via ksqldb CLI

```bash
docker exec -it ksqldb-cli sh
ksql http://ksqldb-server:8088
show streams;
SET 'auto.offset.reset' = 'earliest';
SELECT * FROM MOVEMENTS EMIT CHANGES;

#create a table PERSON_STATS
CREATE TABLE PERSON_STATS WITH (VALUE_FORMAT='AVRO') AS
  SELECT PERSON,
    LATEST_BY_OFFSET(LOCATION) AS LATEST_LOCATION,
    COUNT(*) AS LOCATION_CHANGES,
    COUNT_DISTINCT(LOCATION) AS UNIQUE_LOCATIONS
  FROM MOVEMENTS
GROUP BY PERSON
EMIT CHANGES;

show tables;
INSERT INTO MOVEMENTS VALUES ('Allison', 'Loveland');
select * from PERSON_STATS WHERE person = 'Allison';

kcat -b localhost:9092 -t PERSON_STATS -s value=avro -r http://localhost:8081
curl --silent -X GET http://localhost:8081/subjects/PERSON_STATS-value/versions/latest | jq '.'
```

## Filtering

```bash
CREATE stream orders (id INTEGER KEY, item VARCHAR, address STRUCT <
city  VARCHAR, state VARCHAR >)
WITH (KAFKA_TOPIC='orders', VALUE_FORMAT='json', partitions=1);

#insert into orders stream
INSERT INTO orders(id, item, address)
VALUES(140, 'Mauve Widget', STRUCT(city:='Ithaca', state:='NY'));
INSERT INTO orders(id, item, address)
VALUES(141, 'Teal Widget', STRUCT(city:='Dallas', state:='TX'));
INSERT INTO orders(id, item, address)
VALUES(142, 'Violet Widget', STRUCT(city:='Pasadena', state:='CA'));
INSERT INTO orders(id, item, address)
VALUES(143, 'Purple Widget', STRUCT(city:='Yonkers', state:='NY'));
INSERT INTO orders(id, item, address)
VALUES(144, 'Tan Widget', STRUCT(city:='Amarillo', state:='TX'));

#new stream for new york orders
CREATE STREAM ny_orders AS SELECT * FROM ORDERS WHERE
 ADDRESS->STATE='NY' EMIT CHANGES;
SELECT * FROM ny_orders EMIT CHANGES;

#two way data flow
kcat -b localhost:9092 -t NY_ORDERS
```

## Lookup and join

```bash
CREATE TABLE items (id VARCHAR PRIMARY KEY, make VARCHAR, model VARCHAR, unit_price DOUBLE)
WITH (KAFKA_TOPIC='items', VALUE_FORMAT='avro', PARTITIONS=1);

INSERT INTO items VALUES('item_3', 'Spalding', 'TF-150', 19.99);
INSERT INTO items VALUES('item_4', 'Wilson', 'NCAA Replica', 29.99);
INSERT INTO items VALUES('item_7', 'SKLZ', 'Control Training', 49.99);

CREATE STREAM orders (ordertime BIGINT, orderid INTEGER, itemid VARCHAR, orderunits INTEGER)
WITH (KAFKA_TOPIC='orders', VALUE_FORMAT='avro', PARTITIONS=1);

CREATE STREAM orders_enriched AS
SELECT o.*, i.*,
  o.orderunits * i.unit_price AS total_order_value
FROM orders o LEFT OUTER JOIN items i
on o.itemid = i.id;

INSERT INTO orders VALUES (1620501334477, 65, 'item_7', 5);
INSERT INTO orders VALUES (1620502553626, 67, 'item_3', 2);
INSERT INTO orders VALUES (1620503110659, 68, 'item_7', 7);
INSERT INTO orders VALUES (1620504934723, 70, 'item_4', 1);
INSERT INTO orders VALUES (1620505321941, 74, 'item_7', 3);
INSERT INTO orders VALUES (1620506437125, 72, 'item_7', 9);
INSERT INTO orders VALUES (1620508354284, 73, 'item_3', 4);

#query the enriched stream
SELECT * FROM orders_enriched EMIT CHANGES;
```
