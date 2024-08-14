ListGrid {
	var chain, list_width, list_height, textview, range_width, num_cols, num_rows, width, height, node_arr, grid, title_height, space, grid_key, view, activeColor, save_key, param_space;
	var window;
	var <>listviews, <>savesystem;
	classvar rangeboxes;

	*new { |window, grid_key, param_space|
		^super.newCopyArgs()
		.window_(window)
		.set_view_(window)
		.grid_key_(grid_key)
		.param_space_(param_space)
		.init()
		.show_()
	}

	front { view.front; }

	window { ^window }
	window_ { |newWindow| window = newWindow; }

	prMatchStatus { |listview, rangeboxes|
		rangeboxes[listview.selection].do({ |rangebox|
			rangebox.highlighted = true;
		});
	}

	init {
		rangeboxes = ();
		/*
		OSCFunc({
			listviews.do({|sk|
				sk.do({ |lv|
					AppClock.sched(0.05, {
						var rand_selection = {(0..lv.items.size - 1).choose} ! lv.items.size;
						lv.selection_(rand_selection);
					});
				});
			});
		}, '/gui/random');
		*/

		OSCFunc({|msg|
			listviews.keys.do({|key|
				AppClock.sched(0.00, {
					var load_selection = SaveSystem().load(key, []);
					listviews.do({|sk|
						sk.do({ |lv|
							lv.selection_(load_selection);
						});
					});
				});
			});
		}, '/util/reload');

		/*
		OSCFunc({|msg|
			listviews.keys.do({|key|
				AppClock.sched(0.00, {
					listviews.do({|sk|
						sk.do({ |lv|
							lv.selection_(load_selection);
							SaveSystem.save()
						});
					});
				});
			});
		}, '/util/preset');
		*/
	}


	grid_key { ^grid_key }
	grid_key_ { |newGridKey| grid_key = newGridKey }

	param_space { ^param_space }
	param_space_ { |newParamSpace| param_space = newParamSpace }

	activeColor { ^activeColor }
	activeColor_ { |newActiveColor|
		activeColor = newActiveColor;
	}

	set_view_ { |parent| view = View(parent, bounds:1200@900); }

	visible_ { |bool| view.visible_(bool); }

	show_ {
		// Get all nodes from ProxySpace

		space = param_space;
		node_arr = List.new;

		space.do({ |node|
			if(node.controlKeys.size != 0 &&
				{node.key.asString[0..3] != "slot"} && {node.key.asString[0..3] != "data"} , {

				node_arr.add(node);
			});


		});

		if(space.size == 0) {
			^"No Ndef Params Available".postln;
		};

		width = 1200;
		height = 900;
		num_cols = 4;
		list_height = 150;
		title_height = 20;
		chain = Rect(2, 2, 510, 264);
		list_width = width - chain.bounds.width / num_cols;
		num_rows = (node_arr.size / num_cols).floor.asInteger;
		listviews = ();

		(0..(num_rows - 1).clip(0, 99)).do({ |row|
			(0..(num_cols - 1).clip(0, 99)).do({ |col|
				var pos_x, pos_y, node, index, selection, save_listview, listview, highlight, rangebox, local_key;
				index = col + (row * num_cols);

				node = if(index >= node_arr.size) {
					Ndef(\);
				} {
					node = node_arr[index];
				};

				pos_y = row * (list_height - title_height);
				pos_x = chain.bounds.width + (col * list_width);
				textview = TextView(view, Rect(pos_x, pos_y, list_width, title_height));
				textview.editable = false;
				textview.string = node.key;

				save_key = (node.key ++ grid_key ++ \listview).asSymbol;
				local_key = save_key.copy;
				selection = [];
				rangebox = Array.fill(node.controlKeys.size, {0});
				selection = SaveSystem().load(local_key, []);

				listview = ListViewK(
					view,
					Rect(pos_x, textview.bounds.bottom, list_width, chain.bounds.height / num_rows - title_height),
					save_key,
				)
				.items_(node.controlKeys)
				.background_(Color.clear)
				.hiliteColor_(activeColor)
				.selectionMode_(\multi)
				.selection_(selection);

				listviews[save_key] = List();

				listview.items.do({|item, i|
					var key;
					highlight = false;
					selection.do({ |select|
						if(select == i) {
							highlight = true;
						}
					});

					key = grid_key ++ \_ ++ node.key ++ \_ ++ item;

					rangebox[i] = RangeBox(
						view,
						pos_x,
						listview.bounds.top + (i * 15),
						listview.bounds.width,
						key,
						highlight
					);
				});

				listview.selectionAction_({ |sbs|
					rangebox.do({|box, i|
						if(listview.selection.includes(i)) {
							box.highlight(true);
						} {
							box.highlight(false);
						}
					});
						["save listview: " ++  local_key ++ "  " ++ save_key];
					SaveSystem().save(listview.key, listview.selection);
				});

				listviews[save_key].add(listview);
			});
		});
	}
}
