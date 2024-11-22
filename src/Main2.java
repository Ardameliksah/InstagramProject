public class Main2 {
    public static void main(String[] args) {
        myBinaryHeap heap = new myBinaryHeap(5);
        Post post = new Post("1","1","annen");
        heap.insert(post);
        Post post1 = new Post("2","2","annen");
        heap.insert(post1);
        Post post2 = new Post("3","3","annen");
        heap.insert(post2);
        Post post3 = new Post("4","4","annen");
        heap.insert(post3);
        Post post4 = new Post("5","5","annen");
        heap.insert(post4);
        Post post5 = new Post("6","6","annen");
        heap.insert(post5);
        Post post6 = new Post("7","7","annen");
        heap.insert(post6);
        Post post7 = new Post("8","8","annen");
        heap.insert(post7);
        Post post8 = new Post("9","9","annen");
        heap.insert(post8);
        Post post9 = new Post("10","10","annen");
        heap.insert(post9);
        Post post10 = new Post("11","11","annen");
        heap.insert(post10);
        System.out.println(heap.deleteMax().getPostId());
        System.out.println(heap.deleteMax().getPostId());

/*
        int num = 11;
        while (num > 0){
            Post xd = heap.deleteMax();
            if (xd == null) break;
            System.out.println(xd.getPostId());
            num--;
        }

 */

    }
}
