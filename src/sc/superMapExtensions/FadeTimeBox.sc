FadeTimeBox {

	var parent, addr, skin, <numbox, <label, <zone;
	classvar sth;

	*new { |parent, addr, skin|
		^super.newCopyArgs(parent, addr, skin)
		.makeZone()
		.init()
	}

	makeZone{
		zone = CompositeView(parent, Rect(0, 0, parent.bounds.width * 0.5, 20));
		zone.addFlowLayout(skin.margin, skin.gap);
	}

	init{
		var rangebox, rect, text;

		text = "Fade Time: ";
		rect = Rect(0, 0, 30, skin.buttonHeight);
		label = StaticText(zone,  Rect(rect.left, rect.top, text.size * 7 , rect.height));
		label.string = text;

		numbox = NumberBox.new(zone, rect);
		numbox.action = {| num |
			addr.sendMsg("/gui/fadeTime", num.value);
		};
		numbox.value = 0.1;
		addr.sendMsg("/gui/fadeTime", numbox.value);
	}

}

