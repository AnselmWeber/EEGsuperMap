(
//////////////////////////////////////////// input parameter function ////////////////////////////////////////////

~bands =  [\delta, \thetha, \alpha, \beta, \gamma];

~bandpower = { |chan = 4|

	var activeChannels= #[0, 1, 2, 3];
	var bands= #[
		["DELTA", 1, 4],  //name, min, max (Hz)
		["THETA", 4, 8],
		["ALPHA", 8, 13],
		["BETA", 13, 30],
		["GAMMA", 30, 55]
	];
	var board= SyntheticData(4, 200, initAction: {|b| b.start});  //edit here to match your board

	var notchFilters, bandpassFilters, smoothingFilters;
	var fft= DataFFT(board, 256);
	var fps= 60;
	var usr, pop1, btn1, btn2, pw, po;
	var verticalScale= #[0.1, 1, 10, 100];
	var f0= Font("Helvetiva", 16);
	var f1= Font("Helvetiva", 14);
	var f2= Font("Helvetiva", 10);
	var colors= [
		Color.new255(133, 152, 193),
		Color.new255(174, 145, 184),
		Color.new255(130, 167, 153),
		Color.new255(231, 206, 109),
		Color.new255(233, 134, 127)
	];
	var channels= {|i|
		(
			active: activeChannels.includes(i),
			data: []
		)
	}.dup(board.numChannels);

	//--window
	var win= Window("band power widget", Rect(10, 100, 512, 320));
	var ts= "EEG Power Bands";
	var as= "Power - (uV)^2 / Hz";
	var input_arr;

	// made avaiable outside function for plotting
	~bandsBuffer= {Array.fill(bands.size, {Array.fill(fps, {0})})}.dup(board.numChannels);  //1sec
	Ndef(\data_bandpower).reshaping = \elastic;

	Ndef(\data_bandpower, { |delta = #[0,0, 0, 0], thetha = #[0, 0, 0, 0], alpha = #[0, 0, 0, 0], beta = #[0, 0, 0, 0], gamma = #[0, 0, 0, 0]|
		var tr, bands, sig;
		tr = Trig.ar(\trigger.tr(0), SampleDur.ir);
		sig = Demand.ar(tr, 0, [delta, thetha, alpha, beta, gamma].flat);
	});

	win.view.minSize= Size(436, 145);
	win.alwaysOnTop_(true);
	win.background= Color.white;
	win.drawFunc= {
		var wb= win.bounds;
		var ub= usr.bounds;
		var step;
		Pen.smoothing= false;
		Pen.color= Color.black;
		Pen.stringAtPoint("Band Power", Point(10, 10), f0);

		Pen.push;
		Pen.translate(ub.left, ub.bottom);
		Pen.stringAtPoint(ts, Point(ub.width-ts.bounds(f1).width/2, wb.height-ub.bottom/2), f1);
		step= ub.width-1/bands.size;
		bands.do{|arr, i|
			var x= step*i+(step*0.5);
			Pen.stringAtPoint(arr[0], Point(x-(arr[0].bounds(f2).width/2), 12), f2);
		};
		Pen.stroke;
		Pen.pop;

		Pen.push;
		Pen.rotate(-0.5pi, wb.width/2, wb.height/2);
		Pen.translate(0-(wb.height-wb.width/2-(wb.height-ub.bottom)), wb.height-wb.width/2+ub.left);
		Pen.stringAtPoint(as, Point(ub.height-as.bounds(f1).width*0.5, 0-ub.left*0.75), f1);
		Pen.line(Point(0, -6), Point(ub.height, -6));
		step= verticalScale[1].explin(verticalScale[0], 100, 0, ub.height-1);
		verticalScale.do{|v, i|
			var str= v.asString;
			var x= v.explin(0.1, 100, 0, ub.height-1);  //log
			Pen.stringAtPoint(str, Point(x-(str.bounds(f2).width/2), -30), f2);
			Pen.line(Point(x, -6), Point(x, -10));
			Pen.line(Point(x, 0), Point(x, ub.width));
			9.do{|j|
				var xx= (j+1).explin(1, 10, 0, step);
				if((x+xx)<=(ub.height-1), {
					Pen.line(Point(x+xx, -6), Point(x+xx, -10));
					Pen.line(Point(x+xx, 0), Point(x+xx, ub.width));
				});
			};
		};
		Pen.stroke;
		Pen.pop;
	};
	win.front;
	CmdPeriod.doOnce({win.close});

	//--plot
	usr= UserView(win, Rect(*#[0.14, 0.19, 0.8, 0.64]*win.bounds.extent.asArray));
	usr.background= Color.new255(245, 245, 245, 220);
	usr.clearOnRefresh= true;
	usr.drawFunc= {
		var data;
		var ub= usr.bounds;
		var step= ub.width-1/bands.size;
		var indices= bands.collect{|b| [fft.freqToIndex(b[1]), fft.freqToIndex(b[2])]};
		var factor= fft.fftSize/board.currentSampleRate;
		var outputband;
		channels.do{|c, i|
			if(c.active and:{c.data.size>0}, {
				c.data= fft.fft(c.data);
				if(pop1.value>0, {
					c.data= smoothingFilters[i].filterLog(c.data.max(0.01));
				});
				bands.do{|b, j|
					var psdArr= c.data.copyRange(indices[j][0], indices[j][1]-1)**2*factor/4;
					~bandsBuffer[i][j].pop;
					~bandsBuffer[i][j].insert(0, psdArr.sum);
				};
			});
		};
		bands.do{|b, j|
			var x, y, arr= Array.new;
			channels.do{|c, i|
				if(c.active, {
					arr = arr++~bandsBuffer[i][j];
				});
			};
			x= step*j+1;
			y= arr.mean.explin(0.1, 100, ub.height, 0);
			Pen.fillColor= colors.wrapAt(j);
			Pen.fillRect(Rect(x, y, step-2, ub.height-y));
		};

		~bands.do({|band, band_index|
			var channelArr;
			//bandsBuffer.flop:  shape -> [ 5, 4, 60 ]
			channelArr = ~bandsBuffer.flop[band_index].collect({|channel| channel.mean});
			Ndef(\data_bandpower).set(band, channelArr);
		});
		Ndef(\data_bandpower).set(\trigger, 1);
	};
	usr.resize= 5;
	usr.canFocus= false;

	//--menus
	pw= win.bounds.width*0.125;
	po= win.bounds.width-((pw+2)*2.5);
	StaticText(win, Rect(po, 1, pw, 20)).align_(\center).string_("Smooth");
	StaticText(win, Rect((pw+2)+po, 1, pw, 20)).align_(\center).string_("Filters");
	pop1= PopUpMenu(win, Rect(po, 20, pw, 20))
	.items_(#["0.0", "0.5", "0.75", "0.9", "0.95", "0.98"])
	.action_{|v|
		smoothingFilters= {DataSmoothing(v.item.asFloat)}.dup(board.numChannels);
	}.valueAction_(3);
	btn1= Button(win, Rect((pw+2)+po, 20, pw/2, 20)).states_(
		[["Off"]]++DataFilterNotch.keys.collect{|x| [x.asString+"Hz"]}
	).action_{|v|
		if(v.value>0, {
			notchFilters= {
				DataFilterNotch(
					DataFilterNotch.keys[v.value-1],
					board.currentSampleRate,
					board.bufferSize
				);
			}.dup(board.numChannels);
		}, {
			notchFilters= nil;
		});
	}.valueAction_(1);
	btn2= Button(win, Rect((pw+2)*1.5+po, 20, pw/2, 20)).states_(
		[["Off"]]++DataFilterBandpass.keys.collect{|x| [x.asString]}
	).action_{|v|
		if(v.value>0, {
			bandpassFilters= {
				DataFilterBandpass(
					DataFilterBandpass.keys[v.value-1],
					board.currentSampleRate,
					board.bufferSize
				);
			}.dup(board.numChannels);
		}, {
			bandpassFilters= nil;
		});
	}.valueAction_(1);
	win.view.children.reject{|v| v==usr}.do{|v| v.resize_(3).font_(f2)};

	board.dataAction= {|num, data, acc|
		channels.do{|c, i|
			if(c.active, {
				c.data= board.buffer[i];
				if(notchFilters.notNil, {
					c.data= notchFilters[i].filterN(c.data.last);
				});
				if(bandpassFilters.notNil, {
					c.data= bandpassFilters[i].filterN(c.data.last);
				});
			});
		};
	};

	Routine({
		var nextTime;
		while({win.isClosed.not}, {
			nextTime= Main.elapsedTime+(1/fps);
			usr.refresh;
			(nextTime-Main.elapsedTime).max(0.001).wait;
		});
	}).play(AppClock);
};


///////////////////////////////////////////////// mapping software /////////////////////////////////////////////////

s.options.memSize_(65536 * 32);
s.options.numBuffers_(1024 * 12);

s.waitForBoot{

	~app = { |proxychains, tbuf|
		var width, height, plots, t_plot, param_output, transfer_function, param_bufs, t;
		var switch_lock, write_lock, grids, guis;
		var parent_w;
		var param_space;

		// Setup GUI skins
		GUI.skins.plot.gridLinePattern = FloatArray[0, 0];
		GUI.skins.plot.fontColor = Color(0.5, 1, 0);
		GUI.skins.plot.gridColorX = Color.yellow(0.5);
		GUI.skins.plot.gridColorY = Color.yellow(0.5);
		GUI.skins.plot.background = Color.black;
		GUI.skins.plot.plotColor = Color.cyan;
		GUI.skins.plot.labelX = "X";
		GUI.skins.plot.labelY = "Y";

		// Setup windows
		param_output = Window("Parameter Output", Rect(Window.screenBounds.right, 0, 520, 400)).front.alwaysOnTop_(true);
		param_output.addFlowLayout(0@0, 5@5);
		transfer_function = Window("Transfer Function", Rect(Window.screenBounds.right, 400, 520, 400)).front.alwaysOnTop_(true);

		t_plot = Plotter("Transfer Plot", parent: transfer_function);
		t_plot.value = (0..199).normalize;

		//Transfer Plot
		t_plot.editMode = true;
		t_plot.editFunc = { |...args| tbuf.setn(0, t_plot.value); SaveSystem().save(\editPlot1, t_plot.copy.value);};

		OSCFunc({
			AppClock.sched(0.0, {
				var lpv = SaveSystem().load(\editPlot1, (0..199).normalize);
				lpv.postln;
				t_plot.value = lpv;
				tbuf.setn(0, t_plot.value);
			})
		}, '/util/reload');

		// Parameter Plot
		plots = [0.0, 0.0];
		param_bufs = Array.fill(proxychains.size, nil);
		proxychains.do({ |proxychain, index|

			var plot = plots[index];
			var param_buf = param_bufs[index];
			param_buf = Buffer.alloc(s, 1024, bufnum: 50 + index);

			// Setup plots
			plot = Plotter("Parameter Plot: " ++ proxychain.key, parent: param_output, bounds: Rect(0, 0, 520, 200));
			plot.value = (0..1024-1).normalize(0, 8pi).sin;
			plot.findSpecs = true;

			// Update task
			Tdef(\update ++ proxychain.key, {
				loop {
					param_buf.getn(0, param_buf.numFrames, { |val|
						AppClock.sched(0.0, { plot.value = val });
					});
					0.07.yield;
				}
			}).play;
		});

		parent_w = Window.new("Window", bounds: Rect(0, 0, 1280, 720)).front;
		parent_w.bounds.left = 0;
		parent_w.bounds.top = 0;
		parent_w.addFlowLayout(2@2, 0@0);

		// Setup window and views

		~listviews = ();

		// Create list grid for each key in proxy dictionary

		proxychains = proxychains.asArray;
		grids = Array.fill(proxychains.size, nil);
		guis = Array.fill(proxychains.size, nil);



		// Synth definition
		Ndef(\x, {
			var snd = Saw.ar(50 * (\freqmul.ar(1) + 1) * [1, 1.01]).tanh * 0.5;
			RLPF.ar(snd, (\freq.ar + 100), 0.25);
			//DC.ar(0);
		});

		Ndef(\x).copy(\y);

		// copy Ndef space before defining ProxyChains --> ProxyChains are Ndefs
		param_space = Ndef.all.deepCopy[\localhost];
		param_space.postln;

		proxychains.do({ |proxychain, index|
			var window, param_buf, extend, height;
			height = 265;

			extend = false;
			if(index == (proxychains.size -1), {extend = true; height = height + 50});

			window = View.new(parent_w, "Window", bounds: 1200 @ height).front;
			~listdict = ();

			proxychain.slotNames.do { |key|

				if(ProxyDictChain.allSources[key].key == \split, {
					~listdict[key] = ListGrid(window, key ++ proxychain.key, param_space).visible_(false);
				});
				grids[index] = ~listdict;
			};

			~colors = [Color(0.4, 0.3, 0.4), Color(0.3, 0.3, 0.6), Color(0.3, 0.3, 0.4)];
			guis[index] = ProxyChainListGui.new(proxychain, 12, window, 10 @ 10, listview: grids[index], splitcolors: ~colors, extended: extend);

			// Activate chain buttons
			guis[index].buttons.do { |but| but[1].valueAction = 1; but[0].valueAction = 1; };
		});
		// Play Ndef
		//Ndef(\x).play(addAction: \addToTail);

		// Handle cleanup on CmdPeriod
		CmdPeriod.doOnce({ parent_w.close; param_output.close; transfer_function.close });
	};

	Spec.add(\band, [0, 5, \lin, 0.001, 2, "Hz"]);
	Spec.add(\channel, [0, 3, \lin, 1, 0]);
	//Spec.add(\focus, [0.0, 1.0, \lin, 0.01, 0]);


	// Buffer for Waveshaping input signal
	t = Buffer.alloc(s, 200, 1, bufnum: 70);

	ProxyDictChain.add(

		\chPan, \mix -> { |band =  2, channel = 1, focus = 0|
			var sig, input, chs;
			input = Ndef(\data_bandpower).ar;
			//input = LFNoise1.ar(1).range(1, 10) !20;
			input = input.reshape(5, 4);
			chs = SelectX.ar(band, input);
			sig = SelectXFocus.ar(channel, chs, focus);
			//sig = LFNoise1.ar(1).range(1, 50);
		},

		\scale, \mix -> { |band = 2, channel = 1, focus = 0|

			var input, old, test, min, max, out, buf, chs, sig, avg;

			// panning
			input = Ndef(\data_bandpower).ar + LFNoise1.ar(0, 110);
			input = input.reshape(5, 4);
			chs = SelectX.ar(band, input);
			avg = chs.sum / chs.numChannels;

			sig = SelectXFocus.ar(channel, chs, focus);
			sig = focus.linlin(0, 1, avg, sig);

			// scaling
			buf = LocalBuf(1, 1);


			old = Dbufrd(buf);
			old = Duty.ar(SampleDur.ir, 0, old);
			/*
			max = sig > max;

			max = Gate.ar(sig, Trig.ar(max, SampleDur.ir));
			max = Dbufwr(max, buf);
			max = Duty.ar(SampleDur.ir, 0, max);
			*/
			out = Sanitize.ar(sig.linlin(0.0, 110, 0.0, 1.0));
			out;
		},

		\rawout, \split -> { |in, rawout| in.asArray[0] * 1 },

		\shape, \split -> { |in, amp = 1, shape|
			// Index into the table with raw Signal
			Index.ar(t, in.asArray[0].abs * BufFrames.kr(t));
		},

		\out, \filter -> { |in|
			var phase, rate;
			in = in.asArray[0];
			rate = 44100 / (1024 * 0.5);
			phase = Phasor.ar(0, 1 / rate, 0, 1024);
			BufWr.ar(in, 50, phase);
			in;
		},

		\out2, \filter -> { |in|
			var phase, rate;
			in = in.asArray[0];
			rate = 44100 / (1024 * 0.5);
			phase = Phasor.ar(0, 1 / rate, 0, 1024);
			BufWr.ar(in, 51, phase);
			in;
		},
	);

	// Clear and set up proxy chain
	d.clear;
	c.clear;
	//d.clear;
	d = ProxyDictChain(\neu123, [\scale, \rawout, \shape, \out]);

	~app.(proxychains: [d], tbuf: t);
	~bandpower.();

};

Ndef(\x).play;
)

ProxyChain




Ndef(\x).play;
(
d.clear;
d = ProxyDictChain(\basic, [\inputsig,  \shaper, \out]);
~app.(proxychains: d, tbuf: t);
)

Ndef.all;

(

Ndef(\x).play;
)


s.plotTree;

d.remove(\shaper);

d.slotNames = [\inputsig, \shaper, \raw, \out];

ProxyDictChain(\basic, [\inputsig, \raw, \out]);
d.clear;

d.

Ndef(\map, {In.ar(17, 1).linlin(0, 1, 0.1, 2)});

Ndef(\x).xset(\freqmul, Ndef(\map));
d.slotNames.do({|slot| slot.postln});

ProxyChain;
(
~name = \shaper;
x = ProxyDictChain.allSources.keys.do({|source|
	if(source.asSymbol == ~name, {	source.postln;})
}
);
)
"hallotest3";
String
Ndef
(
Ndef(\x, {
	var snd = Saw.ar(50 * (\freqmul.ar(1) + 1) * [1, 1.01]).tanh * 0.5;
	RLPF.ar(snd, (\freq.ar + 100), 0.25);
	//DC.ar(0);
});
)
Ndef(\x).fadeTime
Ndef(\x).play;
x.key;

s.plotTree;
s.numSynths;

s.groups

Ndef(\y).play;
Ndef(\x).xset(\freqmul, 1.0);

Archive.global(\hible) = ();
ProxyChain
c.proxy;
x = [0, 1, 2, 3, 4];
x = x.rotate;
z = d.proxy;

c;
d;
d.proxy.key_(\hello);
Ndef(\x)[20] = \split -> {|in| DC.ar(0)};

// Bandpower chain



~bandpower.();
(
ProxyDictChain(\test3).add(\sig, \mix -> { |sinefreq = 1, dustdec = 0.02, dustfreq = 600|
	LFNoise1.ar(10).range(0, 1)});
)

ProxyDictChain(\test3).slotsInUse;

x = Ndef.all.deepCopy[\localhost];

(
x.do({|node|
	if(node.key == \test3, {
		x.remove(node.key);
	});
)
node.key.postln;});
ProxyChain

x[\x];

s.plotTree;

this.quit

[\hello, \hu]


Ndef(\poll, {In.ar(21)});

Ndef(\shaperbasic_x_freqmul).clear;

Group

Ndef(\tt, {Normalizer.ar(LFNoise1.ar(1).range(0, 20)).poll;});


(
{
	var array = { Saw.ar(rrand(1.0, 3.0)) * Saw.ar(rrand(100.0, 3000.0)) } ! 8;
	SelectXFocus.ar(MouseX.kr(-20, 20), array, 1, true) * 0.1
}.play
)


(
Ndef(\fogus3, {
	var a, sig, focus;
	a = DC.ar((1..4));

	focus = 1;

	//sig = a[2];
	sig = SelectXFocus.ar(1, a, focus) / a.numChannels * (focus * a.numChannels + 1);
	//sig = SelectX.ar(0, [sig, sig.sum / sig.])
	sig.poll;
	DC.ar(0);


}).play;
)


(
Ndef(\a, {
	var a, focus, sig, avg;

	// Beispiel-Signale
	a = DC.ar((1..4));  // Erzeugt vier konstante Signale
	avg = a.sum / a.numChannels;

	sig = SelectXFocus.ar(3, a, focus);
	sig = focus.linlin(0, 1, avg, sig);

	sig.poll;
	sig;
}).play;
)

Max



\band.asSpec.dump;
\freq.asSpec.dump;

Ndef(\a, {|freq| SinOsc.ar(100 + freq) * 0.5});

Ndef(\a).clea;

Ndef(\x).play;

Ndef(\b, {DC.ar(10)});
Ndef(\b).fadeTime = 4;

Ndef(\b).clear;

Ndef(\c).clear;

Ndef(\a).fadeTime = 4;

Ndef(\c, {LFNoise1.ar(100).range(1, 200)});

Ndef(\c).fadeTime = 10;
(
Ndef(\a).set(\freq, Ndef(\c));
)
Ndef(\a).play;

Ndef

Quarks.gui;

Ndef(\neushit);

Ndef(\neushit).numChannels
nil.fadeTime = 6;


nil.clear;
