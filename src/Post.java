public class Post {
    private String postId;
    private String content;
    private String author;
    private int likeCount;

    public Post(String userId,String postId, String content) {
        this.postId = postId;
        this.content = content;
        this.author = userId;
        this.likeCount = 0;
    }
}
