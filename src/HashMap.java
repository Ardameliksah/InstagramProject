import java.util.ArrayList;

public class HashMap<Key, Value> {
    private static final int FIRST_CAPACITY = 1001;
    private static final float LOAD_FACTOR = 0.6f;

    public int size() {
        return size;
    }

    private int capacity;
    private float loadFactor;
    private int size;
    private Node<Key, Value>[] myTable;

    private static class Node<Key, Value>{
        Key key;
        Value value;
        boolean DELETED;
        Node next;

        public Node(Key key, Value value) {
            this.key = key;
            this.value = value;
            this.DELETED = false;
            this.next = null;
        }

        public boolean isDELETED() {
            return DELETED == true;
        }
    }

    public HashMap() {
        this.myTable = new Node[FIRST_CAPACITY];
        this.size = 0;
        this.loadFactor = LOAD_FACTOR;
        this.capacity = FIRST_CAPACITY;
    }
    public HashMap(int capacity){
        this.size = 0;
        this.loadFactor = LOAD_FACTOR;
        this.capacity = capacity;
        this.myTable = new Node[capacity];
    }

    private int hash(Key key){
        return Math.abs(key.hashCode()) % capacity;
    }
    private void rehashing(){
        this.capacity = capacity*2;
        Node<Key, Value>[] newTable = new Node[capacity];
        for (int i = 0; i < myTable.length; i++){
            if (myTable[i] == null) continue;
            int index = hash(myTable[i].key);
            while (newTable[index] != null){
                index = (index + 1) % capacity;
            }
            newTable[index] = new Node<>(myTable[i].key, myTable[i].value);
        }
        this.myTable = newTable;
    }
    public boolean insert(Key key, Value value){
        int index = hash(key);
        while (myTable[index] != null &&  !myTable[index].isDELETED()){
            if (myTable[index].key.equals(key)) return false;
            index = (index + 1) % capacity;
        }
        myTable[index] = new Node<>(key, value);
        size += 1;
        if ((float) size /capacity > loadFactor){
            rehashing();
        }
        return true;
    }

    public boolean contains(Key key){
        int index = hash(key);
        while (myTable[index] != null){
            if (myTable[index].key.equals(key)){
                return true;
            }
            index = (index + 1) % capacity;
        }
        return false;
    }
    public Value find(Key key){
        int index = hash(key);
        while (myTable[index] != null){
            if (myTable[index].key.equals(key)){
                return myTable[index].value;
            }
            index = (index + 1) % capacity;
        }
        return null;
    }

    public boolean remove(Key key) {
        int index = hash(key);
        while (myTable[index] != null) {

            if (myTable[index].key.equals(key)) {
                myTable[index].DELETED = true;
                size -= 1;
                return true;
            }
            index = (index + 1) % capacity;
        }
        return false;
    }
    public ArrayList keyList(){
        ArrayList arrayList = new ArrayList<>();
        for(Node<Key, Value> node: myTable){
            if (node!= null && !node.isDELETED()){
                arrayList.add(node.key);
            }
        }
        return arrayList;
    }
    public myBinaryHeap heapPost(myBinaryHeap binaryHeap){
        for(Node<Key, Value> node: myTable){
            if (node!= null && !node.isDELETED()){
                binaryHeap.insert((Post) node.value);
            }
        }
        return binaryHeap;
    }
}
