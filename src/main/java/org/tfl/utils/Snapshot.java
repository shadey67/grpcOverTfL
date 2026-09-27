package org.tfl.utils;

import org.tfl.tflOverGRPC.ArrivalsUpdate;

public record Snapshot(long version, ArrivalsUpdate update) {
}
