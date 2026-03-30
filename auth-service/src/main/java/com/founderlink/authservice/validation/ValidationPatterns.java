package com.founderlink.authservice.validation;

/** Shared regex for request DTO validation (validated before service layer). */
public final class ValidationPatterns {

  private ValidationPatterns() {}

  /** RFC 5322–oriented practical email subset. */
  public static final String EMAIL =
      "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$";

  /** Min 8; at least one lower, upper, digit, special. */
  public static final String PASSWORD =
      "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,128}$";

  /** Letters and spaces only (e.g. "Jane", "Mary Ann"). */
  public static final String PERSON_NAME = "^[A-Za-z]+(?: [A-Za-z]+)*$";
}
