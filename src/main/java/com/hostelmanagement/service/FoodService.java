package com.hostelmanagement.service;

import com.hostelmanagement.dao.FoodDAO;
import com.hostelmanagement.dao.FoodDAO.AttendanceSummary;
import com.hostelmanagement.dao.FoodDAO.DatedMenuItem;
import com.hostelmanagement.dao.FoodDAO.FeedbackSummary;
import com.hostelmanagement.dao.FoodDAO.MealSchedule;
import com.hostelmanagement.dao.FoodDAO.MenuItem;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class FoodService {
    private final FoodDAO dao = new FoodDAO();

    public List<MealSchedule> getMealSchedules(long adminUserId) throws SQLException {
        return dao.getMealSchedules(adminUserId);
    }

    public void updateMealSchedule(long adminUserId, String mealType,
                                   LocalTime startTime, LocalTime endTime) throws SQLException {
        dao.updateMealSchedule(adminUserId, mealType, startTime, endTime);
    }

    public List<MenuItem> getAdminMenu(long adminUserId) throws SQLException {
        return dao.getMenusForAdmin(adminUserId);
    }

    public List<MenuItem> getAdminMenu(long adminUserId, int dayOfWeek) throws SQLException {
        return dao.getMenusForAdmin(adminUserId, dayOfWeek);
    }

    public List<MenuItem> getPublishedMenu(LocalDate date) throws SQLException {
        if (date == null) throw new IllegalArgumentException("Please select a menu date.");
        return dao.getPublishedMenu(date);
    }

    public List<DatedMenuItem> getPublishedMenuForNextSevenDays(LocalDate startDate) throws SQLException {
        if (startDate == null) throw new IllegalArgumentException("Please select a week start date.");
        return dao.getPublishedMenuForNextSevenDays(startDate);
    }

    public long saveMenu(long adminUserId, Long menuId, int dayOfWeek,
                         String mealType, String description, boolean published) throws SQLException {
        return dao.saveMenu(adminUserId, menuId, dayOfWeek, mealType, description, published);
    }

    public int publishAllMenus(long adminUserId) throws SQLException {
        return dao.publishAllMenus(adminUserId);
    }

    public void submitFeedback(long userId, long menuId, LocalDate date,
                               String rating, String comments) throws SQLException {
        dao.submitFeedback(userId, menuId, date, rating, comments);
    }

    public void saveAttendance(long userId, LocalDate mealDate,
                               String mealType, boolean willAttend) throws SQLException {
        dao.saveAttendance(userId, mealDate, mealType, willAttend);
    }

    public List<FeedbackSummary> getWeeklyFeedback(long adminUserId,
                                                   LocalDate fromDate, LocalDate toDate) throws SQLException {
        return dao.getFeedbackSummary(adminUserId, fromDate, toDate);
    }

    public List<AttendanceSummary> getMealAttendance(long adminUserId,
                                                     LocalDate fromDate, LocalDate toDate) throws SQLException {
        return dao.getAttendanceSummary(adminUserId, fromDate, toDate);
    }
}
