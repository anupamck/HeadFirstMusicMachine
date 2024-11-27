import javax.sound.midi.*;

import static javax.sound.midi.ShortMessage.*;

public class MiniMiniMusicApp {

	public static void main(String[] args) {
		MiniMiniMusicApp mini = new MiniMiniMusicApp();
		mini.play();
	}

	public void play() {
		try {
			Sequencer player = MidiSystem.getSequencer();
			player.open();

			Sequence seq = new Sequence(Sequence.PPQ, 4);

			Track track = seq.createTrack();

			ShortMessage msg0 = new ShortMessage();
			msg0.setMessage(192, 1, 105, 0);
			MidiEvent noteOn0 = new MidiEvent(msg0, 1);
			track.add(noteOn0);

			ShortMessage msg1 = new ShortMessage();
			msg1.setMessage(NOTE_ON, 1, 44, 100);
			MidiEvent noteOn = new MidiEvent(msg1, 1);
			track.add(noteOn);

			ShortMessage msg2 = new ShortMessage();
			msg2.setMessage(NOTE_OFF, 1, 44, 100);
			MidiEvent noteOff = new MidiEvent(msg2, 16);
			track.add(noteOff);

			player.setSequence(seq);

			player.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
