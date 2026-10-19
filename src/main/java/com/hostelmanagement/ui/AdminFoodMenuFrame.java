package com.hostelmanagement.ui;

import com.hostelmanagement.dao.FoodDAO.MealSchedule;
import com.hostelmanagement.dao.FoodDAO.MenuItem;
import com.hostelmanagement.model.User;
import com.hostelmanagement.service.FoodService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Admin editor for shared meal timings and the complete seven-day menu. */
public class AdminFoodMenuFrame extends JFrame {
    private static final String[] MEAL_TYPES = {"BREAKFAST", "LUNCH", "SNACK", "DINNER"};
    // The database stores MySQL DAYOFWEEK values: Sunday=1 ... Saturday=7.
    private static final String[] DAYS = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
    private static final DateTimeFormatter INPUT_TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    private final User admin;
    private final FoodService service = new FoodService();
    private final List<MealSchedule> schedules = new ArrayList<>();
    // Kept aligned with the 28 rows in weeklyMenuModel.
    private final List<MenuItem> rowMenus = new ArrayList<>();

    private final DefaultTableModel scheduleModel = new DefaultTableModel(
            new Object[]{"Meal", "Starts At", "Ends At"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable scheduleTable = new JTable(scheduleModel);
    private final JTextField scheduleStartField = new JTextField("07:00 AM", 12);
    private final JTextField scheduleEndField = new JTextField("09:00 AM", 12);

    private final DefaultTableModel weeklyMenuModel = new DefaultTableModel(
            new Object[]{"Day", "Meal", "Menu Description — edit this cell", "Serving Window", "Status"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return column == 2; }
    };
    private final JTable weeklyMenuTable = new JTable(weeklyMenuModel);

    public AdminFoodMenuFrame(User admin) {
        this.admin = admin;
        setTitle("Hostel Management - Weekly Food Menu");
        setSize(1180, 780);
        setMinimumSize(new Dimension(900, 620));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        buildUI();
        refreshSchedules();
        refreshWeeklyMenu();
    }

    private void buildUI() {
        JLabel title = new JLabel("Weekly Food Menu Management", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        JLabel subtitle = new JLabel(
                "Prepare Sunday–Saturday in one table. Edit any menu cell, then publish the week.",
                SwingConstants.CENTER);
        JPanel header = new JPanel(new GridLayout(2, 1, 5, 5));
        header.add(title);
        header.add(subtitle);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Meal Timings — All Days", buildSchedulePanel());
        tabs.addTab("Weekly Menu — All 7 Days", buildWeeklyMenuPanel());

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(new EmptyBorder(15, 15, 15, 15));
        content.add(header, BorderLayout.NORTH);
        content.add(tabs, BorderLayout.CENTER);
        setContentPane(content);
    }

    private JPanel buildSchedulePanel() {
        JLabel explanation = new JLabel(
                "Set each meal's timing once here. The same meal time applies to every day of the week.");
        scheduleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scheduleTable.setRowHeight(30);
        scheduleTable.setFillsViewportHeight(true);
        scheduleTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadSelectedSchedule();
        });
        JScrollPane tableScroll = new JScrollPane(scheduleTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Shared Meal Timings"));

        JPanel editor = new JPanel(new GridBagLayout());
        editor.setBorder(BorderFactory.createTitledBorder("Change Selected Timing"));
        GridBagConstraints gbc = constraints();
        addFormRow(editor, gbc, 0, "Start Time (e.g. 07:00 AM):", scheduleStartField);
        addFormRow(editor, gbc, 1, "End Time (e.g. 09:00 AM):", scheduleEndField);

        JButton save = new JButton("Save Shared Timing");
        JButton refresh = new JButton("Refresh Timings");
        save.addActionListener(e -> saveSchedule());
        refresh.addActionListener(e -> refreshSchedules());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(refresh);
        actions.add(save);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.add(editor, BorderLayout.CENTER);
        bottom.add(actions, BorderLayout.SOUTH);
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 5, 5, 5));
        panel.add(explanation, BorderLayout.NORTH);
        panel.add(tableScroll, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildWeeklyMenuPanel() {
        JLabel help = new JLabel(
                "Edit descriptions directly in the table. Blank new slots are ignored; do not clear an existing description.");
        help.setBorder(new EmptyBorder(4, 4, 8, 4));

        weeklyMenuTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        weeklyMenuTable.setRowHeight(30);
        weeklyMenuTable.setFillsViewportHeight(true);
        weeklyMenuTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        weeklyMenuTable.getColumnModel().getColumn(0).setPreferredWidth(90);
        weeklyMenuTable.getColumnModel().getColumn(1).setPreferredWidth(105);
        weeklyMenuTable.getColumnModel().getColumn(2).setPreferredWidth(475);
        weeklyMenuTable.getColumnModel().getColumn(3).setPreferredWidth(175);
        weeklyMenuTable.getColumnModel().getColumn(4).setPreferredWidth(105);
        JScrollPane tableScroll = new JScrollPane(weeklyMenuTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Weekly Menu — 7 Days × 4 Meals"));

        JButton refresh = new JButton("Refresh Week");
        JButton save = new JButton("Save Draft Changes");
        JButton publish = new JButton("Publish Entire Week");
        refresh.addActionListener(e -> refreshWeeklyMenu());
        save.addActionListener(e -> saveWeeklyDraftsWithMessage());
        publish.addActionListener(e -> publishEntireWeek());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        actions.add(refresh);
        actions.add(save);
        actions.add(publish);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 5, 5, 5));
        panel.add(help, BorderLayout.NORTH);
        panel.add(tableScroll, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private GridBagConstraints constraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(7, 8, 7, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        return gbc;
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent component) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.weighty = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(component, gbc);
    }

    private void refreshSchedules() {
        try {
            String selected = selectedMealType();
            schedules.clear();
            schedules.addAll(service.getMealSchedules(admin.getUserId()));
            scheduleModel.setRowCount(0);
            for (MealSchedule s : schedules) {
                scheduleModel.addRow(new Object[]{s.mealType, formatTime(s.startTime), formatTime(s.endTime)});
            }
            if (!schedules.isEmpty()) {
                int index = 0;
                if (selected != null) {
                    for (int i = 0; i < schedules.size(); i++) {
                        if (schedules.get(i).mealType.equals(selected)) { index = i; break; }
                    }
                }
                scheduleTable.setRowSelectionInterval(index, index);
                loadSelectedSchedule();
            }
        } catch (SQLException e) {
            showError("Could not load meal timings:\n" + e.getMessage());
        }
    }

    private String selectedMealType() {
        int row = scheduleTable.getSelectedRow();
        return row < 0 || row >= schedules.size() ? null : schedules.get(row).mealType;
    }

    private void loadSelectedSchedule() {
        int row = scheduleTable.getSelectedRow();
        if (row < 0 || row >= schedules.size()) return;
        MealSchedule s = schedules.get(row);
        scheduleStartField.setText(formatTime(s.startTime));
        scheduleEndField.setText(formatTime(s.endTime));
    }

    private void saveSchedule() {
        int row = scheduleTable.getSelectedRow();
        if (row < 0 || row >= schedules.size()) {
            showError("Select a meal timing first.");
            return;
        }
        try {
            LocalTime start = parseTime(scheduleStartField.getText());
            LocalTime end = parseTime(scheduleEndField.getText());
            if (!end.isAfter(start)) throw new IllegalArgumentException("End time must be later than start time.");
            String meal = schedules.get(row).mealType;
            service.updateMealSchedule(admin.getUserId(), meal, start, end);
            JOptionPane.showMessageDialog(this,
                    meal + " timing updated to " + formatTime(start) + " – " + formatTime(end)
                            + ".\nIt applies to all seven days.");
            refreshSchedules();
            refreshWeeklyMenu();
        } catch (DateTimeParseException e) {
            showError("Use a time such as 07:00 AM, 12:30 PM or 07:00 PM.");
        } catch (SQLException | IllegalArgumentException e) {
            showError("Could not update timing:\n" + e.getMessage());
        }
    }

    private String slotKey(int dayOfWeek, String mealType) {
        return dayOfWeek + "|" + mealType;
    }

    private void refreshWeeklyMenu() {
        try {
            List<MenuItem> allMenus = service.getAdminMenu(admin.getUserId());
            Map<String, MenuItem> bySlot = new HashMap<>();
            for (MenuItem item : allMenus) bySlot.put(slotKey(item.dayOfWeek, item.mealType), item);

            rowMenus.clear();
            weeklyMenuModel.setRowCount(0);
            Map<String, MealSchedule> scheduleByMeal = new HashMap<>();
            for (MealSchedule schedule : schedules) scheduleByMeal.put(schedule.mealType, schedule);

            for (int day = 1; day <= 7; day++) {
                for (String meal : MEAL_TYPES) {
                    MenuItem item = bySlot.get(slotKey(day, meal));
                    rowMenus.add(item);
                    MealSchedule schedule = scheduleByMeal.get(meal);
                    String serving = schedule == null ? "Timing not set"
                            : formatTime(schedule.startTime) + " – " + formatTime(schedule.endTime);
                    String status = item == null ? "NOT SET" : item.published ? "PUBLISHED" : "DRAFT";
                    weeklyMenuModel.addRow(new Object[]{
                            DAYS[day - 1], meal, item == null ? "" : item.description, serving, status
                    });
                }
            }
        } catch (SQLException | IllegalArgumentException e) {
            showError("Could not load weekly menu:\n" + e.getMessage());
        }
    }

    private int saveWeeklyDrafts() throws SQLException {
        if (weeklyMenuTable.isEditing() && !weeklyMenuTable.getCellEditor().stopCellEditing()) {
            throw new IllegalArgumentException("Finish editing the selected menu cell first.");
        }
        // Validate first so a cleared existing entry cannot cause a half-saved week.
        for (int row = 0; row < weeklyMenuModel.getRowCount(); row++) {
            String description = String.valueOf(weeklyMenuModel.getValueAt(row, 2)).trim();
            if (rowMenus.get(row) != null && description.isEmpty()) {
                throw new IllegalArgumentException(
                        "The " + DAYS[row / 4] + " " + MEAL_TYPES[row % 4]
                                + " entry already exists. Edit its text instead of leaving it blank.");
            }
        }

        int changed = 0;
        for (int row = 0; row < weeklyMenuModel.getRowCount(); row++) {
            int day = row / 4 + 1;
            String meal = MEAL_TYPES[row % 4];
            String description = String.valueOf(weeklyMenuModel.getValueAt(row, 2)).trim();
            MenuItem existing = rowMenus.get(row);
            if (description.isEmpty()) continue;
            if (existing == null || !description.equals(existing.description)) {
                service.saveMenu(admin.getUserId(), existing == null ? null : existing.id,
                        day, meal, description, false);
                changed++;
            }
        }
        refreshWeeklyMenu();
        return changed;
    }

    private void saveWeeklyDraftsWithMessage() {
        try {
            int changed = saveWeeklyDrafts();
            JOptionPane.showMessageDialog(this, changed == 0
                    ? "No menu changes detected."
                    : changed + " menu change(s) saved as draft.\nClick Publish Entire Week when ready.");
        } catch (SQLException | IllegalArgumentException e) {
            showError("Could not save weekly menu:\n" + e.getMessage());
        }
    }

    private void publishEntireWeek() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Save any edited cells and publish every saved menu to students?\n\nContinue?",
                "Publish Entire Week", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;
        try {
            int changed = saveWeeklyDrafts();
            int published = service.publishAllMenus(admin.getUserId());
            refreshWeeklyMenu();
            JOptionPane.showMessageDialog(this,
                    "Weekly menu published successfully.\n"
                            + "New/edited entries saved: " + changed
                            + "\nMenu records published now: " + published
                            + "\nStudents can view the next seven calendar days.");
        } catch (SQLException | IllegalArgumentException e) {
            showError("Could not publish weekly menu:\n" + e.getMessage());
        }
    }

    private LocalTime parseTime(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("Start and end times are required.");
        return LocalTime.parse(raw.trim().toUpperCase(Locale.ENGLISH), INPUT_TIME);
    }

    private String formatTime(LocalTime time) {
        return time == null ? "—" : DISPLAY_TIME.format(time);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Weekly Food Menu Management", JOptionPane.ERROR_MESSAGE);
    }
}
