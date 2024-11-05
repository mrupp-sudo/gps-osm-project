package com.ai.gps_osm_project;

public class Main {

	public static void main(String[] args) {
      // Start the server in a separate thread
      Server server = new Server(5000);
      Thread serverThread = new Thread(server::run);
      serverThread.start();

      // Start the client in another thread
      Client client = new Client();
      Thread clientThread = new Thread(() -> client.connect("localhost", 5000));
      clientThread.start();
	}
}
