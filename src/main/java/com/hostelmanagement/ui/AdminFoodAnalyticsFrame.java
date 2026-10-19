package com.hostelmanagement.ui;

import com.hostelmanagement.dao.FoodDAO.AttendanceSummary;
import com.hostelmanagement.dao.FoodDAO.FeedbackSummary;
import com.hostelmanagement.model.User;
import com.hostelmanagement.service.FoodService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public class AdminFoodAnalyticsFrame extends JFrame {
    private final User admin;
    private final FoodService service = new FoodService();
    private final JTextField fromField = new JTextField(LocalDate.now().minusDays(6).toString(), 11);
    private final JTextField toField = new JTextField(LocalDate.now().toString(), 11);

    private final DefaultTableModel feedbackModel = new DefaultTableModel(
            new Object[]{"Day", "Meal", "Menu", "Responses", "Good", "Average", "Suggestions"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final DefaultTableModel attendanceModel = new DefaultTableModel(
            new Object[]{"Meal Date", "Meal", "Responses", "Attending", "Not Attending"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    private final JTable feedbackTable = new JTable(feedbackModel);
    private final JTable attendanceTable = new JTable(attendanceModel);
    private final FeedbackBarChart barChart = new FeedbackBarChart();
    private final JLabel chartSummary = new JLabel("No feedback submitted for this period.");

    public AdminFoodAnalyticsFrame(User admin) {
        this.admin = admin;
        setTitle("Hostel Management - Food Feedback & Analytics");
        setSize(1150, 760);
        setMinimumSize(new Dimension(850, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        buildUI();
        loadAnalytics();
    }

    private void buildUI() {
        JLabel title = new JLabel("Food Feedback & Attendance Analytics", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 23));
        JPanel filter = new JPanel(new FlowLayout(FlowLayout.CENTER, 9, 8));
        filter.add(new JLabel("From (YYYY-MM-DD):"));
        filter.add(fromField);
        filter.add(new JLabel("To (YYYY-MM-DD):"));
        filter.add(toField);
        JButton load = new JButton("Load Analytics");
        load.addActionListener(e -> loadAnalytics());
        filter.add(load);
        JPanel header = new JPanel(new BorderLayout());
        header.add(title, BorderLayout.NORTH);
        header.add(filter, BorderLayout.SOUTH);

        feedbackTable.setAutoCreateRowSorter(true);
        feedbackTable.setRowHeight(25);
        feedbackTable.setFillsViewportHeight(true);
        attendanceTable.setAutoCreateRowSorter(true);
        attendanceTable.setRowHeight(25);
        attendanceTable.setFillsViewportHeight(true);

        JPanel feedbackTab = new JPanel(new BorderLayout(8, 8));
        JPanel chartContainer = new JPanel(new BorderLayout(4, 4));
        chartContainer.setBorder(BorderFactory.createTitledBorder("Feedback Rating Bar Chart — Selected Date Range"));
        chartContainer.add(barChart, BorderLayout.CENTER);
        chartSummary.setBorder(new EmptyBorder(2, 6, 4, 6));
        chartContainer.add(chartSummary, BorderLayout.SOUTH);
        JScrollPane feedbackScroll = new JScrollPane(feedbackTable);
        feedbackScroll.setBorder(BorderFactory.createTitledBorder("Feedback by Menu and Day"));
        JSplitPane feedbackSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chartContainer, feedbackScroll);
        feedbackSplit.setResizeWeight(0.42);
        feedbackSplit.setDividerLocation(245);
        feedbackTab.add(feedbackSplit, BorderLayout.CENTER);

        JScrollPane attendanceScroll = new JScrollPane(attendanceTable);
        attendanceScroll.setBorder(BorderFactory.createTitledBorder("Meal Attendance Responses"));
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Food Feedback & Bar Chart", feedbackTab);
        tabs.addTab("Meal Attendance", attendanceScroll);

        JLabel note = new JLabel("Counts represent recorded responses, not all hostel residents.");
        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(new EmptyBorder(15, 15, 15, 15));
        content.add(header, BorderLayout.NORTH);
        content.add(tabs, BorderLayout.CENTER);
        content.add(note, BorderLayout.SOUTH);
        setContentPane(content);
    }

    private void loadAnalytics() {
        try {
            LocalDate from = LocalDate.parse(fromField.getText().trim());
            LocalDate to = LocalDate.parse(toField.getText().trim());
            if (from.isAfter(to)) throw new IllegalArgumentException("From date must be on or before To date.");

            List<FeedbackSummary> feedback = service.getWeeklyFeedback(admin.getUserId(), from, to);
            List<AttendanceSummary> attendance = service.getMealAttendance(admin.getUserId(), from, to);

            feedbackModel.setRowCount(0);
            int good = 0, average = 0, suggestions = 0, total = 0;
            for (FeedbackSummary row : feedback) {
                feedbackModel.addRow(new Object[]{dayName(row.dayOfWeek), row.mealType, row.description,
                        row.total, row.good, row.average, row.suggestions});
                total += row.total;
                good += row.good;
                average += row.average;
                suggestions += row.suggestions;
            }
            barChart.setValues(good, average, suggestions);
            chartSummary.setText("Total feedback: " + total + "     Good: " + good
                    + "     Average: " + average + "     Suggestions: " + suggestions);

            attendanceModel.setRowCount(0);
            for (AttendanceSummary row : attendance) {
                attendanceModel.addRow(new Object[]{row.mealDate, row.mealType,
                        row.totalResponses, row.attending, row.notAttending});
            }
        } catch (DateTimeParseException e) {
            showError("Enter dates in YYYY-MM-DD format.");
        } catch (SQLException | IllegalArgumentException e) {
            showError("Could not load analytics:\n" + e.getMessage());
        }
    }

    private String dayName(int day) {
        return switch (day) {
            case 1 -> "Sunday"; case 2 -> "Monday"; case 3 -> "Tuesday";
            case 4 -> "Wednesday"; case 5 -> "Thursday"; case 6 -> "Friday";
            case 7 -> "Saturday"; default -> "Unknown";
        };
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Food Analytics", JOptionPane.ERROR_MESSAGE);
    }

    /** Lightweight Swing chart; no third-party chart library is required. */
    private static class FeedbackBarChart extends JPanel {
        private int good, average, suggestions;
        FeedbackBarChart() {
            setPreferredSize(new Dimension(600, 170));
            setMinimumSize(new Dimension(400, 150));
            setBackground(Color.WHITE);
        }
        void setValues(int good, int average, int suggestions) {
            this.good = good; this.average = average; this.suggestions = suggestions;
            repaint();
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int left = 105, top = 18, right = 28, barHeight = 24, rowGap = 19;
                int chartWidth = Math.max(1, getWidth() - left - right);
                int max = Math.max(1, Math.max(good, Math.max(average, suggestions)));
                String[] labels = {"Good", "Average", "Suggestions"};
                int[] values = {good, average, suggestions};
                Color[] colors = {new Color(54, 145, 89), new Color(224, 169, 52), new Color(77, 126, 190)};
                g.setColor(new Color(220, 220, 220));
                g.drawLine(left, top - 2, left, top + 3 * (barHeight + rowGap) - rowGap + 4);
                for (int i = 0; i < labels.length; i++) {
                    int y = top + i * (barHeight + rowGap);
                    g.setColor(Color.DARK_GRAY);
                    g.drawString(labels[i], 15, y + 17);
                    int w = (int) Math.round(chartWidth * (values[i] / (double) max));
                    g.setColor(new Color(238, 238, 238));
                    g.fillRoundRect(left, y, chartWidth, barHeight, 7, 7);
                    if (values[i] > 0) {
                        g.setColor(colors[i]);
                        g.fillRoundRect(left, y, Math.max(4, w), barHeight, 7, 7);
                    }
                    g.setColor(Color.DARK_GRAY);
                    int labelX = Math.min(left + chartWidth - 20, left + Math.max(5, w) + 8);
                    g.drawString(Integer.toString(values[i]), labelX, y + 17);
                }
                if (good + average + suggestions == 0) {
                    g.setColor(Color.GRAY);
                    g.drawString("No feedback data yet — the chart updates when students submit feedback.", left + 8, getHeight() - 8);
                }
            } finally {
                g.dispose();
            }
        }
    }
}
