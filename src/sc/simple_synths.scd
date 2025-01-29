// Boot Server


// Play Some Synth to Control them with EEsuperMap
(
Ndef(\blips, { |dens = 10, freq = 100, decay = 5|

	var snd, trig, a, r1, r2, dec, pos;

	trig = Dust.ar(dens);
	dec = TRand.ar(0.01, decay  * 0.1, trig);
	pos = TRand.ar(-1, 1, trig);

	r1 = Ringz.ar(trig, freq, dec);
	r2 = Ringz.ar(trig, freq * 4, dec * 0.5);

	a = [SinOsc.ar(50) * 2, WhiteNoise.ar(0.5), r1, r1, r2, SinOsc.ar(freq * 2, 0, 0.7), PinkNoise.ar(0.75)] * 0.5;

	snd = TChoose.ar(trig, a.tanh);
	snd = Pan2.ar(snd, pos);
});

Ndef(\bass, { |width = 4, freq = 100, rq = 5|

	var snd;

	snd = VarSaw.ar(50 * [1, 1.01], 0, width / 10);
	snd = RLPF.ar(snd, freq, rq * 0.1) * 5;
	snd = Compander.ar(snd, Ndef(\blips).ar * 40, 0.5, 1.0, 0.001).tanh;
	snd * 0.25;

});

Ndef(\mix, {(Ndef(\blips).ar + Ndef(\bass).ar).tanh * 0.5});
Ndef(\mix).play;

// Start EEGsuperMap
~superMap.(\neu2);
)

