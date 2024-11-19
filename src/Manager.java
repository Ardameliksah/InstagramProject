import java.util.ArrayList;

public class Manager {
    private HashMap<String, User> userMap;
    private HashMap<String, Post> postMap;
    private myBinaryHeap likeHeap;
    public Manager() {
        this.userMap = new HashMap<>();
        this.postMap = new HashMap<>();
    }

    public String create_user(String userId){

        if (!userMap.insert(userId,new User(userId))) return "Some error occurred in create_user.";
        else return "Created user with Id " + userId + ".";
    }
    public String follow_user(String userId1, String userId2){
        User user2 = userMap.find(userId2);
        User user1 = userMap.find(userId1);
        if (userId1.equals(userId2)) return "Some error occurred in follow_user.";
        if (user1 == null || user2 == null) return "Some error occurred in follow_user.";
        if (user1.followedBy(userId2,user2)) return userId1 + " followed "+ userId2+".";
        else if (!user1.followedBy(userId2,user2)) return "Some error occurred in follow_user.";
        return "Some error occurred in follow_user.";
    }
    public String unfollow_user(String userId1, String userId2){
        User user2 =  userMap.find(userId2);
        User user1 =  userMap.find(userId1);
        if (userId1.equals(userId2)) return "Some error occurred in unfollow_user.";
        if (user1 == null || user2 == null) return "Some error occurred in unfollow_user.";
        if (user1.unfollowedBy(userId2)) return userId1 + " unfollowed "+ userId2+".";
        else if (!user1.unfollowedBy(userId2)) return "Some error occurred in unfollow_user.";
        return "Some error occurred in unfollow_user.";
    }
    public String create_post(String userId, String postId, String content){
        Post post = new Post(userId,postId,content);
        User user = userMap.find(userId);
        if (user == null) return "Some error occurred in create_post.";
        if (!postMap.insert(postId, post)) return "Some error occurred in create_post.";
        user.createPost(postId);
        return userId+" created a post with Id " + postId + ".";
    }
    public String see_post(String userId, String postId){
        User user = userMap.find(userId);
        if (user==null || !postMap.contains(postId)) return "Some error occurred in see_post.";
        user.seenPosts.insert(postId,true);
        return userId + " saw " + postId+ ".";

    }
    public String see_all_posts_from_user(String viewerId, String viewedId){
        User user2 = userMap.find(viewedId);
        User user1 = userMap.find(viewerId);
        if (user1 == null || user2 == null) return "Some error occurred in see_all_posts_from_user.";
        user1.seeAllPosts(user2.getCreatedPosts());
        return viewerId + " saw all posts of " + viewedId + ".";
    }
    public String toggle_like(String userId, String postId){
        User user = userMap.find(userId);
        Post post = postMap.find(postId);
        if (user == null || post == null) return "Some error occurred in toggle_like.";
        if(user.likePost(post)){
            return userId + " liked " + postId +".";
        }
        return userId + " unliked " + postId +".";

    }
    public String generate_feed(String userId, int num){
        likeHeap = new myBinaryHeap(postMap.size());
        likeHeap = postMap.heapPost(likeHeap);
        User user = userMap.find(userId);
        if (user == null) return "Some error occurred in generate_feed.";
        while (num > 0){
            Post post = likeHeap.deleteMax();
            if (post == null) return "No more posts available for "+userId+".";
            if (!user.seenPosts.contains(post.getPostId())) {
                user.createFeed(post);
                num -= 1;
            }
        }
        return null;
    }
    public ArrayList<String> scroll_through_feed(String userId, int num, int... like ){
        User user = userMap.find(userId);
        ArrayList<String> result = new ArrayList<String>();
        if (user == null){
            result.add("Some error occurred in scroll_through_feed.");
            return result;
        }
        result.add(userId + " is scrolling through feed:");
        int likeIndex = 0;
        int feedSize = user.feed.size();
        while(likeIndex < feedSize && likeIndex < num){
            Post post = user.feed.get(likeIndex);
            if (like[likeIndex] == 0){
                user.seePost(post.getPostId());
                result.add(userId + " saw "+ post.getPostId() + " while scrolling.");
            } else if (like[likeIndex] == 1) {
                user.likePost(post);
                result.add(userId + " saw " + post.getPostId() + " while scrolling and clicked the like button.");
            }
            likeIndex+=1;
        }
        if (likeIndex < num) result.add("No more posts in feed.");
        return result;

    }
    public void sort_posts(String userId){

    }
}
