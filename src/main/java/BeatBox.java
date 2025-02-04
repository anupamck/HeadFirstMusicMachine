import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Track;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static javax.sound.midi.ShortMessage.*;

public class BeatBox {

	private final Integer numberOfBeats = 16;
	private final Vector<String> listVector = new Vector<>();
	private final HashMap<String, boolean[]> otherSeqsMap = new HashMap<>();
	Map<String, Integer> instrumentMap = new LinkedHashMap<>();
	private JList<String> incomingList;
	private JTextArea userMessage;
	private String userName;

	private int nextNum;

	private ObjectOutputStream out;
	private ObjectInputStream in;

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
		new BeatBox().startUp(args[0]);
	}

	public void startUp(String name) {
		userName = name;
		// open connection to server
		try {
			Socket socket = new Socket("127.0.0.1", 4242);
			out = new ObjectOutputStream(socket.getOutputStream());
			in = new ObjectInputStream(socket.getInputStream());
			ExecutorService executor = Executors.newSingleThreadExecutor();
			executor.submit(new RemoteReader());
		} catch (Exception ex) {
			System.out.println("Couldn't connect - you'll have to play alone.");
		}
		setUpMidi();
		buildGUI();

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

		JButton sendIt = new JButton("sendIt");
		sendIt.addActionListener(e -> sendMessageAndTracks());
		buttonBox.add(sendIt);

		userMessage = new JTextArea();
		userMessage.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				handleEnter(e);
			}
		});
		userMessage.setLineWrap(true);
		userMessage.setWrapStyleWord(true);
		JScrollPane messageScroller = new JScrollPane(userMessage);
		messageScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		messageScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		buttonBox.add(messageScroller);

		incomingList = new JList<>();
		incomingList.addListSelectionListener(new MyListSelectionListener());
		incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane theList = new JScrollPane(incomingList);
		buttonBox.add(theList);
		incomingList.setListData(listVector);

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

	private void handleEnter(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			if (e.isShiftDown()) {
				userMessage.append("\n");
			} else {
				e.consume();
				sendMessageAndTracks();
			}
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

	private void sendMessageAndTracks() {
		int numberOfBoxes = instrumentMap.size() * numberOfBeats;
		boolean[] checkBoxState = new boolean[numberOfBoxes];
		for (int i = 0; i < numberOfBoxes; i++) {
			JCheckBox check = checkBoxList.get(i);
			if (check.isSelected()) {
				checkBoxState[i] = true;
			}
		}
		try {
			out.writeObject(userName + nextNum++ + ": " + userMessage.getText());
			out.writeObject(checkBoxState);
		} catch (IOException e) {
			System.out.println("Terribly sorry. Could not send it to the server.");
			e.printStackTrace();
		}
		userMessage.setText("");
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

	private void changeSequence(boolean[] checkboxState) {
		for (int i = 0; i < instrumentMap.size() * numberOfBeats; i++) {
			JCheckBox check = checkBoxList.get(i);
			check.setSelected(checkboxState[i]);
		}
	}

	public class MyListSelectionListener implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent lse) {
			if (!lse.getValueIsAdjusting()) {
				String selected = incomingList.getSelectedValue();
				if (selected != null) {
					// now go to the map and change the sequence
					boolean[] selectedState = otherSeqsMap.get(selected);
					changeSequence(selectedState);
					sequencer.stop();
					buildTrackAndStart();
				}
			}
		}
	}

	public class RemoteReader implements Runnable {
		public void run() {
			try {
				Object obj;
				while ((obj = in.readObject()) != null) {
					System.out.println("got an object from server");
					System.out.println(obj.getClass());

					String nameToShow = (String) obj;
					boolean[] checkBoxState = (boolean[]) in.readObject();
					otherSeqsMap.put(nameToShow, checkBoxState);

					listVector.add(nameToShow);
					incomingList.setListData(listVector);
				}
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
}
