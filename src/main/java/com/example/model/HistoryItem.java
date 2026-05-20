package com.example.model;

import java.time.LocalDateTime;

public class HistoryItem {

  private int id;
  private int userId;
  private String filePath;
  private LocalDateTime lastOpened;

  public HistoryItem() {
  }

  public HistoryItem(int id, int userId, String filePath, LocalDateTime lastOpened) {
    this.id = id;
    this.userId = userId;
    this.filePath = filePath;
    this.lastOpened = lastOpened;
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

  public LocalDateTime getLastOpened() {
    return lastOpened;
  }

  public void setLastOpened(LocalDateTime lastOpened) {
    this.lastOpened = lastOpened;
  }
}
