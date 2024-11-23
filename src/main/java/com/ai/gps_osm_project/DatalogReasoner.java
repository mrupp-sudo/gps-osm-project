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
    
    static final String GREEN = "\u001B[92m";
    static final String RESET = "\u001B[0m";

    private Model model; // Core model for storing RDF data
    private InfModel infModel; // Inference model using rules

    public DatalogReasoner() {
        this.model = ModelFactory.createDefaultModel(); // Initialize core RDF model

        List<Rule> rulesList = Rule.rulesFromURL(RULES_FILE_PATH);

        GenericRuleReasoner reasoner = new GenericRuleReasoner(rulesList); // Initialize reasoner with loaded rules
        reasoner.setMode(GenericRuleReasoner.HYBRID); // Set to hybrid mode for efficiency

        this.infModel = ModelFactory.createInfModel(reasoner, this.model); // Initialize inference model
    }
    
    // Load facts from a file, parse each fact, and apply it to the model
    public void loadFacts(String factFilePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(factFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String fact = line.trim();
                // Process 'add' or 'delete' facts by parsing and updating the model
                if (fact.startsWith("add(")) {
                    String actualFact = fact.substring(4, fact.length() - 1);
                    infModel.add(parseFact(actualFact));
                } else if (fact.startsWith("delete(")) {
                    String actualFact = fact.substring(7, fact.length() - 1);
                    infModel.remove(parseFact(actualFact));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Query the inference model and print inferred facts
    public void queryInferences() {
        System.out.println("CLIENT: Inferred Yield Signs:");
        Resource yieldSign = infModel.createResource(NAMESPACE + "yieldSign");
        infModel.listSubjectsWithProperty(RDF.type, yieldSign)
            .forEachRemaining(resource -> System.out.println("    " + GREEN + stripNamespace(resource) + RESET));
        
        System.out.println("CLIENT: Inferred Stop Signs:");
        Resource stopSign = infModel.createResource(NAMESPACE + "stopSign");
        infModel.listSubjectsWithProperty(RDF.type, stopSign)
            .forEachRemaining(resource -> System.out.println("    " + GREEN + stripNamespace(resource) + RESET));
        
        System.out.println("CLIENT: Inferred Traffic Signals:");
        Resource trafficSignal = infModel.createResource(NAMESPACE + "trafficSignal");
        infModel.listSubjectsWithProperty(RDF.type, trafficSignal)
            .forEachRemaining(resource -> System.out.println("    " + GREEN + stripNamespace(resource) + RESET));
        
        System.out.println("CLIENT: Inferred Pedestrian Crossings:");
        Resource pedestrianCrossing = infModel.createResource(NAMESPACE + "pedestrianCrossing");
        infModel.listSubjectsWithProperty(RDF.type, pedestrianCrossing)
            .forEachRemaining(resource -> System.out.println("    " + GREEN + stripNamespace(resource) + RESET));
        
        System.out.println("CLIENT: Inferred Tram Crossings:");
        Resource tramCrossing = infModel.createResource(NAMESPACE + "tramCrossing");
        infModel.listSubjectsWithProperty(RDF.type, tramCrossing)
            .forEachRemaining(resource -> System.out.println("    " + GREEN + stripNamespace(resource) + RESET));
        
        System.out.println("CLIENT: Inferred Train Crossings:");
        Resource trainCrossing = infModel.createResource(NAMESPACE + "trainCrossing");
        infModel.listSubjectsWithProperty(RDF.type, trainCrossing)
            .forEachRemaining(resource -> System.out.println("    " + GREEN + stripNamespace(resource) + RESET));
        
        System.out.println("CLIENT: Inferred Bus Stations:");
        Resource busStation = infModel.createResource(NAMESPACE + "busStation");
        infModel.listSubjectsWithProperty(RDF.type, busStation)
            .forEachRemaining(resource -> System.out.println("    " + GREEN + stripNamespace(resource) + RESET));
        
        System.out.println("CLIENT: Inferred Tram Stations:");
        Resource tramStation = infModel.createResource(NAMESPACE + "tramStation");
        infModel.listSubjectsWithProperty(RDF.type, tramStation)
            .forEachRemaining(resource -> System.out.println("    " + GREEN + stripNamespace(resource) + RESET));
        
        System.out.println("CLIENT: Inferred Intermodal Stations:");
        Resource intermodalStation = infModel.createResource(NAMESPACE + "intermodalStation");
        infModel.listSubjectsWithProperty(RDF.type, intermodalStation)
            .forEachRemaining(resource -> System.out.println("    " + GREEN + stripNamespace(resource) + RESET));
        
        System.out.println("CLIENT: Inferred Kindergartens:");
        Resource kindergarten = infModel.createResource(NAMESPACE + "kindergarten");
        infModel.listSubjectsWithProperty(RDF.type, kindergarten)
            .forEachRemaining(resource -> System.out.println("    " + GREEN + stripNamespace(resource) + RESET));
        
        System.out.println("CLIENT: Inferred Schools:");
        Resource school = infModel.createResource(NAMESPACE + "school");
        infModel.listSubjectsWithProperty(RDF.type, school)
            .forEachRemaining(resource -> System.out.println("    " + GREEN + stripNamespace(resource) + RESET));
        
        System.out.println("CLIENT: Inferred Weather Condition:");
        Property weatherCondition = infModel.createProperty(NAMESPACE + "weatherCondition");
        infModel.listObjectsOfProperty(weatherCondition)
            .forEachRemaining(resource -> System.out.println("    " + GREEN + resource + RESET));
    }
    
    // Strip the namespace from a resource URI
    private String stripNamespace(Resource resource) {
    	String uri = resource.toString();
        if (uri.startsWith(NAMESPACE)) {
            return uri.substring(NAMESPACE.length());
        }
        return uri;
    }

    // Parse individual fact strings
    private Statement[] parseFact(String fact) {
        if (fact.startsWith("position(")) {
            String id = fact.substring(9, fact.length() - 2);
            Resource node = infModel.createResource(NAMESPACE + id);
            
            Statement statement = infModel.createStatement(node, RDF.type, infModel.createResource(NAMESPACE + "position"));
            return new Statement[]{statement};
            
        } else if (fact.startsWith("node(")) {
            String id = fact.substring(5, fact.length() - 2);
            Resource node = infModel.createResource(NAMESPACE + id);
            
            Statement statement = infModel.createStatement(node, RDF.type, infModel.createResource(NAMESPACE + "node"));
            return new Statement[]{statement};
            
        } else if (fact.startsWith("nodeTag(")) {
            String[] parts = smartSplit(fact.substring(8, fact.length() - 2));
            Resource node = infModel.createResource(NAMESPACE + parts[0]);
            Resource tag = infModel.createResource();

            Statement statement1 = infModel.createStatement(node, infModel.createProperty(NAMESPACE + "nodeTag"), tag);
            Statement statement2 = infModel.createStatement(tag, infModel.createProperty(NAMESPACE + "tagKey"), parts[1].replace("\"", ""));
            Statement statement3 = infModel.createStatement(tag, infModel.createProperty(NAMESPACE + "tagValue"), parts[2].replace("\"", ""));
            return new Statement[]{statement1, statement2, statement3};

        } else if (fact.startsWith("way(")) {
            String id = fact.substring(4, fact.length() - 2);
            Resource way = infModel.createResource(NAMESPACE + id);
            
            Statement statement = infModel.createStatement(way, RDF.type, infModel.createResource(NAMESPACE + "way"));
            return new Statement[]{statement};

        } else if (fact.startsWith("wayTag(")) {
            String[] parts = smartSplit(fact.substring(7, fact.length() - 2));
            Resource way = infModel.createResource(NAMESPACE + parts[0]);
            Resource tag = infModel.createResource();

            Statement statement1 = infModel.createStatement(way, infModel.createProperty(NAMESPACE + "wayTag"), tag);
            Statement statement2 = infModel.createStatement(tag, infModel.createProperty(NAMESPACE + "tagKey"), parts[1].replace("\"", ""));
            Statement statement3 = infModel.createStatement(tag, infModel.createProperty(NAMESPACE + "tagValue"), parts[2].replace("\"", ""));
            return new Statement[]{statement1, statement2, statement3};

        } else if (fact.startsWith("nextInWay(")) {
            String[] parts = fact.substring(10, fact.length() - 2).split(", ");
            Resource fromNode = infModel.createResource(NAMESPACE + parts[0]);
            Resource toNode = infModel.createResource(NAMESPACE + parts[1]);
            Resource way = infModel.createResource(NAMESPACE + parts[2]);
            Resource nextInWay = infModel.createResource();
            
            Statement statement1 = infModel.createStatement(nextInWay, infModel.createProperty(NAMESPACE + "niw:fromNode"), fromNode);
            Statement statement2 = infModel.createStatement(nextInWay, infModel.createProperty(NAMESPACE + "niw:toNode"), toNode);
            Statement statement3 = infModel.createStatement(nextInWay, infModel.createProperty(NAMESPACE + "niw:inWay"), way);
            return new Statement[]{statement1, statement2, statement3};

        } else if (fact.startsWith("relation(")) {
            String id = fact.substring(9, fact.length() - 2);
            Resource relation = infModel.createResource(NAMESPACE + id);
            
            Statement statement = infModel.createStatement(relation, RDF.type, infModel.createResource(NAMESPACE + "relation"));
            return new Statement[]{statement};

        } else if (fact.startsWith("relationTag(")) {
            String[] parts = smartSplit(fact.substring(12, fact.length() - 2));
            Resource relation = infModel.createResource(NAMESPACE + parts[0]);
            Resource tag = infModel.createResource();

            Statement statement1 = infModel.createStatement(relation, infModel.createProperty(NAMESPACE + "relationTag"), tag);
            Statement statement2 = infModel.createStatement(tag, infModel.createProperty(NAMESPACE + "tagKey"), parts[1].replace("\"", ""));
            Statement statement3 = infModel.createStatement(tag, infModel.createProperty(NAMESPACE + "tagValue"), parts[2].replace("\"", ""));
            return new Statement[]{statement1, statement2, statement3};

        } else if (fact.startsWith("nextInRelation(")) {
            String[] parts = fact.substring(15, fact.length() - 2).split(", ");
            Resource fromMember = infModel.createResource(NAMESPACE + parts[0]);
            Resource toMember = infModel.createResource(NAMESPACE + parts[1]);
            Resource relation = infModel.createResource(NAMESPACE + parts[2]);
            Resource nextInRelation = infModel.createResource();
            
            Statement statement1 = infModel.createStatement(nextInRelation, infModel.createProperty(NAMESPACE + "fromMember"), fromMember);
            Statement statement2 = infModel.createStatement(nextInRelation, infModel.createProperty(NAMESPACE + "toMember"), toMember);
            Statement statement3 = infModel.createStatement(nextInRelation, infModel.createProperty(NAMESPACE + "inRelation"), relation);
            return new Statement[]{statement1, statement2, statement3};

        } else if (fact.startsWith("relationMember(")) {
            String[] parts = fact.substring(15, fact.length() - 2).split(", ");
            Resource member = infModel.createResource(NAMESPACE + parts[0]);
            Resource relation = infModel.createResource(NAMESPACE + parts[3]);
            Resource relationMember = infModel.createResource();
            
            Statement statement1 = infModel.createStatement(relationMember, infModel.createProperty(NAMESPACE + "member"), member);
            Statement statement2 = infModel.createStatement(relationMember, infModel.createProperty(NAMESPACE + "element"), parts[1].replace("\"", ""));
            Statement statement3 = infModel.createStatement(relationMember, infModel.createProperty(NAMESPACE + "role"), parts[2].replace("\"", ""));
            Statement statement4 = infModel.createStatement(relationMember, infModel.createProperty(NAMESPACE + "relation"), relation);
            return new Statement[]{statement1, statement2, statement3, statement4};
        }
        
        else if (fact.startsWith("weatherParameter(")) {
            String[] parts = fact.substring(17, fact.length() - 2).split(", ");
            Resource weatherParameter = infModel.createResource();
            
            Statement statement1 = infModel.createStatement(weatherParameter, infModel.createProperty(NAMESPACE + "parameterKey"), parts[0].replace("\"", ""));
            Statement statement2 = infModel.createStatement(weatherParameter, infModel.createProperty(NAMESPACE + "parameterValue"), parts[1].replace("\"", ""));
            return new Statement[]{statement1, statement2};
        }
        return null;
    }

    // Split a fact string intelligently, accounting for quotes around strings
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