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


package com.io7m.idstore_gui.admin.internal.users;

import com.io7m.idstore_gui.admin.IdAGConfiguration;
import com.io7m.idstore_gui.admin.internal.IdAGStringsType;
import com.io7m.idstore_gui.admin.internal.main.IdAGScreenControllerType;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdValidityException;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * An email creation controller.
 */

public final class IdAGUserEmailAddController
  implements IdAGScreenControllerType
{
  private final IdAGConfiguration configuration;
  private final IdAGStringsType strings;
  private final Stage stage;

  @FXML private Button buttonCreate;
  @FXML private Button buttonCancel;
  @FXML private Label emailFieldBad;
  @FXML private TextField emailField;

  private Optional<IdEmail> result;

  /**
   * An email creation controller.
   *
   * @param inConfiguration The configuration
   * @param inStrings       The string resources
   * @param inStage         The owning stage
   */

  IdAGUserEmailAddController(
    final IdAGConfiguration inConfiguration,
    final IdAGStringsType inStrings,
    final Stage inStage)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "inConfiguration");
    this.strings =
      Objects.requireNonNull(inStrings, "inStrings");
    this.stage =
      Objects.requireNonNull(inStage, "stage");
    this.result =
      Optional.empty();
  }

  /**
   * @return The result of the email creation, if creation wasn't cancelled
   */

  public Optional<IdEmail> result()
  {
    return this.result;
  }

  @FXML
  private void onCancelSelected()
  {
    this.stage.close();
  }

  @FXML
  private void onCreateSelected()
  {
    this.result = Optional.of(new IdEmail(this.emailField.getText()));
    this.stage.close();
  }

  @FXML
  private void onEmailFieldTyped()
  {
    try {
      new IdEmail(this.emailField.getText());
      this.emailFieldBad.setVisible(false);
      this.buttonCreate.setDisable(false);
    } catch (final IdValidityException e) {
      this.emailFieldBad.setVisible(true);
      this.buttonCreate.setDisable(true);
    }
  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    this.emailFieldBad.setVisible(true);
    this.buttonCreate.setDisable(true);
  }
}
