package com.github.serserser.kafka.etl.impl;

import com.github.serserser.kafka.etl.impl.data.Commodity;
import com.github.serserser.kafka.etl.impl.data.Country;
import com.github.serserser.kafka.etl.impl.data.PointOfSale;
import com.github.serserser.kafka.etl.impl.serializers.CustomSerdes;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static com.github.serserser.kafka.etl.impl.Topics.*;

public class DictionariesProcessor implements Runnable, Loader {

    private static final Logger logger = LoggerFactory.getLogger(DictionariesProcessor.class);

    private static final String APPLICATION_ID = "dictionaries-processor";

    @Override
    public void load() throws URISyntaxException, IOException {
        URI countriesUri = getClass().getClassLoader().getResource("data/countries.txt").toURI();
        String[] countriesUriElements = countriesUri.toString().split("!");
        try ( Producer<String, Country> producer = new KafkaProducer<>(Utils.createKafkaProperties(CustomSerdes.country().getClass()));
              FileSystem fs = getFileSystem(countriesUriElements[0]);
              Stream<String> countries = Files.lines(fs.getPath(countriesUriElements[1])) ) {
            countries.map(this::createCountry)
                    .forEach(cmdty -> send(producer, cmdty, COUNTRIES_TOPIC_NAME));
        }

        logger.info("Loaded countries (1/3)");


        URI commoditiesUri = getClass().getClassLoader().getResource("data/commodities.txt").toURI();
        String[] commoditiesUriElements = commoditiesUri.toString().split("!");
        try ( Producer<String, Commodity> producer = new KafkaProducer<>(Utils.createKafkaProperties(CustomSerdes.commodity().getClass()));
              FileSystem fs = getFileSystem(commoditiesUriElements[0]);
              Stream<String> commodities = Files.lines(fs.getPath(commoditiesUriElements[1])) ) {
            commodities.map(this::createCommodity)
                    .forEach(cmdty -> producer.send(new ProducerRecord<>(COMMODITIES_TOPIC_NAME, cmdty)));
        }

        logger.info("Loaded commodities (2/3)");

        URI pointsOfSaleUri = getClass().getClassLoader().getResource("data/pointsOfSale.txt").toURI();
        String[] pointsOfSaleUriElements = pointsOfSaleUri.toString().split("!");
        try ( Producer<Integer, Integer> producer = new KafkaProducer<>(Utils.createKafkaProperties(IntegerSerializer.class, IntegerSerializer.class));
              FileSystem fs = getFileSystem(pointsOfSaleUriElements[0]);
            Stream<String> pointsOfSale = Files.lines(fs.getPath(pointsOfSaleUriElements[1]))) {
            pointsOfSale.map(this::createPointOfSale)
                    .forEach(cmdty -> producer.send(new ProducerRecord<>(POINT_OF_SALE_TOPIC_NAME, cmdty.getShopId(), cmdty.getCountryId())));
        }

        logger.info("Loaded points of sale (3/3)");


        logger.info("Loaded all data");
    }

    private <T> Future<RecordMetadata> send(Producer<String, T> producer, T cmdty, String topicName) {
        logger.info("sending item");
        return producer.send(new ProducerRecord<>(topicName, cmdty));
    }

    private FileSystem getFileSystem(String uriElement) throws IOException {
        URI uri = URI.create(uriElement);
        try {
            return FileSystems.getFileSystem(uri);
        } catch ( FileSystemNotFoundException e ) {
            return FileSystems.newFileSystem(uri, new HashMap<>());
        }
    }

    private PointOfSale createPointOfSale(String line) {
        String[] fields = line.split(",");
        return new PointOfSale(toInt(fields[0]), toInt(fields[1]));
    }

    private Country createCountry(String line) {
        String[] fields = line.split(",");
        return new Country(toInt(fields[0]), fields[1], fields[2]);
    }

    private Commodity createCommodity(String line) {
        String[] fields = line.split(",");
        return new Commodity(toInt(fields[0]), toDouble(fields[1]));
    }

    @Override
    public void run() {
        logger.info("started processing");
        StreamsBuilder builder = new StreamsBuilder();
        KStream<Integer, Double> commoditiesWithPricesStream = builder.stream(COMMODITIES_TOPIC_NAME, Consumed.with(Serdes.Integer(), CustomSerdes.commodity()))
                .map((key, value) -> map(value));
        commoditiesWithPricesStream.to(COMMODITIES_KEY_PRICE_TOPIC_NAME, Produced.with(Serdes.Integer(), Serdes.Double()));

        KafkaStreams streams = new KafkaStreams(builder.build(), Utils.createStreamsKafkaProperties(APPLICATION_ID));
        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    private KeyValue<Integer, Double> map(Commodity value) {
        logger.info("mapping commodity with id: " + value.getCommodityId());
        return new KeyValue<>(value.getCommodityId(), value.getPrice());
    }
}
