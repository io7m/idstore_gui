/*
 * Copyright © 2021 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.idstore_gui.admin.internal.users;

import com.io7m.hibiscus.api.HBStateType;
import com.io7m.hibiscus.api.HBStateType.HBStateDisconnected;
import com.io7m.idstore_gui.admin.IdAGConfiguration;
import com.io7m.idstore_gui.admin.internal.IdAGStringsType;
import com.io7m.idstore_gui.admin.internal.client.IdAGClientService;
import com.io7m.idstore.model.IdBan;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdLogin;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPage;
import com.io7m.idstore.model.IdPassword;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdTimeRange;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.model.IdUserCreate;
import com.io7m.idstore.model.IdUserSummary;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.URL;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;

import static javafx.scene.control.SelectionMode.SINGLE;

/**
 * The user tab controller.
 */

public final class IdAGUsersController implements Initializable
{
  private final IdAGConfiguration configuration;
  private final RPServiceDirectoryType mainServices;
  private final IdAGStringsType strings;
  private final IdAGClientService client;
  private final ObservableList<IdAGUser> users;
  private final ObservableList<IdEmail> userEmails;
  private final ObservableList<IdLogin> userLoginHistory;
  private IdUser user;
  private IdBan ban;
  private IdAGUserSearchKind searchKindAtStart;

  @FXML private Button banBan;
  @FXML private Button banUnban;
  @FXML private Button emailAdd;
  @FXML private Button emailDelete;
  @FXML private Button userCreate;
  @FXML private Button userDelete;
  @FXML private Button userPageNext;
  @FXML private Button userPagePrev;
  @FXML private CheckBox banExpires;
  @FXML private ChoiceBox<IdAGUserSearchKind> searchKind;
  @FXML private DatePicker banExpiryPicker;
  @FXML private Label banLabel;
  @FXML private Label userPageLabel;
  @FXML private TableView<IdAGUser> userTable;
  @FXML private ListView<IdEmail> userEmailList;
  @FXML private Parent userDetailContainer;
  @FXML private Parent userTableContainer;
  @FXML private Tab tabLoginHistory;
  @FXML private TextArea banReason;
  @FXML private TextField userIdField;
  @FXML private TextField userIdNameField;
  @FXML private TextField userRealNameField;
  @FXML private TextField userSearchField;
  @FXML private TextField userPasswordField;
  @FXML private TableView<IdLogin> loginHistoryTable;

  /**
   * The user tab controller.
   *
   * @param inMainServices  The service directory
   * @param inConfiguration The configuration
   */

  IdAGUsersController(
    final RPServiceDirectoryType inMainServices,
    final IdAGConfiguration inConfiguration)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "inConfiguration");
    this.mainServices =
      Objects.requireNonNull(inMainServices, "mainServices");
    this.strings =
      this.mainServices.requireService(IdAGStringsType.class);
    this.client =
      this.mainServices.requireService(IdAGClientService.class);
    this.users =
      FXCollections.observableArrayList();
    this.userEmails =
      FXCollections.observableArrayList();
    this.userLoginHistory =
      FXCollections.observableArrayList();
  }

  private void onUserLoginHistoryReceived(
    final List<IdLogin> received)
  {
    Platform.runLater(() -> {
      if (received == null) {
        this.userLoginHistory.clear();
        return;
      }
      this.userLoginHistory.setAll(received);
    });
  }

  @FXML
  private void onBanSelected()
  {
    final Optional<OffsetDateTime> expiration;
    if (this.banExpires.isSelected()) {
      expiration = Optional.of(
        OffsetDateTime.of(
          this.banExpiryPicker.getValue(),
          LocalTime.MIDNIGHT,
          ZoneOffset.UTC)
      );
    } else {
      expiration = Optional.empty();
    }

    final var future = this.client.userBanCreate(
      new IdBan(
        this.user.id(),
        this.banReason.getText(),
        expiration
      )
    );

    future.whenComplete((received, exception) -> {
      if (received != null) {
        this.onUserBanReceived(Optional.of(received));
      }
    });
  }

  @FXML
  private void onBanUnbanSelected()
  {
    final var future =
      this.client.userBanDelete(this.user.id());

    future.whenComplete((received, exception) -> {
      if (received != null) {
        this.onUserBanReceived(Optional.empty());
      }
    });
  }

  @FXML
  private void onBanExpiresSelected()
  {

  }

  @FXML
  private void onBanReasonChanged()
  {
    this.banBan.setDisable(this.banReason.getText().isBlank());
  }

  private void onClientStatusChanged(
    final HBStateType<?, ?, ?, ?> statusNew)
  {
    if (statusNew instanceof HBStateDisconnected) {
      Platform.runLater(() -> {
        this.users.clear();
        this.user = null;
        this.userDetailsLock();
        this.userTableControlsLock();
      });
    }
  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    this.searchKind.setConverter(
      new IdAGUserSearchKindStringConverter(this.strings));
    this.searchKind.setItems(
      FXCollections.observableArrayList(IdAGUserSearchKind.values())
    );
    this.searchKind.getSelectionModel()
      .selectedItemProperty()
      .addListener((obs, kindOld, kindNew) -> {
        this.onSearchKindSelected(kindNew);
      });
    this.searchKind.getSelectionModel()
      .select(IdAGUserSearchKind.BY_DETAILS);

    this.userDetailsLock();
    this.userTableControlsLock();

    this.initializeUserTable();
    this.initializeLoginHistoryTable();

    this.userEmailList.setItems(this.userEmails);
    this.userEmailList.getSelectionModel()
      .selectedItemProperty()
      .addListener((obs, emailOld, emailNew) -> {
        this.onEmailSelected(emailNew);
      });

    this.client.status()
      .addListener((obs, statusOld, statusNew) -> {
        this.onClientStatusChanged(statusNew);
      });

    this.banExpires.selectedProperty()
      .addListener((obs, vOld, vNew) -> {
        this.banExpiryPicker.setDisable(!this.banExpires.isSelected());
      });
    this.banExpiryPicker.setDisable(true);
  }

  private void initializeUserTable()
  {
    final var tableColumns =
      this.userTable.getColumns();

    final var tableIDColumn =
      (TableColumn<IdAGUser, UUID>) tableColumns.get(0);
    final var tableIdNameColumn =
      (TableColumn<IdAGUser, IdName>) tableColumns.get(1);
    final var tableRealNameColumn =
      (TableColumn<IdAGUser, IdRealName>) tableColumns.get(2);

    tableIDColumn.setSortable(true);
    tableIDColumn.setReorderable(false);
    tableIDColumn.setComparator(UUID::compareTo);
    tableIDColumn.setCellValueFactory(
      param -> {
        return new SimpleObjectProperty<>(
          param.getValue().id()
        );
      });

    tableIdNameColumn.setSortable(true);
    tableIdNameColumn.setReorderable(false);
    tableIdNameColumn.setComparator(IdName::compareTo);
    tableIdNameColumn.setCellValueFactory(
      param -> param.getValue().idName());

    tableRealNameColumn.setSortable(true);
    tableRealNameColumn.setReorderable(false);
    tableRealNameColumn.setComparator(IdRealName::compareTo);
    tableRealNameColumn.setCellValueFactory(
      param -> param.getValue().realName());

    this.userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS);
    this.userTable.getSelectionModel().setSelectionMode(SINGLE);
    this.userTable.setItems(this.users);
    this.userTable.setPlaceholder(new Label());
    this.userTable.getSelectionModel()
      .selectedItemProperty()
      .addListener((observable, oldValue, newValue) -> {
        this.onUserSelected(newValue);
      });
  }

  private void initializeLoginHistoryTable()
  {
    final var tableColumns =
      this.loginHistoryTable.getColumns();

    final var tableTimeColumn =
      (TableColumn<IdLogin, OffsetDateTime>) tableColumns.get(0);
    final var tableHostColumn =
      (TableColumn<IdLogin, String>) tableColumns.get(1);
    final var tableUserAgentColumn =
      (TableColumn<IdLogin, String>) tableColumns.get(2);

    tableTimeColumn.setSortable(true);
    tableTimeColumn.setReorderable(false);
    tableTimeColumn.setComparator(OffsetDateTime::compareTo);
    tableTimeColumn.setCellValueFactory(
      param -> {
        return new SimpleObjectProperty<>(
          param.getValue().time()
        );
      });

    tableHostColumn.setSortable(true);
    tableHostColumn.setReorderable(false);
    tableHostColumn.setComparator(String::compareTo);
    tableHostColumn.setCellValueFactory(
      param -> {
        return new SimpleObjectProperty<>(
          param.getValue().host()
        );
      });

    tableUserAgentColumn.setSortable(true);
    tableUserAgentColumn.setReorderable(false);
    tableUserAgentColumn.setComparator(String::compareTo);
    tableUserAgentColumn.setCellValueFactory(
      param -> {
        return new SimpleObjectProperty<>(
          param.getValue().userAgent()
        );
      });

    this.loginHistoryTable.setPlaceholder(new Label());
    this.loginHistoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS);
    this.loginHistoryTable.getSelectionModel().setSelectionMode(SINGLE);
    this.loginHistoryTable.setItems(this.userLoginHistory);
  }

  private void onSearchKindSelected(
    final IdAGUserSearchKind kind)
  {
    switch (kind) {
      case BY_DETAILS -> {
        this.userSearchField.setPromptText(
          this.strings.format("users.searchPrompt.searchDetails")
        );
      }
      case BY_EMAIL -> {
        this.userSearchField.setPromptText(
          this.strings.format("users.searchPrompt.searchEmail")
        );
      }
    }
  }

  private void onEmailSelected(
    final IdEmail email)
  {
    this.emailDelete.setDisable(true);

    if (email == null) {
      return;
    }

    this.emailDelete.setDisable(this.userEmails.size() <= 1);
  }

  private void userTableControlsLock()
  {
    this.userPageNext.setDisable(true);
    this.userPagePrev.setDisable(true);
    this.userPageLabel.setText("");
  }

  private void userDetailsLock()
  {
    this.userDelete.setDisable(true);
    this.userDetailContainer.setDisable(true);
    this.userIdField.setText("");
    this.userIdNameField.setText("");
    this.userRealNameField.setText("");
    this.userEmails.clear();
    this.emailAdd.setDisable(true);
    this.emailDelete.setDisable(true);
    this.userLoginHistory.clear();
  }

  private void userDetailsUnlock()
  {
    this.userDelete.setDisable(false);
    this.userDetailContainer.setDisable(false);
  }

  private void onUserSelected(
    final IdAGUser userNew)
  {
    this.userDetailsLock();
    if (userNew == null) {
      return;
    }

    {
      final var future = this.client.userGet(userNew.id());
      future.whenComplete((received, exception) -> {
        if (received != null) {
          this.onUserReceived(received);
        }
      });
    }

    {
      final var future = this.client.userBanGet(userNew.id());
      future.whenComplete((received, exception) -> {
        if (received != null) {
          this.onUserBanReceived(received);
        }
      });
    }

    {
      final var future = this.client.userLoginHistory(userNew.id());
      future.whenComplete((received, exception) -> {
        if (received != null) {
          this.onUserLoginHistoryReceived(received);
        }
      });
    }

    this.userDetailsUnlock();
  }

  private void onUserBanReceived(
    final Optional<IdBan> banOpt)
  {
    Platform.runLater(() -> {
      if (banOpt.isEmpty()) {
        this.ban = null;
        this.banReason.setText("");
        this.banLabel.setText(this.strings.format("users.ban.notBanned"));
        this.banExpires.setSelected(false);
        this.banUnban.setDisable(true);
        this.banBan.setDisable(false);
        return;
      }

      this.ban = banOpt.get();
      this.banUnban.setDisable(false);
      this.banBan.setDisable(!this.ban.reason().isBlank());
      this.banReason.setText(this.ban.reason());
      this.banLabel.setText(this.strings.format("users.ban.banned"));

      final var expiresOpt = this.ban.expires();
      this.banExpires.setSelected(expiresOpt.isPresent());
      expiresOpt.ifPresent(time -> {
        this.banExpiryPicker.setValue(time.toLocalDate());
      });
    });
  }

  private void onUserReceived(
    final Optional<IdUser> userOpt)
  {
    Platform.runLater(() -> {
      this.userDetailsLock();

      if (userOpt.isEmpty()) {
        this.user = null;
        return;
      }

      this.user = userOpt.get();
      this.userIdField.setText(this.user.id().toString());
      this.userIdNameField.setText(this.user.idName().toString());
      this.userRealNameField.setText(this.user.realName().toString());
      this.userEmails.setAll(this.user.emails().toList());
      this.userPasswordField.setText("%s".formatted(this.user.password()));
      this.userDetailsUnlock();
      this.emailAdd.setDisable(false);
      this.emailDelete.setDisable(true);
    });
  }

  @FXML
  private void onUserCreateSelected()
    throws IOException
  {
    final var controller =
      new IdAGUserCreateControllers(
        this.mainServices,
        this.configuration,
        this.strings)
        .openDialogAndWait(null);

    final Optional<IdUserCreate> create = controller.result();
    create.ifPresent(this.client::userCreate);
  }

  @FXML
  private void onPasswordChangeSelected()
    throws IOException
  {
    final var controller =
      new IdAGUserPasswordChangeControllers(
        this.mainServices,
        this.configuration,
        this.strings
      ).openDialogAndWait(null);

    final Optional<IdPassword> password = controller.result();
    if (password.isPresent()) {
      final var future =
        this.client.userUpdate(
          this.user.id(),
          Optional.empty(),
          Optional.empty(),
          password
        );

      future.whenComplete((received, exception) -> {
        if (received != null) {
          this.onUserReceived(Optional.of(received));
        }
      });
    }
  }

  @FXML
  private void onUserDeleteSelected()
    throws IOException
  {
    final var controller =
      new IdAGUserDeleteConfirmControllers(
        this.mainServices,
        this.configuration,
        this.strings
      ).openDialogAndWait(this.user);

    if (controller.isDeleteRequested()) {
      final var future = this.client.userDelete(this.user.id());
      future.whenComplete((page, exception) -> {
        // OK
      });
    }
  }

  @FXML
  private void onUserPageNext()
  {
    switch (this.searchKindAtStart) {
      case BY_DETAILS -> {
        final var future = this.client.userSearchNext();
        future.whenComplete((page, exception) -> {
          if (page != null) {
            this.onPageReceived(page);
          }
        });
      }
      case BY_EMAIL -> {
        final var future = this.client.userSearchByEmailNext();
        future.whenComplete((page, exception) -> {
          if (page != null) {
            this.onPageReceived(page);
          }
        });
      }
    }
  }

  @FXML
  private void onUserPagePrevious()
  {
    switch (this.searchKindAtStart) {
      case BY_DETAILS -> {
        final var future = this.client.userSearchPrevious();
        future.whenComplete((page, exception) -> {
          if (page != null) {
            this.onPageReceived(page);
          }
        });
      }
      case BY_EMAIL -> {
        final var future = this.client.userSearchByEmailPrevious();
        future.whenComplete((page, exception) -> {
          if (page != null) {
            this.onPageReceived(page);
          }
        });
      }
    }
  }

  @FXML
  private void onUserSearchSelected()
  {
    this.searchKindAtStart = this.searchKind.getValue();

    switch (this.searchKind.getValue()) {
      case BY_DETAILS -> {
        final var future =
          this.client.userSearchBegin(
            IdTimeRange.largest(),
            IdTimeRange.largest(),
            Optional.of(this.userSearchField.getCharacters().toString())
          );

        future.whenComplete((page, exception) -> {
          if (page != null) {
            this.onPageReceived(page);
          }
        });
      }

      case BY_EMAIL -> {
        final var future =
          this.client.userSearchByEmailBegin(
            IdTimeRange.largest(),
            IdTimeRange.largest(),
            this.userSearchField.getCharacters().toString()
          );

        future.whenComplete((page, exception) -> {
          if (page != null) {
            this.onPageReceived(page);
          }
        });
      }
    }
  }

  private void onPageReceived(
    final IdPage<IdUserSummary> page)
  {
    Platform.runLater(() -> {
      final var pageIndex = page.pageIndex();
      final var pageCount = page.pageCount();

      this.userPagePrev.setDisable(pageIndex == 1);
      this.userPageNext.setDisable(pageIndex == pageCount);

      this.userPageLabel.setText(
        this.strings.format(
          "users.page",
          Integer.valueOf(pageIndex),
          Integer.valueOf(pageCount))
      );

      this.users.setAll(
        page.items()
          .stream()
          .map(IdAGUser::of)
          .toList()
      );
    });
  }

  @FXML
  private void onEmailAddSelected()
    throws IOException
  {
    final var controller =
      new IdAGUserEmailAddControllers(
        this.mainServices,
        this.configuration,
        this.strings
      ).openDialogAndWait(null);

    final var emailOpt = controller.result();
    if (emailOpt.isPresent()) {
      final var future =
        this.client.userEmailAdd(this.user.id(), emailOpt.get());
      future.whenComplete((received, exception) -> {
        if (received != null) {
          this.onUserReceived(Optional.of(received));
        }
      });
    }
  }

  @FXML
  private void onEmailDeleteSelected()
  {
    final var future =
      this.client.userEmailRemove(
        this.user.id(),
        this.userEmailList.getSelectionModel().getSelectedItem()
      );
    future.whenComplete((received, exception) -> {
      if (received != null) {
        this.onUserReceived(Optional.of(received));
      }
    });
  }

  @FXML
  private void onUserUpdateSelected()
  {
    final var future =
      this.client.userUpdate(
        this.user.id(),
        Optional.of(new IdName(this.userIdNameField.getText())),
        Optional.of(new IdRealName(this.userRealNameField.getText())),
        Optional.empty()
      );

    future.whenComplete((received, exception) -> {
      if (received != null) {
        this.onUserReceived(Optional.of(received));
      }
    });
  }
}
