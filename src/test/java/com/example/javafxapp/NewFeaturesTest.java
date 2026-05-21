package com.example.javafxapp;

import com.example.model.*;
import com.example.service.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class NewFeaturesTest {

  @Test
  void testFavoriteConstructor() {
    LocalDateTime now = LocalDateTime.now();
    Favorite fav = new Favorite(1, 100, "C:\\doc.txt", "doc.txt", now);
    assertEquals(1, fav.getId());
    assertEquals(100, fav.getUserId());
    assertEquals("C:\\doc.txt", fav.getFilePath());
    assertEquals("doc.txt", fav.getTitle());
    assertEquals(now, fav.getCreatedAt());
  }

  @Test
  void testFavoriteSetters() {
    Favorite fav = new Favorite();
    fav.setId(2);
    fav.setUserId(200);
    fav.setFilePath("C:\\file.pdf");
    fav.setTitle("file.pdf");
    LocalDateTime time = LocalDateTime.now();
    fav.setCreatedAt(time);
    assertEquals(2, fav.getId());
    assertEquals(200, fav.getUserId());
    assertEquals("C:\\file.pdf", fav.getFilePath());
    assertEquals("file.pdf", fav.getTitle());
    assertEquals(time, fav.getCreatedAt());
  }

  @Test
  void testHistoryItemConstructor() {
    LocalDateTime now = LocalDateTime.now();
    HistoryItem item = new HistoryItem(10, 25, "C:\\doc.docx", now);
    assertEquals(10, item.getId());
    assertEquals(25, item.getUserId());
    assertEquals("C:\\doc.docx", item.getFilePath());
    assertEquals(now, item.getLastOpened());
  }

  @Test
  void testDocumentNoteConstructor() {
    LocalDateTime now = LocalDateTime.now();
    DocumentNote note = new DocumentNote(5, 25, "C:\\note.txt", "my text", now);
    assertEquals(5, note.getId());
    assertEquals(25, note.getUserId());
    assertEquals("C:\\note.txt", note.getFilePath());
    assertEquals("my text", note.getNoteText());
    assertEquals(now, note.getUpdatedAt());
  }

  private List<Favorite> parseFavorites(String json) {
    JSONArray arr = new JSONArray(json);
    List<Favorite> list = new ArrayList<>();
    for (int i = 0; i < arr.length(); i++) {
      JSONObject obj = arr.getJSONObject(i);
      list.add(new Favorite(
          obj.getInt("id"),
          obj.getInt("user_id"),
          obj.getString("file_path"),
          obj.getString("title"),
          LocalDateTime.parse(obj.getString("created_at"))));
    }
    return list;
  }

  @Test
  void testParseFavorites() {
    String json = "[{\"id\":1,\"user_id\":25,\"file_path\":\"C:\\\\test.txt\",\"title\":\"test.txt\",\"created_at\":\"2025-05-20T10:00:00\"}]";
    List<Favorite> favs = parseFavorites(json);
    assertEquals(1, favs.size());
    Favorite fav = favs.get(0);
    assertEquals(1, fav.getId());
    assertEquals(25, fav.getUserId());
    assertEquals("C:\\test.txt", fav.getFilePath());
    assertEquals("test.txt", fav.getTitle());
    assertEquals("2025-05-20T10:00", fav.getCreatedAt().toString().substring(0, 16));
  }

  @Test
  void testParseEmptyFavorites() {
    String json = "[]";
    List<Favorite> favs = parseFavorites(json);
    assertTrue(favs.isEmpty());
  }

  private String encodeParam(String param) throws UnsupportedEncodingException {
    return URLEncoder.encode(param, StandardCharsets.UTF_8.name());
  }

  @Test
  void testEncodeParamWithSpaces() throws Exception {
    assertEquals("C%3A%5CMy+Docs%5Cfile.txt", encodeParam("C:\\My Docs\\file.txt"));
  }

  @Test
  void testEncodeParamWithCyrillic() throws Exception {
    String encoded = encodeParam("Привет мир");
    assertTrue(encoded.contains("%D0%9F%D1%80%D0%B8%D0%B2%D0%B5%D1%82"));
    assertTrue(encoded.contains("%D0%BC%D0%B8%D1%80"));
  }

  @Nested
  @ExtendWith(MockitoExtension.class)
  class UpsertMockTest {

    @Mock
    private DatabaseService dbServiceMock;

    @Test
    void testAddOrUpdateHistory_WhenExists() throws Exception {
      DatabaseService spy = spy(new DatabaseService());
      // имитируем, что GET-запрос вернул существующую запись
      doReturn("[{\"id\":1}]").when(spy).sendGetRequest(anyString());
      doReturn("{}").when(spy).sendPatchRequest(anyString(), anyString());

      spy.addOrUpdateHistory(25, "C:\\test.txt");

      verify(spy, times(1)).sendPatchRequest(anyString(), anyString());
      verify(spy, never()).sendPostRequest(anyString(), anyString());
    }

    @Test
    void testAddOrUpdateHistory_WhenNotExists() throws Exception {
      DatabaseService spy = spy(new DatabaseService());
      doReturn("[]").when(spy).sendGetRequest(anyString());
      doReturn("{}").when(spy).sendPostRequest(anyString(), anyString());

      spy.addOrUpdateHistory(25, "C:\\test.txt");

      verify(spy, times(1)).sendPostRequest(anyString(), anyString());
      verify(spy, never()).sendPatchRequest(anyString(), anyString());
    }

    @Test
    void testSaveOrUpdateNote_WhenExists() throws Exception {
      DatabaseService spy = spy(new DatabaseService());
      doReturn("[{\"id\":1}]").when(spy).sendGetRequest(anyString());
      doReturn("{}").when(spy).sendPatchRequest(anyString(), anyString());

      spy.saveOrUpdateNote(25, "C:\\note.txt", "new text");

      verify(spy, times(1)).sendPatchRequest(anyString(), anyString());
      verify(spy, never()).sendPostRequest(anyString(), anyString());
    }

    @Test
    void testSaveOrUpdateNote_WhenNotExists() throws Exception {
      DatabaseService spy = spy(new DatabaseService());
      doReturn("[]").when(spy).sendGetRequest(anyString());
      doReturn("{}").when(spy).sendPostRequest(anyString(), anyString());

      spy.saveOrUpdateNote(25, "C:\\note.txt", "new text");

      verify(spy, times(1)).sendPostRequest(anyString(), anyString());
      verify(spy, never()).sendPatchRequest(anyString(), anyString());
    }
  }
}