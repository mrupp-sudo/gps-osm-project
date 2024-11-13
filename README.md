# Driving Simulation with OpenStreetMap

This project implements a system that provides dynamically generated data, including map information and sensor readings (i.e., GPS). Specifically, it uses publicly available data from [OpenStreetMap](https://www.openstreetmap.org/) to provide map data, as well as generate data streams that simulate the movement of a car based on time-dependent GPS coordinates. The generated data is represented as Datalog facts (in Prolog syntax) to enable reasoning with Datalog rules. The rules are designed to address the criticality of traffic situations. While an [Apache Jena](https://jena.apache.org/) reasoner is integrated as an example, the data stream can be accessed through a socket connection with any custom reasoner.

*To be continued…*

