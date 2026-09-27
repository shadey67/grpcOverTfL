package org.tfl.constants;

import java.time.Duration;

public class Constants {

    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    public static final Duration POLL_INTERVAL = Duration.ofSeconds(30);
    public static final Duration TFL_TIMEOUT = Duration.ofSeconds(5);
    public static final int MAX_CONSECUTIVE_FAILURES = 3;
    public static final String BASE_URL = "https://api.tfl.gov.uk";
}
