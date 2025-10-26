package com.example.acadease.utils;

import com.example.acadease.model.Schedule;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class ScheduleUtility {

    private static final Map<String, Integer> DAY_MAP = Map.of(
            "MON", Calendar.MONDAY, "TUE", Calendar.TUESDAY, "WED", Calendar.WEDNESDAY,
            "THU", Calendar.THURSDAY, "FRI", Calendar.FRIDAY, "SAT", Calendar.SATURDAY, "SUN", Calendar.SUNDAY
    );

    /**
     * Generates a list of concrete 'sessions' objects (Maps) based on the recurring schedule rule.
     * CRITICAL FIX: Injects the scheduled HH:MM time into the Timestamp object in IST.
     */
    public static List<Map<String, Object>> generateSessions(Schedule schedule, String scheduleId) {
        List<Map<String, Object>> sessionList = new ArrayList<>();

        Date startDate = schedule.getStartDate().toDate();
        Date endDate = schedule.getEndDate().toDate();

        // Extract hour and minute from the 24hr time string (e.g., "14:00")
        String timeString = schedule.getStartTime();
        int hour = Integer.parseInt(timeString.substring(0, 2));
        int minute = Integer.parseInt(timeString.substring(3, 5));

        // Set up the Calendar in IST/Kolkata TimeZone
        Calendar current = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
        current.setTime(startDate);

        while (!current.getTime().after(endDate)) {

            int currentDay = current.get(Calendar.DAY_OF_WEEK);

            for (String dayString : schedule.getDaysOfWeek()) {
                Integer targetDay = DAY_MAP.get(dayString.toUpperCase());

                if (targetDay != null && currentDay == targetDay) {

                    // --- TIME INJECTION FIX ---
                    // Create a new Calendar object for the session using IST
                    Calendar sessionCal = (Calendar) current.clone();

                    // Inject the HH:MM time from the schedule blueprint
                    sessionCal.set(Calendar.HOUR_OF_DAY, hour);
                    sessionCal.set(Calendar.MINUTE, minute);
                    sessionCal.set(Calendar.SECOND, 0);
                    sessionCal.set(Calendar.MILLISECOND, 0);

                    Timestamp sessionTimestamp = new Timestamp(sessionCal.getTime());
                    // --- END TIME INJECTION FIX ---

                    Map<String, Object> sessionData = Map.of(
                            "scheduleId", scheduleId,
                            "courseCode", schedule.getCourseCode(),
                            "facultyId", schedule.getFacultyId(),
                            "sessionTime", sessionTimestamp,
                            "venue", schedule.getVenue(),
                            "type", schedule.getType()
                    );
                    sessionList.add(sessionData);
                    break;
                }
            }

            current.add(Calendar.DATE, 1); // Move to the next day
        }

        return sessionList;
    }
}