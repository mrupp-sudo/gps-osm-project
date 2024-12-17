% Datalog rules
roadSegment(Node1_ID, Node2_ID, Way_ID) :-
	node(Node1_ID),
	node(Node2_ID),
	nextInWay(Node1_ID, Node2_ID, Way_ID),
	way(Way_ID),
	(
		wayTag(Way_ID, "highway", "motorway");
		wayTag(Way_ID, "highway", "trunk");
		wayTag(Way_ID, "highway", "primary");
		wayTag(Way_ID, "highway", "secondary");
		wayTag(Way_ID, "highway", "tertiary");
		wayTag(Way_ID, "highway", "unclassified");
		wayTag(Way_ID, "highway", "residential");
		wayTag(Way_ID, "highway", "motorway_link");
		wayTag(Way_ID, "highway", "trunk_link");
		wayTag(Way_ID, "highway", "primary_link");
		wayTag(Way_ID, "highway", "secondary_link");
		wayTag(Way_ID, "highway", "tertiary_link");
		wayTag(Way_ID, "highway", "living_street");
		wayTag(Way_ID, "highway", "service")
	),
	\+ wayTag(Way_ID, "motor_vehicle", "no"),
	\+ wayTag(Way_ID, "motor_vehicle", "private"),
	\+ wayTag(Way_ID, "motor_vehicle", "psv"),
	\+ wayTag(Way_ID, "vehicle", "no"),
	\+ wayTag(Way_ID, "vehicle", "private"),
	\+ wayTag(Way_ID, "access", "no"),
	\+ wayTag(Way_ID, "access", "private"),
	\+ wayTag(Way_ID, "oneway", "-1").
roadSegment(Node2_ID, Node1_ID, Way_ID) :-
	node(Node1_ID),
	node(Node2_ID),
	nextInWay(Node1_ID, Node2_ID, Way_ID),
	way(Way_ID),
	(
		wayTag(Way_ID, "highway", "motorway");
		wayTag(Way_ID, "highway", "trunk");
		wayTag(Way_ID, "highway", "primary");
		wayTag(Way_ID, "highway", "secondary");
		wayTag(Way_ID, "highway", "tertiary");
		wayTag(Way_ID, "highway", "unclassified");
		wayTag(Way_ID, "highway", "residential");
		wayTag(Way_ID, "highway", "motorway_link");
		wayTag(Way_ID, "highway", "trunk_link");
		wayTag(Way_ID, "highway", "primary_link");
		wayTag(Way_ID, "highway", "secondary_link");
		wayTag(Way_ID, "highway", "tertiary_link");
		wayTag(Way_ID, "highway", "living_street");
		wayTag(Way_ID, "highway", "service")
	),
	\+ wayTag(Way_ID, "motor_vehicle", "no"),
	\+ wayTag(Way_ID, "motor_vehicle", "private"),
	\+ wayTag(Way_ID, "motor_vehicle", "psv"),
	\+ wayTag(Way_ID, "vehicle", "no"),
	\+ wayTag(Way_ID, "vehicle", "private"),
	\+ wayTag(Way_ID, "access", "no"),
	\+ wayTag(Way_ID, "access", "private"),
	\+ wayTag(Way_ID, "oneway", "yes").
isReachable(Node_ID) :-
	node(Node_ID),
	position(Node_ID).
isReachable(Node2_ID) :-
	position(Node1_ID),
	roadSegment(Node1_ID, Node2_ID, _).
isReachable(Node3_ID) :-
	isReachable(Node1_ID),
	isReachable(Node2_ID),
	roadConnection(Node1_ID, Node2_ID, Node3_ID).
roadConnection(Node1_ID, Node2_ID, Node3_ID) :-
	roadSegment(Node1_ID, Node2_ID, _),
	roadSegment(Node2_ID, Node3_ID, _),
	\+ forbiddenDirection(Node1_ID, Node2_ID, Node3_ID),
	\+ mandatoryDirectionExists(Node1_ID, Node2_ID).
roadConnection(Node1_ID, Node2_ID, Node3_ID) :-
	mandatoryDirection(Node1_ID, Node2_ID, Node3_ID).
rightTurnRestriction(Node1_ID, Node2_ID, Node3_ID) :-
    relation(Relation_ID),
    relationTag(Relation_ID, "restriction", "no_right_turn"),
    (
        relationMember(Way1_ID, "way", "from", Relation_ID),
        relationMember(Node2_ID, "node", "via", Relation_ID);
        relationMember(Way1_ID, "way", "via", Relation_ID)
    ),
    relationMember(Way2_ID, "way", "to", Relation_ID),
    roadSegment(Node1_ID, Node2_ID, Way1_ID),
    roadSegment(Node2_ID, Node3_ID, Way2_ID).
onlyRightTurnRestriction(Node1_ID, Node2_ID, Node3_ID) :-
   	relation(Relation_ID),
    relationTag(Relation_ID, "restriction", "only_right_turn"),
    (
        relationMember(Way1_ID, "way", "from", Relation_ID),
        relationMember(Node2_ID, "node", "via", Relation_ID);
        relationMember(Way1_ID, "way", "via", Relation_ID)
    ),
    relationMember(Way2_ID, "way", "to", Relation_ID),
    roadSegment(Node1_ID, Node2_ID, Way1_ID),
    roadSegment(Node2_ID, Node3_ID, Way2_ID).
leftTurnRestriction(Node1_ID, Node2_ID, Node3_ID) :-
    relation(Relation_ID),
    relationTag(Relation_ID, "restriction", "no_left_turn"),
    (
        relationMember(Way1_ID, "way", "from", Relation_ID),
        relationMember(Node2_ID, "node", "via", Relation_ID);
        relationMember(Way1_ID, "way", "via", Relation_ID)
    ),
    relationMember(Way2_ID, "way", "to", Relation_ID),
    roadSegment(Node1_ID, Node2_ID, Way1_ID),
    roadSegment(Node2_ID, Node3_ID, Way2_ID).
onlyLeftTurnRestriction(Node1_ID, Node2_ID, Node3_ID) :-
    relation(Relation_ID),
    relationTag(Relation_ID, "restriction", "only_left_turn"),
    (
        relationMember(Way1_ID, "way", "from", Relation_ID),
        relationMember(Node2_ID, "node", "via", Relation_ID);
        relationMember(Way1_ID, "way", "via", Relation_ID)
    ),
    relationMember(Way2_ID, "way", "to", Relation_ID),
    roadSegment(Node1_ID, Node2_ID, Way1_ID),
    roadSegment(Node2_ID, Node3_ID, Way2_ID).
straightOnRestriction(Node1_ID, Node2_ID, Node3_ID) :-
    relation(Relation_ID),
    relationTag(Relation_ID, "restriction", "no_straight_on"),
    (
        relationMember(Way1_ID, "way", "from", Relation_ID),
        relationMember(Node2_ID, "node", "via", Relation_ID);
        relationMember(Way1_ID, "way", "via", Relation_ID)
    ),
    relationMember(Way2_ID, "way", "to", Relation_ID),
    roadSegment(Node1_ID, Node2_ID, Way1_ID),
    roadSegment(Node2_ID, Node3_ID, Way2_ID).
onlyStraightOnRestriction(Node1_ID, Node2_ID, Node3_ID) :-
    relation(Relation_ID),
    relationTag(Relation_ID, "restriction", "only_straight_on"),
    (
        relationMember(Way1_ID, "way", "from", Relation_ID),
        relationMember(Node2_ID, "node", "via", Relation_ID);
        relationMember(Way1_ID, "way", "via", Relation_ID)
    ),
    relationMember(Way2_ID, "way", "to", Relation_ID),
    roadSegment(Node1_ID, Node2_ID, Way1_ID),
    roadSegment(Node2_ID, Node3_ID, Way2_ID).
uTurnRestriction(Node1_ID, Node2_ID, Node3_ID) :-
    relation(Relation_ID),
    relationTag(Relation_ID, "restriction", "no_u_turn"),
    (
        relationMember(Way1_ID, "way", "from", Relation_ID),
        relationMember(Node2_ID, "node", "via", Relation_ID);
        relationMember(Way1_ID, "way", "via", Relation_ID)
    ),
    relationMember(Way2_ID, "way", "to", Relation_ID),
    roadSegment(Node1_ID, Node2_ID, Way1_ID),
    roadSegment(Node2_ID, Node3_ID, Way2_ID).
entryRestriction(Node1_ID, Node2_ID, Node3_ID) :-
    relation(Relation_ID),
    relationTag(Relation_ID, "restriction", "no_entry"),
    (
        relationMember(Way1_ID, "way", "from", Relation_ID),
        relationMember(Node2_ID, "node", "via", Relation_ID);
        relationMember(Way1_ID, "way", "via", Relation_ID)
    ),
    relationMember(Way2_ID, "way", "to", Relation_ID),
    roadSegment(Node1_ID, Node2_ID, Way1_ID),
    roadSegment(Node2_ID, Node3_ID, Way2_ID).
exitRestriction(Node1_ID, Node2_ID, Node3_ID) :-
    relation(Relation_ID),
    relationTag(Relation_ID, "restriction", "no_exit"),
    (
        relationMember(Way1_ID, "way", "from", Relation_ID),
        relationMember(Node2_ID, "node", "via", Relation_ID);
        relationMember(Way1_ID, "way", "via", Relation_ID)
    ),
    relationMember(Way2_ID, "way", "to", Relation_ID),
    roadSegment(Node1_ID, Node2_ID, Way1_ID),
    roadSegment(Node2_ID, Node3_ID, Way2_ID).
forbiddenDirection(Node1_ID, Node2_ID, Node3_ID) :-
	(
		rightTurnRestriction(Node1_ID, Node2_ID, Node3_ID);
		leftTurnRestriction(Node1_ID, Node2_ID, Node3_ID);
		straightOnRestriction(Node1_ID, Node2_ID, Node3_ID);
		uTurnRestriction(Node1_ID, Node2_ID, Node3_ID);
		entryRestriction(Node1_ID, Node2_ID, Node3_ID);
		exitRestriction(Node1_ID, Node2_ID, Node3_ID)
	).
mandatoryDirectionExists(Node1_ID, Node2_ID) :-
    relation(Relation_ID),
    (
        relationTag(Relation_ID, "restriction", "only_right_turn");
        relationTag(Relation_ID, "restriction", "only_left_turn");
        relationTag(Relation_ID, "restriction", "only_straight_on")
    ),
    (
        relationMember(Way_ID, "way", "from", Relation_ID),
        relationMember(Node2_ID, "node", "via", Relation_ID);
        relationMember(Way_ID, "way", "via", Relation_ID)
    ),
	roadSegment(Node1_ID, Node2_ID, Way_ID).
mandatoryDirection(Node1_ID, Node2_ID, Node3_ID) :-
	(
		onlyRightTurnRestriction(Node1_ID, Node2_ID, Node3_ID);
		onlyLeftTurnRestriction(Node1_ID, Node2_ID, Node3_ID);
		onlyStraightOnRestriction(Node1_ID, Node2_ID, Node3_ID)
	).
yieldSign(Node_ID) :-
	node(Node_ID),
	nodeTag(Node_ID, "highway", "give_way").
stopSign(Node_ID) :-
	node(Node_ID),
	nodeTag(Node_ID, "highway", "stop").
trafficSignal(Node_ID) :-
	node(Node_ID),
	nodeTag(Node_ID, "highway", "traffic_signals").
pedestrianCrossing(Node_ID) :-
	node(Node_ID),
	nodeTag(Node_ID, "highway", "crossing").
tramCrossing(Node_ID) :-
	node(Node_ID),
	nodeTag(Node_ID, "railway", "tram_level_crossing").
trainCrossing(Node_ID) :-
	node(Node_ID),
	nodeTag(Node_ID, "railway", "level_crossing").
busStation(Node_ID) :-
	node(Node_ID),
	nodeTag(Node_ID, "public_transport", "stop_position"),
	nodeTag(Node_ID, "bus", "yes"),
	\+ nodeTag(Node_ID, "tram", "yes").
tramStation(Node_ID) :-
	node(Node_ID),
	nodeTag(Node_ID, "public_transport", "stop_position"),
	nodeTag(Node_ID, "tram", "yes"),
	\+ nodeTag(Node_ID, "bus", "yes").
intermodalStation(Node_ID) :-
	node(Node_ID),
	nodeTag(Node_ID, "public_transport", "stop_position"),
	nodeTag(Node_ID, "bus", "yes"),
	nodeTag(Node_ID, "tram", "yes").
kindergarten(Node_ID) :-
	node(Node_ID),
	nodeTag(Node_ID, "amenity", "kindergarten").
kindergarten(Way_ID) :-
	way(Way_ID),
	wayTag(Way_ID, "amenity", "kindergarten").
school(Node_ID) :-
	node(Node_ID),
	nodeTag(Node_ID, "amenity", "school").
school(Way_ID) :-
	way(Way_ID),
	wayTag(Way_ID, "amenity", "school").
weatherCondition("dangerous") :-
	(
        	weatherParameter("temperature", "freezing");
        	weatherParameter("precipitation", "heavy")
	).
weatherCondition("challenging") :-
        weatherParameter("temperature", "cold"),
        weatherParameter("precipitation", "moderate").
weatherCondition("harmless") :-
	(
        	weatherParameter("temperature", "mild");
        	weatherParameter("temperature", "warm")
	),
	weatherParameter("precipitation", "no").
