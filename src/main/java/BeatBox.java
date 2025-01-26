import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Track;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

import static javax.sound.midi.ShortMessage.*;

public class BeatBox {
	private final Integer numberOfBeats = 16;
	Map<String, Integer> instrumentMap = new LinkedHashMap<>();
	private boolean isPlaying = false;
	private ArrayList<JCheckBox> checkBoxList;
	private Sequencer sequencer;
	private Sequence sequence;
	private Track track;

	private JFrame frame;

	public BeatBox() {
		instrumentMap.put("Bass Drum", 35);
		instrumentMap.put("Closed Hi-Hat", 42);
		instrumentMap.put("Open Hi-Hat", 46);
		instrumentMap.put("Acoustic Snare", 38);
		instrumentMap.put("Crash Cymbal", 49);
		instrumentMap.put("Hand Clap", 39);
		instrumentMap.put("High Tom", 50);
		instrumentMap.put("Hi Bongo", 60);
		instrumentMap.put("Maracas", 70);
		instrumentMap.put("Whistle", 72);
		instrumentMap.put("Low Conga", 64);
		instrumentMap.put("Cowbell", 56);
		instrumentMap.put("Vibraslap", 58);
		instrumentMap.put("Low-mid Tom", 47);
		instrumentMap.put("High Agogo", 67);
		instrumentMap.put("Open Hi Conga", 53);
	}

	public static void main(String[] args) {
		new BeatBox().buildGUI();
	}

	public void buildGUI() {
		frame = new JFrame("Cyber BeatBox");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		BorderLayout layout = new BorderLayout();
		JPanel background = new JPanel(layout);
		background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		Box buttonBox = new Box(BoxLayout.Y_AXIS);

		JButton start = new JButton("Start");
		start.addActionListener(e -> buildTrackAndStart());
		buttonBox.add(start);

		JButton stop = new JButton("Stop");
		stop.addActionListener(e -> stopPlayer());
		buttonBox.add(stop);

		JButton upTempo = new JButton("Tempo Up");
		upTempo.addActionListener(e -> changeTempo(1.03f));
		buttonBox.add(upTempo);

		JButton downTempo = new JButton("Tempo Down");
		downTempo.addActionListener(e -> changeTempo(0.97f));
		buttonBox.add(downTempo);

		JButton clear = new JButton("Clear");
		clear.addActionListener(e -> clearChecks());
		buttonBox.add(clear);

		JButton savePattern = new JButton("Save Pattern");
		savePattern.addActionListener(e -> savePattern());
		buttonBox.add(savePattern);

		JButton loadPattern = new JButton("Load Pattern");
		loadPattern.addActionListener(e -> loadPattern());
		buttonBox.add(loadPattern);

		Box nameBox = new Box(BoxLayout.Y_AXIS);
		for (String instrumentName : instrumentMap.keySet()) {
			JLabel instrumentLabel = new JLabel(instrumentName);
			instrumentLabel.setBorder(BorderFactory.createEmptyBorder(4, 1, 4, 1));
			nameBox.add(instrumentLabel);
		}

		background.add(BorderLayout.EAST, buttonBox);
		background.add(BorderLayout.WEST, nameBox);

		frame.getContentPane().add(background);

		GridLayout grid = new GridLayout(instrumentMap.size(), numberOfBeats);
		grid.setVgap(1);
		grid.setHgap(2);

		JPanel mainPanel = new JPanel(grid);
		background.add(BorderLayout.CENTER, mainPanel);

		checkBoxList = new ArrayList<>();
		for (int i = 0; i < instrumentMap.size() * numberOfBeats; i++) {
			JCheckBox c = new JCheckBox();
			c.setSelected(false);
			c.addActionListener(e -> addCheck());
			checkBoxList.add(c);
			mainPanel.add(c);
		}

		setUpMidi();

		frame.setBounds(50, 50, 300, 300);
		frame.pack();
		frame.setVisible(true);
	}

	private void stopPlayer() {
		sequencer.stop();
		isPlaying = false;
	}

	private void addCheck() {
		if (isPlaying) {
			buildTrackAndStart();
		}
	}

	private void setUpMidi() {
		try {
			sequencer = MidiSystem.getSequencer();
			sequencer.open();
			sequence = new Sequence(Sequence.PPQ, 4);
			track = sequence.createTrack();
			sequencer.setTempoInBPM(120);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void buildTrackAndStart() {
		sequence.deleteTrack(track);
		track = sequence.createTrack();

		int[] trackList;

		List<Integer> instrumentValues = new ArrayList<>(instrumentMap.values());

		for (int i = 0; i < instrumentMap.size(); i++) {
			trackList = new int[numberOfBeats];
			int key = instrumentValues.get(i);

			for (int j = 0; j < numberOfBeats; j++) {
				JCheckBox jc = checkBoxList.get(j + numberOfBeats * i);
				if (jc.isSelected()) {
					trackList[j] = key;
				} else {
					trackList[j] = 0;
				}
			}

			makeTracks(trackList);
			track.add(Utility.makeEvent(CONTROL_CHANGE, 1, 127, 0, numberOfBeats));
		}

		track.add(Utility.makeEvent(PROGRAM_CHANGE, 9, 1, 0, numberOfBeats - 1));

		try {
			sequencer.setSequence(sequence);
			sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
			sequencer.setTempoInBPM(120);
			sequencer.start();
			isPlaying = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void changeTempo(float tempoMultiplier) {
		float tempoFactor = sequencer.getTempoFactor();
		sequencer.setTempoFactor(tempoFactor * tempoMultiplier);
		if (isPlaying) {
			buildTrackAndStart();
		}
	}

	private void clearChecks() {
		for (JCheckBox checkBox : checkBoxList) {
			checkBox.setSelected(false);
		}
		if (isPlaying) {
			sequencer.stop();
		}
	}

	private void makeTracks(int[] list) {
		for (int i = 0; i < numberOfBeats; i++) {
			int key = list[i];

			if (key != 0) {
				track.add(Utility.makeEvent(NOTE_ON, 9, key, 100, i));
				track.add(Utility.makeEvent(NOTE_OFF, 9, key, 100, i + 1));
			}
		}
	}

	private void savePattern() {
		if (isPlaying) {
			sequencer.stop();
		}
		boolean[] checkboxState = new boolean[instrumentMap.size() * numberOfBeats];

		for (int i = 0; i < instrumentMap.size() * numberOfBeats; i++) {
			JCheckBox box = checkBoxList.get(i);
			if (box.isSelected()) {
				checkboxState[i] = true;
			}
		}
		JFileChooser fileSave = new JFileChooser();
		File saveFolder = new File(System.getProperty("user.dir"), "savedPatterns");
		fileSave.setCurrentDirectory(saveFolder);
		fileSave.showSaveDialog(frame);
		File selectedFile = fileSave.getSelectedFile();
		if (!(selectedFile == null)) {
			try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(fileSave.getSelectedFile() + ".ser"))) {
				os.writeObject(checkboxState);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void loadPattern() {
		if (isPlaying) {
			sequencer.stop();
		}
		clearChecks();
		JFileChooser patternOpen = new JFileChooser();
		File loadFolder = new File(System.getProperty("user.dir"), "savedPatterns");
		patternOpen.setCurrentDirectory(loadFolder);
		patternOpen.showOpenDialog(frame);
		File selectedFile = patternOpen.getSelectedFile();
		if (!(selectedFile == null)) {
			try (ObjectInputStream is = new ObjectInputStream(new FileInputStream(patternOpen.getSelectedFile()))) {
				boolean[] savedBoxes = (boolean[]) is.readObject();
				for (int i = 0; i < instrumentMap.size() * numberOfBeats; i++) {
					if (savedBoxes[i]) {
						checkBoxList.get(i).setSelected(true);
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
}
