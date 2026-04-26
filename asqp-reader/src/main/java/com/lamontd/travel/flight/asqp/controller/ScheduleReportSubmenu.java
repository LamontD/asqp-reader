package com.lamontd.travel.flight.asqp.controller;

import com.lamontd.travel.flight.asqp.index.FlightDataIndex;
import com.lamontd.travel.flight.asqp.view.FlightScheduleView;
import com.lamontd.travel.flight.asqp.view.RouteScheduleView;
import com.lamontd.travel.flight.asqp.view.CurrentRouteScheduleView;

import java.util.Scanner;

/**
 * Submenu controller for Schedule Report options
 */
public class ScheduleReportSubmenu {

    public void display(FlightDataIndex index, Scanner scanner) {
        boolean running = true;

        while (running) {
            displayMenu();

            String input = scanner.nextLine().trim();

            switch (input) {
                case "1":
                    new FlightScheduleView().render(index, scanner);
                    break;
                case "2":
                    new RouteScheduleView().render(index, scanner);
                    break;
                case "3":
                    new CurrentRouteScheduleView().render(index, scanner);
                    break;
                case "4":
                    running = false;
                    break;
                default:
                    System.out.println("\nInvalid option. Please select 1-4.");
            }
        }
    }

    private void displayMenu() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("SCHEDULE REPORT");
        System.out.println("=".repeat(50));
        System.out.println("1. Flight Schedule Analysis (by flight number)");
        System.out.println("2. Route Schedule Lookup (between two airports)");
        System.out.println("3. Current Route Schedule (active flights only)");
        System.out.println("4. Return to Main Menu");
        System.out.println("=".repeat(50));
        System.out.print("Select an option (1-4): ");
    }
}
