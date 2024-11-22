import java.lang.reflect.Array;
import java.util.ArrayList;

public class User {
    private String userId;
    private HashMap followerMap = new HashMap<>(10);
    public HashMap<String, Boolean> seenPosts = new HashMap<>(10);
    public HashMap<String, Boolean> createdPosts = new HashMap<>(10);
    public myBinaryHeap feed = new myBinaryHeap(10);


    public User(String userId) {
        this.userId = userId;
    }
    public boolean followedBy(String userId,User user){
        if (followerMap.contains(userId)) return false;
        followerMap.insert(userId,user);
        return true;
    }
    public String scrolling(int num, Post post){
        seenPosts.insert(post.getPostId(),true);
        if (num == 1){
            return userId + " saw " + post.getPostId() + " while scrolling and clicked the like button.";
        } else{
            return userId + " saw " + post.getPostId() + " while scrolling.";
        }
    }
    public boolean addFeed(Post post){
        if (createdPosts.contains(post.getPostId()) || seenPosts.contains(post.getPostId())){
            return false;
        }
        feed.basicAdd(post);
        return true;
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
