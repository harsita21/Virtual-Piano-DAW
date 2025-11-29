import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.sound.midi.*;
import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.text.SimpleDateFormat;

public class VP_DAW extends Frame implements MouseListener, ActionListener {

    private Label noteLabel;
    private Rectangle[] whiteKeys;
    private Rectangle[] blackKeys;
    private Map<Rectangle, Integer> keyToMidi; 
    private Synthesizer synthesizer;
    private MidiChannel channel;
    
    // Recording functionality
    private boolean isRecording = false;
    private ArrayList<MidiEvent> recordedEvents;
    private long recordStartTime;
    private Button recordButton;
    private Button playButton;
    private Button tempoButton;
    private int tempo = 120; // Default tempo (BPM)
    
    // Saved recordings
    private List<String> savedRecordings;
    private java.awt.List recordingsList;
    private Button deleteButton;
    private Button renameButton;
    private Button downloadButton;
    private Button playRecordingButton;

    private static final int OCTAVES = 3; 
    private static final int WHITE_KEY_WIDTH = 40;
    private static final int WHITE_KEY_HEIGHT = 200;
    private static final int BLACK_KEY_WIDTH = 25;
    private static final int BLACK_KEY_HEIGHT = 120;

    // MIDI template for one octave
    private static final int[] WHITE_NOTES = {0, 2, 4, 5, 7, 9, 11};
    private static final int[] BLACK_NOTES = {1, 3, 6, 8, 10};
    
    // UI Colors
    private final Color BACKGROUND_COLOR = new Color(0x3f, 0x13, 0x70); // Purple #3f1370
    private final Color BUTTON_COLOR = new Color(0x9b, 0x59, 0xb6); // Light purple
    private final Color TEXT_COLOR = Color.WHITE;
    
    private PianoCanvas canvas;

    public VP_DAW() {
        setTitle("🎹 Virtual Piano DAW");
        setSize(WHITE_KEY_WIDTH * 7 * OCTAVES + 300, 500);
        setBackground(BACKGROUND_COLOR);
        setLayout(new BorderLayout(10, 10));

        // Label
        noteLabel = new Label("Click a key to play a note!", Label.CENTER);
        noteLabel.setFont(new Font("Arial", Font.BOLD, 18));
        noteLabel.setForeground(TEXT_COLOR);
        noteLabel.setBackground(BACKGROUND_COLOR);
        add(noteLabel, BorderLayout.NORTH);

        // Initialize piano keys
        initializePianoKeys();
        
        // Main panel with piano
        Panel centerPanel = new Panel(new BorderLayout());
        centerPanel.setBackground(BACKGROUND_COLOR);
        canvas = new PianoCanvas();
        centerPanel.add(canvas, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
        canvas.addMouseListener(this);

        // Control panel at bottom
        Panel controlPanel = new Panel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        controlPanel.setBackground(BACKGROUND_COLOR);
        
        recordButton = createButton("⏺ Record");
        playButton = createButton("▶ Play");
        tempoButton = createButton("Tempo: " + tempo + " BPM");
        
        controlPanel.add(recordButton);
        controlPanel.add(playButton);
        controlPanel.add(tempoButton);
        
        add(controlPanel, BorderLayout.SOUTH);
        
        // Recordings panel on the right
        Panel recordingsPanel = new Panel(new BorderLayout(5, 5));
        recordingsPanel.setBackground(BACKGROUND_COLOR);
        recordingsPanel.setPreferredSize(new Dimension(200, 0));
        
        Label recordingsLabel = new Label("Saved Recordings", Label.CENTER);
        recordingsLabel.setFont(new Font("Arial", Font.BOLD, 14));
        recordingsLabel.setForeground(TEXT_COLOR);
        recordingsLabel.setBackground(BACKGROUND_COLOR);
        recordingsPanel.add(recordingsLabel, BorderLayout.NORTH);
        
        // Initialize the list of saved recordings
        savedRecordings = new ArrayList<>();
        recordingsList = new java.awt.List(10);
        recordingsList.setBackground(new Color(0x55, 0x33, 0x77));
        recordingsList.setForeground(TEXT_COLOR);
        recordingsPanel.add(recordingsList, BorderLayout.CENTER);
        
        // Recording control buttons
        Panel recordingControls = new Panel(new GridLayout(1, 4, 5, 5));
        recordingControls.setBackground(BACKGROUND_COLOR);
        
        playRecordingButton = createButton("Play");
        renameButton = createButton("Rename");
        deleteButton = createButton("Delete");
        downloadButton = createButton("Download");
        
        recordingControls.add(playRecordingButton);
        recordingControls.add(renameButton);
        recordingControls.add(deleteButton);
        recordingControls.add(downloadButton);
        
        recordingsPanel.add(recordingControls, BorderLayout.SOUTH);
        
        add(recordingsPanel, BorderLayout.EAST);

        // MIDI setup
        try {
            synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            channel = synthesizer.getChannels()[0];
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Close on exit
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (synthesizer != null) synthesizer.close();
                dispose();
            }
        });

        setVisible(true);
    }
    
    private Button createButton(String text) {
        Button button = new Button(text);
        button.setBackground(BUTTON_COLOR);
        button.setForeground(TEXT_COLOR);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.addActionListener(this);
        return button;
    }
    
    private void initializePianoKeys() {
        whiteKeys = new Rectangle[7 * OCTAVES];
        blackKeys = new Rectangle[5 * OCTAVES];
        keyToMidi = new HashMap<>();

        int baseNote = 48; // Start at C3 (MIDI 48)

        for (int o = 0; o < OCTAVES; o++) {
            int whiteOffset = o * 7;
            int blackOffset = o * 5;

            // White keys
            for (int i = 0; i < 7; i++) {
                int x = (whiteOffset + i) * WHITE_KEY_WIDTH;
                whiteKeys[whiteOffset + i] = new Rectangle(x, 0, WHITE_KEY_WIDTH, WHITE_KEY_HEIGHT);
                int midiNote = baseNote + o * 12 + WHITE_NOTES[i];
                keyToMidi.put(whiteKeys[whiteOffset + i], midiNote);
            }

            // Black keys (C#, D#, F#, G#, A#)
            int blackIndex = 0;
            for (int i = 0; i < 7; i++) {
                if (i != 2 && i != 6) { 
                    int x = (whiteOffset + i) * WHITE_KEY_WIDTH + (WHITE_KEY_WIDTH - BLACK_KEY_WIDTH / 2);
                    blackKeys[blackOffset + blackIndex] = new Rectangle(x, 0, BLACK_KEY_WIDTH, BLACK_KEY_HEIGHT);
                    int midiNote = baseNote + o * 12 + BLACK_NOTES[blackIndex];
                    keyToMidi.put(blackKeys[blackOffset + blackIndex], midiNote);
                    blackIndex++;
                }
            }
        }
    }

    // Canvas
    class PianoCanvas extends Canvas {
        public PianoCanvas() {
            setPreferredSize(new Dimension(WHITE_KEY_WIDTH * 7 * OCTAVES, WHITE_KEY_HEIGHT + 50));
        }
        
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;

            // White keys
            for (Rectangle r : whiteKeys) {
                GradientPaint gp = new GradientPaint(r.x, 0, Color.WHITE, r.x, WHITE_KEY_HEIGHT, Color.LIGHT_GRAY);
                g2.setPaint(gp);
                g2.fill(r);
                g2.setColor(Color.BLACK);
                g2.drawRect(r.x, r.y, r.width, r.height);
            }

            // Black keys
            for (Rectangle r : blackKeys) {
                if (r != null) {
                    g2.setColor(Color.BLACK);
                    g2.fillRect(r.x, r.y, r.width, r.height);
                }
            }
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        
        if (source == recordButton) {
            toggleRecording();
        } else if (source == playButton) {
            playRecording();
        } else if (source == tempoButton) {
            changeTempo();
        } else if (source == playRecordingButton) {
            playSelectedRecording();
        } else if (source == renameButton) {
            renameSelectedRecording();
        } else if (source == deleteButton) {
            deleteSelectedRecording();
        } else if (source == downloadButton) {
            downloadSelectedRecording();
        }
    }
    
    private void toggleRecording() {
        if (!isRecording) {
            // Start recording
            recordedEvents = new ArrayList<>();
            recordStartTime = System.currentTimeMillis();
            isRecording = true;
            recordButton.setLabel("⏹ Stop");
            recordButton.setBackground(Color.RED);
            noteLabel.setText("Recording...");
        } else {
            // Stop recording
            isRecording = false;
            recordButton.setLabel("⏺ Record");
            recordButton.setBackground(BUTTON_COLOR);
            noteLabel.setText("Recording stopped");
            
            // Save recording
            saveRecording();
        }
    }
    
    private void saveRecording() {
        if (recordedEvents.isEmpty()) {
            noteLabel.setText("No notes recorded");
            return;
        }
        
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = "recording_" + timestamp + ".wav";
        
        try {
            // Convert MIDI events to audio and save as WAV
            saveMidiToWav(recordedEvents, filename);
            savedRecordings.add(filename);
            recordingsList.add(filename);
            recordingsList.select(recordingsList.getItemCount() - 1);
            noteLabel.setText("Saved: " + filename);
        } catch (Exception ex) {
            ex.printStackTrace();
            noteLabel.setText("Error saving recording");
        }
    }
    
    private void saveMidiToWav(ArrayList<MidiEvent> events, String filename) throws Exception {
        // This is a simplified implementation
        // In a real application, you would need to properly convert MIDI to audio
        
        // Create a byte array output stream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // Write WAV header (simplified)
        writeWavHeader(baos, 44100, 16, 1, events.size() * 1000); // Approximate length
        
        // For each MIDI event, generate audio samples
        // This is a very simplified approach - a real implementation would be more complex
        for (MidiEvent event : events) {
            // Generate simple sine wave samples for the note
            // In reality, you'd use the MIDI synthesizer to generate proper audio
            int note = event.getNote();
            int velocity = event.getVelocity();
            long duration = event.getDuration();
            
            // Simplified audio generation
            generateTone(baos, note, velocity, duration);
        }
        
        // Write to file
        FileOutputStream fos = new FileOutputStream(filename);
        baos.writeTo(fos);
        fos.close();
    }
    
    private void writeWavHeader(ByteArrayOutputStream baos, int sampleRate, int bitsPerSample, 
                               int channels, int dataSize) throws IOException {
        // WAV header implementation
        byte[] header = new byte[44];
        
        // RIFF header
        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
        int fileSize = dataSize + 36;
        header[4] = (byte)(fileSize & 0xff);
        header[5] = (byte)((fileSize >> 8) & 0xff);
        header[6] = (byte)((fileSize >> 16) & 0xff);
        header[7] = (byte)((fileSize >> 24) & 0xff);
        
        // WAVE header
        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';
        
        // fmt chunk
        header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0; // chunk size
        header[20] = 1; header[21] = 0; // PCM format
        header[22] = (byte)channels; header[23] = 0; // channels
        header[24] = (byte)(sampleRate & 0xff);
        header[25] = (byte)((sampleRate >> 8) & 0xff);
        header[26] = (byte)((sampleRate >> 16) & 0xff);
        header[27] = (byte)((sampleRate >> 24) & 0xff);
        
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        header[28] = (byte)(byteRate & 0xff);
        header[29] = (byte)((byteRate >> 8) & 0xff);
        header[30] = (byte)((byteRate >> 16) & 0xff);
        header[31] = (byte)((byteRate >> 24) & 0xff);
        
        header[32] = (byte)(channels * bitsPerSample / 8); // block align
        header[33] = 0;
        header[34] = (byte)bitsPerSample; // bits per sample
        header[35] = 0;
        
        // data chunk
        header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';
        header[40] = (byte)(dataSize & 0xff);
        header[41] = (byte)((dataSize >> 8) & 0xff);
        header[42] = (byte)((dataSize >> 16) & 0xff);
        header[43] = (byte)((dataSize >> 24) & 0xff);
        
        baos.write(header);
    }
    
    private void generateTone(ByteArrayOutputStream baos, int note, int velocity, long duration) {
        // Simplified tone generation
        // In a real application, you would use proper audio synthesis
        double frequency = 440.0 * Math.pow(2, (note - 69) / 12.0);
        int sampleRate = 44100;
        int samples = (int)(duration * sampleRate / 1000);
        
        for (int i = 0; i < samples; i++) {
            double angle = 2.0 * Math.PI * i * frequency / sampleRate;
            short sample = (short)(Short.MAX_VALUE * Math.sin(angle) * velocity / 127.0);
            
            byte[] bytes = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(sample).array();
            baos.write(bytes, 0, bytes.length);
        }
    }
    
    private void playRecording() {
        if (recordedEvents == null || recordedEvents.isEmpty()) {
            noteLabel.setText("No recording to play");
            return;
        }
        
        noteLabel.setText("Playing recording...");
        
        // Play recorded events with proper timing
        new Thread(() -> {
            long startTime = System.currentTimeMillis();
            
            for (MidiEvent event : recordedEvents) {
                long eventTime = event.getTimestamp() - recordStartTime;
                long currentTime = System.currentTimeMillis() - startTime;
                
                if (currentTime < eventTime) {
                    try {
                        Thread.sleep(eventTime - currentTime);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
                
                if (event.getType() == MidiEvent.Type.NOTE_ON) {
                    channel.noteOn(event.getNote(), event.getVelocity());
                } else if (event.getType() == MidiEvent.Type.NOTE_OFF) {
                    channel.noteOff(event.getNote());
                }
            }
            
            noteLabel.setText("Playback finished");
        }).start();
    }
    
    private void changeTempo() {
        // Simple tempo adjustment dialog
        String input = javax.swing.JOptionPane.showInputDialog(this, 
                "Enter tempo (BPM):", String.valueOf(tempo));
        
        if (input != null) {
            try {
                int newTempo = Integer.parseInt(input);
                if (newTempo > 0 && newTempo <= 300) {
                    tempo = newTempo;
                    tempoButton.setLabel("Tempo: " + tempo + " BPM");
                } else {
                    noteLabel.setText("Tempo must be between 1 and 300");
                }
            } catch (NumberFormatException ex) {
                noteLabel.setText("Invalid tempo value");
            }
        }
    }
    
    private void playSelectedRecording() {
        int selectedIndex = recordingsList.getSelectedIndex();
        if (selectedIndex >= 0) {
            String filename = savedRecordings.get(selectedIndex);
            noteLabel.setText("Playing: " + filename);
            
            // Play the WAV file
            playWavFile(filename);
        } else {
            noteLabel.setText("No recording selected");
        }
    }
    
    private void playWavFile(String filename) {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(filename));
            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            clip.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            noteLabel.setText("Error playing recording");
        }
    }
    
    private void renameSelectedRecording() {
        int selectedIndex = recordingsList.getSelectedIndex();
        if (selectedIndex >= 0) {
            String oldName = savedRecordings.get(selectedIndex);
            String newName = javax.swing.JOptionPane.showInputDialog(this, 
                    "Rename recording:", oldName);
            
            if (newName != null && !newName.trim().isEmpty()) {
                if (!newName.endsWith(".wav")) {
                    newName += ".wav";
                }
                
                File oldFile = new File(oldName);
                File newFile = new File(newName);
                
                if (oldFile.renameTo(newFile)) {
                    savedRecordings.set(selectedIndex, newName);
                    recordingsList.remove(selectedIndex);
                    recordingsList.add(newName, selectedIndex);
                    recordingsList.select(selectedIndex);
                    noteLabel.setText("Renamed to: " + newName);
                } else {
                    noteLabel.setText("Error renaming file");
                }
            }
        } else {
            noteLabel.setText("No recording selected");
        }
    }
    
    private void deleteSelectedRecording() {
        int selectedIndex = recordingsList.getSelectedIndex();
        if (selectedIndex >= 0) {
            String filename = savedRecordings.get(selectedIndex);
            int response = javax.swing.JOptionPane.showConfirmDialog(this, 
                    "Delete " + filename + "?", "Confirm Delete", 
                    javax.swing.JOptionPane.YES_NO_OPTION);
            
            if (response == javax.swing.JOptionPane.YES_OPTION) {
                File file = new File(filename);
                if (file.delete()) {
                    savedRecordings.remove(selectedIndex);
                    recordingsList.remove(selectedIndex);
                    noteLabel.setText("Deleted: " + filename);
                } else {
                    noteLabel.setText("Error deleting file");
                }
            }
        } else {
            noteLabel.setText("No recording selected");
        }
    }
    
    private void downloadSelectedRecording() {
        int selectedIndex = recordingsList.getSelectedIndex();
        if (selectedIndex >= 0) {
            String filename = savedRecordings.get(selectedIndex);
            noteLabel.setText("Download functionality would save: " + filename);
            // In a real application, this would implement proper file download
        } else {
            noteLabel.setText("No recording selected");
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        Point click = e.getPoint();

        // Black keys first
        for (Rectangle key : blackKeys) {
            if (key != null && key.contains(click)) {
                playKey(key, true);
                return;
            }
        }

        // White keys
        for (Rectangle key : whiteKeys) {
            if (key.contains(click)) {
                playKey(key, true);
                return;
            }
        }
    }

    private void playKey(Rectangle key, boolean record) {
        int midiNote = keyToMidi.get(key);
        channel.noteOn(midiNote, 600);
        noteLabel.setText("You played: " + midiNoteToName(midiNote));
        
        if (record && isRecording) {
            long timestamp = System.currentTimeMillis();
            recordedEvents.add(new MidiEvent(MidiEvent.Type.NOTE_ON, midiNote, 600, timestamp));
        }
        
        new Timer().schedule(new TimerTask() {
            public void run() {
                channel.noteOff(midiNote);
                
                if (record && isRecording) {
                    long timestamp = System.currentTimeMillis();
                    recordedEvents.add(new MidiEvent(MidiEvent.Type.NOTE_OFF, midiNote, 0, timestamp));
                }
            }
        }, 500);
    }

    private String midiNoteToName(int midiNote) {
        String[] names = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        return names[midiNote % 12] + (midiNote / 12 - 1);
    }

    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    public static void main(String[] args) {
        new VP_DAW();
    }
    
    // Helper class for MIDI events
    class MidiEvent {
        public enum Type { NOTE_ON, NOTE_OFF }
        
        private Type type;
        private int note;
        private int velocity;
        private long timestamp;
        private long duration;
        
        public MidiEvent(Type type, int note, int velocity, long timestamp) {
            this.type = type;
            this.note = note;
            this.velocity = velocity;
            this.timestamp = timestamp;
        }
        
        public Type getType() { return type; }
        public int getNote() { return note; }
        public int getVelocity() { return velocity; }
        public long getTimestamp() { return timestamp; }
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
    }
}