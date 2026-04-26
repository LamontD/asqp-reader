package com.lamontd.travel.flight.asqp.controller;

import com.lamontd.travel.flight.asqp.index.FlightDataIndex;
import com.lamontd.travel.flight.asqp.view.FlightView;

import java.util.Scanner;

/**
 * Submenu controller for Flight Report options
 */
public class FlightReportSubmenu {

    public void display(FlightDataIndex index, Scanner scanner) {
        boolean running = true;

        while (running) {
            displayMenu();

            String input = scanner.nextLine().trim();

            switch (input) {
                case "1":
                    new FlightView(FlightView.RenderMode.OVERVIEW).render(index, scanner);
                    break;
                case "2":
                    new FlightView(FlightView.RenderMode.DETAILS).render(index, scanner);
                    break;
                case "3":
                    running = false;
                    break;
                default:
                    System.out.println("\nInvalid option. Please select 1-3.");
            }
        }
    }

    private void displayMenu() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("FLIGHT REPORT");
        System.out.println("=".repeat(50));
        System.out.println("1. Flight Overview");
        System.out.println("2. Flight Details");
        System.out.println("3. Return to Main Menu");
        System.out.println("=".repeat(50));
        System.out.print("Select an option (1-3): ");
    }
}
