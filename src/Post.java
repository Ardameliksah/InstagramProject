public class Post {
    private String postId;
    private String content;
    private String author;
    private int likeCount;
    private HashMap<String, Boolean> likedBy = new HashMap<>(10);

    public String getPostId() {
        return postId;
    }

    public String getContent() {
        return content;
    }

    public String getAuthor() {
        return author;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public Post(String userId, String postId, String content) {
        this.postId = postId;
        this.content = content;
        this.author = userId;
        this.likeCount = 0;
    }
    public boolean toggle_like(String userId){
        if (likedBy.contains(userId)){
            likedBy.remove(userId);
            this.likeCount -= 1;
            return false;
        } else {
            this.likeCount += 1;
            likedBy.insert(userId, true);
            return true;
        }
    }
}
