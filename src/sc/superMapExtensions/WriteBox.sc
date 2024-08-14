WriteBox {

	var parent, addr, skin, <wrButton, <rdButton, <label, <zone;
	classvar sth;

	*new { |parent, addr, skin|
		^super.newCopyArgs(parent, addr, skin)
		.makeZone()
		.init()
	}

	makeZone{
		zone = CompositeView(parent, Rect(0, 0, parent.bounds.width, 20));
		zone.addFlowLayout(skin.margin, skin.gap);
	}

	init{
		var rangebox, rect, text, filepath;

		text = "File Path:";
		rect = Rect(0, 0, 50, skin.buttonHeight);
		label = StaticText(zone,  Rect(rect.left, rect.top, text.size * 7 , rect.height));
		label.string = text;

		filepath =  TextField(zone,Rect(10,10, 50,20)).focus(true);

		rdButton = Button.new(zone, rect);
		rdButton.states_( [["read", Color.grey, Color.white],["read", Color.black, Color.red]]);
		rdButton.action_({
			addr.sendMsg("/py/write", filepath.string);
		});

		wrButton = Button.new(zone, rect);
		wrButton.states_( [["write", Color.grey, Color.white],["write", Color.black,skin.onColor]]);
		wrButton.action_({
			addr.sendMsg("/py/write", filepath.string);
		});
		//addr.sendMsg("/gui/fadeTime", numbox.value);
	}

}

