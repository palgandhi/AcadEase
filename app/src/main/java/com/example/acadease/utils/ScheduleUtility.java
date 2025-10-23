package com.example.acadease.utils;

import com.example.acadease.model.Schedule;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ScheduleUtility {

    // Map of day string to Calendar field constant
    private static final Map<String, Integer> DAY_MAP = Map.of(
            "MON", Calendar.MONDAY,
            "TUE", Calendar.TUESDAY,
            "WED", Calendar.WEDNESDAY,
            "THU", Calendar.THURSDAY,
            "FRI", Calendar.FRIDAY,
            "SAT", Calendar.SATURDAY,
            "SUN", Calendar.SUNDAY
    );

    /**
     * Generates a list of concrete 'sessions' objects (Maps) based on the recurring schedule rule.
     * This is the list that will be written to the 'sessions' collection.
     * * @param schedule The Schedule POJO containing the recurrence rule.
     * @param scheduleId The Firestore ID of the parent schedule document.
     * @return A List of Map<String, Object> representing the sessions to be created.
     */
    public static List<Map<String, Object>> generateSessions(Schedule schedule, String scheduleId) {
        List<Map<String, Object>> sessionList = new ArrayList<>();

        Date startDate = schedule.getStartDate().toDate();
        Date endDate = schedule.getEndDate().toDate();

        Calendar current = Calendar.getInstance();
        current.setTime(startDate);

        // Loop through all dates between start and end
        while (!current.getTime().after(endDate)) {

            int currentDay = current.get(Calendar.DAY_OF_WEEK);

            // Check if the current day matches any recurring day
            for (String dayString : schedule.getDaysOfWeek()) {
                Integer targetDay = DAY_MAP.get(dayString.toUpperCase());

                if (targetDay != null && currentDay == targetDay) {

                    // Found a session day! Create the session document map.

                    // CRITICAL: We need to combine the current date with the time string.
                    // This is complex, but we'll approximate the start of the day for now
                    // and assume time adjustments happen later.

                    Map<String, Object> sessionData = Map.of(
                            "scheduleId", scheduleId,
                            "courseCode", schedule.getCourseCode(),
                            "facultyId", schedule.getFacultyId(),
                            "sessionTime", new Timestamp(current.getTime()), // The date of the session
                            "venue", schedule.getVenue(),
                            "type", schedule.getType()
                    );
                    sessionList.add(sessionData);
                    break; // Move to the next day
                }
            }

            // Move to the next day
            current.add(Calendar.DATE, 1);
        }

        return sessionList;
    }
}