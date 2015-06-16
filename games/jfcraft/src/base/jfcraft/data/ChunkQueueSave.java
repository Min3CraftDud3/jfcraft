package jfcraft.data;

/** Not in use
 *
 * @author pquiring
 *
 * Created : Jun 14, 2014
 */

public class ChunkQueueSave {
  private static final int BUFSIZ = 1024 * 4;
  private Chunk[] chunks = new Chunk[BUFSIZ];
  private int tail, head1, head2;

  public ChunkQueueSave() {}

  public void process(World world) {
    if (tail == head1) {
      return;
    }
    try {
      int cnt = 0;
      int pos = tail;
      while (pos != head1) {
        Chunk chunk = chunks[pos];
        world.chunks.saveChunk(chunk);
        pos++;
        if (pos == BUFSIZ) pos = 0;
        tail = pos;
        cnt++;
      }
//Static.log("saved chunks:" + cnt);
    } catch (Exception e) {
      Static.log(e);
    }
  }

  public void add(Chunk chunk) {
    //scan head1->head2
    int pos = head1;
    while (pos != head2) {
      if (chunks[pos] == chunk) {
        return;
      }
      pos++;
      if (pos == BUFSIZ) pos = 0;
    }
    //add to queue
    chunks[pos] = chunk;
    pos++;
    if (pos == BUFSIZ) pos = 0;
    if (pos == tail) {
      Static.log("ERROR:Chunk processing queue overflow!!!");
      return;
    }
    head2 = pos;
  }

  public void signal() {
    head1 = head2;
  }
}
