
import java.util.*;

public final class ARCCache {

	private final int CACHEBLOCKSIZE = 1048576;

	private final HashMap<String, Node> data;
	private final int maximumSize;

	private final Node headT1;
	private final Node headT2;
	private final Node headB1;
	private final Node headB2;

	private int sizeT1;
	private int sizeT2;
	private int sizeB1;
	private int sizeB2;
	private int p;

	private long totalAccesses;
	private long totalHits;
	private long totalSize;
	private long totalHitsSize;

	public ARCCache(int cacheSize) {
		this.maximumSize = cacheSize;
		this.data = new HashMap<String, Node>();
		this.headT1 = new Node();
		this.headT2 = new Node();
		this.headB1 = new Node();
		this.headB2 = new Node();
	}

	public boolean accessCache(Block block) {
		/*for (int i = 0; i < Math.ceil((double)block.size / (double)CacheSim.CACHE_BLOCK_SIZE); i++) {
			long internalId = ((int) block.blockId) + ((int) i) * 100000000000L;
			int internalSize = CacheSim.CACHE_BLOCK_SIZE;
			if ((i + 1) * CacheSim.CACHE_BLOCK_SIZE > block.size) {
				internalSize = block.size - (i * CacheSim.CACHE_BLOCK_SIZE);
			}
			switch (block.blockOperation) {
			case CacheSim.OPERATION_READ:
			case CacheSim.OPERATION_WRITE:
				customInsert(internalId, internalSize, block);
				break;
			}
		}*/

		boolean wasHitForAll = true;
		int nCacheBlocks = (int)Math.ceil((double)block.size / (double)CACHEBLOCKSIZE);
		for (int i = 0; i < nCacheBlocks; i++) {
			String cacheBlockId = block.blockId + "_" + i;
			if (!customInsert(cacheBlockId, CACHEBLOCKSIZE, block)) {
				wasHitForAll = false;
			}
		}

		if (wasHitForAll) {
			totalHits += nCacheBlocks;
			totalHitsSize += nCacheBlocks * CACHEBLOCKSIZE;
		}

		return wasHitForAll;
	}

	public boolean customInsert(String id, int internalSize, Block block) {
		Node node = data.get(id);
		boolean wasHit = false;
		totalAccesses++;
		totalSize += internalSize;
		if (node == null) {
			onMiss(id, internalSize, block);
		} else if (node.type == QueueType.B1) {
			if (block.blockOperation == CacheSim.OPERATION_READ) {
				wasHit = true;
			}
			onHitB1(node, internalSize, block);
		} else if (node.type == QueueType.B2) {
			if (block.blockOperation == CacheSim.OPERATION_READ) {
				wasHit = true;
			}
			onHitB2(node, internalSize, block);
		} else {
			if (block.blockOperation == CacheSim.OPERATION_READ) {
				wasHit = true;
			}
			onHit(node, internalSize, block);
		}
		return wasHit;
	}

	private void onHit(Node node, int internalSize, Block block) {

		if (node.type == QueueType.T1) {
			sizeT1--;
			sizeT2++;
		}
		node.remove();
		node.type = QueueType.T2;
		node.appendToTail(headT2);
	}

	private void onHitB1(Node node, int internalSize, Block block)  {

		p = Math.min(maximumSize, p + Math.max(sizeB2 / sizeB1, 1));
		evict(node);

		sizeT2++;
		sizeB1--;
		node.remove();
		node.type = QueueType.T2;
		node.appendToTail(headT2);
	}

	private void onHitB2(Node node, int internalSize, Block block) {

		p = Math.max(0, p - Math.max(sizeB1 / sizeB2, 1));
		evict(node);

		sizeT2++;
		sizeB2--;
		node.remove();
		node.type = QueueType.T2;
		node.appendToTail(headT2);
	}

	private void onMiss(String key, int internalSize, Block block) {

		Node node = new Node(key);
		node.type = QueueType.T1;

		int sizeL1 = (sizeT1 + sizeB1);
		int sizeL2 = (sizeT2 + sizeB2);
		if (sizeL1 == maximumSize) {
			if (sizeT1 < maximumSize) {
				Node victim = headB1.next;
				data.remove(victim.key);
				victim.remove();
				sizeB1--;

				evict(node);
			} else {
				Node victim = headT1.next;
				data.remove(victim.key);
				victim.remove();
				sizeT1--;
			}
		} else if ((sizeL1 < maximumSize) && ((sizeL1 + sizeL2) >= maximumSize)) {
			if ((sizeL1 + sizeL2) >= (2 * maximumSize)) {
				Node victim = headB2.next;
				data.remove(victim.key);
				victim.remove();
				sizeB2--;
			}
			evict(node);
		}

		sizeT1++;
		data.put(key, node);
		node.appendToTail(headT1);
	}

	/** Evicts while the map exceeds the maximum capacity. */
	private void evict(Node candidate) {

		if ((sizeT1 >= 1) && (((candidate.type == QueueType.B2) && (sizeT1 == p)) || (sizeT1 > p))) {
			Node victim = headT1.next;
			victim.remove();
			victim.type = QueueType.B1;
			victim.appendToTail(headB1);
			sizeT1--;
			sizeB1++;
		} else {
			Node victim = headT2.next;
			victim.remove();
			victim.type = QueueType.B2;
			victim.appendToTail(headB2);
			sizeT2--;
			sizeB2++;
		}
	}

	private enum QueueType {
		T1, B1, T2, B2;
	}

	static final class Node {
		final String key;

		Node prev;
		Node next;
		QueueType type;

		Node() {
			this.key = null;
			this.prev = this;
			this.next = this;
		}

		Node(String key) {
			this.key = key;
		}

		/** Appends the node to the tail of the list. */
		public void appendToTail(Node head) {
			Node tail = head.prev;
			head.prev = this;
			tail.next = this;
			next = head;
			prev = tail;
		}

		/** Removes the node from the list. */
		public void remove() {
			if(key != null) {
				prev.next = next;
				next.prev = prev;
				prev = next = null;
				type = null;
			}
		}
	}

	public void report() {
		if (totalAccesses == 0){
			System.out.println("No Activity");
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
