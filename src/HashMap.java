public class HashMap<Key, Value> {
    private static final int FIRST_CAPACITY = 1001;
    private static final float LOAD_FACTOR = 0.6f;

    private int capacity;
    private float loadFactor;
    private int size;
    private Node<Key, Value>[] myTable;

    private static class Node<Key, Value>{
        Key key;
        Value value;
        boolean DELETED;

        public Node(Key key, Value value) {
            this.key = key;
            this.value = value;
            this.DELETED = false;
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

    private int hash(Key key){
        return key.hashCode() % capacity;
    }

    private void insert(Key key, Value value){
        int index = hash(key);
        while (myTable[index] != null ||  myTable[index].isDELETED() !=)
    }
}
