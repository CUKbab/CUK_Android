package com.cukbab.widget

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class WidgetScheduleTest {
    // 2026-11-17 is a Tuesday; 2026-11-20 a Friday; 2026-11-21 a Saturday.
    private fun at(date: String, time: String) = LocalDateTime.parse("${date}T$time")
    private fun d(s: String) = LocalDate.parse(s)

    @Test fun autoFollowsTimeOfDay() {
        assertEquals(WidgetTarget(d("2026-11-17"), "Morning"), WidgetSchedule.resolve(WIDGET_CATEGORY_AUTO, at("2026-11-17", "07:00")))
        assertEquals(WidgetTarget(d("2026-11-17"), "Pranzo-Korean"), WidgetSchedule.resolve(WIDGET_CATEGORY_AUTO, at("2026-11-17", "09:30")))
        assertEquals(WidgetTarget(d("2026-11-17"), "Pranzo-Dinner"), WidgetSchedule.resolve(WIDGET_CATEGORY_AUTO, at("2026-11-17", "15:00")))
        assertEquals(WidgetTarget(d("2026-11-18"), "Morning"), WidgetSchedule.resolve(WIDGET_CATEGORY_AUTO, at("2026-11-17", "19:00")))
    }

    @Test fun fridayEveningAndWeekendGoToMonday() {
        assertEquals(WidgetTarget(d("2026-11-23"), "Morning"), WidgetSchedule.resolve(WIDGET_CATEGORY_AUTO, at("2026-11-20", "20:00")))
        assertEquals(WidgetTarget(d("2026-11-23"), "Morning"), WidgetSchedule.resolve(WIDGET_CATEGORY_AUTO, at("2026-11-21", "12:00")))
        assertEquals(WidgetTarget(d("2026-11-23"), "Bona-Rice-Bowl"), WidgetSchedule.resolve("Bona-Rice-Bowl", at("2026-11-22", "12:00")))
    }

    @Test fun fixedCategoryRollsOverAfterItsWindow() {
        assertEquals(WidgetTarget(d("2026-11-17"), "Pranzo-Korean"), WidgetSchedule.resolve("Pranzo-Korean", at("2026-11-17", "13:59")))
        assertEquals(WidgetTarget(d("2026-11-18"), "Pranzo-Korean"), WidgetSchedule.resolve("Pranzo-Korean", at("2026-11-17", "14:00")))
        assertEquals(WidgetTarget(d("2026-11-17"), "Pranzo-Dinner"), WidgetSchedule.resolve("Pranzo-Dinner", at("2026-11-17", "14:00")))
        assertEquals(WidgetTarget(d("2026-11-18"), "Morning"), WidgetSchedule.resolve("1000-won-Morning", at("2026-11-17", "10:00")))
    }

    @Test fun nextBoundary() {
        assertEquals(at("2026-11-17", "14:00"), WidgetSchedule.nextBoundary(at("2026-11-17", "10:00")))
        assertEquals(at("2026-11-18", "00:05"), WidgetSchedule.nextBoundary(at("2026-11-17", "19:00")))
    }
}
