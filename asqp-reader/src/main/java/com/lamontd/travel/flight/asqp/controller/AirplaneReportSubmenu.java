package com.lamontd.travel.flight.asqp.controller;

import com.lamontd.travel.flight.asqp.index.FlightDataIndex;
import com.lamontd.travel.flight.asqp.view.AirplaneOverviewView;
import com.lamontd.travel.flight.asqp.view.AirplaneRoutesView;
import com.lamontd.travel.flight.asqp.view.AirplaneView;

import java.util.Scanner;

/**
 * Submenu controller for Airplane Report options
 */
public class AirplaneReportSubmenu {

    public void display(FlightDataIndex index, Scanner scanner) {
        boolean running = true;

        while (running) {
            displayMenu();

            String input = scanner.nextLine().trim();

            switch (input) {
                case "1":
                    new AirplaneOverviewView().render(index, scanner);
                    break;
                case "2":
                    new AirplaneRoutesView().render(index, scanner);
                    break;
                case "3":
                    new AirplaneView().render(index, scanner);
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
        System.out.println("AIRPLANE REPORT");
        System.out.println("=".repeat(50));
        System.out.println("1. Airplane Overview");
        System.out.println("2. Airplane Routes");
        System.out.println("3. Airplane Flight Log");
        System.out.println("4. Return to Main Menu");
        System.out.println("=".repeat(50));
        System.out.print("Select an option (1-4): ");
    }
}
