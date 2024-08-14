RangeBox {

	var parent, x, y, total_width;
	var rect, color, offsets, <num, width, height, spacing, map_index;
	var <>range, <>key, <> highlighted, <>savesystem;
	var marked;
	classvar fadeTime = 0.5;
	classvar schedlist;


	*new { |parent, x, y, total_width, key, highlighted|
		^super.newCopyArgs(parent, x, y, total_width, key, highlighted)
		//.range_(range)
		//.highlighted_(false)
		.key_(key)
		.highlighted_(highlighted)
		.init()
		.load_data()
		.show()
		//.map()
	}

	highlight{ |on|
		var old;
		// maybe return if nothing changes?

		old = highlighted;
		highlighted = on;
		if(old == highlighted, {^this});

		//highlighted = highlighted.not;
		//[key ++ " " ++ "highlight set to:  " ++ highlighted].postln;
		this.map;
	}

	init{
		var splitKeys, node, param;
		splitKeys = key.asString.split($_);
		node = Ndef.all[\localhost][splitKeys[1].asSymbol];
		param = splitKeys[2].asSymbol;

		map_index = 0;
		schedlist = (1..100) / 100;

		AppClock.sched(schedlist[0]  + 0.1, {
			Ndef(key).clear;
			node.set(param, Ndef(key, {DC.ar(0)}));
			schedlist.rotate;
		});

		OSCFunc({
			var rand_range = ({10.0.rand} !2).sort;
			AppClock.sched(0.05, {
				marked = false;
				num.do({|num, i|
					num.value = rand_range[i]
				});
				this.range_(rand_range);
				SaveSystem().save(key.asSymbol, this);
			});
		}, '/gui/random');

		OSCFunc({|msg|
			var rand_range = ({10.0.rand} !2).sort;
			AppClock.sched(0.0, {
				fadeTime = msg[1];
				["set fade time to: " ++ fadeTime ].postln;
			});
		}, '/gui/fadeTime');

		OSCFunc({|msg|
			AppClock.sched(0.0, {
				"reload".postln;
				//marked = false;
				this.load_data();
				this.map();
			});
		}, '/util/reload');

		OSCFunc( { |msg|

			var addr;
			AppClock.sched(0.0, {
				SaveSystem().save(key.asSymbol, this);
			});

		}, '/gui/preset');
	}

	load_data{

		var rangebox;

		range = [1, 8];

		//rangebox = SaveSystem(key.asSymbol).load(Array.zeroFill(2));
		rangebox = SaveSystem().load(key.asSymbol, [1, 1]);

		if(rangebox.class == RangeBox, {
			range = rangebox.range;
			// sanitize for buggy nil in rangebox!
			range = range.collect{|r_val|
				if(r_val.isNil, {r_val = 0});
				r_val;
			}
		});

		["LOADED: "++ " " ++ key ++ "  " ++ " "++ range ++ " "  ++ rangebox.class].postln;

		num.do({|num, i|
			num.value = range[i];
		});
		this.range_(range);
	}

	map{
		var splitKeys, node, param, input, randKey, tempProx, bus, busName;

		splitKeys = key.asString.split($_);
		node = Ndef.all[\localhost][splitKeys[1].asSymbol];
		param = splitKeys[2].asSymbol;

		if(highlighted == false, {
			//node.set(param, 0);
			Ndef(key).fadeTime = fadeTime;
			Ndef(key, {DC.ar(0)});
			^this;
		});

		busName = splitKeys[0].asSymbol;

		// bus from split node proxyrole
		bus = BusDictManager().getOrSet(busName);


		["mapping busName: " ++ busName].postln;
		["mapping busIndex + range: " ++ bus.index  ++ "  "++ "range: " ++  this.range].postln;
		["node: " ++  splitKeys[1].asSymbol].postln;
		["fadeTime: " ++  fadeTime].postln;


		//input = {LinLin.ar(In.ar(bus, 1) ,0.0, 1.0, range[0], range[1])};
		//input = {LFNoise2.ar(1).range(1, 2)};
		//tempProx.clear;
		//tempProx = NodeProxy(Server.local, \audio, 1, 0);

		// for order of execution !!!!!!
		//Ndef(key).clear;

		// temp node proxy with bus as input and range from gui

		//["This range is being mapped: " ++ this.num[0].value ++ " " ++ this.num[1].value ++  " " ++ splitKeys].postln;
		//tempProx = Ndef(key);
		node.fadeTime = fadeTime;
		/*
		AppClock.sched(schedlist[0] , {
			node.set(param, tempProx);
		});
		*/
		//tempProx.fadeTime = fadeTime;
		Ndef(key).fadeTime = fadeTime;
		//tempProx = Ndef(key, {LinLin.ar(In.ar(bus.index, 1) ,0.0, 1.0, this.num[0].value, this.num[1].value)});
		Ndef(key, {LinLin.ar(In.ar(bus.index, 1) ,0.0, 1.0, this.num[0].value, this.num[1].value)});

		// fadeTime from button
		//tempProx.fadeTime = fadeTime;


		// mini delay randomize button + a lot of synths (too many xset at one time)
		//schedlist = schedlist.rotate;
	}

	show{

		var path, splitKeys, rangebox, field, bt_text;

		splitKeys = key.asString.split($_);

		key = key.asSymbol;

		spacing = 0;
		width = total_width - spacing  / 2;
		height = 16;

		offsets = [0, width / 2];
		num = Array.zeroFill(2);

		offsets.do({ |offset, i|

			rect = Rect(x + (total_width / 2) + offset, y, total_width / 4, height);
			num[i] = NumberBox(parent, rect);
			num[i].background = Color.clear;
			num[i].stringColor = Color.white;
			num[i].normalColor = Color.white;
			num[i].value = range[i];
			num[i].action = {
				range[i] = num[i].value;
				//SaveSystem(key).save(this);
				SaveSystem().save(key, this);
				//["NUM ACTION:   " ++  range].postln;
				this.map()
			}

		});
	}

}




