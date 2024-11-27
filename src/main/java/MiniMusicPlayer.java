import javax.sound.midi.*;
import javax.swing.*;

import java.awt.*;
import java.util.Random;

import static javax.sound.midi.ShortMessage.*;

public class MiniMusicPlayer {
	private final Random random = new Random();
	private MyDrawPanel panel;

	public static void main(String[] args) {
		MiniMusicPlayer mini = new MiniMusicPlayer();
		mini.go();
	}

	public void go() {
		setUpGui();

		try {
			Sequencer sequencer = MidiSystem.getSequencer();
			sequencer.open();

			sequencer.addControllerEventListener(panel, new int[]{127});

			Sequence seq = new Sequence(Sequence.PPQ, 4);
			Track track = seq.createTrack();

			int note;
			for (int i = 5; i < 100; i += 2) {
				note = random.nextInt(100) + 1;
				track.add(Utility.makeEvent(NOTE_ON, 1, note, 100, i));
				track.add(Utility.makeEvent(CONTROL_CHANGE, 1, 127, 0, i));
				track.add(Utility.makeEvent(NOTE_OFF, 1, note, 100, i + 2));
			}
			sequencer.setSequence(seq);
			sequencer.start();
			sequencer.setTempoInBPM(120);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void setUpGui() {
		JFrame frame = new JFrame("My First Music Video");
		panel = new MyDrawPanel();
		frame.setContentPane(panel);
		frame.setBounds(30, 30, 300, 300);
		frame.setVisible(true);
	}

	class MyDrawPanel extends JPanel implements ControllerEventListener {
		private boolean msg = false;

		public void controlChange(ShortMessage event) {
			msg = true;
			repaint();
		}

		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (msg) {
				int r = random.nextInt(250);
				int gr = random.nextInt(250);
				int b = random.nextInt(250);

				g.setColor(new Color(r, gr, b));

				int height = random.nextInt(120) + 10;
				int width = random.nextInt(120) + 10;

				int xPos = random.nextInt(40) + 10;
				int yPos = random.nextInt(40) + 10;

				g.fillRect(xPos, yPos, width, height);
				msg = false;
			}
		}
	}
}
