public class Manager {
    public String create_user(String userId){
        return "Created user with Id "+userId +".";
    }
    public String follow_user(String userId1, String userId2){
        return "somethign";
    }
    public String create_post(String userId, String postId, String content){
        return "Something";
    }
    public String see_post(String userId, String postId){
        return "user1 saw post1";
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
