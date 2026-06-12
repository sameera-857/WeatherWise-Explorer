package com.example;

import java.io.*;
import java.util.Date;

public class DataManager 
{

    private static final String HISTORY_FILE = "search_history.txt";

    public void saveSearchHistory(String activity) 
    {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(HISTORY_FILE, true))) 
        {
            bw.write(activity + "  |  " + new Date());
            bw.newLine();
        } 
        catch (IOException e) 
        {
            System.out.println("  Could not save history: " + e.getMessage());
        }
    }

    public void loadSearchHistory() 
    {
        File file = new File(HISTORY_FILE);
        if (!file.exists()) 
        {
            System.out.println("\n  No search history yet.");
            return;
        }
        System.out.println("\n  Your Search History:");
        System.out.println("  ─────────────────────────────────────────");
        try (BufferedReader br = new BufferedReader(new FileReader(file)))
        {
            String line;
            int count = 1;
            while ((line = br.readLine()) != null)
                System.out.println("  " + count++ + ".  " + line);
            if (count == 1) System.out.println("  (History is empty)");
        }
        catch (IOException e) 
        {
            System.out.println("  Could not read history: " + e.getMessage());
        }
        System.out.println("  ─────────────────────────────────────────");
    }
}