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

package com.io7m.idstore_gui.admin.internal.admins;

import com.io7m.idstore_gui.admin.internal.IdAGStringsType;
import javafx.util.StringConverter;

import java.util.Objects;

/**
 * A search kind string converter.
 */

public final class IdAGAdminSearchKindStringConverter
  extends StringConverter<IdAGAdminSearchKind>
{
  private final IdAGStringsType strings;

  /**
   * A search kind string converter.
   *
   * @param inStrings The string resources
   */

  public IdAGAdminSearchKindStringConverter(
    final IdAGStringsType inStrings)
  {
    this.strings = Objects.requireNonNull(inStrings, "strings");
  }

  @Override
  public String toString(
    final IdAGAdminSearchKind kind)
  {
    return switch (kind) {
      case BY_DETAILS -> this.strings.format("users.search.searchDetails");
      case BY_EMAIL -> this.strings.format("users.search.searchEmail");
    };
  }

  @Override
  public IdAGAdminSearchKind fromString(
    final String s)
  {
    return null;
  }
}
