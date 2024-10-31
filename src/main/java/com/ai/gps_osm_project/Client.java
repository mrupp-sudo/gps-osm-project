package com.ai.gps_osm_project;

import java.io.*;
import java.net.*;

public class Client {

    private Socket socket;                // Socket for connecting to the server
    private PrintWriter out;              // Output stream to send data to the server
    private BufferedReader in;            // Input stream to receive data from the server
    private DatalogReasoner datalogReasoner;

    public Client() {
    	// Initialize the datalog reasoner for inference processing
        this.datalogReasoner = new DatalogReasoner();
    }

    // Connects to the server at the specified host and port
    private void connect(String host, int port) {
        try {
            socket = new Socket(host, port); // Establishes a socket connection to the server
            System.out.println("Client connected to server: " + host + " on port " + port);
            
            // Set up input and output streams for server communication
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Initiates communication protocol with the server
            communicateWithServer();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Manages communication with the server, handling inputs and outputs
    private void communicateWithServer() {
        try {
            String serverMessage;
            boolean validInput = false; // Tracks if the server accepted the user's input

            // Continuously listens for messages from the server
            while (true) {
                serverMessage = in.readLine();
                if (serverMessage != null) {

                    // Checks if server indicates valid input received
                    if (serverMessage.equals("Valid input")) {
                        validInput = true;
                    }

                    // Prompts user for input if valid input has not been received
                    if (!validInput) {
                        System.out.println(serverMessage);
                        String userInput = getUserInput();      
                        out.println(userInput);                 
                    }

                    // Initiates fact data streaming from server if indicated
                    if (serverMessage.equals("SENDING FACTS")) {
                        System.out.println("\nReceiving and processing facts..");
                        receiveAndProcessFacts();
                    } else if (serverMessage.equals("EOS")) {
                        System.out.println("\nEnd of stream reached.");
                        break;
                    }
                } else {
                    System.out.println("Server disconnected.");
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Collects user input from the console for server communication
    private String getUserInput() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            return reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Receives streamed facts from the server, writes to file, and processes with reasoner
    private void receiveAndProcessFacts() {
        String factFilePath = "src/main/resources/received_facts.txt";
        try {
            String line;
            BufferedWriter fileWriter = new BufferedWriter(new FileWriter(factFilePath));

            // Writes each fact line from the server to the file
            while (!(line = in.readLine()).equals("EOF")) {
                fileWriter.write(line);
                fileWriter.newLine();
            }
            fileWriter.close();

            // Loads facts into reasoner and performs inference operations
            datalogReasoner.loadFacts(factFilePath);
            datalogReasoner.queryInferences();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
        	 File factFile = new File(factFilePath);
        	 factFile.delete();
        }
    }

    // Main method to connect to the server
    public static void main(String[] args) {
        Client client = new Client();
        client.connect("localhost", 5000);
    }
}