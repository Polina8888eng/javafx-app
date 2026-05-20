package com.example.model;

import java.time.LocalDateTime;

public class DocumentNote {

  private int id;
  private int userId;
  private String filePath;
  private String noteText;
  private LocalDateTime updatedAt;

  public DocumentNote() {
  }

  public DocumentNote(int id, int userId, String filePath, String noteText,
      LocalDateTime updatedAt) {
    this.id = id;
    this.userId = userId;
    this.filePath = filePath;
    this.noteText = noteText;
    this.updatedAt = updatedAt;
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

  public String getNoteText() {
    return noteText;
  }

  public void setNoteText(String noteText) {
    this.noteText = noteText;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
