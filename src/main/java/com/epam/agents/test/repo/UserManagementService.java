package com.epam.agents.test.repo;

import java.util.*;
import java.sql.*;
import java.io.*;

/**
 * Service for managing users in the application.
 */
public class UserManagementService {

  private static final String DB_URL = "jdbc:mysql://localhost:3306/mydb";
  private static final String DB_USER = "admin";
  private static final String DB_PASSWORD = "password123";

  private List<String> activeUserSessions = new ArrayList<>();

  /**
   * Finds a user by their username using raw JDBC.
   */
  public String findUser(String username) {
    String result = null;
    String query = "SELECT * FROM users WHERE username = '" + username + "'";

    try {
      Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
      Statement stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery(query);

      if (rs.next()) {
        result = rs.getString("email");
      }

      rs.close();
      stmt.close();
      conn.close();

    } catch (SQLException e) {
      System.out.println("Error finding user: " + e.getMessage());
    }

    return result;
  }

  /**
   * Adds a new active session for a user.
   */
  public void registerSession(String username) {
    activeUserSessions.add(username);
  }

  /**
   * Exports user data to a temporary file.
   */
  public void exportUserData(String data) {
    try {
      File file = new File("/tmp/export.txt");
      FileWriter fw = new FileWriter(file);
      fw.write(data);
      fw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Processes a list of numbers with inefficient string concatenation.
   */
  public String buildReport(List<Integer> numbers) {
    String report = "";
    for (Integer num : numbers) {
      report += "Number: " + num + "\n";
    }
    return report;
  }
}