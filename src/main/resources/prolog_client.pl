% Load libraries
:- use_module(library(socket)).
:- use_module(library(readutil)).

% Dynamic declarations for handling runtime facts
:- dynamic add/1, delete/1.
:- dynamic position/1, node/1, nodeTag/3.
:- dynamic way/1, wayTag/3, nextInWay/3.
:- dynamic relation/1, relationTag/3, nextInRelation/3, relationMember/4.

% Tabled predicates for caching computation results
:- table isReachable/1.
:- table weatherCondition/1.

% Main entry point for starting the client
start_client(Host, Port) :-
    setup_call_cleanup(
        tcp_socket(Socket),
        ( tcp_connect(Socket, Host:Port),
          tcp_open_socket(Socket, InStream, OutStream),
		  writeln('Client connected to server [localhost on port 5000]'),
          handle_start(InStream, OutStream)
        ),
        tcp_close_socket(Socket)
    ).

% Manage initial interaction with user
handle_start(InStream, OutStream) :-
	read_server_prompt(InStream),
	repeat,
    get_user_input(Message),
    format(OutStream, '~w~n', [Message]),  
    flush_output(OutStream),
    read_line_to_string(InStream, Response),
    ( Response == "Valid input" ->
        ( Message == "stop" -> 
            writeln('Connection closed'), !, fail
        ; Message == "start" -> 
            consult('datalog_rules.pl'),
            client_loop(InStream)
        )
    ; 
        ( Response == "Invalid input. Please enter 'start' or 'stop':" -> 
            writeln(Response),
            fail
        )
    ).

% Get initial server prompt
read_server_prompt(InStream) :-
    read_line_to_string(InStream, Prompt),
    writeln(Prompt).

% Get input from the user
get_user_input(Message) :-
	prompt(_, ''),
    read_line_to_string(user_input, Input),
    Message = Input.

% Process the data stream
client_loop(InStream) :-
    repeat,
    read_response(InStream, EndOfStream),
    ( EndOfStream == true
    -> 
        cleanup_facts,           
        ( exists_file('received_facts.pl') 
        -> delete_file('received_facts.pl')
        ; true     
        ),
        !  % Exit the loop
    ; fail  % Continue the loop
    ).

% Read and process responses from the server
read_response(InStream, EndOfStream) :-
    read_line_to_string(InStream, Response),
    ( Response == "SENDING FACTS"
    -> writeln('Receiving and processing facts'),
       receive_file(InStream, 'received_facts.pl'),
       consult('received_facts.pl'),
	   update_facts,
       perform_reasoning_with_timing,
       EndOfStream = false
    ; Response == "EOS"
    -> writeln('End-of-stream signal received, disconnecting from the server'),
       EndOfStream = true
    ).

% Receive and write data sent by the server
receive_file(InStream, FileName) :-
    open(FileName, write, File, [encoding(utf8)]),
    repeat,
    read_line_to_string(InStream, Line),
    ( Line == "EOF"
    -> close(File), !
    ; writeln(File, Line), fail
    ).

% Update facts
update_facts :-
    findall(X, add(X), AddFacts),
    findall(X, delete(X), DeleteFacts),
    maplist(assertz, AddFacts),
    maplist(retract, DeleteFacts),
    retractall(add(_)),
    retractall(delete(_)).

% Perform reasoning with the current set of facts and rules while stopping the time
perform_reasoning_with_timing :-
    writeln('Printing inference results'),
    statistics(walltime, _),
    findall(X, isReachable(X), ReachableNodes),
    findall(X, yieldSign(X), YieldSigns),
    findall(X, stopSign(X), StopSigns),
    findall(X, trafficSignal(X), TrafficSignals),
    findall(X, pedestrianCrossing(X), PedestrianCrossings),
    findall(X, tramCrossing(X), TramCrossings),
    findall(X, trainCrossing(X), TrainCrossings),
    findall(X, busStation(X), BusStations),
    findall(X, tramStation(X), TramStations),
    findall(X, intermodalStation(X), IntermodalStations),    
    findall(X, kindergarten(X), Kindergartens),
    findall(X, school(X), Schools),
    findall(X, weatherCondition(X), WeatherCondition),
    statistics(walltime, [_, Time]),
    format('    Reachable Nodes: '),
    ( ReachableNodes \= [] 
    -> format('~w~n', [ReachableNodes])
    ;   writeln('')
    ),
    format('    Yield Signs: '),
    ( YieldSigns \= [] 
    -> format('~w~n', [YieldSigns])
    ;   writeln('')
    ),
    format('    Stop Signs: '),
    ( StopSigns \= [] 
    -> format('~w~n', [StopSigns])
    ;   writeln('')
    ),
    format('    Traffic Signals: '),
    ( TrafficSignals \= [] 
    -> format('~w~n', [TrafficSignals])
    ;   writeln('')
    ),
    format('    Pedestrian crossings: '),
    ( PedestrianCrossings \= [] 
    -> format('~w~n', [PedestrianCrossings])
    ;   writeln('')
    ),
    format('    Tram Crossings: '),
    ( TramCrossings \= [] 
    -> format('~w~n', [TramCrossings])
    ;   writeln('')
    ),
    format('    Train Crossings: '),
    ( TrainCrossings \= [] 
    -> format('~w~n', [TrainCrossings])
    ;   writeln('')
    ),
    format('    Bus Stations: '),
    ( BusStations \= [] 
    -> format('~w~n', [BusStations])
    ;   writeln('')
    ),
    format('    Tram Stations: '),
    ( TramStations \= [] 
    -> format('~w~n', [TramStations])
    ;   writeln('')
    ),
    format('    Intermodal Stations: '),
    ( IntermodalStations \= [] 
    -> format('~w~n', [IntermodalStations])
    ;   writeln('')
    ),    
    format('    Kindergartens: '),
    ( Kindergartens \= [] 
    -> format('~w~n', [Kindergartens])
    ;   writeln('')
    ),
    format('    Schools: '),
    ( Schools \= [] 
    -> format('~w~n', [Schools])
    ;   writeln('')
    ),
    format('    Weather Condition: '),
    ( WeatherCondition \= [] 
    -> format('~w~n', [WeatherCondition])
    ;   writeln('')
    ),
    
    abolish_all_tables,
    format('Time taken for reasoning: ~w ms~n', [Time]).


% Clean up all facts before exiting
cleanup_facts :-
    abolish(position/1),
    abolish(node/1),
    abolish(nodeTag/3),
    abolish(way/1),
    abolish(wayTag/3),
    abolish(nextInWay/3),
    abolish(relation/1),
    abolish(relationTag/3),
    abolish(nextInRelation/3),
    abolish(relationMember/4).

% Initialize the client
:- initialization((start_client('localhost', 5000) -> true ; writeln('Initialization failed.'))).