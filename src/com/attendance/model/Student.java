package com.attendance.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Student {
    private final String id;
    private final String name;
    private final String rollNo;

    public Student(String id, String name) {
        this.name = name;
        this.id = id;
        this.rollNo = rollNumberForId(id);
    }

    public Student(String id, String name, String rollNo) {
        this.id = id;
        this.name = name;
        this.rollNo = rollNo;
    }

    public static String rollNumberForId(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Student ID cannot be null");
        }
        Matcher matcher = Pattern.compile("(\\d+)$").matcher(id);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Student ID must end in digits");
        }
        String numericSuffix = matcher.group(1);
        return numericSuffix.substring(Math.max(0, numericSuffix.length() - 2));
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getRollNo() { return rollNo; }
}