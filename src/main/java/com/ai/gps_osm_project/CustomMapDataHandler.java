package com.ai.gps_osm_project;

import de.westnordost.osmapi.map.data.BoundingBox;
import de.westnordost.osmapi.map.data.LatLon;
import de.westnordost.osmapi.map.data.Node;
import de.westnordost.osmapi.map.data.Relation;
import de.westnordost.osmapi.map.data.Way;
import de.westnordost.osmapi.map.handler.MapDataHandler;
import io.jenetics.jpx.Length;
import io.jenetics.jpx.WayPoint;
import io.jenetics.jpx.geom.Geoid;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CustomMapDataHandler implements MapDataHandler {

    private WayPoint point; // WayPoint for calculating distances to other nodes (e.g. from the current location)
    // Lists for storing accessed data about nodes, ways, and relations in the map
    private List<Node> nodes;
    private List<Way> ways;
    private List<Relation> relations;
    private Node closestRoadNode; // Store the closest road node to the given trackpoint

    public CustomMapDataHandler() {
        nodes = new ArrayList<>();
        ways = new ArrayList<>();
        relations = new ArrayList<>();
    }

    public CustomMapDataHandler(WayPoint point) {
        this.point = point;
        nodes = new ArrayList<>();
        ways = new ArrayList<>();
        relations = new ArrayList<>();
    }

    @Override
    public void handle(BoundingBox bounds) {
        // Placeholder for handling BoundingBox
    }

    // Add the provided node to the list of nodes
    @Override
    public void handle(Node node) {
        nodes.add(node);
    }

    // Add the provided way to the list of ways
    @Override
    public void handle(Way way) {
        ways.add(way);
    }

    // Add the provided relation to the list of relations
    @Override
    public void handle(Relation relation) {
        relations.add(relation);
    }

    // Return the list of nodes in the map
    public List<Node> getNodes() {
        return nodes;
    }

    // Return the list of ways in the map
    public List<Way> getWays() {
        return ways;
    }

    // Return the list of relations in the map
    public List<Relation> getRelations() {
        return relations;
    }

    // Find nodes present in the current map data that are not in the data of the other handler
    public List<Node> getNewNodes(CustomMapDataHandler other) {
        Set<Long> otherNodeIds = new HashSet<>();
        for (Node otherNode : other.getNodes()) {
            otherNodeIds.add(otherNode.getId());
        }

        List<Node> newNodes = new ArrayList<>();
        for (Node node : nodes) {
            if (!otherNodeIds.contains(node.getId())) {
                newNodes.add(node);
            }
        }
        return newNodes; // List of nodes unique to the current handler
    }

    // Find nodes that are in the other handler but not in the current map data
    public List<Node> getDeletedNodes(CustomMapDataHandler other) {
        Set<Long> currentNodeIds = new HashSet<>();
        for (Node node : nodes) {
            currentNodeIds.add(node.getId());
        }

        List<Node> deletedNodes = new ArrayList<>();
        for (Node otherNode : other.getNodes()) {
            if (!currentNodeIds.contains(otherNode.getId())) {
                deletedNodes.add(otherNode);
            }
        }
        return deletedNodes; // List of nodes removed from the current handler
    }

    // Find ways present in the current map data that are not in the data of the other handler
    public List<Way> getNewWays(CustomMapDataHandler other) {
        Set<Long> otherWayIds = new HashSet<>();
        for (Way way : other.getWays()) {
            otherWayIds.add(way.getId());
        }

        List<Way> newWays = new ArrayList<>();
        for (Way way : ways) {
            if (!otherWayIds.contains(way.getId())) {
                newWays.add(way);
            }
        }
        return newWays; // List of ways unique to the current handler
    }

    // Find ways that are in the other handler but not in the current map data
    public List<Way> getDeletedWays(CustomMapDataHandler other) {
        Set<Long> currentWayIds = new HashSet<>();
        for (Way way : ways) {
            currentWayIds.add(way.getId());
        }

        List<Way> deletedWays = new ArrayList<>();
        for (Way way : other.getWays()) {
            if (!currentWayIds.contains(way.getId())) {
                deletedWays.add(way);
            }
        }
        return deletedWays; // List of ways removed from the current handler
    }

    // Find relations present in the current map data that are not in the data of the other handler
    public List<Relation> getNewRelations(CustomMapDataHandler other) {
        Set<Long> otherRelationIds = new HashSet<>();
        for (Relation relation : other.getRelations()) {
            otherRelationIds.add(relation.getId());
        }

        List<Relation> newRelations = new ArrayList<>();
        for (Relation relation : relations) {
            if (!otherRelationIds.contains(relation.getId())) {
                newRelations.add(relation);
            }
        }
        return newRelations; // List of relations unique to the current handler
    }

    // Find relations that are in the other handler but not in the current map data
    public List<Relation> getDeletedRelations(CustomMapDataHandler other) {
        Set<Long> currentRelationIds = new HashSet<>();
        for (Relation relation : relations) {
            currentRelationIds.add(relation.getId());
        }

        List<Relation> deletedRelations = new ArrayList<>();
        for (Relation relation : other.getRelations()) {
            if (!currentRelationIds.contains(relation.getId())) {
                deletedRelations.add(relation);
            }
        }
        return deletedRelations; // List of relations removed from the current handler
    }

    // Find the closest road node to the given trackpoint, filtering only suitable car roads
    public Node findClosestRoadNode() {
        closestRoadNode = null;
        double shortestDistance = Double.MAX_VALUE;

        // Check each way to identify car roads and calculate distances to the point
        for (Way way : ways) {
            Map<String, String> tags = way.getTags();

            // Filter only car roads
            if (isCarRoad(tags)) {
                List<Long> nodeIdsInWay = way.getNodeIds();

                // Iterate over nodes in this way to find the closest node to the point
                for (Long nodeId : nodeIdsInWay) {
                    Node node = findNodeById(nodes, nodeId);
                    if (node == null) continue;

                    LatLon nodePosition = node.getPosition();
                    Length distance = Geoid.WGS84.distance(point, WayPoint.of(nodePosition.getLatitude(), nodePosition.getLongitude()));

                    // Update if a shorter distance is found
                    if (distance.doubleValue() < shortestDistance) {
                        shortestDistance = distance.doubleValue();
                        closestRoadNode = node;
                    }
                }
            }
        }
        return closestRoadNode; // Return the closest road node, if found
    }

    // Check if a way is suitable for cars based on tags and access restrictions
    private boolean isCarRoad(Map<String, String> tags) {
        String highway = tags.get("highway");
        if (highway == null) return false;

        // Determine if it is a car road based on standard classifications
        boolean isCarRoad = highway.equals("motorway") || highway.equals("trunk") ||
                            highway.equals("primary") || highway.equals("secondary") ||
                            highway.equals("tertiary") || highway.equals("unclassified") ||
                            highway.equals("residential") || highway.equals("motorway_link") ||
                            highway.equals("trunk_link") || highway.equals("primary_link") ||
                            highway.equals("secondary_link") || highway.equals("tertiary_link") ||
                            highway.equals("living_street") || highway.equals("service");

        // Exit early if not a car road
        if (!isCarRoad) {
        	return false;
        }

        // Verify access permissions
        String motorVehicle = tags.get("motor_vehicle");
        if (motorVehicle != null && (motorVehicle.equals("no") || motorVehicle.equals("private") || motorVehicle.equals("psv"))) {
            return false;
        }

        String vehicle = tags.get("vehicle");
        if (vehicle != null && (vehicle.equals("no") || vehicle.equals("private"))) {
            return false;
        }

        String access = tags.get("access");
        if (access != null && (access.equals("no") || access.equals("private"))) {
            return false;
        }

        return true; // If all checks pass, it is a valid car road
    }

    // Find a node by its ID in the list of nodes
    private Node findNodeById(List<Node> nodes, long nodeId) {
        for (Node node : nodes) {
            if (node.getId() == nodeId) {
                return node; // Node found, return it
            }
        }
        return null; // Node not found, return null
    }
    
    // Return the closest road node to the given trackpoint
    public Node getClosestRoadNode() {
        return closestRoadNode;
    }
}