import java.util.ArrayList;

public class User {
    private String userId;
    private HashMap followerMap = new HashMap(10);
    private HashMap posts = new HashMap<>(10);


    public User(String userId) {
        this.userId = userId;
    }
    public boolean followedBy(String userId,User user){
        if (followerMap.contains(userId)) return false;
        followerMap.insert(userId,user);
        return true;
    }
    public boolean unfollowedBy(String userId){
        return followerMap.remove(userId);

    }

}
