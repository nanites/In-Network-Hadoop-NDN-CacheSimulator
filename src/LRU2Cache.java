import java.util.HashMap;
import java.util.Map;

public class LRU2Cache<K, V extends Comparable<V>> {

	class MaxHeap<K, V extends Comparable<V>> {
    private Node<K, V>[] heap;
    private int currentSize;
    private long count;

    MaxHeap(int size) {
      count = 0;
      currentSize = 1;
      heap = new Node[size + 1];
    }

    boolean isFull() {
      return currentSize >= heap.length;
    }

    Node<K, V> add(Node<K, V> value) {
      Node<K, V> previous = value;
      if (currentSize >= heap.length) {
        previous = removeMax();
      }
      if (value.getLastSecondTime() != Node.INIT) {
        value.setLastSecondTime(value.getLastTime());
      } else {
        value.setLastSecondTime(count);
      }
      value.setLastTime(count++);
      value.setIndex(currentSize);
      heap[currentSize++] = value;
      siftUp(currentSize - 1);
      return previous;
    }

    Node<K, V> getMax() {
      return heap[0];
    }

    Node<K, V> removeMax() {
      return remove(0);
    }

    Node<K, V> reVisited(int index) {
      Node<K, V> node = heap[index];
      remove(node.getIndex());
      add(node);
      return node;
    }

    Node<K, V> remove(int index) {
      Node<K, V> previous = heap[index];
      heap[index] = heap[--currentSize];
      siftDown(index);
      return previous;
    }

    private void siftDown(int index) {
      int left = 2 * index;
      int right = 2 * index + 1;
      int largest;
      if (left < currentSize && heap[left].compareTo(heap[index]) > 0)
        largest = left;
      else
        largest = index;
      if (right < currentSize && heap[right].compareTo(heap[largest]) > 0)
        largest = right;
      if (largest != index) {
        Node<K, V> temp = heap[index];
        heap[index] = heap[largest];
        heap[largest] = temp;
        heap[index].setIndex(largest);
        heap[largest].setIndex(index);
        siftDown(largest);
      }
    }

    private void siftUp(int index) {
      while (index > 1 && heap[index].compareTo(heap[index / 2]) > 0) {
        Node<K, V> temp = heap[index];
        heap[index] = heap[index / 2];
        heap[index / 2] = temp;
        heap[index].setIndex(index / 2);
        heap[index / 2].setIndex(index);
        index = index / 2;
      }
    }
	}
	class Node<K, V extends Comparable<V>> implements Comparable<Node<K, V>> {
    private long key;
    private int value;
    private int index;
    private long lastTime;
    private long lastSecondTime;
    public static final long INIT = -1;

    Node(long id, int i) {
      this.key = id;
      this.value = i;
      lastTime = INIT;
      lastSecondTime = INIT;
    }

    @Override
    public int compareTo(Node<K, V> o) {
      return (int)(lastSecondTime-o.getLastSecondTime());
    }

    long getKey() {
      return key;
    }

    int getValue() {
      return value;
    }

    int getIndex() {
      return index;
    }

    void setIndex(int index) {
      this.index = index;
    }

    long getLastTime() {
      return lastTime;
    }

    void setLastTime(long lastTime) {
      this.lastTime = lastTime;
    }

    long getLastSecondTime() {
      return lastSecondTime;
    }

    void setLastSecondTime(long lastSecondTime) {
      this.lastSecondTime = lastSecondTime;
    }
	}

	private long totalAccesses;
	private long totalHits;
	private long totalSize;
	private long totalHitsSize;

	public boolean accessCache(long segmentId) {
		totalAccesses++;
    totalSize += CacheSim.CACHE_BLOCK_SIZE;
		return customInsert(segmentId);
	}

  private MaxHeap<K, V> maxHeap;
  private Map<Long, Node<K, V>> map;

  public LRU2Cache(int cacheSize) {
    maxHeap = new MaxHeap<K, V>(cacheSize);
    map = new HashMap<Long, Node<K, V>>((int) ((float) cacheSize / 0.75F + 1.0F));
  }

  public boolean customInsert(long id) {
		boolean wasHit = false;
    Node<K, V> previous;
    if ((previous = map.get(id)) != null) {
      maxHeap.remove(previous.getIndex());
			totalHits++;
			totalHitsSize += internalSize;
			wasHit = true;
    }
    if (maxHeap.isFull()) {
			if (maxHeap.getMax() != null) {
				map.remove(maxHeap.getMax().getKey());
			}
		}
    previous = new Node<K, V>(id, 1);
    map.put(id, previous);
    maxHeap.add(previous);
		return wasHit;
  }

  public void report() {
		if (totalAccesses == 0){
			System.out.println("0,0,0,0,0,0");
		}
		else {
			System.out.print(totalAccesses + "," + totalHits + ",");
			System.out.printf("%.16f", ((double)totalHits)/((double)totalAccesses));
			System.out.print("," + totalSize + "," + totalHitsSize + ",");
			System.out.printf("%.16f", ((double)totalHitsSize)/((double)totalSize));
			System.out.println();
		}
	}
}
