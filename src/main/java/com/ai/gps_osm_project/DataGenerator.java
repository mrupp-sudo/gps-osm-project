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
    private final int RADIUS = 200; // Specify radius of accessed data around trackpoints
    
    private GPX gpx;
    private Stream<WayPoint> pointsStream;
    private Iterator<WayPoint> iterator;
    private OsmConnection connection;
    private OverpassMapDataApi overpass;

    private CustomMapDataHandler handler, previousHandler;
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

    // Writes facts by identifying changes in map data
    private void writeFacts(String factsFilePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(factsFilePath))) {
            List<Node> newNodes = handler.getNewNodes(previousHandler);
            List<Node> deletedNodes = handler.getDeletedNodes(previousHandler);
            
            List<Way> newWays = handler.getNewWays(previousHandler);
            List<Way> deletedWays = handler.getDeletedWays(previousHandler);
            
            List<Relation> newRelations = handler.getNewRelations(previousHandler);
            List<Relation> deletedRelations = handler.getDeletedRelations(previousHandler);
            
            Node closestRoadNode = handler.getClosestRoadNode();
            Node previousClosestRoadNode = previousHandler.getClosestRoadNode();

            // Write deleted facts
            writeDeletedFacts(writer, deletedNodes, deletedWays, deletedRelations, previousClosestRoadNode);
            
            // Write new facts
            writeNewFacts(writer, newNodes, newWays, newRelations, closestRoadNode);

            System.out.println("Datalog facts have been updated");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Writes deleted facts for nodes, ways, and relations
    private void writeDeletedFacts(BufferedWriter writer, List<Node> deletedNodes, List<Way> deletedWays, List<Relation> deletedRelations, Node previousClosestRoadNode) throws IOException {
        if (previousClosestRoadNode != null) {
            writer.write(String.format("delete(position(%d)).\n", previousClosestRoadNode.getId()));
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
    }

    // Writes new facts for nodes, ways, and relations
    private void writeNewFacts(BufferedWriter writer, List<Node> newNodes, List<Way> newWays, List<Relation> newRelations, Node closestRoadNode) throws IOException {
        if (closestRoadNode != null) {
            writer.write(String.format("add(position(%d)).\n", closestRoadNode.getId()));
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
    }


    // Streams track data, queries map data, and triggers fact-writing at each point
    public void streamTrack() {
        previousHandler = new CustomMapDataHandler();
        WayPoint previousPoint = null;

        while (nextPointExists()) {
            WayPoint currentPoint = getPoint();
            if (currentPoint != null) {
                handler = new CustomMapDataHandler(currentPoint);

                if (previousPoint != null) {
                    sleepBetweenPoints(previousPoint, currentPoint);
                }

                System.out.println("Access data around trackpoint(lat=" + currentPoint.getLatitude() +
                        ", lon=" + currentPoint.getLongitude() + ") at time " + currentPoint.getTime().orElse(null));

                // Query the area and update facts based on changes
                queryAndUpdateFacts(currentPoint);
                previousHandler = handler;
                previousPoint = currentPoint;
            }
        }
        notifyEndOfStream();
    }

    // Sleeps for the duration between points to simulate driving
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

    // Queries Overpass API and updates facts for the current track point
    private void queryAndUpdateFacts(WayPoint currentPoint) {
        String factsFilePath = "src/main/resources/sent_facts.pl";
        String query = "nwr(around:" + RADIUS + "," + currentPoint.getLatitude() + "," + currentPoint.getLongitude() + "); out body;";
        overpass.queryElements(query, handler);
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