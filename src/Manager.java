public class Manager {
    private HashMap userMap;
    private HashMap postMap;
    public Manager() {
        this.userMap = new HashMap<>();
    }

    public String create_user(String userId){

        int result = userMap.insert(userId,new User(userId));
        if (result == -1) return "Some error occurred in create_user.";
        else return "Created user with Id " + userId + ".";
    }
    public String follow_user(String userId1, String userId2){
        User user2 = (User) userMap.find(userId2);
        User user1 = (User) userMap.find(userId1);
        if (userId1.equals(userId2)) return "Some error occurred in follow_user.";
        if (user1 == null || user2 == null) return "Some error occurred in follow_user.";
        if (user1.followedBy(userId2,user2)) return userId1 + " followed "+ userId2+".";
        else if (!user1.followedBy(userId2,user2)) return "Some error occurred in follow_user.";
        return "Some error occurred in follow_user.";
    }
    public String unfollow_user(String userId1, String userId2){
        User user2 = (User) userMap.find(userId2);
        User user1 = (User) userMap.find(userId1);
        if (userId1.equals(userId2)) return "Some error occurred in unfollow_user.";
        if (user1 == null || user2 == null) return "Some error occurred in unfollow_user.";
        if (user1.unfollowedBy(userId2)) return userId1 + " unfollowed "+ userId2+".";
        else if (!user1.unfollowedBy(userId2)) return "Some error occurred in unfollow_user.";
        return "Some error occurred in unfollow_user.";
    }
    public String create_post(String userId, String postId, String content){
        if (!userMap.contains(userId)) return "Some error occurred in create_post.";
        Post post = new Post(userId,postId,content);
        int result = postMap.insert(postId,post);
        if (result == -1) return "Some error occurred in create_post.";
        else return userId+" created a post with Id " + postId+".";
    }
    public String see_post(String userId, String postId){
        if (!userMap.contains(userId) || !postMap.contains(postId)) return "Some error occured in see_post.";


    }
    public String see_all_posts_from_user(String viewerId, String viewedId){
        return "xd";
    }
    public String toggle_like(String userId, String postId){
        return "xd";
    }
    public void generate_feed(String userId, int num){

    }
    public void scroll_through_feed(String userId, int num, int... like ){

    }
    public void sort_posts(String userId){

    }
}
