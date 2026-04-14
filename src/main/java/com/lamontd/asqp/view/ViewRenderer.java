package com.lamontd.asqp.view;

import java.util.Scanner;

import com.lamontd.asqp.model.index.FlightDataIndex;

/**
 * Base interface for view renderers
 */
public interface ViewRenderer {
    /**
     * Renders the view
     * @param index Flight data index
     * @param scanner Scanner for user input
     */
    void render(FlightDataIndex index, Scanner scanner);
}
