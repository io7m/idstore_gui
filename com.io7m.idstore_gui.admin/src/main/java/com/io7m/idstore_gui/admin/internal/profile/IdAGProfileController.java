/*
 * Copyright © 2023 Mark Raynsford <code@io7m.com> https://www.io7m.com
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


package com.io7m.idstore_gui.admin.internal.profile;

import com.io7m.hibiscus.api.HBStateType;
import com.io7m.hibiscus.api.HBStateType.HBStateDisconnected;
import com.io7m.hibiscus.api.HBStateType.HBStateExecutingLoginSucceeded;
import com.io7m.idstore_gui.admin.IdAGConfiguration;
import com.io7m.idstore_gui.admin.internal.IdAGStringsType;
import com.io7m.idstore_gui.admin.internal.client.IdAGClientService;
import com.io7m.idstore_gui.admin.internal.users.IdAGUserEmailAddControllers;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdAdminPermission;
import com.io7m.idstore.model.IdEmail;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * The profile screen controller.
 */

public final class IdAGProfileController
  implements Initializable
{
  private final RPServiceDirectoryType services;
  private final IdAGConfiguration configuration;
  private final IdAGClientService client;
  private final ObservableList<IdAdminPermission> permissions;
  private final ObservableList<IdEmail> emails;
  private final IdAGStringsType strings;

  @FXML private Parent container;
  @FXML private ListView<IdAdminPermission> permissionsList;
  @FXML private TextField adminIdField;
  @FXML private TextField adminIdNameField;
  @FXML private TextField adminRealNameField;
  @FXML private ListView<IdEmail> emailList;

  private volatile IdAdmin admin;

  /**
   * The profile screen controller.
   *
   * @param inConfiguration The configuration
   * @param inServices      The service directory
   */

  IdAGProfileController(
    final RPServiceDirectoryType inServices,
    final IdAGConfiguration inConfiguration)
  {
    this.services =
      Objects.requireNonNull(inServices, "services");
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.client =
      inServices.requireService(IdAGClientService.class);
    this.strings =
      inServices.requireService(IdAGStringsType.class);
    this.permissions =
      FXCollections.observableArrayList();
    this.emails =
      FXCollections.observableArrayList();
  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    this.container.setDisable(true);
    this.permissionsList.setItems(this.permissions);
    this.emailList.setItems(this.emails);

    this.client.status()
      .addListener((obs, statusOld, statusNew) -> {
        this.onClientStatusChanged(statusNew);
      });
  }

  private void onAdminReceived(
    final IdAdmin newAdmin)
  {
    this.admin = newAdmin;

    Platform.runLater(() -> {
      this.container.setDisable(false);
      this.adminIdField.setText(newAdmin.id().toString());
      this.adminIdNameField.setText(newAdmin.idName().value());
      this.adminRealNameField.setText(newAdmin.realName().value());
      this.permissions.setAll(
        newAdmin.permissions()
          .impliedPermissions()
          .stream()
          .sorted()
          .toList()
      );
      this.emails.setAll(newAdmin.emails().toList());
    });
  }

  private void onClientStatusChanged(
    final HBStateType<?, ?, ?, ?> statusNew)
  {
    if (statusNew instanceof HBStateExecutingLoginSucceeded) {
      this.client.adminSelf().thenAcceptAsync(this::onAdminReceived);
      return;
    }

    if (statusNew instanceof HBStateDisconnected) {
      this.admin = null;
      this.container.setDisable(true);
    }
  }

  @FXML
  private void onEmailAddSelected()
    throws IOException
  {
    final var controller =
      new IdAGUserEmailAddControllers(
        this.services,
        this.configuration,
        this.strings
      ).openDialogAndWait(null);

    final var emailOpt = controller.result();
    emailOpt.ifPresent(email -> this.executeEmailAdd(this.admin.id(), email));
  }

  private void executeEmailAdd(
    final UUID id,
    final IdEmail email)
  {
    final var future = this.client.adminEmailAdd(id, email);
    future.whenComplete((received, exception) -> {
      if (received != null) {
        this.onAdminReceived(received);
      }
    });
  }

  @FXML
  private void onEmailDeleteSelected()
  {
    this.executeEmailRemove(
      this.admin.id(),
      this.emailList.getSelectionModel().getSelectedItem()
    );
  }

  private void executeEmailRemove(
    final UUID id,
    final IdEmail email)
  {
    final var future = this.client.adminEmailRemove(id, email);
    future.whenComplete((received, exception) -> {
      if (received != null) {
        this.onAdminReceived(received);
      }
    });
  }
}
