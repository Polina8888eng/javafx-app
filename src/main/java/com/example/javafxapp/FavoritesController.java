package com.example.javafxapp;

import com.example.model.Favorite;
import com.example.service.DatabaseService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.util.List;

public class FavoritesController {

  @FXML
  private ListView<Favorite> listView;
  private ObservableList<Favorite> favorites = FXCollections.observableArrayList();
  private DatabaseService db = new DatabaseService();
  private EditorController parentController;
  private int userId;

  public void setParentController(EditorController parent) {
    this.parentController = parent;
    this.userId = parentController.getCurrentUser().getId();
    loadFavorites();
  }

  private void loadFavorites() {
    try {
      List<Favorite> favs = db.getFavorites(userId);
      favorites.setAll(favs);
      listView.setItems(favorites);
      listView.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {

        @Override
        protected void updateItem(Favorite item, boolean empty) {
          super.updateItem(item, empty);
          setText(empty || item == null ? null : item.getTitle() + " (" + item.getFilePath() + ")");
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @FXML
  private void openSelected() {
    Favorite selected = listView.getSelectionModel().getSelectedItem();
    if (selected != null && parentController != null) {
      parentController.openFileFromFavorites(selected.getFilePath());
      closeWindow();
    }
  }

  @FXML
  private void deleteSelected() {
    Favorite selected = listView.getSelectionModel().getSelectedItem();
    if (selected != null) {
      try {
        db.deleteFavorite(selected.getId());
        loadFavorites();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @FXML
  private void closeWindow() {
    ((Stage) listView.getScene().getWindow()).close();
  }
}
