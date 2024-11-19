import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {

        Manager manager = new Manager();
        try {
            PrintStream outstream = new PrintStream("my_outputs/output.txt");
            Scanner scanner = new Scanner(new File("input/type3_large.txt"));
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
                        String cre_userId = parts[1];
                        String cre_postId = parts[2];
                        String cre_content = parts[3];
                        outstream.println(manager.create_post(cre_userId,cre_postId,cre_content));
                        break;
                    case "see_post":
                        String see_userId = parts[1];
                        String see_postId = parts[2];
                        outstream.println(manager.see_post(see_userId,see_postId));
                        break;
                    case "see_all_posts_from_user":
                        String see_all_viewerId = parts[1];
                        String see_all_viewedId = parts[2];
                        outstream.println(manager.see_all_posts_from_user(see_all_viewerId,see_all_viewedId));
                        break;
                    case "toggle_like":
                        String like_userId = parts[1];
                        String like_postId = parts[2];
                        outstream.println(manager.toggle_like(like_userId,like_postId));
                        break;
                    case "generate_feed":
                        String gen_userId = parts[1];
                        String gen_num = parts[2];
                        outstream.println(manager.generate_feed(gen_userId, Integer.parseInt(gen_num)));
                        break;
                    case "scroll_through_feed":
                        String scr_userId = parts[1];
                        int scr_num = Integer.parseInt(parts[2]);
                        int[] scr_like = new int[scr_num];
                        for(int i = 0; i < scr_num; i++){
                           scr_like[i] = Integer.parseInt(parts[3+i]);
                        }
                        ArrayList<String> results = manager.scroll_through_feed(scr_userId,scr_num,scr_like);
                        for ( String result: results){
                            outstream.println(result);
                        }
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
