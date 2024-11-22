import java.util.ArrayList;
import java.util.Arrays;

public class myBinaryHeap {
    private static final int DEFAULT_CAPACITY = 15;
    private int currentSize;
    public Post[] array;
    private int capacity;

    public int size() {
        return currentSize;
    }

    public myBinaryHeap() {
        this.capacity = DEFAULT_CAPACITY;
        this.array = new Post[capacity];
        this.currentSize = 0;
    }

    public myBinaryHeap(int capacity) {
        this.capacity = capacity;
        this.array = new Post[capacity];
        this.currentSize = 0;
    }

    public myBinaryHeap(Post[] list) {
        this.capacity = list.length + 1;
        this.array = new Post[capacity];
        this.currentSize = list.length;
        System.arraycopy(list, 0, array, 0, list.length);
        buildHeap();
    }

    public Post findMax() {
        return array[1];
    }

    public Post deleteMax() {
        if (currentSize == 0) return null;
        Post max = findMax();
        array[1] = array[currentSize--];
        percolateDown(1);
        return max;
    }

    private void percolateDown(int hole) {

        int child;

        Post dummy = array[hole];
        if (dummy == null) return;
        for (; hole * 2 < currentSize; hole = child) {
            child = hole * 2;
            if (array[child] == null || array[child + 1] == null) return;
            if (array[child].getLikeCount() < array[child + 1].getLikeCount() && child != currentSize) child++;
            if (array[child] != null && array[child].getLikeCount() > dummy.getLikeCount()) array[hole] = array[child];
            else break;
        }
        array[hole] = dummy;
    }

    public void buildHeap() {
        for (int i = currentSize; i > 0; i--) {
            percolateDown(i);
        }
    }

    public void basicAdd(Post post) {
        if (currentSize == array.length - 1) enlargeArray(array.length * 2 + 1);
        array[currentSize++] = post;
    }

    public void insert(Post post) {

        if (currentSize == array.length - 1) enlargeArray(array.length * 2 + 1);

        int hole = currentSize++;
        for (array[0] = post; post.getLikeCount() > array[hole / 2].getLikeCount(); hole = hole / 2) {
            array[hole] = array[hole / 2];
        }
        array[hole] = post;
    }

    private void enlargeArray(int size) {
        Post[] newArray = new Post[size];
        System.arraycopy(array, 0, newArray, 0, array.length);
        this.array = newArray;
    }
}