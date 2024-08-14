ProxyChainListGui : JITGui {
	var <guiFuncs; // move to classvar!
	var <butZone, <buttonSpecs, <buttons, <namedButtons, <specialButtons, <editGui, <colorButtons, <extensionZone, <randomZone, <randomButton, <extensionButtons, <extraZone;
	var listview;
	classvar splitColors, activeColor, addr, boardAddr, extend;

	*new { |chain, numItems = 16, parent, bounds, makeSkip = true, options, listview, splitcolors, extended|

		//splitcolors = [Color.red, Color.green, Color.blue];
		splitColors = [Color(0.3, 0.3, 0.8)] ++ splitcolors;
		activeColor = Color(0.5, 0.5, 0.57);
		addr = NetAddr("localhost", NetAddr.langPort);
		boardAddr = NetAddr("localhost", 5555);
		extend = extended;

		options = options ?? { if (chain.notNil) { chain.slotNames.asArray } };
		^super.new(nil, numItems, parent, bounds, makeSkip, options)
		.chain_(chain)
		.listview_(listview)
	}

	/*
	accepts { |obj| ^(obj.isNil
		or: { obj.isKindOf(ProxyChain) }
		or: {obj.isKindOf(Event)})
	or: {obj.isKindOf(ProxyDictChain)}
	or: {obj.isKindOf(Boolean)}
	}
	*/

	chain_ { |chain| ^this.object_(chain) }
	chain { ^object }

	listview{^listview}
	listview_ { |newListview| listview = newListview}

	makeZone {
		parent.addFlowLayout(skin.margin, skin.gap * 10);
		zone = CompositeView(parent, Rect(bounds.left, bounds.top, bounds.width, bounds.height)).background_(skin.background);
		zone.addFlowLayout(skin.margin * 5, skin.gap);
		zone.background_(skin.foreground);
		if(extend == false, {^nil});

		extraZone = CompositeView(parent, Rect(bounds.left, bounds.top, bounds.width * 2.5, 50)).background_(skin.background);
		extraZone.addFlowLayout(skin.margin, skin.gap);

	}
	setDefaults { |options|
		if (parent.isNil) {
			defPos = 610@260
		} {
			defPos = skin.margin;
		};
		minSize = 510 @ (numItems * skin.buttonHeight + (skin.headHeight * 2));
	}

	makeViews { |options|

		namedButtons = ();
		specialButtons = ();

		options = options ?? { if(object.notNil) { object.slotNames.asArray } };

		guiFuncs =  (
			btlabel: { |but, name| but.states_([[name, Color.black, Color(1, 0.5, 0)]]) },
			label: { |but, name| but.states_([[name, Color.white, Color(1, 0.5, 0)]]) },
			slotCtl: { | but, name, level, index|
				var srcDict = ProxyDictChain.sourceDicts[name];
				var defLevel = level ?? { srcDict !? { srcDict[\level] } } ? 1;

				if(index == 0, {
					but.states_([[ ">" , Color.black, Color.white], [">", Color.black, activeColor],]);
				}, {
					but.states_(
						[
							["[" + name + "]", Color.black, Color.white	],
							[name, Color.black, activeColor],
					]);
				});

				but.action_({ |but|
					[
						{
							this.chain.remove(name);
							this.listview.do({|item|
								item.visible_(false);
							});
						},
						{

							this.chain.add(name, defLevel.value);
							this.listview.do({|item|
								item.visible_(false);
							});


							if(this.listview[name].notNil, {
								this.listview[name].visible_(true);
							});

							if(index == 0, {
								but.states_([[ ">"], [">", Color.black, activeColor], ]);
							}, {
								but.states_([[ name ], [name, Color.black, activeColor], ]);
							});

						},
					][but.value].value
				});
			},

			extra: { |but, name, func|
				but.states_([[name, Color.black, Color(1, 0.7, 0)]]);
				but.action_(func);
			},
		);

		butZone = CompositeView(zone, Rect(0,0, 110, bounds.height - (skin.margin.y * 2)));
		butZone.addFlowLayout(2@2, 1@0);

		buttons = numItems.collect {
			[
			Button.new(butZone, Rect(0,0, 50, skin.buttonHeight)).states_([["-"]]),
			Button.new(butZone, Rect(0,0, 50, skin.buttonHeight)).states_([["-"]])
			];
		};

		this.setButtons(options.asArray);
		this.makeEditGui;
		if(extend == true, { this.makeExtensionButtons});
	}

	makeExtensionButtons{
		var fadeTime, writeFile;
		//randomZone = CompositeView(extensionZone, Rect(0, 0, 100, 50));
		extensionZone = CompositeView(extraZone, parent.bounds);
		extensionZone.addFlowLayout(2@2, 1@0);

		colorButtons =  splitColors.collect{|color, i|
			Button.new(extensionZone, Rect(20 * i, 0, 20, 20))
			.states_([
				["+",Color.grey, Color.grey],
				["-",Color.grey, color]
			]);
		};

		randomButton = Button.new(extensionZone, Rect(0, 0, 100, skin.buttonHeight))
		.states_([
			["Randomize", Color.white, Color.grey]
		])
		.action_({|this_but|

			//var n = NetAddr("localhost", NetAddr.langPort);
			addr.sendMsg("/gui/random", 0);
		});


		fadeTime = FadeTimeBox.new(extensionZone, addr, skin);
		writeFile = WriteBox.new(extensionZone, boardAddr, skin);

		//addr.sendMsg("/gui/preset", 0);

		colorButtons.do({|c_button, index|
			var newColor;
			c_button.action_({ |this_but|
				"c button ACTION".postln;
				colorButtons.do({|other_button|
					other_button.value_(0);
				});

				//newColor = this_but.states[1][2];
				this_but.value_(this_but.value * -1 + 1);
				//activeColor = newColor;
				addr.sendMsg("/gui/preset", index);
			});
		});
		colorButtons[0].valueAction = 1;
	}

	makeEditGui { editGui = NdefGui(nil, numItems, zone); }

	setButtons { |specs|

		var objSlotNames = if (object.notNil) { object.slotNames.asArray } { [] };

		specs = (specs ? []);
		if (specs.size > buttons.size) {
			"ProxyChainGui: out of buttons... fix later".postln;
		};

		buttons.do { |but_arr, i|
			var name, kind, funcOrLevel, setupFunc;
			var list = specs[i];

			but_arr.do({|but, index|

				but.visible_(list.notNil);

				if (list.notNil) {
					#name, kind, funcOrLevel, setupFunc = list.asArray;
					kind = kind ? \slotCtl;
					if (kind == \slotCtl) {
						namedButtons.put(name, but);
					} {
						specialButtons.put(name, but);
					};
					if (name.notNil) {
						guiFuncs[kind].value(but, name, funcOrLevel, index);
						setupFunc.value(this, but);
					};
					but.enabled_(name.notNil);
				}
			});
		};

		buttonSpecs = specs;
	}

	getState {
		var state = (object: object, slotsInUse: [], slotNames: []);
		if (object.notNil) {
			state
			.put(\slotsInUse, object.slotsInUse.asArray)
			.put(\slotNames, object.slotNames.asArray)
		};
		^state
	}

	checkUpdate {
		var newState = this.getState;

		if (newState[\object].isNil) {
			this.name_('none');
			editGui.object_(object);
			butZone.enabled_(false);

			prevState = newState;
			^this
		};


		if (newState == prevState) { ^this };

		if (newState[\object] != prevState[\object]) {
			this.name_(object.key);
			butZone.enabled_(true);
			editGui.object_(object.proxy);
			editGui.name_(object.key);
		} {
			editGui.checkUpdate;
		};

		if (newState[\slotNames] != prevState[\slotNames]) {

			namedButtons.clear;
			buttons.select { |bt| specialButtons.includes(bt).not }.do { |but_arr, i|

				but_arr.do({|but, index|

					var newName = newState[\slotNames][i];

					if(index == 1, {
						but.states_(but.states.collect(_.put(0, newName ? "-")));
					});
					but.visible = newName.notNil;
					but.refresh;
					if (newName.notNil) {
						namedButtons.put(newName, but)
					}
				});
			};

			object.slotNames.do { |name, i|
				editGui.addReplaceKey(("wet" ++ i).asSymbol, name, \amp.asSpec);
				editGui.addReplaceKey(("mix" ++ i).asSymbol, name, \amp.asSpec);
			};
		};

		if (newState[\slotsInUse] != prevState[\slotsInUse]) {
			namedButtons.keysValuesDo { |name, but|
				but.value_(newState[\slotsInUse].includes(name).binaryValue);
			}
		};

		prevState = newState;
	}
}

MainFXListGui : ProxyChainListGui {

	accepts { |obj| ^(obj.isNil or: { obj.isKindOf(MainFX) }) }

	name_ { |name|
		if (hasWindow) { parent.name_(name.asString) };
	}

	makeEditGui {
		var editGuiOptions = [ 'CLR', 'reset', 'doc', 'fade', 'wake', 'end', 'pausR', 'sendR' ];
		editGui = NdefGui(nil, numItems, zone, bounds: 400@0);
	}
}

MasterFXListGui : MainFXGui {

	*new { |chain, numItems = 16, parent, bounds, makeSkip = true, options|
		"MasterFX has been renamed MainFX, and MasterFXGui has been renamed MainFXGui.\n"
		"Please adapt your code accordingly.".postln;
		^MainFXGui(chain, numItems, parent, bounds, makeSkip, options);
	}

}



