package com.lamontd.travel.flight.asqp.controller;

import com.lamontd.travel.flight.asqp.index.FlightDataIndex;
import com.lamontd.travel.flight.asqp.view.AirportView;
import com.lamontd.travel.flight.asqp.view.CarrierView;

import java.util.Scanner;

/**
 * Submenu controller for Data View options (Carrier and Airport views)
 */
public class DataViewSubmenu {

    public void display(FlightDataIndex index, Scanner scanner) {
        boolean running = true;

        while (running) {
            displayMenu();

            String input = scanner.nextLine().trim();

            switch (input) {
                case "1":
                    new CarrierView().render(index, scanner);
                    break;
                case "2":
                    new AirportView().render(index, scanner);
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
        System.out.println("DATA VIEW");
        System.out.println("=".repeat(50));
        System.out.println("1. Carrier View");
        System.out.println("2. Airport View");
        System.out.println("3. Return to Main Menu");
        System.out.println("=".repeat(50));
        System.out.print("Select an option (1-3): ");
    }
}
