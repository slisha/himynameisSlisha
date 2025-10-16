import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;


public class PreAttentiveExperiment {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ExperimentConfigGUI().setVisible(true);
        });
    }
}


class ExperimentConfigGUI extends JFrame {
    private JTextField subjectIdField;
    private JComboBox<String> trialTypeCombo;
    private JComboBox<Integer> distractorCountCombo;
    private JButton startButton;

    public ExperimentConfigGUI() {
        setupGUI();
    }

    private void setupGUI() {
        //Set window properties
        setTitle("Pre-Attentive Processing Experiment");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(4, 2, 10, 10));

        //Create form components - Updated with new trial types
        subjectIdField = new JTextField(20);
        trialTypeCombo = new JComboBox<>(new String[]{"Color", "Shape", "Combo", "Size", "Letter"});
        distractorCountCombo = new JComboBox<>(new Integer[]{10, 20, 30, 40, 50});
        startButton = new JButton("Start Trial");

        //Add components to form
        add(new JLabel("Subject ID:"));
        add(subjectIdField);
        add(new JLabel("Trial Type:"));
        add(trialTypeCombo);
        add(new JLabel("Number of Distractors:"));
        add(distractorCountCombo);
        add(new JLabel()); // Empty cell for spacing
        add(startButton);

        //Set up button action
        startButton.addActionListener(e -> startTrial());

        //Size and position window
        pack();
        setLocationRelativeTo(null); // Center on screen
    }

    private void startTrial() {
        //Get values from form
        String subjectId = subjectIdField.getText().trim();
        String trialType = (String) trialTypeCombo.getSelectedItem();
        int distractorCount = (Integer) distractorCountCombo.getSelectedItem();

        //Validate input
        if (subjectId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a Subject ID");
            return;
        }

        //Start the actual trial
        new TrialRunner(subjectId, trialType, distractorCount).runTrial();
    }
}


class TrialRunner {
    private String subjectId;
    private String trialType;
    private int distractorCount;
    private int displayInterval = 150; // Start with 150ms
    private int correctCount = 0;
    private int trialsCompleted = 0;
    private Random random = new Random();
    private TrialDisplay currentDisplay;

    public TrialRunner(String subjectId, String trialType, int distractorCount) {
        this.subjectId = subjectId;
        this.trialType = trialType;
        this.distractorCount = distractorCount;
    }

    public void runTrial() {
        //Create the trial display
        currentDisplay = new TrialDisplay(trialType, distractorCount, displayInterval, this);
        currentDisplay.setVisible(true);
    }

    //Callback method called by TrialDisplay when it closes
    public void trialCompleted(boolean wasTargetPresent) {
        //Ask user if target was present
        boolean userSaidPresent = askUserIfTargetPresent();

        //Check if user was correct
        if (userSaidPresent == wasTargetPresent) {
            correctCount++;
            trialsCompleted++;

            if (correctCount >= 10) {
                //Record data and reset
                recordSuccessData();
                JOptionPane.showMessageDialog(null,
                        "Trial completed successfully!\n" +
                                "Final interval: " + displayInterval + "ms\n" +
                                "Data recorded to file.");
                resetForNewTrial();
            } else {
                //Continue with same interval
                runNextTrial();
            }
        } else {
            //User was wrong, increase interval and reset correct count
            displayInterval += 25;
            correctCount = 0;
            trialsCompleted++;
            JOptionPane.showMessageDialog(null,
                    "Incorrect response. Target was " + (wasTargetPresent ? "PRESENT" : "ABSENT") + "\n" +
                            "Display interval increased to: " + displayInterval + "ms");
            runNextTrial();
        }
    }

    private boolean askUserIfTargetPresent() {
        int response = JOptionPane.showConfirmDialog(
                null,
                "Was the target shape present?",
                "Response Required",
                JOptionPane.YES_NO_OPTION
        );
        return response == JOptionPane.YES_OPTION;
    }

    private void runNextTrial() {
        if (trialsCompleted < 100) {
            javax.swing.Timer delayTimer = new javax.swing.Timer(1000, e -> runTrial());
            delayTimer.setRepeats(false);
            delayTimer.start();
        } else {
            JOptionPane.showMessageDialog(null, "Maximum trial count reached. Ending session.");
        }
    }

    private void recordSuccessData() {
        //Write data to file
        try (FileWriter fw = new FileWriter("experiment_data.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            String dataLine = String.format("%s, %s, %d, %d",
                    subjectId, trialType, distractorCount, displayInterval);
            out.println(dataLine);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error saving data: " + e.getMessage());
        }
    }

    private void resetForNewTrial() {
        //Reset for a new set of trials
        displayInterval = 150;
        correctCount = 0;
        trialsCompleted = 0;
    }
}


class TrialDisplay extends JFrame {
    private String trialType;
    private int distractorCount;
    private int displayTime;
    private boolean targetPresent;
    private Random random = new Random();
    private TrialRunner trialRunner;

    public TrialDisplay(String trialType, int distractorCount, int displayTime, TrialRunner runner) {
        this.trialType = trialType;
        this.distractorCount = distractorCount;
        this.displayTime = displayTime;
        this.trialRunner = runner;


        targetPresent = random.nextBoolean();

        setupDisplay();
    }

    private void setupDisplay() {
        setTitle("Trial - Watch Carefully!");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        //Create drawing panel
        StimuliPanel panel = new StimuliPanel(trialType, distractorCount, targetPresent);
        add(panel);
    }

    public boolean wasTargetPresent() {
        return targetPresent;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (visible) {
            javax.swing.Timer closeTimer = new javax.swing.Timer(displayTime, e -> {
                setVisible(false);
                dispose();
                trialRunner.trialCompleted(targetPresent);
            });
            closeTimer.setRepeats(false);
            closeTimer.start();
        }
    }
}


class StimuliPanel extends JPanel {
    private String trialType;
    private int distractorCount;
    private boolean targetPresent;
    private Random random = new Random();

    //Define colors
    private final Color RED = new Color(255, 0, 0);
    private final Color BLUE = new Color(0, 0, 255);
    private final Color BLACK = Color.BLACK;

    public StimuliPanel(String trialType, int distractorCount, boolean targetPresent) {
        this.trialType = trialType;
        this.distractorCount = distractorCount;
        this.targetPresent = targetPresent;
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        //Get panel dimensions
        int width = getWidth();
        int height = getHeight();

        //Calculate stimulus size based on screen size
        int stimulusSize = Math.min(width, height) / 15;
        if (stimulusSize < 20) stimulusSize = 20;
        if (stimulusSize > 100) stimulusSize = 100;

        //Create lists to store positions to avoid overlap
        ArrayList<Point> usedPositions = new ArrayList<>();

        //Draw stimuli based on trial type
        switch (trialType) {
            case "Color":
                drawColorTrial(g2d, width, height, stimulusSize, usedPositions);
                break;
            case "Shape":
                drawShapeTrial(g2d, width, height, stimulusSize, usedPositions);
                break;
            case "Combo":
                drawComboTrial(g2d, width, height, stimulusSize, usedPositions);
                break;
            case "Size":
                drawSizeTrial(g2d, width, height, stimulusSize, usedPositions);
                break;
            case "Letter":
                drawLetterTrial(g2d, width, height, stimulusSize, usedPositions);
                break;
        }
    }

    private void drawColorTrial(Graphics2D g2d, int width, int height, int size, ArrayList<Point> usedPositions) {
        //Color trial: Find red circle among blue circles

        int actualDistractors = targetPresent ? distractorCount - 1 : distractorCount;

        //Draw blue circle distractors
        for (int i = 0; i < actualDistractors; i++) {
            Point position = getRandomPosition(width, height, size, usedPositions);
            drawCircle(g2d, position.x, position.y, size, BLUE);
            usedPositions.add(position);
        }

        //Draw target last
        if (targetPresent) {
            Point targetPos = getRandomPosition(width, height, size, usedPositions);
            drawCircle(g2d, targetPos.x, targetPos.y, size, RED);
            usedPositions.add(targetPos);
        }

        g2d.setColor(Color.BLACK);
        g2d.drawString("Color Trial - Target: " + (targetPresent ? "RED CIRCLE" : "NONE"), 20, 20);
        g2d.drawString("Distractors: " + actualDistractors + " BLUE CIRCLES", 20, 40);
    }

    private void drawShapeTrial(Graphics2D g2d, int width, int height, int size, ArrayList<Point> usedPositions) {
        //Shape trial: Find red square among red circles

        //Calculate actual number of distractors
        int actualDistractors = targetPresent ? distractorCount - 1 : distractorCount;

        //Draw red circle distractors
        for (int i = 0; i < actualDistractors; i++) {
            Point position = getRandomPosition(width, height, size, usedPositions);
            drawCircle(g2d, position.x, position.y, size, RED);
            usedPositions.add(position);
        }

        //Draw target last
        if (targetPresent) {
            Point targetPos = getRandomPosition(width, height, size, usedPositions);
            drawSquare(g2d, targetPos.x, targetPos.y, size, RED);
            usedPositions.add(targetPos);
        }


        g2d.setColor(Color.BLACK);
        g2d.drawString("Shape Trial - Target: " + (targetPresent ? "RED SQUARE" : "NONE"), 20, 20);
        g2d.drawString("Distractors: " + actualDistractors + " RED CIRCLES", 20, 40);
    }

    private void drawComboTrial(Graphics2D g2d, int width, int height, int size, ArrayList<Point> usedPositions) {
        //Combo trial: Find red circle among red squares and blue circles

        //Calculate actual number of distractors
        int actualDistractors = targetPresent ? distractorCount - 1 : distractorCount;
        int halfDistractors = actualDistractors / 2;

        //Draw red square distractors
        for (int i = 0; i < halfDistractors; i++) {
            Point position = getRandomPosition(width, height, size, usedPositions);
            drawSquare(g2d, position.x, position.y, size, RED);
            usedPositions.add(position);
        }

        //Draw blue circle distractors
        for (int i = 0; i < (actualDistractors - halfDistractors); i++) {
            Point position = getRandomPosition(width, height, size, usedPositions);
            drawCircle(g2d, position.x, position.y, size, BLUE);
            usedPositions.add(position);
        }

        //Draw target last
        if (targetPresent) {
            Point targetPos = getRandomPosition(width, height, size, usedPositions);
            drawCircle(g2d, targetPos.x, targetPos.y, size, RED);
            usedPositions.add(targetPos);
        }

        g2d.setColor(Color.BLACK);
        g2d.drawString("Combo Trial - Target: " + (targetPresent ? "RED CIRCLE" : "NONE"), 20, 20);
        g2d.drawString("Distractors: " + halfDistractors + " RED SQUARES + " +
                (actualDistractors - halfDistractors) + " BLUE CIRCLES", 20, 40);
    }

    private void drawSizeTrial(Graphics2D g2d, int width, int height, int size, ArrayList<Point> usedPositions) {
        //Size trial: Find larger circle among smaller circles (Pre-Attentive)

        int actualDistractors = targetPresent ? distractorCount - 1 : distractorCount;
        int smallSize = size;
        int largeSize = (int)(size * 1.8); // 80% larger

        //Draw small circle distractors
        for (int i = 0; i < actualDistractors; i++) {
            Point position = getRandomPosition(width, height, smallSize, usedPositions);
            drawCircle(g2d, position.x, position.y, smallSize, BLUE);
            usedPositions.add(position);
        }

        //Draw target last (larger circle)
        if (targetPresent) {
            Point targetPos = getRandomPosition(width, height, largeSize, usedPositions);
            drawCircle(g2d, targetPos.x, targetPos.y, largeSize, BLUE);
            usedPositions.add(targetPos);
        }

        g2d.setColor(Color.BLACK);
        g2d.drawString("Size Trial - Target: " + (targetPresent ? "LARGER CIRCLE" : "NONE"), 20, 20);
        g2d.drawString("Distractors: " + actualDistractors + " SMALLER CIRCLES", 20, 40);
    }

    private void drawLetterTrial(Graphics2D g2d, int width, int height, int size, ArrayList<Point> usedPositions) {
        //Letter trial: Find R among Ps (Non Pre-Attentive)

        int actualDistractors = targetPresent ? distractorCount - 1 : distractorCount;
        int fontSize = size;

        //Draw P distractors
        for (int i = 0; i < actualDistractors; i++) {
            Point position = getRandomPosition(width, height, fontSize, usedPositions);
            drawLetter(g2d, position.x, position.y, fontSize, "P");
            usedPositions.add(position);
        }

        //Draw target last (R)
        if (targetPresent) {
            Point targetPos = getRandomPosition(width, height, fontSize, usedPositions);
            drawLetter(g2d, targetPos.x, targetPos.y, fontSize, "R");
            usedPositions.add(targetPos);
        }

        g2d.setColor(Color.BLACK);
        g2d.drawString("Letter Trial - Target: " + (targetPresent ? "LETTER R" : "NONE"), 20, 20);
        g2d.drawString("Distractors: " + actualDistractors + " LETTER Ps", 20, 40);
    }

    private void drawLetter(Graphics2D g2d, int x, int y, int size, String letter) {
        //Draw letter with black outline
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("SansSerif", Font.BOLD, size));

        //Center the letter in the position
        FontMetrics fm = g2d.getFontMetrics();
        int letterWidth = fm.stringWidth(letter);
        int letterHeight = fm.getAscent();

        int centeredX = x + (size - letterWidth) / 2;
        int centeredY = y + (size + letterHeight) / 2;

        g2d.drawString(letter, centeredX, centeredY);
    }

    private void drawCircle(Graphics2D g2d, int x, int y, int size, Color fillColor) {
        //Draw filled circle with black outline
        g2d.setColor(fillColor);
        g2d.fillOval(x, y, size, size);
        g2d.setColor(BLACK);
        g2d.drawOval(x, y, size, size);
    }

    private void drawSquare(Graphics2D g2d, int x, int y, int size, Color fillColor) {
        //Draw filled square with black outline
        g2d.setColor(fillColor);
        g2d.fillRect(x, y, size, size);
        g2d.setColor(BLACK);
        g2d.drawRect(x, y, size, size);
    }

    private Point getRandomPosition(int width, int height, int size, ArrayList<Point> usedPositions) {
        int maxAttempts = 100;
        int attempt = 0;

        while (attempt < maxAttempts) {
            int margin = size + 20;
            int x = random.nextInt(width - margin * 2) + margin;
            int y = random.nextInt(height - margin * 2) + margin;

            Point newPoint = new Point(x, y);

            boolean overlaps = false;
            for (Point existing : usedPositions) {
                if (newPoint.distance(existing) < size * 1.5) {
                    overlaps = true;
                    break;
                }
            }

            if (!overlaps) {
                return newPoint;
            }

            attempt++;
        }

        int margin = size + 20;
        int x = random.nextInt(width - margin * 2) + margin;
        int y = random.nextInt(height - margin * 2) + margin;
        return new Point(x, y);
    }

    @Override
    public Dimension getPreferredSize() {
        return Toolkit.getDefaultToolkit().getScreenSize();
    }
}