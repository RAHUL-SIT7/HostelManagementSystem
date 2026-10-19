package com.hostelmanagement.ui;

import com.hostelmanagement.dao.FoodDAO.DatedMenuItem;
import com.hostelmanagement.model.User;
import com.hostelmanagement.service.FoodService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Student screen: one table for a selected date and the following six days. */
public class StudentFoodFeedbackFrame extends JFrame {
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private final User student;
    private final FoodService service = new FoodService();
    private final JTextField dateField = new JTextField(LocalDate.now().toString(), 12);
    private final List<DatedMenuItem> menus = new ArrayList<>();

    private final DefaultTableModel menuModel = new DefaultTableModel(
            new Object[]{"Date", "Day", "Meal", "Serving Window", "Menu"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable menuTable = new JTable(menuModel);
    private final JComboBox<String> ratingCombo = new JComboBox<>(new String[]{"Good", "Average", "Suggestions"});
    private final JTextArea commentsArea = new JTextArea(3, 35);
    private final JRadioButton yesDinner = new JRadioButton("Yes, I will attend");
    private final JRadioButton noDinner = new JRadioButton("No, I will not attend");

    public StudentFoodFeedbackFrame(User student) {
        this.student = student;
        setTitle("Hostel Management - Weekly Food Menu & Feedback");
        setSize(1180, 790);
        setMinimumSize(new Dimension(900, 650));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        buildUI();
        refreshMenu();
    }

    private void buildUI() {
        JLabel title = new JLabel("Weekly Food Menu & Feedback", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        JLabel hint = new JLabel(
                "Choose a start date to see that day and the following six days together.",
                SwingConstants.CENTER);
        JPanel header = new JPanel(new BorderLayout(8, 8));
        header.add(title, BorderLayout.NORTH);

        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        datePanel.add(new JLabel("Start Date (YYYY-MM-DD):"));
        datePanel.add(dateField);
        JButton refresh = new JButton("Show Next 7 Days");
        refresh.addActionListener(e -> refreshMenu());
        datePanel.add(refresh);
        JPanel dateContainer = new JPanel(new BorderLayout(5, 5));
        dateContainer.add(hint, BorderLayout.NORTH);
        dateContainer.add(datePanel, BorderLayout.SOUTH);
        header.add(dateContainer, BorderLayout.SOUTH);

        menuTable.setRowHeight(28);
        menuTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        menuTable.setFillsViewportHeight(true);
        menuTable.getColumnModel().getColumn(0).setPreferredWidth(105);
        menuTable.getColumnModel().getColumn(1).setPreferredWidth(110);
        menuTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        menuTable.getColumnModel().getColumn(3).setPreferredWidth(180);
        menuTable.getColumnModel().getColumn(4).setPreferredWidth(460);
        JScrollPane menuScroll = new JScrollPane(menuTable);
        menuScroll.setBorder(BorderFactory.createTitledBorder(
                "Published Menu — Next 7 Days (up to 28 meal entries)"));

        JPanel feedbackPanel = new JPanel(new GridBagLayout());
        feedbackPanel.setBorder(BorderFactory.createTitledBorder("Feedback for Selected Date & Meal"));
        commentsArea.setLineWrap(true);
        commentsArea.setWrapStyleWord(true);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        row(feedbackPanel, gbc, 0, "Food Rating:", ratingCombo);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; gbc.fill = GridBagConstraints.BOTH;
        feedbackPanel.add(new JLabel("Suggestions / Comments:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        feedbackPanel.add(new JScrollPane(commentsArea), gbc);
        JButton submitFeedback = new JButton("Submit Feedback for Selected Meal");
        submitFeedback.addActionListener(e -> submitFeedback());
        gbc.gridx = 1; gbc.gridy = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        feedbackPanel.add(submitFeedback, gbc);

        JPanel dinnerPanel = new JPanel(new GridLayout(0, 1, 6, 6));
        dinnerPanel.setBorder(BorderFactory.createTitledBorder("Tomorrow's Dinner Attendance"));
        dinnerPanel.add(new JLabel("Dinner date: " + LocalDate.now().plusDays(1)));
        ButtonGroup group = new ButtonGroup();
        group.add(yesDinner);
        group.add(noDinner);
        dinnerPanel.add(yesDinner);
        dinnerPanel.add(noDinner);
        JButton saveAttendance = new JButton("Save Dinner Response");
        saveAttendance.addActionListener(e -> saveDinnerAttendance());
        dinnerPanel.add(saveAttendance);

        JPanel menuTab = new JPanel(new BorderLayout(10, 10));
        menuTab.add(menuScroll, BorderLayout.CENTER);
        menuTab.add(feedbackPanel, BorderLayout.SOUTH);
        JPanel attendanceTab = new JPanel(new BorderLayout());
        attendanceTab.add(dinnerPanel, BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Weekly Menu & Feedback", menuTab);
        tabs.addTab("Dinner Attendance", attendanceTab);

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(new EmptyBorder(15, 15, 15, 15));
        content.add(header, BorderLayout.NORTH);
        content.add(tabs, BorderLayout.CENTER);
        setContentPane(content);
    }

    private void row(JPanel panel, GridBagConstraints gbc, int index, String label, JComponent component) {
        gbc.gridx = 0; gbc.gridy = index; gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(component, gbc);
    }

    private LocalDate selectedStartDate() {
        return LocalDate.parse(dateField.getText().trim());
    }

    private void refreshMenu() {
        try {
            LocalDate start = selectedStartDate();
            LocalDate end = start.plusDays(6);
            menus.clear();
            menus.addAll(service.getPublishedMenuForNextSevenDays(start));
            menuModel.setRowCount(0);

            for (DatedMenuItem dated : menus) {
                DayOfWeek day = dated.menuDate.getDayOfWeek();
                menuModel.addRow(new Object[]{
                        dated.menuDate.toString(),
                        day.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                        dated.menu.mealType,
                        CLOCK.format(dated.menu.mealStartTime) + " – " + CLOCK.format(dated.menu.mealEndTime),
                        dated.menu.description
                });
            }
            if (menus.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "No published menus were found between " + start + " and " + end + ".");
            }
        } catch (DateTimeParseException e) {
            showError("Enter the start date in YYYY-MM-DD format.");
        } catch (SQLException | IllegalArgumentException e) {
            showError("Could not load weekly menu:\n" + e.getMessage());
        }
    }

    private void submitFeedback() {
        int viewRow = menuTable.getSelectedRow();
        if (viewRow < 0) {
            showError("Select a date and meal from the weekly menu table first.");
            return;
        }
        int row = menuTable.convertRowIndexToModel(viewRow);
        if (row < 0 || row >= menus.size()) {
            showError("Refresh the weekly menu and try again.");
            return;
        }

        try {
            DatedMenuItem selected = menus.get(row);
            String rating = ((String) ratingCombo.getSelectedItem()).toUpperCase(Locale.ROOT);
            service.submitFeedback(student.getUserId(), selected.menu.id, selected.menuDate,
                    rating, commentsArea.getText().trim());
            JOptionPane.showMessageDialog(this,
                    "Your feedback for " + selected.menuDate + " — " + selected.menu.mealType + " has been saved.");
            commentsArea.setText("");
        } catch (SQLException | IllegalArgumentException e) {
            showError("Could not submit feedback:\n" + e.getMessage());
        }
    }

    private void saveDinnerAttendance() {
        if (!yesDinner.isSelected() && !noDinner.isSelected()) {
            showError("Please choose Yes or No.");
            return;
        }
        try {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            service.saveAttendance(student.getUserId(), tomorrow, "DINNER", yesDinner.isSelected());
            JOptionPane.showMessageDialog(this, "Dinner response for " + tomorrow + " saved.");
        } catch (SQLException | IllegalArgumentException e) {
            showError("Could not save dinner response:\n" + e.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message,
                "Weekly Food Menu & Feedback", JOptionPane.ERROR_MESSAGE);
    }
}
