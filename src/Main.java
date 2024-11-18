import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {

        Manager manager = new Manager();
        try {
            PrintStream outstream = new PrintStream("my_outputs/output.txt");
            Scanner scanner = new Scanner(new File("input/type1_large.txt"));
            long startTime = System.currentTimeMillis();
            while (scanner.hasNextLine()) {
                String input = scanner.nextLine().trim();
                String[] parts = input.split(" ");

                switch (parts[0]) {
                    case "create_user":
                        String userId = parts[1];
                        outstream.println(manager.create_user(userId));
                        break;
                    case "follow_user":
                        String fol_userId1 = parts[1];
                        String fol_userId2 = parts[2];
                        outstream.println(manager.follow_user(fol_userId1,fol_userId2));
                        break;
                    case "unfollow_user":
                        String unf_userId1 = parts[1];
                        String unf_userId2 = parts[2];
                        outstream.println(manager.unfollow_user(unf_userId1,unf_userId2));
                        break;
                    case "create_post":

                        break;
                    case "see_post":

                        break;
                    case "see_all_posts_from_user":

                        break;
                    case "toggle_like":
                        break;
                    case "generate_feed":
                        break;
                    case "scroll_through_feed":
                        break;
                    case "sort_posts":
                        break;

                }
            }
            long endTime = System.currentTimeMillis(); // End time for performance measurement
            long duration = endTime - startTime; // Calculate the duration
            System.out.println("Execution time: " + duration + " milliseconds");

            // Close resources
            scanner.close();
            outstream.close();
        }
        catch (FileNotFoundException e){
                System.out.println("No such file.");
            }


    }
}
