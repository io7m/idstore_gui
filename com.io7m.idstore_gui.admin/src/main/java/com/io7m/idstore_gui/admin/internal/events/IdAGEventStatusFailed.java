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

package com.io7m.idstore_gui.admin.internal.events;

import com.io7m.idstore.error_codes.IdErrorCode;
import com.io7m.jaffirm.core.Preconditions;
import com.io7m.seltzer.api.SStructuredErrorType;
import com.io7m.taskrecorder.core.TRTask;
import com.io7m.taskrecorder.core.TRTaskFailed;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The event indicates that an operation failed.
 *
 * @param task              The failed task
 * @param errorCode         The error code
 * @param attributes        The attributes
 * @param exception         The exception
 * @param remediatingAction The remediating action
 * @param message           The error message
 */

public record IdAGEventStatusFailed(
  TRTask<?> task,
  IdErrorCode errorCode,
  String message,
  Map<String, String> attributes,
  Optional<String> remediatingAction,
  Optional<Throwable> exception)
  implements IdAGEventStatusType, SStructuredErrorType<IdErrorCode>
{
  /**
   * The event indicates that an operation failed.
   *
   * @param task              The failed task
   * @param errorCode         The error code
   * @param attributes        The attributes
   * @param exception         The exception
   * @param remediatingAction The remediating action
   * @param message           The error message
   */

  public IdAGEventStatusFailed
  {
    Objects.requireNonNull(task, "task");
    Objects.requireNonNull(errorCode, "errorCode");
    Objects.requireNonNull(message, "message");
    Objects.requireNonNull(attributes, "attributes");
    Objects.requireNonNull(remediatingAction, "remediatingAction");
    Objects.requireNonNull(exception, "exception");

    Preconditions.checkPreconditionV(
      task.resolution() instanceof TRTaskFailed,
      "Task resolution %s must be 'Failed'",
      task.resolution()
    );
  }
}
