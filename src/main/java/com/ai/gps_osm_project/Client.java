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

    // Connect to the server at the specified host and port
    public void connect(String host, int port) {
        try {
            socket = new Socket(host, port); // Establish a socket connection to the server
            System.out.println("CLIENT: Client connected to server [" + host + " on port " + port + "]");
            
            // Set up input and output streams for server communication
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Initiate communication protocol with the server
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

    // Manage communication with the server, handling inputs and outputs
    private void communicateWithServer() {
        try {
            String serverMessage;
            boolean validInput = false; // Track if the server accepted the user's input

            // Continuously listen for messages from the server
            while (true) {
                serverMessage = in.readLine();
                if (serverMessage != null) {

                    // Check if server indicates valid input received
                    if (serverMessage.equals("Valid input")) {
                        validInput = true;
                    }

                    // Prompt user for input if valid input has not been received
                    if (!validInput) {
                        System.out.println("CLIENT: " + serverMessage);
                        String userInput = getUserInput();      
                        out.println(userInput);                 
                    }

                    // Initiate fact data streaming from server if indicated
                    if (serverMessage.equals("SENDING FACTS")) {
                        System.out.println("CLIENT: Receiving and processing facts");
                        receiveAndProcessFacts();
                    } else if (serverMessage.equals("EOS")) {
                    	System.out.println("CLIENT: End-of-stream signal received, disconnecting from the server");
                        break;
                    }
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Collect user input from the console for server communication
    private String getUserInput() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            return reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Receive streamed facts from the server, write to file, and process with reasoner
    private void receiveAndProcessFacts() {
        String factFilePath = "src/main/resources/received_facts.txt";
        try {
            String line;
            BufferedWriter fileWriter = new BufferedWriter(new FileWriter(factFilePath));

            // Write each fact line from the server to the file
            while (!(line = in.readLine()).equals("EOF")) {
                fileWriter.write(line);
                fileWriter.newLine();
            }
            fileWriter.close();

            // Load facts into reasoner and perform inference operations
            datalogReasoner.loadFacts(factFilePath);
            datalogReasoner.queryInferences();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
        	 File factFile = new File(factFilePath);
        	 factFile.delete();
        }
    }
}