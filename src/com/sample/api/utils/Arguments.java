package com.sample.api.utils;

public class Arguments {
    int port = 8500;
    public int getPort() { return port; }

    static public Arguments parseArguments(String[] args) {
        Arguments result = new Arguments();
        for (String arg : args) {
            if (arg.equals("-h") || arg.equals("-?") || arg.equals("--help")) {
                printHelp();
                return null;
            }
            if (arg.startsWith("--port=")) {
                try {
                    result.port = Integer.parseInt(arg.substring("--port=".length()));
                } catch (NumberFormatException ex) {
                    System.out.println("Invalid switch: " + arg);
                    printHelp();
                    return null;
                }
            }
        }
        return result;
    }

    static void printHelp() {
        System.out.println("Transaction API switches:");
        System.out.println("  --port=<port>");
    }

}
