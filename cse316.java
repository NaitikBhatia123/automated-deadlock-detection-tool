import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class DeadlockDetection {
    private final int[][] allocation;
    private final int[][] request;
    private final int[] available;
    private final int processes;
    private final int resources;

    public DeadlockDetection(int[][] allocation, int[][] request, int[] available, int processes, int resources) {
        this.processes = processes;
        this.resources = resources;
        this.allocation = allocation;
        this.request = request;
        this.available = available;
    }

    public List<Integer> detectDeadlock() {
        boolean[] finish = new boolean[processes];
        int[] work = Arrays.copyOf(available, available.length);
        List<Integer> deadlockedProcesses = new ArrayList<>();
        int count = 0;  // Count processes that have finished

        while (count < processes) {
            boolean found = false;
            for (int i = 0; i < processes; i++) {
                if (!finish[i] && canExecute(i, work)) {
                    for (int j = 0; j < resources; j++) {
                        work[j] += allocation[i][j];  // Release allocated resources
                    }
                    finish[i] = true;
                    found = true;
                    count++;
                }
            }
            if (!found) break;  // No progress means potential deadlock
        }

        for (int i = 0; i < processes; i++) {
            if (!finish[i]) {
                deadlockedProcesses.add(i);
            }
        }
        return deadlockedProcesses;
    }


    private boolean canExecute(int process, int[] work) {
        for (int j = 0; j < resources; j++) {
            if (request[process][j] > work[j]) return false;
        }
        return true;
    }
}

class DeadlockGUI extends JFrame {
    private JTable allocationTable, requestTable;
    private DefaultTableModel allocationModel, requestModel;
    private JTextField[] availableFields;
    private JTextArea outputArea;
    private int processes, resources;
    private JLabel resultLabel;

    public DeadlockGUI() {
        setTitle("Deadlock Detection System");
        setSize(800, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        showProcessResourceDialog();
    }

    private void showProcessResourceDialog() {
        JPanel panel = new JPanel(new GridLayout(2, 2));
        JTextField processField = new JTextField();
        JTextField resourceField = new JTextField();

        panel.add(new JLabel("Enter Number of Processes:"));
        panel.add(processField);
        panel.add(new JLabel("Enter Number of Resources:"));
        panel.add(resourceField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Enter Process & Resource Count",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                processes = Integer.parseInt(processField.getText().trim());
                resources = Integer.parseInt(resourceField.getText().trim());

                if (processes <= 0 || resources <= 0) {
                    throw new NumberFormatException("Values must be greater than zero.");
                }
                initializeGUI();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid input! Please enter valid numbers.", "Error", JOptionPane.ERROR_MESSAGE);
                showProcessResourceDialog();
            }
        } else {
            dispose();
        }
    }

    private void initializeGUI() {
        getContentPane().removeAll();
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // *Allocation Table Panel*
        allocationModel = new DefaultTableModel(processes, resources);
        allocationTable = new JTable(allocationModel);
        JScrollPane allocationScrollPane = new JScrollPane(allocationTable);
        allocationScrollPane.setBorder(BorderFactory.createTitledBorder("Allocation Matrix"));
        allocationScrollPane.setPreferredSize(new Dimension(300, 120));

        // *Request Table Panel*
        requestModel = new DefaultTableModel(processes, resources);
        requestTable = new JTable(requestModel);
        JScrollPane requestScrollPane = new JScrollPane(requestTable);
        requestScrollPane.setBorder(BorderFactory.createTitledBorder("Request Matrix"));
        requestScrollPane.setPreferredSize(new Dimension(300, 120));

        // *Available Resources Panel*
        JPanel availablePanel = new JPanel(new FlowLayout());
        availablePanel.setBorder(BorderFactory.createTitledBorder("Available Resources"));
        availableFields = new JTextField[resources];
        for (int j = 0; j < resources; j++) {
            availableFields[j] = new JTextField(3);
            availablePanel.add(availableFields[j]);
        }

        // *Button Panel*
        JPanel buttonPanel = new JPanel();
        JButton detectButton = new JButton("Detect Deadlock");
        detectButton.addActionListener(this::detectDeadlock);
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> clearFields());
        buttonPanel.add(detectButton);
        buttonPanel.add(clearButton);

        // *Results Panel*
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(BorderFactory.createTitledBorder("Results"));

        resultLabel = new JLabel("Result will be displayed here", SwingConstants.CENTER);
        resultLabel.setFont(new Font("Arial", Font.BOLD, 16));

        outputArea = new JTextArea(10, 50);
        outputArea.setEditable(false);
        JScrollPane outputScrollPane = new JScrollPane(outputArea);

        resultPanel.add(resultLabel, BorderLayout.NORTH);
        resultPanel.add(outputScrollPane, BorderLayout.CENTER);

        // *Adding Components to Layout*
        gbc.gridy = 0;
        inputPanel.add(allocationScrollPane, gbc);
        gbc.gridy = 1;
        inputPanel.add(requestScrollPane, gbc);
        gbc.gridy = 2;
        inputPanel.add(availablePanel, gbc);
        gbc.gridy = 3;
        inputPanel.add(buttonPanel, gbc);

        add(inputPanel, BorderLayout.NORTH);
        add(resultPanel, BorderLayout.CENTER);
        setVisible(true);
    }

    private void detectDeadlock(ActionEvent e) {
        try {
            int[][] allocation = getMatrixFromTable(allocationTable,processes,resources);
            int[][] request = getMatrixFromTable(requestTable,processes,resources);
            int[] available = new int[resources];

            for (int j = 0; j < resources; j++) {
                available[j] = Integer.parseInt(availableFields[j].getText().trim());
            }

            DeadlockDetection detector = new DeadlockDetection(allocation, request, available, processes, resources);
            List<Integer> deadlockedProcesses = detector.detectDeadlock();

            if (deadlockedProcesses.isEmpty()) {
                resultLabel.setText("No Deadlock Detected!");
                resultLabel.setForeground(Color.GREEN);
                outputArea.setText("All processes are executing safely.");
            } else {
                resultLabel.setText("Deadlock Detected!");
                resultLabel.setForeground(Color.RED);
                outputArea.setText("Deadlocked Processes: " + deadlockedProcesses);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid Input! Enter numbers only.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFields() {
        allocationModel.setRowCount(0);
        requestModel.setRowCount(0);
        allocationModel.setRowCount(processes);
        requestModel.setRowCount(processes);
        for (JTextField field : availableFields) {
            field.setText("");
        }
        outputArea.setText("");
        resultLabel.setText("Result will be displayed here");
        resultLabel.setForeground(Color.BLACK);
    }
    private int[][] getMatrixFromTable(JTable table, int rows, int cols) {
        int[][] matrix = new int[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Object cellValue = table.getModel().getValueAt(i, j);

                // ✅ Fix: Handle null or empty cells
                if (cellValue == null || cellValue.toString().trim().isEmpty()) {
                    matrix[i][j] = 0;  // Set default value (0) if the cell is empty
                } else {
                    matrix[i][j] = Integer.parseInt(cellValue.toString().trim());
                }
            }
        }
        return matrix;
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(DeadlockGUI::new);
    }
}
// done with the code
