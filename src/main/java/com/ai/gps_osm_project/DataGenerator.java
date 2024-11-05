package com.ai.gps_osm_project;

import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import de.westnordost.osmapi.OsmConnection;
import de.westnordost.osmapi.map.data.Node;
import de.westnordost.osmapi.map.data.Relation;
import de.westnordost.osmapi.map.data.RelationMember;
import de.westnordost.osmapi.map.data.Way;
import de.westnordost.osmapi.overpass.OverpassMapDataApi;
import io.jenetics.jpx.GPX;
import io.jenetics.jpx.Track;
import io.jenetics.jpx.TrackSegment;
import io.jenetics.jpx.WayPoint;

public class DataGenerator {

    private final String TRACK_FILE_PATH = "src/main/resources/track.gpx"; // File path to GPX data
    private final int RADIUS = 100; // Specify radius of accessed data around trackpoints
    
    private GPX gpx;
    private Stream<WayPoint> pointsStream;
    private Iterator<WayPoint> iterator;
    private OsmConnection connection;
    private OverpassMapDataApi overpass;
    private WeatherService weatherService;
    private CustomMapDataHandler mapHandler, previousMapHandler;
    private WeatherHandler weatherHandler, previousWeatherHandler;
    private StreamListener streamListener;

    public DataGenerator() {
        try {
            // Load GPX data for the track from file
            gpx = GPX.Reader.DEFAULT.read(TRACK_FILE_PATH);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Stream points from loaded track data
        pointsStream = gpx.tracks()
                .flatMap(Track::segments)
                .flatMap(TrackSegment::points);
        iterator = pointsStream.iterator();

        // Initialize Overpass connection for map data queries
        connection = new OsmConnection("https://overpass-api.de/api/", "my user agent");
        overpass = new OverpassMapDataApi(connection);
        
        weatherService = new WeatherService(); // Initialize Open-Meteo connection for weather data queries
    }

    // Set a listener for streaming events
    public void setStreamListener(StreamListener streamListener) {
        this.streamListener = streamListener;
    }

    // Checks if another track point exists
    private boolean nextPointExists() {
        return iterator.hasNext();
    }

    // Gets the next point from the track iterator
    private WayPoint getPoint() {
        if (nextPointExists()) {
            return iterator.next();
        }
        return null;
    }

    // Replaces invalid characters in tags to ensure compatibility
    private String replaceInvalidCharacters(String input) {
        return input
                .replace("ä", "ae")
                .replace("ö", "oe")
                .replace("ü", "ue")
                .replace("Ä", "Ae")
                .replace("Ö", "Oe")
                .replace("Ü", "Ue")
                .replace("ß", "ss")
                .replace("\"", "'");
    }

    // Writes facts by identifying changes in map and weather data
    private void writeFacts(String factsFilePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(factsFilePath))) {
            Node newClosestRoadNode = mapHandler.findClosestRoadNode();
            Node deletedClosestRoadNode = previousMapHandler.getClosestRoadNode();
        	
            List<Node> newNodes = mapHandler.getNewNodes(previousMapHandler);
            List<Node> deletedNodes = mapHandler.getDeletedNodes(previousMapHandler);
            
            List<Way> newWays = mapHandler.getNewWays(previousMapHandler);
            List<Way> deletedWays = mapHandler.getDeletedWays(previousMapHandler);
            
            List<Relation> newRelations = mapHandler.getNewRelations(previousMapHandler);
            List<Relation> deletedRelations = mapHandler.getDeletedRelations(previousMapHandler);
            
            String newTemperatureCategory = weatherHandler.getNewTemperatureCategory(previousWeatherHandler);
            String newPrecipitationCategory = weatherHandler.getNewPrecipitationCategory(previousWeatherHandler);
            
            String deletedTemperatureCategory = weatherHandler.getDeletedTemperatureCategory(previousWeatherHandler);
            String deletedPrecipitationCategory = weatherHandler.getDeletedPrecipitationCategory(previousWeatherHandler);

            // Write deleted facts
            writeDeletedFacts(writer, deletedClosestRoadNode, deletedNodes, deletedWays, deletedRelations, deletedTemperatureCategory, deletedPrecipitationCategory);
            
            // Write new facts
            writeNewFacts(writer, newClosestRoadNode, newNodes, newWays, newRelations, newTemperatureCategory, newPrecipitationCategory);

            System.out.println("Datalog facts have been updated");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Writes deleted facts for map and weather data
    private void writeDeletedFacts(BufferedWriter writer, Node deletedClosestRoadNode, List<Node> deletedNodes, List<Way> deletedWays, List<Relation> deletedRelations, String deletedTemperatureCategory, String deletedPrecipitationCategory) throws IOException {
        if (deletedClosestRoadNode != null) {
            writer.write(String.format("delete(position(%d)).\n", deletedClosestRoadNode.getId()));
        }
        
        for (Node node : deletedNodes) {
            Long nodeId = node.getId();
            writer.write(String.format("delete(node(%d)).\n", nodeId));
            for (Map.Entry<String, String> tagEntry : node.getTags().entrySet()) {
                String key = replaceInvalidCharacters(tagEntry.getKey());
                String value = replaceInvalidCharacters(tagEntry.getValue());
                writer.write(String.format("delete(nodeTag(%d, \"%s\", \"%s\")).\n", nodeId, key, value));
            }
        }

        for (Way way : deletedWays) {
            Long wayId = way.getId();
            writer.write(String.format("delete(way(%d)).\n", wayId));
            for (Map.Entry<String, String> tagEntry : way.getTags().entrySet()) {
                String key = replaceInvalidCharacters(tagEntry.getKey());
                String value = replaceInvalidCharacters(tagEntry.getValue());
                writer.write(String.format("delete(wayTag(%d, \"%s\", \"%s\")).\n", wayId, key, value));
            }
            List<Long> nodes = way.getNodeIds();
            for (int j = 0; j < nodes.size() - 1; j++) {
                writer.write(String.format("delete(nextInWay(%d, %d, %d)).\n", nodes.get(j), nodes.get(j + 1), wayId));
            }
        }

        for (Relation relation : deletedRelations) {
            Long relationId = relation.getId();
            writer.write(String.format("delete(relation(%d)).\n", relationId));
            for (Map.Entry<String, String> tagEntry : relation.getTags().entrySet()) {
                String key = replaceInvalidCharacters(tagEntry.getKey());
                String value = replaceInvalidCharacters(tagEntry.getValue());
                writer.write(String.format("delete(relationTag(%d, \"%s\", \"%s\")).\n", relationId, key, value));
            }
            List<RelationMember> members = relation.getMembers();
            for (int j = 0; j < members.size() - 1; j++) {
                RelationMember member1 = members.get(j);
                RelationMember member2 = members.get(j + 1);
                writer.write(String.format("delete(nextInRelation(%d, %d, %d)).\n", member1.getRef(), member2.getRef(), relationId));
                writer.write(String.format("delete(relationMember(%d, \"%s\", \"%s\", %d)).\n", member1.getRef(), member1.getType().toString().toLowerCase(), member1.getRole(), relationId));
                if (j == members.size() - 2) {
                    writer.write(String.format("delete(relationMember(%d, \"%s\", \"%s\", %d)).\n", member2.getRef(), member2.getType().toString().toLowerCase(), member2.getRole(), relationId));
                }
            }
        }
        
        if (deletedTemperatureCategory != null) {
            writer.write(String.format("delete(weatherParameter(\"temperature\", \"%s\")).\n", deletedTemperatureCategory));
        }
        
        if (deletedPrecipitationCategory != null) {
            writer.write(String.format("delete(weatherParameter(\"precipitation\", \"%s\")).\n", deletedPrecipitationCategory));
        }
    }

    // Writes new facts for map and weather data
    private void writeNewFacts(BufferedWriter writer, Node newClosestRoadNode, List<Node> newNodes, List<Way> newWays, List<Relation> newRelations, String newTemperatureCategory, String newPrecipitationCategory) throws IOException {
        if (newClosestRoadNode != null) {
            writer.write(String.format("add(position(%d)).\n", newClosestRoadNode.getId()));
        }

        for (Node node : newNodes) {
            Long nodeId = node.getId();
            writer.write(String.format("add(node(%d)).\n", nodeId));
            for (Map.Entry<String, String> tagEntry : node.getTags().entrySet()) {
                String key = replaceInvalidCharacters(tagEntry.getKey());
                String value = replaceInvalidCharacters(tagEntry.getValue());
                writer.write(String.format("add(nodeTag(%d, \"%s\", \"%s\")).\n", nodeId, key, value));
            }
        }

        for (Way way : newWays) {
            Long wayId = way.getId();
            writer.write(String.format("add(way(%d)).\n", wayId));
            for (Map.Entry<String, String> tagEntry : way.getTags().entrySet()) {
                String key = replaceInvalidCharacters(tagEntry.getKey());
                String value = replaceInvalidCharacters(tagEntry.getValue());
                writer.write(String.format("add(wayTag(%d, \"%s\", \"%s\")).\n", wayId, key, value));
            }
            List<Long> nodes = way.getNodeIds();
            for (int j = 0; j < nodes.size() - 1; j++) {
                writer.write(String.format("add(nextInWay(%d, %d, %d)).\n", nodes.get(j), nodes.get(j + 1), wayId));
            }
        }

        for (Relation relation : newRelations) {
            Long relationId = relation.getId();
            writer.write(String.format("add(relation(%d)).\n", relationId));
            for (Map.Entry<String, String> tagEntry : relation.getTags().entrySet()) {
                String key = replaceInvalidCharacters(tagEntry.getKey());
                String value = replaceInvalidCharacters(tagEntry.getValue());
                writer.write(String.format("add(relationTag(%d, \"%s\", \"%s\")).\n", relationId, key, value));
            }
            List<RelationMember> members = relation.getMembers();
            for (int j = 0; j < members.size() - 1; j++) {
                RelationMember member1 = members.get(j);
                RelationMember member2 = members.get(j + 1);
                writer.write(String.format("add(nextInRelation(%d, %d, %d)).\n", member1.getRef(), member2.getRef(), relationId));
                writer.write(String.format("add(relationMember(%d, \"%s\", \"%s\", %d)).\n", member1.getRef(), member1.getType().toString().toLowerCase(), member1.getRole(), relationId));
                if (j == members.size() - 2) {
                    writer.write(String.format("add(relationMember(%d, \"%s\", \"%s\", %d)).\n", member2.getRef(), member2.getType().toString().toLowerCase(), member2.getRole(), relationId));
                }
            }
        }
        
        if (newTemperatureCategory != null) {
            writer.write(String.format("add(weatherParameter(\"temperature\", \"%s\")).\n", newTemperatureCategory));
        }
        
        if (newPrecipitationCategory != null) {
            writer.write(String.format("add(weatherParameter(\"precipitation\", \"%s\")).\n", newPrecipitationCategory));
        }
    }


    // Streams GPS track, queries map and weather data, and triggers fact-writing at each trackpoint
    public void streamTrack() {
        previousMapHandler = new CustomMapDataHandler();
        previousWeatherHandler = new WeatherHandler();
        WayPoint previousPoint = null;

        while (nextPointExists()) {
            WayPoint currentPoint = getPoint();
            if (currentPoint != null) {
                mapHandler = new CustomMapDataHandler(currentPoint);
                weatherHandler = null;

                if (previousPoint != null) {
                    sleepBetweenPoints(previousPoint, currentPoint);
                }

                System.out.println("Access data around trackpoint(lat=" + currentPoint.getLatitude() +
                        ", lon=" + currentPoint.getLongitude() + ") at timestamp " + currentPoint.getTime().orElse(null));

                // Execute data queries and update facts based on changes
                queryAndUpdateFacts(currentPoint);
                previousMapHandler = mapHandler;
                previousWeatherHandler = weatherHandler;
                previousPoint = currentPoint;
            }
        }
        notifyEndOfStream();
    }

    // Sleeps for the duration between trackpoints to simulate driving
    private void sleepBetweenPoints(WayPoint previousPoint, WayPoint currentPoint) {
        Optional<Instant> previousTime = previousPoint.getTime();
        Optional<Instant> currentTime = currentPoint.getTime();

        if (previousTime.isPresent() && currentTime.isPresent()) {
            long sleepTime = Duration.between(previousTime.get(), currentTime.get()).toMillis();
            if (sleepTime > 0) {
            	System.out.println("Car is driving..");
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            }
        }
    }

    // Queries map and weather data and updates facts for the current track point
    private void queryAndUpdateFacts(WayPoint currentPoint) {
        // Query Overpass for map data
    	String query = "nw(around:" + RADIUS + "," + currentPoint.getLatitude() + "," + currentPoint.getLongitude() + "); out body;";
        overpass.queryElements(query, mapHandler);
        // Query Open-Meteo for weather data
		weatherHandler = weatherService.queryWeather(currentPoint.getTime().orElse(null).toString(), currentPoint.getLatitude().doubleValue(), currentPoint.getLongitude().doubleValue());
        
        String factsFilePath = "src/main/resources/sent_facts.pl";
        writeFacts(factsFilePath);
        if (streamListener != null) {
            streamListener.onFactsCreated(new File(factsFilePath));
        }
    }

    // Notifies end of stream to the listener
    private void notifyEndOfStream() {
        if (streamListener != null) {
            streamListener.onEndOfStream();
        }
    }

    public interface StreamListener {
        void onFactsCreated(File file);
        void onEndOfStream();
    }
}