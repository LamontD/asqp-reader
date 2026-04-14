package com.lamontd.asqp.view;

/**
 * Utility methods for view rendering
 */
public class ViewUtils {

    /**
     * Creates a simple text-based bar for histogram visualization
     *
     * @param value Current value
     * @param maxBarLength Maximum length of the bar in characters
     * @param maxValue Maximum value in the dataset
     * @return String representation of the bar
     */
    public static String createBar(long value, int maxBarLength, long maxValue) {
        if (maxValue == 0) return "";
        int barLength = (int) ((value * maxBarLength) / maxValue);
        return "[" + "=".repeat(Math.max(0, barLength)) + "]";
    }
}
