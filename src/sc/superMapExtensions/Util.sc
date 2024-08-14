SaveSystem {
	var <>key;
	classvar path = \data;
	*new {
		^super.newCopyArgs()
		.init();
	}

	init{
		var pathnames, addr;
		pathnames = [\data, \data1, \data2, \data3];

		OSCdef(\switch_forward, { |msg|

			var addr;
			AppClock.sched(0.25, {
				// switch preset
				path = pathnames[msg[1]];
				addr = NetAddr("localhost", NetAddr.langPort);

				// reload and redo mapping
				addr.sendMsg("/util/reload")
			});

		}, '/gui/preset');
	}

	path_{|newPath|
		path = newPath;
	}

	path{|newPath|
		^path;
	}

	load { |key, empty|
		var item;

		var range = [7, 7];
		var rangebox = RangeBox;

		if(Archive.global.at(path).isNil, {
			^empty;
		});

		if(Archive.global.at(path)[key].isNil, {
			^empty;
		});

		item = Archive.global.at(path)[key];
		["loaded: " ++  item ++ " under: " ++ key].postln;
		^item;
	}

	save { |key, item|

		var range = [0, 0];

		item = item.copy;

		if(Archive.global.at(path).isNil, {
			Archive.global.put(path, ())
		});

		Archive.global.at(path)[key] = item;
	}
}

BusDictManager {

	classvar bus_path = \bus_data5neu;
	*new {
		^super.newCopyArgs()
	}

	put{ |key, bus|
		if(Archive.global.at(bus_path).isNil, {
			Archive.global[bus_path] = ();
		});
		Archive.global.at(bus_path)[key] = bus;
	}

	get{|key|
		^Archive.global.at(bus_path)[key.asSymbol];
	}

	getOrSet{|key|
		["getOrSetBusNAme:  " ++ key].postln;
		if(Archive.global.at(bus_path).isNil, {
			Archive.global.put(bus_path, ());
		});

		if(Archive.global.at(bus_path)[key.asSymbol].isNil, {
			Archive.global.at(bus_path)[key.asSymbol] = Bus.audio(Server.local, 1);
		});
		^Archive.global.at(bus_path)[key.asSymbol];
	}
}

