//--supercollider openbci generate dummy synthetic test data

OSCDataGUI : OpenBCI {
	classvar <numChannels = 8;
	classvar <defaultSampleRate = 250;
	classvar oscData = 0;
	var task, muted;
	uVScale {|gain= 24| ^4.5/gain/(2**23-1)*10000000}
	accScale {^0.002/(2**4)}

	*new {|numChannels= 4, sampleRate= 200, dataAction, initAction, bufferSize= 512|
		^super.new(dataAction, nil, initAction, bufferSize).initSyntheticData(numChannels, sampleRate);
	}
	initSyntheticData {|argNumChannels, argSampleRate|
		currentSampleRate= argSampleRate;
		numChannels= argNumChannels;
		buffer= {List.fill(bufferSize, {0})}.dup(numChannels);  //need to recreate buffer because numChannels is nil
		muted= false.dup(numChannels);
		oscData = (0 ! numChannels);

		initAction.value(this, "init synthetic data");
		CmdPeriod.add(this);
	}
	start {
		var addr_base = '/openbci/time-series-raw/';
		task.stop;
		oscData = 0 ! numChannels;

		// Update sr according to sending board
		OSCdef(\sr, { |msg|
			currentSampleRate = msg[1];
		}, "/board/sr");


		(1..numChannels).do({|channel, index|

			var name = \ch ++ channel.asSymbol;
			var address = addr_base ++ name.asSymbol;

			OSCdef(name, { |msg|
				//["raw value from gui", msg[1]].postln;

				oscData[index] = msg[1] * this.uVScale(24);
				//["scaled value", oscData[index]].postln;
			}, address);
			("trying to find msg here: " ++ address).postln;


		});

		task= Routine({this.prTask}).play(SystemClock);
	}
	stop {
		"%: stopping synthetic test data".format(this.class.name).postln;
		task.stop;
		CmdPeriod.remove(this);
	}
	softReset {
		currentSampleRate= this.class.defaultSampleRate;
	}
	close {
		this.stop;
	}
	cmdPeriod {
		this.stop;
	}

	off {|channel= 1|
		muted.clipPut(channel-1, true);
	}
	on {|channel= 1|
		muted.clipPut(channel-1, false);
	}

	//--private
	prTask {
		var theta= 0;
		var atheta= 0.0;
		var aux= [0, 0, 0];  //TODO
		var lastTime= 0, deltaTime;

		inf.do{|num|
			/*
			numChannels.do{|i|


				var val;

				if(muted[i].not, {
					val= 0.gauss(1)*(currentSampleRate/2).sqrt;
					switch(i,
						0, {
							val= val*10;
						},
						1, {
							val= val+(10*2.sqrt*sin(theta*10));
						},
						2, {
							val= val+(20*2.sqrt*sin(theta*15));
						},
						3, {
							val= val+(30*2.sqrt*sin(theta*20));
						},
						4, {
							val= val+(40*2.sqrt*sin(theta*25));
						},
						5, {
							val= val+(50*2.sqrt*sin(theta*30));
						},
						6, {
							val= val+(20*2.sqrt*sin(theta*60));
						}
					);
					data[i]= (val/this.uVScale(24)).round*this.uVScale(24);  //TODO deal with gain changes
				});
			};
			*/



			data = oscData;
			//theta= theta+(1/currentSampleRate*2pi);

			deltaTime= Main.elapsedTime-lastTime;
			/*
			if(deltaTime>=0.04, {  //25Hz
				accel= {|j| (sin(atheta*2pi/3.75+(j*0.5pi))*32767).asInteger*this.accScale}.dup(3);
				accelAction.value(accel);
				atheta= atheta+deltaTime;
				lastTime= Main.elapsedTime;
			}, {
				accel= #[0.0, 0.0, 0.0];
			});
			*/

			this.updateBuffer(data);
			dataAction.value(num.asInteger%256, data);

			(1/currentSampleRate).wait;
		};
	}
}

