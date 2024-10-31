package com.ai.gps_osm_project;

import java.io.*;
import java.net.*;

import com.ai.gps_osm_project.DataGenerator.StreamListener;

public class Server {

    private ServerSocket serverSocket; // Server socket that listens for incoming client connections
    private Socket clientSocket; // Socket for each connected client
    private PrintWriter out; // Output stream to send data to the client
    private BufferedReader in; // Input stream to receive data from the client
    private final int TIMEOUT = 20000; // Timeout period in milliseconds for waiting on client connections

    public Server(int port) {
        try {
            serverSocket = new ServerSocket(port); // Initialize the server on a specified port
            serverSocket.setSoTimeout(TIMEOUT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Runs the server to listen for incoming connections
    public void run() {
        System.out.println("Server is listening on port " + serverSocket.getLocalPort() + "..");
        while (true) {
            try {
                // Wait for a client to connect (blocks until a client connects or timeout occurs)
                clientSocket = serverSocket.accept();
                System.out.println("Client " + clientSocket + " connected");

                // Handle communication with the connected client
                communicateWithClient(clientSocket);
            } catch (SocketTimeoutException e) {
                System.out.println("No client connected within the timeout period. Shutting down the server.");
                break;  // Exit the loop and shut down the server after timeout
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Clean up by closing the server socket after exiting the loop
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Handles all communication with the client through the provided socket
    private void communicateWithClient(Socket clientSocket) {
        try {
        	// Set up input and output streams for client communication
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            // Initialize the data generator and set up a listener for streaming data
            DataGenerator dataGenerator = new DataGenerator();
            setUpStreamListener(dataGenerator);

            String clientMessage;
            boolean firstPrompt = true;  // Track if the initial prompt has been sent

            // Loop to continuously communicate with the client
            while (true) {
                // Send initial prompt if it is the first iteration
                if (firstPrompt) {
                    out.println("Enter 'start' to begin streaming or 'stop' to quit:");
                    firstPrompt = false;
                }

                // Read client input message
                clientMessage = in.readLine();
                if (clientMessage != null) {
                    System.out.println("Received from client: " + clientMessage);

                    // Handle 'stop' command: Valid input to terminate the connection
                    if (clientMessage.equals("stop")) {
                        out.println("Valid input");
                        System.out.println("Stopping server connection");
                        break;

                    // Handle 'start' command: Valid input to begin data streaming
                    } else if (clientMessage.equals("start")) {
                        out.println("Valid input");
                        System.out.println("Start streaming of GPS track");
                        new Thread(dataGenerator::streamTrack).start(); // Start streaming in a new thread

                    // Handle invalid input by prompting again
                    } else {
                        out.println("Invalid input. Please enter 'start' or 'stop':");
                    }

                // Handle client disconnection scenario
                } else {
                    System.out.println("Client disconnected");
                    break;  // Exit loop on client disconnection
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Clean up by closing client socket after communication ends
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Sets up the listener for data generation events
    private void setUpStreamListener(DataGenerator dataGenerator) {
        dataGenerator.setStreamListener(new StreamListener() {

            // Callback when facts are created: sends them to the client
            @Override
            public void onFactsCreated(File file) {
                sendFactsToClient(file);
            }

            // Callback for end-of-stream signal: informs the client of end of data
            @Override
            public void onEndOfStream() {
                out.println("EOS");
            }
        });
    }

    // Sends the facts data to the client by reading from a file line by line
    private void sendFactsToClient(File file) {
        try (BufferedReader fileReader = new BufferedReader(new FileReader(file))) {
            out.println("SENDING FACTS");  // Inform client that facts data is being sent
            String line;
            while ((line = fileReader.readLine()) != null) {
                out.println(line);
            }
            out.println("EOF");  // Send "End of File" signal after all lines are sent
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
       	 	file.delete();
        }
    }

    // Main method to start the server
    public static void main(String[] args) {
        Server server = new Server(5000);
        server.run();;
    }
}