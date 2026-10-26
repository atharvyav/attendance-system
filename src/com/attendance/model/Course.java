package com.attendance.model;

public class Course {
    private final String code;
    private final String title;

    public Course(String code, String title) {
        this.code = code.toUpperCase().trim();
        this.title = title.trim();
    }

    public String getCode() { return code; }
    public String getTitle() { return title; }
}