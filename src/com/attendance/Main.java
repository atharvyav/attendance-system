package com.attendance;
import com.attendance.dao.AttendanceDAO;
import com.attendance.dao.CourseDAO;
import com.attendance.dao.DatabaseManager;
import com.attendance.dao.StudentDAO;
import com.attendance.dao.UserDAO;
import com.attendance.model.*;
import com.attendance.service.AttendanceService;
import com.attendance.service.PasswordHasher;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        AttendanceService attendanceService = new AttendanceService();
        if (!DatabaseManager.initializeDatabase()) {
            System.err.println("Cannot start because the database could not be initialized.");
            scanner.close();
            return;
        }
        AttendanceDAO attendanceDAO = new AttendanceDAO();
        StudentDAO studentDAO = new StudentDAO();
        UserDAO userDAO = new UserDAO();
        CourseDAO courseDAO = new CourseDAO();
        System.out.println("Application ready!");

        try {
            if (userDAO.removeKnownDefaultAdmin()) {
                System.out.println("[SECURITY] Removed the known default administrator account.");
            }
            if (!userDAO.hasAdmin() && !createInitialAdmin(scanner, userDAO)) {
                scanner.close();
                return;
            }
        } catch (java.sql.SQLException e) {
            System.err.println("[DATABASE ERROR] Could not verify administrator accounts: " + e.getMessage());
            scanner.close();
            return;
        }

        // Saving existing course codes is harmless; SQLite rejects duplicates.
        courseDAO.save(new Course("21CSS101J", "Object Oriented Programming"));
        courseDAO.save(new Course("21MAB102", "Advanced Calculus"));
        courseDAO.save(new Course("21LEH104T", "German Language"));

        User loggedInUser = null;

        // Login / Sign Up Navigation Menu
       while (loggedInUser == null) {
            System.out.println("\n=========================================");
            System.out.println("   ATTENDANCE MANAGEMENT SYSTEM");
            System.out.println("=========================================");
            System.out.println("1. Login");
            System.out.println("2. Register New Student User Account");
            System.out.println("3. Exit");
            System.out.print("Choose option (1-3): ");

            String choice = readLine(scanner);

            if("1".equals(choice)) {
                System.out.print("\nEnter Email: ");
                String email = readLine(scanner).trim();
                String password = readSecret(scanner, "Enter Password: ");

                java.util.Optional<User> userOpt = userDAO.findByEmailAndPassword(email, password);
                if (userOpt.isPresent()) {
                  loggedInUser = userOpt.get();
                  System.out.println("\n[SUCCESS] Welcome, " + loggedInUser.getEmail() + " (" + loggedInUser.getRole() + ")");
                 } else {
                 System.out.println("\n[ERROR] Invalid credentials. Try again.");
                 }
           } else if ("2".equals(choice)) {
    System.out.println("\n--- NEW USER ACCOUNT REGISTRATION ---");
    System.out.print("Enter Student Full Name: ");
    String name = readLine(scanner).trim();
    System.out.print("Enter Roll Number : ");
    String rollNo = readLine(scanner).trim();

    System.out.print("Enter Account Email: ");
    String email = readLine(scanner).trim();
    String password = readSecret(scanner, "Enter Account Password (6-128 characters): ");

    if (name.isBlank() || rollNo.isBlank() || !isValidEmail(email)
            || !PasswordHasher.meetsPolicy(password)) {
        System.out.println("\n[ERROR] Enter a name, roll number, valid email, and a password of 6-128 characters.");
        continue;
    }
    if (userDAO.emailExists(email)) {
        System.out.println("\n[ERROR] Email already registered in database!");
        continue;
    }

    String nextId = studentDAO.generateNextStudentId();
    Student newStudent = new Student(nextId, name, rollNo);
    boolean registered = studentDAO.save(newStudent)
            && userDAO.save(email, password, "STUDENT", newStudent.getId());
    if (registered) {
        System.out.println("\n[SUCCESS] Registered and saved to database!");
        System.out.println("Assigned Student ID: " + newStudent.getId());
    } else {
        System.out.println("\n[ERROR] Registration failed. Check that the roll number is unique.");
    }
            } else if ("3".equals(choice)) {
                System.out.println("Exiting application.");
                scanner.close();
                return;
            }
        }

        // Post-Login Operations Dashboard
        boolean running = true;
        while (running) {
            if ("ADMIN".equals(loggedInUser.getRole())) {
                System.out.println("\n=== ADMIN CONTROL PANEL ===");
                System.out.println("1. Register New Student Profile");
                System.out.println("2. Create New Course");
                System.out.println("3. Enroll Student in Course");
                System.out.println("4. Mark Student Attendance");
                System.out.println("5. Generate Threshold Alert Report (< 75%)");
                System.out.println("6. View All Courses & Enrolled Students");
                System.out.println("7. Logout");
                System.out.println("8. Change Password");
                System.out.println("9. Create Admin Account");

                String adminOpt = readLine(scanner);

                switch (adminOpt) {
                    case "1" -> {
                    System.out.print("\nEnter Student Full Name: ");
                  String name = readLine(scanner).trim();
                 System.out.print("Enter Register / Roll Number: ");
                 String rollNo = readLine(scanner).trim();

                  if (name.isBlank() || rollNo.isBlank()) {
                    System.out.println("[ERROR] Name and roll number are required.");
                    break;
                  }

                  String nextId = studentDAO.generateNextStudentId();
                  Student newStudent = new Student(nextId, name, rollNo);
                  System.out.print("Enter Login Email for Student: ");
                  String email = readLine(scanner).trim();
                   String password = readSecret(scanner, "Enter Login Password (6-128 characters): ");

                   if (!isValidEmail(email) || !PasswordHasher.meetsPolicy(password)
                           || userDAO.emailExists(email)) {
                     System.out.println("[ERROR] Enter a valid email and 6-128 character password; the email must be unused.");
                     break;
                   }
                   boolean created = studentDAO.save(newStudent)
                         && userDAO.save(email, password, "STUDENT", newStudent.getId());

                   if (created) {
                   System.out.println("\n[SUCCESS] Student profile and login account created!");
                 System.out.println("Assigned Student ID: " + newStudent.getId());
                 } else {
                  System.out.println("\n[ERROR] Failed to create login account (email may already exist).");
                  }
                }
                    
                    case "2" -> {
                        
                        
                      System.out.println("\n--- CREATE NEW COURSE ---");
                        System.out.print("Enter Course Code (e.g., 21CSC203P): ");
                          String code = readLine(scanner).trim();
                           System.out.print("Enter Course Title: ");
                        String title = readLine(scanner).trim();

                        if (code.isBlank() || title.isBlank()) {
                            System.out.println("[ERROR] Course code and title are required.");
                        } else if (courseDAO.save(new Course(code, title))) {
                            System.out.println("[SUCCESS] Course created successfully!");
                        } else {
                            System.out.println("[ERROR] Course could not be created. Check for a duplicate code.");
                        }
 
                       
                    }
                    case "3" -> {
    System.out.println("\n--- ADMIN: ENROLL STUDENT IN COURSE ---");

    System.out.println("Registered Students:");
    for (Student s : studentDAO.findAll()) {
        System.out.printf("  ID: %-10s | Name: %-18s | Roll: %s%n", s.getId(), s.getName(), s.getRollNo());
    }

    System.out.println("\nAvailable Courses:");
    for (Course c : courseDAO.findAll()) {
        System.out.printf("  Code: %-12s | Title: %s%n", c.getCode(), c.getTitle());
    }

    System.out.print("\nEnter Student ID (e.g., STU-1001): ");
    String studentId = readLine(scanner).trim();
    System.out.print("Enter Course Code (e.g., 21CSS101J): ");
    String courseCode = readLine(scanner).trim();

    boolean enrolled = attendanceDAO.enrollStudent(studentId, courseCode);
    if (enrolled) {
        System.out.println("\n[SUCCESS] Student " + studentId + " enrolled in " + courseCode + "!");
    } else {
        System.out.println("\n[ERROR] Enrollment failed or student is already enrolled in this course.");
    }
}
                    case "4" -> {
    System.out.print("\nEnter Student ID: ");
    String stuId = scanner.nextLine().trim();
    System.out.print("Enter Course Code: ");
    String cCode = scanner.nextLine().trim();

    if (!attendanceDAO.isStudentEnrolledInCourse(stuId, cCode)) {
        System.out.println("[ERROR] Student is not enrolled in this course!");
    } else {
        System.out.print("Session date (YYYY-MM-DD, blank for today): ");
        String dateInput = readLine(scanner).trim();
        LocalDate sessionDate;
        try {
            sessionDate = dateInput.isEmpty() ? LocalDate.now() : LocalDate.parse(dateInput);
        } catch (DateTimeParseException e) {
            System.out.println("[ERROR] Invalid date. Use YYYY-MM-DD.");
            break;
        }
        System.out.print("Select Status (1. PRESENT, 2. ABSENT, 3. EXCUSED): ");
        String statusInput = readLine(scanner).trim();
        AttendanceStatus status = switch (statusInput) {
            case "1" -> AttendanceStatus.PRESENT;
            case "2" -> AttendanceStatus.ABSENT;
            case "3" -> AttendanceStatus.EXCUSED;
            default -> null;
        };

        if (status == null) {
            System.out.println("[ERROR] Invalid attendance status.");
        } else if (attendanceDAO.markAttendance(stuId, cCode, status, sessionDate)) {
            System.out.println("[SUCCESS] Marked " + status + " for " + stuId + " in " + cCode
                    + " on " + sessionDate);
        } else {
            System.out.println("[ERROR] Attendance could not be saved.");
        }
    }
}
                    case "5" -> {
                        System.out.print("\nEnter Course Code to evaluate: ");
                        String cCode = readLine(scanner);
                        double threshold = 75.0;

                       List<Student> students = studentDAO.findAll();

                        System.out.println("\n--- LOW ATTENDANCE REPORT (< " + threshold + "%) FOR " + cCode + " ---");
                          boolean foundAny = false;
                       for (Student s : students) {
        // Fetch student's attendance records directly from SQLite
        List<AttendanceRecord> sRecs = attendanceDAO.findByStudent(s.getId()).stream()
                .filter(r -> r.getCourseCode().equalsIgnoreCase(cCode))
                .toList();

        if (attendanceDAO.isStudentEnrolledInCourse(s.getId(), cCode) && !sRecs.isEmpty()) {
            double pct = attendanceService.calculateAttendancePercentage(sRecs);
            if (pct < threshold) {
                foundAny = true;
                System.out.printf("ALERT: %s (ID: %s | Roll: %s) Current: %.2f%%\n",
                        s.getName(), s.getId(), s.getRollNo(), pct);
            }
        }
    }

    if (!foundAny) {
        System.out.println("All enrolled students meet the attendance threshold.");
    }
}
                    case "6" -> {
                        System.out.println("\n--- SYSTEM COURSES & ENROLLED STUDENTS ---");
    List<Student> dbStudents = studentDAO.findAll();
    for (Course c : courseDAO.findAll()) {
        System.out.println("\nCourse: [" + c.getCode() + "] " + c.getTitle());
        List<Student> enrolled = dbStudents.stream()
                .filter(s -> attendanceDAO.isStudentEnrolledInCourse(s.getId(), c.getCode()))
                .toList();
        if (enrolled.isEmpty()) {
            System.out.println("  (No students enrolled)");
        } else {
            enrolled.forEach(s -> System.out.println("  - " + s.getName() + " (" + s.getId() + ")"));
        }
    }
                    }
                    case "7" -> running = false;
                    case "8" -> changePassword(scanner, userDAO, loggedInUser);
                    case "9" -> createAdminAccount(scanner, userDAO);
                    default -> System.out.println("Invalid option.");
                }

            } else if ("STUDENT".equals(loggedInUser.getRole())) {
                System.out.println("\n=== STUDENT DASHBOARD ===");
                System.out.println("1. View My Attendance (Enrolled Courses)");
                System.out.println("2. Logout");
                System.out.println("3. Change Password");
                System.out.print("Select action (1-3): ");

                String studentOpt = readLine(scanner);

                if ("1".equals(studentOpt)) {
                    String studentId = loggedInUser.getStudentId();
                    List<Course> myCourses = courseDAO.findAll().stream()
                            .filter(c -> attendanceDAO.isStudentEnrolledInCourse(studentId, c.getCode()))
                            .toList();

                    System.out.println("\n--- MY ENROLLED COURSES & ATTENDANCE ---");
                    if (myCourses.isEmpty()) {
                        System.out.println("You are not currently enrolled in any courses.");
                    } else {
                        List<AttendanceRecord> allMyRecords = attendanceDAO.findByStudent(studentId);
                        for (Course c : myCourses) {
                            List<AttendanceRecord> myRecs = allMyRecords.stream()
                            .filter(r -> r.getCourseCode().equalsIgnoreCase(c.getCode()))
                            .toList();

                            double percentage = attendanceService.calculateAttendancePercentage(myRecs);
                            System.out.printf("[%s] %s: %.2f%% ", c.getCode(), c.getTitle(), percentage);

                            if (myRecs.isEmpty()) {
                                System.out.println("(No sessions recorded)");
                            } else if (percentage < 75.0) {
                                System.out.println("-> [WARNING] Attendance below 75% threshold!");
                            } else {
                                System.out.println("-> [OK] Good standing");
                            }
                        }
                    }
                } else if ("2".equals(studentOpt)) {
                    running = false;
                } else if ("3".equals(studentOpt)) {
                    changePassword(scanner, userDAO, loggedInUser);
                }
            }
        }

        System.out.println("Logged out successfully.");
        scanner.close();
    }

    private static boolean createInitialAdmin(Scanner scanner, UserDAO userDAO) {
        System.out.println("\n--- FIRST-RUN ADMINISTRATOR SETUP ---");
        System.out.println("No administrator account exists. Create one to continue.");
        while (true) {
            System.out.print("Administrator email: ");
            String email = readLine(scanner).trim();
            String password = readSecret(scanner, "Administrator password (6-128 characters): ");
            String confirmation = readSecret(scanner, "Confirm password: ");

            if (!isValidEmail(email)) {
                System.out.println("[ERROR] Enter a valid email address.");
            } else if (!PasswordHasher.meetsPolicy(password)) {
                System.out.println("[ERROR] Password must be between 6 and 128 characters.");
            } else if (!password.equals(confirmation)) {
                System.out.println("[ERROR] Passwords do not match.");
            } else if (userDAO.save(email, password, "ADMIN", null)) {
                System.out.println("[SUCCESS] Administrator account created.");
                return true;
            } else {
                System.out.println("[ERROR] Could not create administrator. The email may already be in use.");
            }
        }
    }

    private static void changePassword(Scanner scanner, UserDAO userDAO, User user) {
        String currentPassword = readSecret(scanner, "Current password: ");
        String newPassword = readSecret(scanner, "New password (6-128 characters): ");
        String confirmation = readSecret(scanner, "Confirm new password: ");
        if (!PasswordHasher.meetsPolicy(newPassword)) {
            System.out.println("[ERROR] Password must be between 6 and 128 characters.");
        } else if (!newPassword.equals(confirmation)) {
            System.out.println("[ERROR] New passwords do not match.");
        } else if (userDAO.changePassword(user.getId(), currentPassword, newPassword)) {
            System.out.println("[SUCCESS] Password changed.");
        } else {
            System.out.println("[ERROR] Password change failed. Check your current password and try again.");
        }
    }

    private static void createAdminAccount(Scanner scanner, UserDAO userDAO) {
        System.out.println("\n--- CREATE ADMIN ACCOUNT ---");
        System.out.print("Administrator email: ");
        String email = readLine(scanner).trim();
        String password = readSecret(scanner, "Password (6-128 characters): ");
        String confirmation = readSecret(scanner, "Confirm password: ");

        if (!isValidEmail(email)) {
            System.out.println("[ERROR] Enter a valid email address.");
        } else if (!PasswordHasher.meetsPolicy(password)) {
            System.out.println("[ERROR] Password must be between 6 and 128 characters.");
        } else if (!password.equals(confirmation)) {
            System.out.println("[ERROR] Passwords do not match.");
        } else if (userDAO.emailExists(email)) {
            System.out.println("[ERROR] That email is already registered.");
        } else if (userDAO.save(email, password, "ADMIN", null)) {
            System.out.println("[SUCCESS] Admin account created for " + email + ".");
        } else {
            System.out.println("[ERROR] Admin account could not be created.");
        }
    }

    private static String readSecret(Scanner scanner, String prompt) {
        java.io.Console console = System.console();
        if (console != null) {
            char[] secret = console.readPassword("%s", prompt);
            if (secret != null) {
                String value = new String(secret);
                java.util.Arrays.fill(secret, '\0');
                return value;
            }
        }
        System.out.print(prompt);
        return scanner.nextLine();
    }

    private static String readLine(Scanner scanner) {
        java.io.Console console = System.console();
        if (console != null) {
            String line = console.readLine();
            if (line != null) {
                return line;
            }
        }
        return scanner.nextLine();
    }

    private static boolean isValidEmail(String email) {
        return email != null && email.length() <= 254
                && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }
}