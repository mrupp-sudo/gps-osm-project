package com.ai.gps_osm_project;

import org.apache.jena.rdf.model.*;
import org.apache.jena.reasoner.rulesys.*;
import org.apache.jena.vocabulary.RDF;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DatalogReasoner {

    private final String RULES_FILE_PATH = "src/main/resources/rdf_rules.txt"; // File path to rules
    private final String NAMESPACE = "http://example.org#"; // Namespace URI for resource definitions

    private Model model; // Core model for storing RDF data
    private InfModel infModel; // Inference model using rules

    public DatalogReasoner() {
        this.model = ModelFactory.createDefaultModel(); // Initialize core RDF model

        List<Rule> rulesList = Rule.rulesFromURL(RULES_FILE_PATH);

        GenericRuleReasoner reasoner = new GenericRuleReasoner(rulesList); // Initialize reasoner with loaded rules
        reasoner.setMode(GenericRuleReasoner.HYBRID); // Set to hybrid mode for efficiency

        this.infModel = ModelFactory.createInfModel(reasoner, this.model); // Initialize inference model
    }

    // Loads facts from a file, parses each fact, and applies it to the model
    public void loadFacts(String factFile) {
        try (BufferedReader br = new BufferedReader(new FileReader(factFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String fact = line.trim();

                // Processes 'add' or 'delete' facts by parsing and updating the model
                if (fact.startsWith("add(")) {
                    String actualFact = fact.substring(4, fact.length() - 1);
                    parseFact(actualFact, true);
                } else if (fact.startsWith("delete(")) {
                    String actualFact = fact.substring(7, fact.length() - 1);
                    parseFact(actualFact, false);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Queries the inference model and prints inferred facts
    public void queryInferences() {
        System.out.println("Inferred Yield Signs:");
        Resource yieldSign = infModel.createResource(NAMESPACE + "yieldSign");
        infModel.listSubjectsWithProperty(RDF.type, yieldSign)
            .forEachRemaining(resource -> System.out.println(resource + " is a Yield Sign"));
        
        System.out.println("\nInferred Stop Signs:");
        Resource stopSign = infModel.createResource(NAMESPACE + "stopSign");
        infModel.listSubjectsWithProperty(RDF.type, stopSign)
            .forEachRemaining(resource -> System.out.println(resource + " is a Stop Sign"));
        
        System.out.println("\nInferred Traffic Signals:");
        Resource trafficSignal = infModel.createResource(NAMESPACE + "trafficSignal");
        infModel.listSubjectsWithProperty(RDF.type, trafficSignal)
            .forEachRemaining(resource -> System.out.println(resource + " is a Traffic Signal"));
        
        System.out.println("\nInferred Pedestrian Crossings:");
        Resource pedestrianCrossing = infModel.createResource(NAMESPACE + "pedestrianCrossing");
        infModel.listSubjectsWithProperty(RDF.type, pedestrianCrossing)
            .forEachRemaining(resource -> System.out.println(resource + " is a Pedestrian Crossing"));
        
        System.out.println("\nInferred Tram Crossings:");
        Resource tramCrossing = infModel.createResource(NAMESPACE + "tramCrossing");
        infModel.listSubjectsWithProperty(RDF.type, tramCrossing)
            .forEachRemaining(resource -> System.out.println(resource + " is a Tram Crossing"));
        
        System.out.println("\nInferred Train Crossings:");
        Resource trainCrossing = infModel.createResource(NAMESPACE + "trainCrossing");
        infModel.listSubjectsWithProperty(RDF.type, trainCrossing)
            .forEachRemaining(resource -> System.out.println(resource + " is a Train Crossing"));
        
        System.out.println("\nInferred Bus Stations:");
        Resource busStation = infModel.createResource(NAMESPACE + "busStation");
        infModel.listSubjectsWithProperty(RDF.type, busStation)
            .forEachRemaining(resource -> System.out.println(resource + " is a Bus Station"));
        
        System.out.println("\nInferred Tram Stations:");
        Resource tramStation = infModel.createResource(NAMESPACE + "tramStation");
        infModel.listSubjectsWithProperty(RDF.type, tramStation)
            .forEachRemaining(resource -> System.out.println(resource + " is a Tram Station"));
        
        System.out.println("\nInferred Intermodal Stations:");
        Resource intermodalStation = infModel.createResource(NAMESPACE + "intermodalStation");
        infModel.listSubjectsWithProperty(RDF.type, intermodalStation)
            .forEachRemaining(resource -> System.out.println(resource + " is an Intermodal Station"));
        
        System.out.println("\nInferred Kindergartens:");
        Resource kindergarten = infModel.createResource(NAMESPACE + "kindergarten");
        infModel.listSubjectsWithProperty(RDF.type, kindergarten)
            .forEachRemaining(resource -> System.out.println(resource + " is a Kindergarten"));
        
        System.out.println("\nInferred Schools:");
        Resource school = infModel.createResource(NAMESPACE + "school");
        infModel.listSubjectsWithProperty(RDF.type, school)
            .forEachRemaining(resource -> System.out.println(resource + " is a School"));
    }

    // Parses individual fact strings and determines if they should be added or removed
    private void parseFact(String fact, boolean isAdded) {
        if (fact.startsWith("position(")) {
            String id = fact.substring(9, fact.length() - 2);
            Resource position = infModel.createResource(NAMESPACE + id);
            Statement statement = infModel.createStatement(position, RDF.type, infModel.createResource(NAMESPACE + "position"));
            processFact(statement, isAdded);

        } else if (fact.startsWith("node(")) {
            String id = fact.substring(5, fact.length() - 2);
            Resource node = infModel.createResource(NAMESPACE + id);
            Statement statement = infModel.createStatement(node, RDF.type, infModel.createResource(NAMESPACE + "node"));
            processFact(statement, isAdded);

        } else if (fact.startsWith("nodeTag(")) {
            String[] parts = smartSplit(fact.substring(8, fact.length() - 2));
            Resource node = infModel.createResource(NAMESPACE + parts[0]);

            Resource tag = infModel.createResource();

            Statement statement1 = infModel.createStatement(node, infModel.createProperty(NAMESPACE + "nodeTag"), tag);
            Statement statement2 = infModel.createStatement(tag, infModel.createProperty(NAMESPACE + "key"), parts[1].replace("\"", ""));
            Statement statement3 = infModel.createStatement(tag, infModel.createProperty(NAMESPACE + "value"), parts[2].replace("\"", ""));
            processFact(statement1, isAdded);
            processFact(statement2, isAdded);
            processFact(statement3, isAdded);

        } else if (fact.startsWith("way(")) {
            String id = fact.substring(4, fact.length() - 2);
            Resource way = infModel.createResource(NAMESPACE + id);
            Statement statement = infModel.createStatement(way, RDF.type, infModel.createResource(NAMESPACE + "way"));
            processFact(statement, isAdded);

        } else if (fact.startsWith("wayTag(")) {
            String[] parts = smartSplit(fact.substring(7, fact.length() - 2));
            Resource way = infModel.createResource(NAMESPACE + parts[0]);

            Resource tag = infModel.createResource();

            Statement statement1 = infModel.createStatement(way, infModel.createProperty(NAMESPACE + "wayTag"), tag);
            Statement statement2 = infModel.createStatement(tag, infModel.createProperty(NAMESPACE + "key"), parts[1].replace("\"", ""));
            Statement statement3 = infModel.createStatement(tag, infModel.createProperty(NAMESPACE + "value"), parts[2].replace("\"", ""));
            processFact(statement1, isAdded);
            processFact(statement2, isAdded);
            processFact(statement3, isAdded);

        } else if (fact.startsWith("nextInWay(")) {
            String[] parts = fact.substring(10, fact.length() - 2).split(", ");
            Resource fromNode = infModel.createResource(NAMESPACE + parts[0]);
            Resource toNode = infModel.createResource(NAMESPACE + parts[1]);
            Resource way = infModel.createResource(NAMESPACE + parts[2]);

            Resource nextInWay = infModel.createResource();
            Statement statement1 = infModel.createStatement(nextInWay, infModel.createProperty(NAMESPACE + "niw:fromNode"), fromNode);
            Statement statement2 = infModel.createStatement(nextInWay, infModel.createProperty(NAMESPACE + "niw:toNode"), toNode);
            Statement statement3 = infModel.createStatement(nextInWay, infModel.createProperty(NAMESPACE + "niw:inWay"), way);
            processFact(statement1, isAdded);
            processFact(statement2, isAdded);
            processFact(statement3, isAdded);

        } else if (fact.startsWith("relation(")) {
            String id = fact.substring(9, fact.length() - 2);
            Resource relation = infModel.createResource(NAMESPACE + id);
            Statement statement = infModel.createStatement(relation, RDF.type, infModel.createResource(NAMESPACE + "relation"));
            processFact(statement, isAdded);

        } else if (fact.startsWith("relationTag(")) {
            String[] parts = smartSplit(fact.substring(12, fact.length() - 2));
            Resource relation = infModel.createResource(NAMESPACE + parts[0]);

            Resource tag = infModel.createResource();

            Statement statement1 = infModel.createStatement(relation, infModel.createProperty(NAMESPACE + "relationTag"), tag);
            Statement statement2 = infModel.createStatement(tag, infModel.createProperty(NAMESPACE + "key"), parts[1].replace("\"", ""));
            Statement statement3 = infModel.createStatement(tag, infModel.createProperty(NAMESPACE + "value"), parts[2].replace("\"", ""));
            processFact(statement1, isAdded);
            processFact(statement2, isAdded);
            processFact(statement3, isAdded);

        } else if (fact.startsWith("nextInRelation(")) {
            String[] parts = fact.substring(15, fact.length() - 2).split(", ");
            Resource fromMember = infModel.createResource(NAMESPACE + parts[0]);
            Resource toMember = infModel.createResource(NAMESPACE + parts[1]);
            Resource relation = infModel.createResource(NAMESPACE + parts[2]);

            Resource nextInRelation = infModel.createResource();
            Statement statement1 = infModel.createStatement(nextInRelation, infModel.createProperty(NAMESPACE + "fromMember"), fromMember);
            Statement statement2 = infModel.createStatement(nextInRelation, infModel.createProperty(NAMESPACE + "toMember"), toMember);
            Statement statement3 = infModel.createStatement(nextInRelation, infModel.createProperty(NAMESPACE + "inRelation"), relation);
            processFact(statement1, isAdded);
            processFact(statement2, isAdded);
            processFact(statement3, isAdded);

        } else if (fact.startsWith("relationMember(")) {
            String[] parts = fact.substring(15, fact.length() - 2).split(", ");
            Resource member = infModel.createResource(NAMESPACE + parts[0]);
            Resource relation = infModel.createResource(NAMESPACE + parts[3]);

            Resource relationMember = infModel.createResource();
            Statement statement1 = infModel.createStatement(relationMember, infModel.createProperty(NAMESPACE + "member"), member);
            Statement statement2 = infModel.createStatement(relationMember, infModel.createProperty(NAMESPACE + "element"), parts[1].replace("\"", ""));
            Statement statement3 = infModel.createStatement(relationMember, infModel.createProperty(NAMESPACE + "role"), parts[2].replace("\"", ""));
            Statement statement4 = infModel.createStatement(relationMember, infModel.createProperty(NAMESPACE + "relation"), relation);
            processFact(statement1, isAdded);
            processFact(statement2, isAdded);
            processFact(statement3, isAdded);
            processFact(statement4, isAdded);
        }
    }

    // Adds or removes statements from the model
    private void processFact(Statement statement, boolean isAdded) {
        if (isAdded) {
            infModel.add(statement);
        } else {
            infModel.remove(statement);
        }
    }

    // Splits a fact string intelligently, accounting for quotes around strings
    private String[] smartSplit(String input) {
        List<String> parts = new ArrayList<>();
        boolean insideQuotes = false;
        StringBuilder currentPart = new StringBuilder();

        for (char c : input.toCharArray()) {
            if (c == '"') {
                insideQuotes = !insideQuotes;
            } else if (c == ',' && !insideQuotes) {
                parts.add(currentPart.toString().trim());
                currentPart.setLength(0);
            } else {
                currentPart.append(c);
            }
        }
        parts.add(currentPart.toString().trim());

        return parts.toArray(new String[0]);
    }
}