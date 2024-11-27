import javax.sound.midi.*;

import static javax.sound.midi.ShortMessage.*;

public class MiniMusicCmdLine {

	public static void main(String[] args) {
		MiniMusicCmdLine mini = new MiniMusicCmdLine();
		if (args.length < 2) {
			System.out.println("You have one job! Give me two arguments");
		} else {
			int instrument = Integer.parseInt(args[0]);
			int note = Integer.parseInt(args[1]);
			mini.play(instrument, note);
		}
	}

	public void play(int instrument, int note) {
		try {
			Sequencer player = MidiSystem.getSequencer();
			player.open();

			Sequence seq = new Sequence(Sequence.PPQ, 4);

			Track track = seq.createTrack();

			ShortMessage msg0 = new ShortMessage();
			msg0.setMessage(PROGRAM_CHANGE, 1, instrument, 0);
			MidiEvent noteOn0 = new MidiEvent(msg0, 1);
			track.add(noteOn0);

			ShortMessage msg1 = new ShortMessage();
			msg1.setMessage(NOTE_ON, 1, note, 100);
			MidiEvent noteOn = new MidiEvent(msg1, 1);
			track.add(noteOn);

			ShortMessage msg2 = new ShortMessage();
			msg2.setMessage(NOTE_OFF, 1, note, 100);
			MidiEvent noteOff = new MidiEvent(msg2, 16);
			track.add(noteOff);

			player.setSequence(seq);
			player.start();
//			player.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
