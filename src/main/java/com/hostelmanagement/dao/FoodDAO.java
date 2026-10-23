package com.hostelmanagement.dao;

import com.hostelmanagement.common.database.MySQLConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Database access for shared meal times, menus, feedback and attendance. */
public class FoodDAO {

    private static final Set<String> MEAL_TYPES = Set.of(
            "BREAKFAST", "LUNCH", "SNACK", "DINNER"
    );
    private static final Set<String> RATINGS = Set.of(
            "GOOD", "AVERAGE", "SUGGESTIONS"
    );

    public static class MealSchedule {
        public final String mealType;
        public final LocalTime startTime;
        public final LocalTime endTime;

        public MealSchedule(String mealType, LocalTime startTime, LocalTime endTime) {
            this.mealType = mealType;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    public static class MenuItem {
        public final long id;
        public final int dayOfWeek; // MySQL DAYOFWEEK: Sunday=1 ... Saturday=7
        public final String mealType;
        public final String description;
        public final LocalTime mealStartTime;
        public final LocalTime mealEndTime;
        public final boolean published;

        public MenuItem(long id, int dayOfWeek, String mealType, String description,
                        LocalTime mealStartTime, LocalTime mealEndTime, boolean published) {
            this.id = id;
            this.dayOfWeek = dayOfWeek;
            this.mealType = mealType;
            this.description = description;
            this.mealStartTime = mealStartTime;
            this.mealEndTime = mealEndTime;
            this.published = published;
        }

        public String getDayName() {
            return switch (dayOfWeek) {
                case 1 -> "Sunday";
                case 2 -> "Monday";
                case 3 -> "Tuesday";
                case 4 -> "Wednesday";
                case 5 -> "Thursday";
                case 6 -> "Friday";
                case 7 -> "Saturday";
                default -> "Unknown";
            };
        }
    }

    /** Weekly recurring menu entry attached to a concrete date for student display. */
    public static class DatedMenuItem {
        public final LocalDate menuDate;
        public final MenuItem menu;

        public DatedMenuItem(LocalDate menuDate, MenuItem menu) {
            this.menuDate = menuDate;
            this.menu = menu;
        }
    }

    public static class FeedbackSummary {
        public final int dayOfWeek;
        public final String mealType;
        public final String description;
        public final int total;
        public final int good;
        public final int average;
        public final int suggestions;

        public FeedbackSummary(int dayOfWeek, String mealType, String description,
                               int total, int good, int average, int suggestions) {
            this.dayOfWeek = dayOfWeek;
            this.mealType = mealType;
            this.description = description;
            this.total = total;
            this.good = good;
            this.average = average;
            this.suggestions = suggestions;
        }
    }

    public static class AttendanceSummary {
        public final LocalDate mealDate;
        public final String mealType;
        public final int totalResponses;
        public final int attending;
        public final int notAttending;

        public AttendanceSummary(LocalDate mealDate, String mealType, int totalResponses,
                                 int attending, int notAttending) {
            this.mealDate = mealDate;
            this.mealType = mealType;
            this.totalResponses = totalResponses;
            this.attending = attending;
            this.notAttending = notAttending;
        }
    }

    private void requireActiveRole(Connection con, long userId, String role) throws SQLException {
        String sql = "SELECT user_id FROM users WHERE user_id = ? AND role = ? AND account_status = 'ACTIVE'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, role);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("An active " + role + " account is required.");
                }
            }
        }
    }

    private long getActiveStudentId(Connection con, long userId) throws SQLException {
        String sql = """
                SELECT s.student_id
                FROM users u
                JOIN students s ON s.user_id = u.user_id
                WHERE u.user_id = ? AND u.role = 'STUDENT' AND u.account_status = 'ACTIVE'
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("An active student profile is required.");
                return rs.getLong("student_id");
            }
        }
    }

    // ---------------- Shared schedule ----------------

    public List<MealSchedule> getMealSchedules(long adminUserId) throws SQLException {
        String sql = """
                SELECT meal_type, start_time, end_time
                FROM meal_time_slots
                ORDER BY FIELD(meal_type, 'BREAKFAST', 'LUNCH', 'SNACK', 'DINNER')
                """;
        List<MealSchedule> result = new ArrayList<>();
        try (Connection con = MySQLConnection.getConnection()) {
            requireActiveRole(con, adminUserId, "ADMIN");
            try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new MealSchedule(rs.getString("meal_type"),
                            rs.getTime("start_time").toLocalTime(),
                            rs.getTime("end_time").toLocalTime()));
                }
            }
        }
        return result;
    }

    public void updateMealSchedule(long adminUserId, String mealType,
                                   LocalTime startTime, LocalTime endTime) throws SQLException {
        requireMealType(mealType);
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be later than start time.");
        }
        String scheduleSql = "UPDATE meal_time_slots SET start_time = ?, end_time = ?, updated_by = ? WHERE meal_type = ?";
        String legacySyncSql = """
                UPDATE food_menu
                SET meal_time = ?, meal_start_time = ?, meal_end_time = ?,
                    updated_by = ?, updated_at = CURRENT_TIMESTAMP
                WHERE meal_type = ?
                """;
        try (Connection con = MySQLConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                requireActiveRole(con, adminUserId, "ADMIN");
                try (PreparedStatement ps = con.prepareStatement(scheduleSql)) {
                    ps.setTime(1, Time.valueOf(startTime));
                    ps.setTime(2, Time.valueOf(endTime));
                    ps.setLong(3, adminUserId);
                    ps.setString(4, mealType);
                    int changed = ps.executeUpdate();
                    if (changed == 0 && !scheduleExists(con, mealType)) {
                        throw new SQLException("No shared timing exists for " + mealType + ". Run the meal-time setup SQL.");
                    }
                }
                // Keep legacy per-menu timing columns in sync for any older code that reads them.
                try (PreparedStatement ps = con.prepareStatement(legacySyncSql)) {
                    ps.setTime(1, Time.valueOf(startTime));
                    ps.setTime(2, Time.valueOf(startTime));
                    ps.setTime(3, Time.valueOf(endTime));
                    ps.setLong(4, adminUserId);
                    ps.setString(5, mealType);
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException | RuntimeException ex) {
                try { con.rollback(); } catch (SQLException rollback) { ex.addSuppressed(rollback); }
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    private boolean scheduleExists(Connection con, String mealType) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM meal_time_slots WHERE meal_type = ?")) {
            ps.setString(1, mealType);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private LocalTime[] getScheduleTimes(Connection con, String mealType) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT start_time, end_time FROM meal_time_slots WHERE meal_type = ?")) {
            ps.setString(1, mealType);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Configure the shared timing for " + mealType + " first.");
                return new LocalTime[]{rs.getTime("start_time").toLocalTime(), rs.getTime("end_time").toLocalTime()};
            }
        }
    }

    // ---------------- Menu management ----------------

    public List<MenuItem> getMenusForAdmin(long adminUserId) throws SQLException {
        return loadAdminMenus(adminUserId, 0);
    }

    public List<MenuItem> getMenusForAdmin(long adminUserId, int dayOfWeek) throws SQLException {
        if (dayOfWeek < 1 || dayOfWeek > 7) throw new IllegalArgumentException("Choose a valid day.");
        return loadAdminMenus(adminUserId, dayOfWeek);
    }

    private List<MenuItem> loadAdminMenus(long adminUserId, int dayOfWeek) throws SQLException {
        String sql = """
                SELECT fm.menu_id, fm.day_of_week, fm.meal_type, fm.menu_description,
                       fm.is_published, mts.start_time AS slot_start_time, mts.end_time AS slot_end_time
                FROM food_menu fm
                JOIN meal_time_slots mts ON mts.meal_type = fm.meal_type
                WHERE (? = 0 OR fm.day_of_week = ?)
                ORDER BY fm.day_of_week,
                         FIELD(fm.meal_type, 'BREAKFAST', 'LUNCH', 'SNACK', 'DINNER')
                """;
        List<MenuItem> rows = new ArrayList<>();
        try (Connection con = MySQLConnection.getConnection()) {
            requireActiveRole(con, adminUserId, "ADMIN");
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, dayOfWeek);
                ps.setInt(2, dayOfWeek);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) rows.add(readMenu(rs));
                }
            }
        }
        return rows;
    }

    public List<MenuItem> getPublishedMenu(LocalDate date) throws SQLException {
        if (date == null) throw new IllegalArgumentException("Choose a menu date.");
        String sql = """
                SELECT fm.menu_id, fm.day_of_week, fm.meal_type, fm.menu_description,
                       fm.is_published, mts.start_time AS slot_start_time, mts.end_time AS slot_end_time
                FROM food_menu fm
                JOIN meal_time_slots mts ON mts.meal_type = fm.meal_type
                WHERE fm.day_of_week = DAYOFWEEK(?) AND fm.is_published = TRUE
                ORDER BY FIELD(fm.meal_type, 'BREAKFAST', 'LUNCH', 'SNACK', 'DINNER')
                """;
        List<MenuItem> rows = new ArrayList<>();
        try (Connection con = MySQLConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(readMenu(rs));
            }
        }
        return rows;
    }

    /** Reads the weekly menu template once and maps it onto seven calendar dates. */
    public List<DatedMenuItem> getPublishedMenuForNextSevenDays(LocalDate startDate) throws SQLException {
        if (startDate == null) throw new IllegalArgumentException("Choose a week start date.");
        String sql = """
                SELECT fm.menu_id, fm.day_of_week, fm.meal_type, fm.menu_description,
                       fm.is_published, mts.start_time AS slot_start_time, mts.end_time AS slot_end_time
                FROM food_menu fm
                JOIN meal_time_slots mts ON mts.meal_type = fm.meal_type
                WHERE fm.is_published = TRUE
                ORDER BY fm.day_of_week,
                         FIELD(fm.meal_type, 'BREAKFAST', 'LUNCH', 'SNACK', 'DINNER')
                """;
        List<MenuItem> templates = new ArrayList<>();
        try (Connection con = MySQLConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) templates.add(readMenu(rs));
        }

        List<DatedMenuItem> result = new ArrayList<>();
        for (int offset = 0; offset < 7; offset++) {
            LocalDate date = startDate.plusDays(offset);
            // Java: Monday=1...Sunday=7; MySQL DAYOFWEEK: Sunday=1...Saturday=7.
            int mysqlDayOfWeek = (date.getDayOfWeek().getValue() % 7) + 1;
            for (MenuItem item : templates) {
                if (item.dayOfWeek == mysqlDayOfWeek) result.add(new DatedMenuItem(date, item));
            }
        }
        return result;
    }

    private MenuItem readMenu(ResultSet rs) throws SQLException {
        return new MenuItem(rs.getLong("menu_id"), rs.getInt("day_of_week"),
                rs.getString("meal_type"), rs.getString("menu_description"),
                rs.getTime("slot_start_time").toLocalTime(),
                rs.getTime("slot_end_time").toLocalTime(), rs.getBoolean("is_published"));
    }

    public long saveMenu(long adminUserId, Long menuId, int dayOfWeek, String mealType,
                         String description, boolean published) throws SQLException {
        if (dayOfWeek < 1 || dayOfWeek > 7) throw new IllegalArgumentException("Choose a valid day.");
        requireMealType(mealType);
        if (description == null || description.isBlank()) throw new IllegalArgumentException("Menu description cannot be empty.");
        if (description.trim().length() > 5000) throw new IllegalArgumentException("Menu description is too long.");

        try (Connection con = MySQLConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                requireActiveRole(con, adminUserId, "ADMIN");
                LocalTime[] times = getScheduleTimes(con, mealType);
                Long existing = findMenuForSlot(con, dayOfWeek, mealType, menuId);
                if (menuId != null && existing != null) {
                    throw new SQLException("A menu already exists for this day and meal. Refresh and select the correct row to edit.");
                }
                if (menuId == null && existing != null) {
                    throw new SQLException("A menu already exists for this day and meal. Select that row in the table to edit it.");
                }

                if (menuId != null) {
                    String sql = """
                            UPDATE food_menu
                            SET day_of_week = ?, meal_type = ?, menu_description = ?,
                                meal_time = ?, meal_start_time = ?, meal_end_time = ?,
                                is_published = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP
                            WHERE menu_id = ?
                            """;
                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setInt(1, dayOfWeek); ps.setString(2, mealType); ps.setString(3, description.trim());
                        ps.setTime(4, Time.valueOf(times[0])); ps.setTime(5, Time.valueOf(times[0]));
                        ps.setTime(6, Time.valueOf(times[1])); ps.setBoolean(7, published);
                        ps.setLong(8, adminUserId); ps.setLong(9, menuId);
                        int changed = ps.executeUpdate();
                        if (changed == 0 && !menuExists(con, menuId)) throw new SQLException("The selected menu no longer exists.");
                    }
                    con.commit();
                    return menuId;
                }

                String sql = """
                        INSERT INTO food_menu
                          (day_of_week, meal_type, menu_description, meal_time,
                           meal_start_time, meal_end_time, is_published, updated_by, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                        """;
                long id;
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, dayOfWeek); ps.setString(2, mealType); ps.setString(3, description.trim());
                    ps.setTime(4, Time.valueOf(times[0])); ps.setTime(5, Time.valueOf(times[0]));
                    ps.setTime(6, Time.valueOf(times[1])); ps.setBoolean(7, published); ps.setLong(8, adminUserId);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Could not obtain new menu ID.");
                        id = keys.getLong(1);
                    }
                }
                con.commit();
                return id;
            } catch (SQLException | RuntimeException ex) {
                try { con.rollback(); } catch (SQLException rollback) { ex.addSuppressed(rollback); }
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    private Long findMenuForSlot(Connection con, int day, String mealType, Long excludeId) throws SQLException {
        String sql = "SELECT menu_id FROM food_menu WHERE day_of_week = ? AND meal_type = ?"
                + (excludeId == null ? "" : " AND menu_id <> ?") + " LIMIT 1 FOR UPDATE";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, day); ps.setString(2, mealType);
            if (excludeId != null) ps.setLong(3, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }

    private boolean menuExists(Connection con, long menuId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM food_menu WHERE menu_id = ?")) {
            ps.setLong(1, menuId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int publishAllMenus(long adminUserId) throws SQLException {
        String sql = "UPDATE food_menu SET is_published = TRUE, updated_by = ?, updated_at = CURRENT_TIMESTAMP WHERE is_published = FALSE";
        try (Connection con = MySQLConnection.getConnection()) {
            requireActiveRole(con, adminUserId, "ADMIN");
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setLong(1, adminUserId);
                return ps.executeUpdate();
            }
        }
    }

    private void requireMealType(String mealType) {
        if (mealType == null || !MEAL_TYPES.contains(mealType)) {
            throw new IllegalArgumentException("Choose a valid meal type.");
        }
    }

    // ---------------- Student feedback ----------------

    public void submitFeedback(long userId, long menuId, LocalDate date,
                               String rating, String comments) throws SQLException {
        if (date == null) throw new IllegalArgumentException("Choose a feedback date.");
        if (rating == null || !RATINGS.contains(rating)) throw new IllegalArgumentException("Choose Good, Average or Suggestions.");
        if (comments != null && comments.length() > 5000) throw new IllegalArgumentException("Feedback is too long.");

        try (Connection con = MySQLConnection.getConnection()) {
            long studentId = getActiveStudentId(con, userId);
            String check = "SELECT menu_id FROM food_menu WHERE menu_id = ? AND is_published = TRUE AND day_of_week = DAYOFWEEK(?)";
            try (PreparedStatement ps = con.prepareStatement(check)) {
                ps.setLong(1, menuId); ps.setDate(2, Date.valueOf(date));
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("This menu is not published for the selected date.");
                }
            }

            String sql = """
                    INSERT INTO food_feedback (menu_id, student_id, feedback_date, rating, comments)
                    VALUES (?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE rating = VALUES(rating), comments = VALUES(comments),
                                            submitted_at = CURRENT_TIMESTAMP
                    """;
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setLong(1, menuId); ps.setLong(2, studentId); ps.setDate(3, Date.valueOf(date)); ps.setString(4, rating);
                if (comments == null || comments.isBlank()) ps.setNull(5, Types.VARCHAR); else ps.setString(5, comments.trim());
                ps.executeUpdate();
            }
        }
    }

    public void saveAttendance(long userId, LocalDate mealDate, String mealType,
                               boolean willAttend) throws SQLException {
        if (mealDate == null || mealDate.isBefore(LocalDate.now())) throw new IllegalArgumentException("Choose today or a future date.");
        requireMealType(mealType);
        try (Connection con = MySQLConnection.getConnection()) {
            long studentId = getActiveStudentId(con, userId);
            String sql = """
                    INSERT INTO meal_attendance (student_id, meal_date, meal_type, will_attend)
                    VALUES (?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE will_attend = VALUES(will_attend), submitted_at = CURRENT_TIMESTAMP
                    """;
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setLong(1, studentId); ps.setDate(2, Date.valueOf(mealDate)); ps.setString(3, mealType); ps.setBoolean(4, willAttend);
                ps.executeUpdate();
            }
        }
    }

    // ---------------- Analytics ----------------

    public List<FeedbackSummary> getFeedbackSummary(long adminUserId,
                                                    LocalDate fromDate, LocalDate toDate) throws SQLException {
        validateDateRange(fromDate, toDate);
        String sql = """
                SELECT fm.day_of_week, fm.meal_type, fm.menu_description,
                       COUNT(ff.feedback_id) AS total_feedback,
                       COALESCE(SUM(CASE WHEN ff.rating = 'GOOD' THEN 1 ELSE 0 END), 0) AS good_count,
                       COALESCE(SUM(CASE WHEN ff.rating = 'AVERAGE' THEN 1 ELSE 0 END), 0) AS average_count,
                       COALESCE(SUM(CASE WHEN ff.rating = 'SUGGESTIONS' THEN 1 ELSE 0 END), 0) AS suggestions_count
                FROM food_menu fm
                LEFT JOIN food_feedback ff ON ff.menu_id = fm.menu_id
                                          AND ff.feedback_date BETWEEN ? AND ?
                GROUP BY fm.menu_id, fm.day_of_week, fm.meal_type, fm.menu_description
                ORDER BY fm.day_of_week, FIELD(fm.meal_type, 'BREAKFAST', 'LUNCH', 'SNACK', 'DINNER')
                """;
        List<FeedbackSummary> result = new ArrayList<>();
        try (Connection con = MySQLConnection.getConnection()) {
            requireActiveRole(con, adminUserId, "ADMIN");
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setDate(1, Date.valueOf(fromDate)); ps.setDate(2, Date.valueOf(toDate));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) result.add(new FeedbackSummary(rs.getInt("day_of_week"), rs.getString("meal_type"),
                            rs.getString("menu_description"), rs.getInt("total_feedback"), rs.getInt("good_count"),
                            rs.getInt("average_count"), rs.getInt("suggestions_count")));
                }
            }
        }
        return result;
    }

    public List<AttendanceSummary> getAttendanceSummary(long adminUserId,
                                                        LocalDate fromDate, LocalDate toDate) throws SQLException {
        validateDateRange(fromDate, toDate);
        String sql = """
                SELECT meal_date, meal_type, COUNT(*) AS total_responses,
                       SUM(CASE WHEN will_attend = TRUE THEN 1 ELSE 0 END) AS attending,
                       SUM(CASE WHEN will_attend = FALSE THEN 1 ELSE 0 END) AS not_attending
                FROM meal_attendance
                WHERE meal_date BETWEEN ? AND ?
                GROUP BY meal_date, meal_type
                ORDER BY meal_date, meal_type
                """;
        List<AttendanceSummary> result = new ArrayList<>();
        try (Connection con = MySQLConnection.getConnection()) {
            requireActiveRole(con, adminUserId, "ADMIN");
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setDate(1, Date.valueOf(fromDate)); ps.setDate(2, Date.valueOf(toDate));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) result.add(new AttendanceSummary(rs.getDate("meal_date").toLocalDate(),
                            rs.getString("meal_type"), rs.getInt("total_responses"), rs.getInt("attending"), rs.getInt("not_attending")));
                }
            }
        }
        return result;
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("Choose a valid date range.");
        }
    }
}
