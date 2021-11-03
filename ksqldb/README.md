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

## Transforming data

```bash
#convert epoch to human readable string
CREATE STREAM orders_no_address_data AS
SELECT TIMESTAMPTOSTRING(ordertime, 'yyyy-MM-dd HH:mm:ss') AS order_timestamp, orderid, itemid, orderunits
FROM orders EMIT CHANGES;

INSERT INTO orders VALUES (1620508354284, 75, 'item_3', 4);

SELECT * FROM orders_no_address_data EMIT CHANGES;
```

## Flatten nested records

```bash
DROP STREAM orders_enriched DELETE TOPIC;
DROP STREAM orders_no_address_data DELETE TOPIC;
DROP STREAM orders DELETE TOPIC;

CREATE STREAM orders (ordertime BIGINT, orderid INTEGER, itemid VARCHAR,
orderunits INTEGER,  address STRUCT< street  VARCHAR, city VARCHAR, state VARCHAR>)
WITH (KAFKA_TOPIC='orders', VALUE_FORMAT='json', PARTITIONS=1);

INSERT INTO orders VALUES (1620504934723, 70, 'item_4', 1,
  STRUCT(street:='210 West Veterans Drive', city:='Sacramento', state:='California'));
INSERT INTO orders VALUES (16205059321941, 72, 'item_7', 9,
  STRUCT(street:='10043 Bella Vista Blvd', city:='Oakland', state:='California'));
INSERT INTO orders VALUES (16205069437125, 73, 'item_3', 4,
  STRUCT(street:='4921 Parker Place', city:='Pasadena', state:='California'));
INSERT INTO orders VALUES (1620508354284, 74, 'item_7', 3,
  STRUCT(street:='1009 First Street', city:='Fresno', state:='California'));

SELECT * FROM orders EMIT CHANGES;

#persistent query stream to flatten the address
CREATE STREAM orders_flat WITH (KAFKA_TOPIC='orders_flat') AS
SELECT ordertime, orderid, itemid, orderunits, address->street AS street, address->city AS city, address->state AS state
FROM orders EMIT CHANGES;


```

## convert data formats

```bash
PRINT orders_flat FROM BEGINNING;

CREATE STREAM orders_csv
WITH(VALUE_FORMAT='delimited', KAFKA_TOPIC='orders_csv') AS
SELECT * FROM orders_flat EMIT CHANGES;

```

## merging streams

```bash
#orders_uk
CREATE STREAM orders_uk (ordertime BIGINT, orderid INTEGER, itemid VARCHAR, orderunits INTEGER,
    address STRUCT< street VARCHAR, city VARCHAR, state VARCHAR>)
WITH (KAFKA_TOPIC='orders_uk', VALUE_FORMAT='json', PARTITIONS=1);

INSERT INTO orders_uk VALUES (1620501334477, 65, 'item_7', 5,
  STRUCT(street:='234 Thorpe Street', city:='York', state:='England'));
INSERT INTO orders_uk VALUES (1620502553626, 67, 'item_3', 2,
  STRUCT(street:='2923 Alexandra Road', city:='Birmingham', state:='England'));
INSERT INTO orders_uk VALUES (1620503110659, 68, 'item_7', 7,
  STRUCT(street:='536 Chancery Lane', city:='London', state:='England'));

#orders_us
CREATE STREAM orders_us (ordertime BIGINT, orderid INTEGER, itemid VARCHAR, orderunits INTEGER,
    address STRUCT< street VARCHAR, city VARCHAR, state VARCHAR>)
WITH (KAFKA_TOPIC='orders_us', VALUE_FORMAT='json', PARTITIONS=1);

INSERT INTO orders_us VALUES (1620501334477, 65, 'item_7', 5,
  STRUCT(street:='6743 Lake Street', city:='Los Angeles', state:='California'));
INSERT INTO orders_us VALUES (1620502553626, 67, 'item_3', 2,
  STRUCT(street:='2923 Maple Ave', city:='Mountain View', state:='California'));
INSERT INTO orders_us VALUES (1620503110659, 68, 'item_7', 7,
  STRUCT(street:='1492 Wandering Way', city:='Berkley', state:='California'));

SELECT * FROM orders_uk EMIT CHANGES;
SELECT * FROM orders_us EMIT CHANGES;

#merged stream
CREATE STREAM orders_combined AS
SELECT 'US' AS source, ordertime, orderid, itemid, orderunits, address
FROM orders_us;

INSERT INTO orders_combined
SELECT 'UK' AS source, ordertime, orderid, itemid, orderunits, address
FROM orders_uk;
```

## materialised views

```bash
DROP STREAM MOVEMENTS DELETE TOPIC;

CREATE STREAM MOVEMENTS(PERSON VARCHAR KEY, LOCATION VARCHAR)
  WITH (VALUE_FORMAT='JSON', PARTITIONS=1, KAFKA_TOPIC='movements');

INSERT INTO MOVEMENTS VALUES ('Robin', 'York');
INSERT INTO MOVEMENTS VALUES ('Robin', 'Leeds');
INSERT INTO MOVEMENTS VALUES ('Allison', 'Denver');
INSERT INTO MOVEMENTS VALUES ('Robin', 'Ilkley');
INSERT INTO MOVEMENTS VALUES ('Allison', 'Boulder');

# count movements by person
SELECT PERSON, COUNT (*)
FROM MOVEMENTS GROUP BY PERSON EMIT CHANGES;

CREATE TABLE PERSON_STATS AS
SELECT PERSON,
		LATEST_BY_OFFSET(LOCATION) AS LATEST_LOCATION,
		COUNT(*) AS LOCATION_CHANGES,
		COUNT_DISTINCT(LOCATION) AS UNIQUE_LOCATIONS
	FROM MOVEMENTS
GROUP BY PERSON
EMIT CHANGES;

INSERT INTO MOVEMENTS VALUES('Robin', 'Manchester');
INSERT INTO MOVEMENTS VALUES('Allison', 'Loveland');
INSERT INTO MOVEMENTS VALUES('Robin', 'London');
INSERT INTO MOVEMENTS VALUES('Allison', 'Aspen');
INSERT INTO MOVEMENTS VALUES('Robin', 'Ilkley');
INSERT INTO MOVEMENTS VALUES('Allison', 'Vail');
INSERT INTO MOVEMENTS VALUES('Robin', 'York');

SELECT * FROM PERSON_STATS WHERE PERSON = 'Allison';

INSERT INTO MOVEMENTS VALUES('Allison', 'Loveland');
```

## Push/Pull queries

```bash
#push query with emit changes - continously running

SELECT LATEST_LOCATION, LOCATION_CHANGES, UNIQUE_LOCATIONS
FROM PERSON_STATS WHERE PERSON = 'Allison' EMIT CHANGES;
```

## lambda fns

```bash
# transform fn

CREATE STREAM stream1 (
    id INT, name VARCHAR,
    exam_scores MAP<STRING, DOUBLE>
) WITH (
    kafka_topic = 'topic1', partitions = 1,
    value_format = 'json'
);

CREATE STREAM transformed
    AS SELECT id, name,
    TRANSFORM(exam_scores,(k, v) => UCASE(k), (k, v) => (ROUND(v))) AS rounded_scores
FROM stream1 EMIT CHANGES;

INSERT into stream1 values(1, 'Lisa', MAP('Nov':=93.53, 'Feb':=94.13, 'May':=96.83));
INSERT into stream1 values(1, 'Larry', MAP('Nov':=83.53, 'Feb':=84.82, 'May':=85.27));
INSERT into stream1 values(1, 'Melissa', MAP('Nov':=97.20, 'Feb':=96.47, 'May':=98.62));
INSERT into stream1 values(1, 'Chris', MAP('Nov':=92.78, 'Feb':=91.15, 'May':=93.91));

#reduce fn
CREATE STREAM stream2 (
    name VARCHAR,
    points ARRAY<INTEGER>
) WITH (
    kafka_topic = 'topic2', partitions = 1,
    value_format = 'json'
);

CREATE STREAM reduced
    AS SELECT name,
    REDUCE(points,0,(s,x)=> (s+x)) AS total
FROM stream2 EMIT CHANGES;

INSERT INTO stream2 VALUES('Misty', Array[7, 5, 8, 8, 6]);
INSERT INTO stream2 VALUES('Marty', Array[3, 5, 4, 6, 4]);
INSERT INTO stream2 VALUES('Mary', Array[9, 7, 8, 7, 8]);
INSERT INTO stream2 VALUES('Mickey', Array[8, 6, 8, 7, 5]);

#filter fn
CREATE STREAM stream3 (
    id VARCHAR,
    numbers ARRAY<INTEGER>
) WITH (
    kafka_topic = 'topic3', partitions = 1,
    value_format = 'json'
);

CREATE STREAM filtered
    AS SELECT id,
    FILTER(numbers,x => (x%2 = 0)) AS even_numbers
FROM stream3 EMIT CHANGES;

SELECT * FROM filtered EMIT CHANGES;

```
