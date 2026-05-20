package com.example.model;

import java.time.LocalDateTime;

public class Favorite {

  private int id;
  private int userId;
  private String filePath;
  private String title;
  private LocalDateTime createdAt;

  public Favorite() {
  }

  public Favorite(int id, int userId, String filePath, String title, LocalDateTime createdAt) {
    this.id = id;
    this.userId = userId;
    this.filePath = filePath;
    this.title = title;
    this.createdAt = createdAt;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getUserId() {
    return userId;
  }

  public void setUserId(int userId) {
    this.userId = userId;
  }

  public String getFilePath() {
    return filePath;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
