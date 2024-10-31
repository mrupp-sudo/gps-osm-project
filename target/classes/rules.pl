% Datalog rules
roadSegment(X1, X2, Y) :-
	node(X1),
	node(X2),
	nextInWay(X1, X2, Y),
	way(Y),
	(
		wayTag(Y, "highway", "motorway");
		wayTag(Y, "highway", "trunk");
		wayTag(Y, "highway", "primary");
		wayTag(Y, "highway", "secondary");
		wayTag(Y, "highway", "tertiary");
		wayTag(Y, "highway", "unclassified");
		wayTag(Y, "highway", "residential");
		wayTag(Y, "highway", "motorway_link");
		wayTag(Y, "highway", "trunk_link");
		wayTag(Y, "highway", "primary_link");
		wayTag(Y, "highway", "secondary_link");
		wayTag(Y, "highway", "tertiary_link");
		wayTag(Y, "highway", "living_street");
		wayTag(Y, "highway", "service")
	),
	\+ wayTag(Y, "motor_vehicle", "no"),
	\+ wayTag(Y, "motor_vehicle", "private"),
	\+ wayTag(Y, "motor_vehicle", "psv"),
	\+ wayTag(Y, "vehicle", "no"),
	\+ wayTag(Y, "vehicle", "private"),
	\+ wayTag(Y, "access", "no"),
	\+ wayTag(Y, "access", "private"),
	\+ wayTag(Y, "oneway", "-1").
roadSegment(X2, X1, Y) :-
	node(X1),
	node(X2),
	nextInWay(X1, X2, Y),
	way(Y),
	(
		wayTag(Y, "highway", "motorway");
		wayTag(Y, "highway", "trunk");
		wayTag(Y, "highway", "primary");
		wayTag(Y, "highway", "secondary");
		wayTag(Y, "highway", "tertiary");
		wayTag(Y, "highway", "unclassified");
		wayTag(Y, "highway", "residential");
		wayTag(Y, "highway", "motorway_link");
		wayTag(Y, "highway", "trunk_link");
		wayTag(Y, "highway", "primary_link");
		wayTag(Y, "highway", "secondary_link");
		wayTag(Y, "highway", "tertiary_link");
		wayTag(Y, "highway", "living_street");
		wayTag(Y, "highway", "service")
	),
	\+ wayTag(Y, "motor_vehicle", "no"),
	\+ wayTag(Y, "motor_vehicle", "private"),
	\+ wayTag(Y, "motor_vehicle", "psv"),
	\+ wayTag(Y, "vehicle", "no"),
	\+ wayTag(Y, "vehicle", "private"),
	\+ wayTag(Y, "access", "no"),
	\+ wayTag(Y, "access", "private"),
	\+ wayTag(Y, "oneway", "yes").
isReachable(X) :-
	node(X),
	position(X).
isReachable(X2) :-
	position(X1),
	roadSegment(X1, X2, _).
isReachable(X3) :-
	isReachable(X1),
	isReachable(X2),
	intersection(X1, X2, X3).
intersection(X1, X2, X3) :-
	roadSegment(X1, X2, _),
	roadSegment(X2, X3, _),
	\+ forbiddenDirection(X1, X2, X3),
	\+ mandatoryDirectionExists(X1, X2).
intersection(X1, X2, X3) :-
	mandatoryDirection(X1, X2, X3).
rightTurnRestriction(X1, X2, X3) :-
    relation(R),
    relationTag(R, "restriction", "no_right_turn"),
    (
        relationMember(Y1, "way", "from", R),
        relationMember(X2, "node", "via", R);
        relationMember(Y1, "way", "via", R)
    ),
    relationMember(Y2, "way", "to", R),
    roadSegment(X1, X2, Y1),
    roadSegment(X2, X3, Y2).
onlyRightTurnRestriction(X1, X2, X3) :-
    relation(R),
    relationTag(R, "restriction", "only_right_turn"),
    (
        relationMember(Y1, "way", "from", R),
        relationMember(X2, "node", "via", R);
        relationMember(Y1, "way", "via", R)
    ),
    relationMember(Y2, "way", "to", R),
    roadSegment(X1, X2, Y1),
    roadSegment(X2, X3, Y2).
leftTurnRestriction(X1, X2, X3) :-
    relation(R),
    relationTag(R, "restriction", "no_left_turn"),
    (
        relationMember(Y1, "way", "from", R),
        relationMember(X2, "node", "via", R);
        relationMember(Y1, "way", "via", R)
    ),
    relationMember(Y2, "way", "to", R),
    roadSegment(X1, X2, Y1),
    roadSegment(X2, X3, Y2).
onlyLeftTurnRestriction(X1, X2, X3) :-
    relation(R),
    relationTag(R, "restriction", "only_left_turn"),
    (
        relationMember(Y1, "way", "from", R),
        relationMember(X2, "node", "via", R);
        relationMember(Y1, "way", "via", R)
    ),
    relationMember(Y2, "way", "to", R),
    roadSegment(X1, X2, Y1),
    roadSegment(X2, X3, Y2).
straightOnRestriction(X1, X2, X3) :-
    relation(R),
    relationTag(R, "restriction", "no_straight_on"),
    (
        relationMember(Y1, "way", "from", R),
        relationMember(X2, "node", "via", R);
        relationMember(Y1, "way", "via", R)
    ),
    relationMember(Y2, "way", "to", R),
    roadSegment(X1, X2, Y1),
    roadSegment(X2, X3, Y2).
onlyStraightOnRestriction(X1, X2, X3) :-
    relation(R),
    relationTag(R, "restriction", "only_straight_on"),
    (
        relationMember(Y1, "way", "from", R),
        relationMember(X2, "node", "via", R);
        relationMember(Y1, "way", "via", R)
    ),
    relationMember(Y2, "way", "to", R),
    roadSegment(X1, X2, Y1),
    roadSegment(X2, X3, Y2).
uTurnRestriction(X1, X2, X3) :-
    relation(R),
    relationTag(R, "restriction", "no_u_turn"),
    (
        relationMember(Y1, "way", "from", R),
        relationMember(X2, "node", "via", R);
        relationMember(Y1, "way", "via", R)
    ),
    relationMember(Y2, "way", "to", R),
    roadSegment(X1, X2, Y1),
    roadSegment(X2, X3, Y2).
entryRestriction(X1, X2, X3) :-
    relation(R),
    relationTag(R, "restriction", "no_entry"),
    (
        relationMember(Y1, "way", "from", R),
        relationMember(X2, "node", "via", R);
        relationMember(Y1, "way", "via", R)
    ),
    relationMember(Y2, "way", "to", R),
    roadSegment(X1, X2, Y1),
    roadSegment(X2, X3, Y2).
exitRestriction(X1, X2, X3) :-
    relation(R),
    relationTag(R, "restriction", "no_exit"),
    (
        relationMember(Y1, "way", "from", R),
        relationMember(X2, "node", "via", R);
        relationMember(Y1, "way", "via", R)
    ),
    relationMember(Y2, "way", "to", R),
    roadSegment(X1, X2, Y1),
    roadSegment(X2, X3, Y2).
forbiddenDirection(X1, X2, X3) :-
	(
		rightTurnRestriction(X1, X2, X3);
		leftTurnRestriction(X1, X2, X3);
		straightOnRestriction(X1, X2, X3);
		uTurnRestriction(X1, X2, X3);
		entryRestriction(X1, X2, X3);
		exitRestriction(X1, X2, X3)
	).
mandatoryDirectionExists(X1, X2) :-
    relation(R),
    (
        relationTag(R, "restriction", "only_right_turn");
        relationTag(R, "restriction", "only_left_turn");
        relationTag(R, "restriction", "only_straight_on")
    ),
    (
        relationMember(Y, "way", "from", R),
        relationMember(X2, "node", "via", R);
        relationMember(Y, "way", "via", R)
    ),
	roadSegment(X1, X2, Y).
mandatoryDirection(X1, X2, X3) :-
	(
		onlyRightTurnRestriction(X1, X2, X3);
		onlyLeftTurnRestriction(X1, X2, X3);
		onlyStraightOnRestriction(X1, X2, X3)
	).
yieldSign(X) :-
	node(X),
	nodeTag(X, "highway", "give_way").
stopSign(X) :-
	node(X),
	nodeTag(X, "highway", "stop").
trafficSignal(X) :-
	node(X),
	nodeTag(X, "highway", "traffic_signals").
pedestrianCrossing(X) :-
	node(X),
	nodeTag(X, "highway", "crossing").
tramCrossing(X) :-
	node(X),
	nodeTag(X, "railway", "tram_level_crossing").
trainCrossing(X) :-
	node(X),
	nodeTag(X, "railway", "level_crossing").
busStation(X) :-
	node(X),
	nodeTag(X, "public_transport", "stop_position"),
	nodeTag(X, "bus", "yes"),
	\+ nodeTag(X, "tram", "yes").
tramStation(X) :-
	node(X),
	nodeTag(X, "public_transport", "stop_position"),
	nodeTag(X, "tram", "yes"),
	\+ nodeTag(X, "bus", "yes").
intermodalStation(X) :-
	node(X),
	nodeTag(X, "public_transport", "stop_position"),
	nodeTag(X, "bus", "yes"),
	nodeTag(X, "tram", "yes").
reachableStation(X) :-
	(
		busStation(X);
		tramStation(X);
		intermodalStation(X)
	),
	isReachable(X).
kindergarten(X) :-
	node(X),
	nodeTag(X, "amenity", "kindergarten").
kindergarten(Y) :-
	way(Y),
	wayTag(Y, "amenity", "kindergarten").
school(X) :-
	node(X),
	nodeTag(X, "amenity", "school").
school(Y) :-
	way(Y),
	wayTag(Y, "amenity", "school").
