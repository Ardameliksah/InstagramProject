import java.lang.reflect.Array;
import java.util.ArrayList;

public class User {
    private String userId;
    private HashMap followerMap = new HashMap<>(10);
    public HashMap<String, Boolean> seenPosts = new HashMap<>(10);
    public HashMap<String, Boolean> createdPosts = new HashMap<>(10);
    public ArrayList<Post> feed = new ArrayList<Post>();


    public User(String userId) {
        this.userId = userId;
    }
    public boolean followedBy(String userId,User user){
        if (followerMap.contains(userId)) return false;
        followerMap.insert(userId,user);
        return true;
    }
    public void createFeed(Post post){
        feed.add(post);
    }
    public boolean unfollowedBy(String userId){
        return followerMap.remove(userId);

    }
    public void createPost(String postId){
        createdPosts.insert(postId,true);

    }
    public ArrayList getCreatedPosts(){
        return createdPosts.keyList();
    }
    public void seePost(String postid){
        seenPosts.insert(postid, true);
    }

    public void seeAllPosts(ArrayList postList){
        for (int i = 0; i < postList.size(); i++){
            seenPosts.insert((String) postList.get(i),true);
        }
    }
    public boolean likePost(Post post){
        if (post.toggle_like(this.userId)){
            seePost(post.getPostId());
            return true;
        }
        return false;
    }
}
