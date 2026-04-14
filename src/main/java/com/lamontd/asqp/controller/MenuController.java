package com.lamontd.asqp.controller;

import com.lamontd.asqp.index.FlightDataIndex;
import com.lamontd.asqp.view.*;

import java.time.LocalDate;
import java.util.Scanner;

/**
 * Controller for the interactive menu
 */
public class MenuController {

    private final FlightDataIndex originalIndex;
    private FlightDataIndex activeIndex;
    private LocalDate filterStartDate;
    private LocalDate filterEndDate;

    public MenuController(FlightDataIndex index) {
        this.originalIndex = index;
        this.activeIndex = index;
        this.filterStartDate = null;
        this.filterEndDate = null;
    }

    /**
     * Runs the interactive menu loop
     */
    public void run() {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            displayMenu();

            String input = scanner.nextLine().trim();

            switch (input) {
                case "1":
                    new DataOverviewView().render(activeIndex, scanner);
                    break;
                case "2":
                    new CarrierView().render(activeIndex, scanner);
                    break;
                case "3":
                    new AirportView().render(activeIndex, scanner);
                    break;
                case "4":
                    new AirplaneView().render(activeIndex, scanner);
                    break;
                case "5":
                    new FlightView().render(activeIndex, scanner);
                    break;
                case "6":
                    handleDateFilter(scanner);
                    break;
                case "7":
                    running = false;
                    System.out.println("\nThank you for using ASQP Flight Data Analysis!");
                    break;
                default:
                    System.out.println("\nInvalid option. Please select 1-7.");
            }
        }
        scanner.close();
    }

    /**
     * Displays the main menu
     */
    private void displayMenu() {
        System.out.println("\n" + "=".repeat(50));
        if (filterStartDate != null || filterEndDate != null) {
            System.out.println("ASQP Flight Data Analysis Menu [FILTERED]");
        } else {
            System.out.println("ASQP Flight Data Analysis Menu");
        }
        System.out.println("=".repeat(50));

        // Show filter status if active
        if (filterStartDate != null || filterEndDate != null) {
            System.out.print("Active Filter: ");
            if (filterStartDate != null && filterEndDate != null) {
                System.out.printf("%s to %s%n", filterStartDate, filterEndDate);
            } else if (filterStartDate != null) {
                System.out.printf("From %s onwards%n", filterStartDate);
            } else {
                System.out.printf("Up to %s%n", filterEndDate);
            }
            System.out.printf("Showing %d of %d flights%n",
                activeIndex.totalFlights, originalIndex.totalFlights);
            System.out.println("-".repeat(50));
        }

        System.out.println("1. Data Overview");
        System.out.println("2. Carrier View");
        System.out.println("3. Airport View");
        System.out.println("4. Airplane View");
        System.out.println("5. Flight View");
        System.out.println("6. Filter by Date Range");
        System.out.println("7. Exit");
        System.out.println("=".repeat(50));
        System.out.print("Select an option (1-7): ");
    }

    /**
     * Handles date filtering
     */
    private void handleDateFilter(Scanner scanner) {
        DateFilterView.DateFilterResult filterResult = new DateFilterView().render(
                originalIndex, filterStartDate, filterEndDate, scanner);
        if (filterResult != null) {
            activeIndex = filterResult.index;
            filterStartDate = filterResult.startDate;
            filterEndDate = filterResult.endDate;
        }
    }
}
