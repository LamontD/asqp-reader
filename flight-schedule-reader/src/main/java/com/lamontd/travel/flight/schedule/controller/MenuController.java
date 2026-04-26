package com.lamontd.travel.flight.schedule.controller;

import com.lamontd.travel.flight.schedule.index.ScheduleFlightIndex;

import java.util.Scanner;

/**
 * Top-level menu controller for Flight Schedule Reader application.
 */
public class MenuController {

    public void display(ScheduleFlightIndex index, Scanner scanner) {
        boolean running = true;

        while (running) {
            displayMenu();

            String input = scanner.nextLine().trim();

            switch (input) {
                case "1":
                    new RouteSearchController().display(index, scanner);
                    break;
                case "2":
                    new FlightSearchController().display(index, scanner);
                    break;
                case "3":
                    new AirportViewController().display(index, scanner);
                    break;
                case "4":
                    new CarrierViewController().display(index, scanner);
                    break;
                case "5":
                    new TravelPlannerController().display(index, scanner);
                    break;
                case "6":
                    new NetworkAnalysisController(index).display(scanner);
                    break;
                case "7":
                    running = false;
                    break;
                default:
                    System.out.println("\nInvalid option. Please select 1-7.");
            }
        }
    }

    private void displayMenu() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("MAIN MENU");
        System.out.println("=".repeat(80));
        System.out.println("1. Route Search (find flights between airports)");
        System.out.println("2. Flight Search (search by carrier/flight number)");
        System.out.println("3. Airport View (view all flights for an airport)");
        System.out.println("4. Carrier View (view all flights for a carrier)");
        System.out.println("5. Travel Planner (find direct and connecting flights)");
        System.out.println("6. Network Analysis (route network visualization)");
        System.out.println("7. Exit");
        System.out.println("=".repeat(80));
        System.out.print("Select an option (1-7): ");
    }
}
