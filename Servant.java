import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;


class LockFreeList
{
  static Node head, tail;
  
  public LockFreeList()
  {
    head = new Node(Integer.MIN_VALUE);
    tail = new Node(Integer.MAX_VALUE);
    head.next.set(tail, false);
  }
  
  public boolean add(int item)
  {
    int key = item;
    while(true)
    {
      Window window = find(head, key);
      Node pred = window.pred, curr = window.curr;
      if(curr.key == key)
      {
        return false;
      } else
      {
        Node node = new Node(item);
        node.next = new AtomicMarkableReference<Node>(curr, false);
        if(pred.next.compareAndSet(curr, node, false, false))
          return true;
      }
    }
    
  }
  
  public boolean remove(int item)
  {
    int key = item;
    boolean snip;
    while(true)
    {
      Window window = find(head, key);
      Node pred = window.pred, curr = window.curr;
      if(curr.key != key)
      {
        return false;
      } else
      {
        Node succ = curr.next.getReference();
        snip = curr.next.compareAndSet(succ, succ, false, true);
        if(!snip)
          continue;
        pred.next.compareAndSet(curr, succ, false, false);
        return true;
      }
    }
  }
  
  public boolean contains(int item)
  {
    int key = item;
    Node curr = head;
    while(curr.key < key)
    {
      curr = curr.next.getReference();
    }
    return (curr.key == key && !curr.next.isMarked());
  }
  
  static class Node
  {
    int key;
    AtomicMarkableReference<Node> next;
    boolean marked = false;
    
    public Node(int item)
    {
      key = item;
      this.next = new AtomicMarkableReference<Node>(null, false);
    }
    
  }
  
  public Window find(Node head, int key)
    {
      Node pred = null, curr = null, succ = null;
      boolean[] marked = {false};
      boolean snip;
      
      if (head.next.getReference() == tail)
      {
          return new Window(head, tail);
      }
      
      retry: while(true)
      {
        pred = head;
        curr = pred.next.getReference();
        while(true)
        {
          succ = curr.next.get(marked);
          while(marked[0])
          {
            snip = pred.next.compareAndSet(curr, succ, false, false);
            if(!snip) continue retry;
            curr = succ;
            succ = curr.next.get(marked);
          }
          if(curr.key >= key)
            return new Window(pred, curr);
          pred = curr;
          curr = succ;
        }
      }
    }
  
  static class Window
  {
    public Node pred, curr;
    
    Window(Node myPred, Node myCurr)
    {
      pred = myPred;
      curr = myCurr;
    }
    
    public Window find(Node head, int key)
    {
      Node pred = null, curr = null, succ = null;
      boolean[] marked = {false};
      boolean snip;
      
      // from GitHub link
      if (head.next.getReference() == tail)
      {
          return new Window(head, tail);
      }
      
      retry: while(true)
      {
        pred = head;
        curr = pred.next.getReference();
        while(true)
        {
          succ = curr.next.get(marked);
          while(marked[0])
          {
            snip = pred.next.compareAndSet(curr, succ, false, false);
            if(!snip) continue retry;
            curr = succ;
            succ = curr.next.get(marked);
          }
          if(curr.key >= key)
            return new Window(pred, curr);
          pred = curr;
          curr = succ;
        }
      }
    }
  }
}


class Servant implements Runnable
{
  static LockFreeList unprocessedPresents;
  private AtomicInteger currentIndex;
  private AtomicInteger thankYouCardsWritten;
  private int present;
  private int[] presentBag;
  
  public Servant(AtomicInteger currentIndex, AtomicInteger thankYouCardsWritten, int[] presentBag)
  {
    this.currentIndex = currentIndex;
    this.thankYouCardsWritten = thankYouCardsWritten;
    this.presentBag = presentBag;
  }
  
  public void run()
  {
    try
    {
      AtomicInteger index = new AtomicInteger();
      // while currentIndex is < 500000
      // grab value from presentBag, increment currentIndex and attach it to the list
      // remove item from list and increment thankYouCardsWritten
      while(currentIndex.get() < 500000)
      {
        // nab the current index and increment the atomic to keep the threads moving
        // this is done through the horrifying power of N E S T E D atomic variables
        // set is done in one step, but this forces another atomic operation to occur on the assignment so that all of this is done in one atomic step
        // from my testing it doesn't really make a difference but I laughed so hard at this that I kept it
        // enjoy
        index.set(currentIndex.getAndIncrement());
        // add whatever value is at this index into the concurrent queue
        unprocessedPresents.add(presentBag[index.intValue()]);
        // System.out.println(currentIndex.get());
        
        // immediately remove it because the threads are supposed to alternate adding and removing and 
        // there's no reason to let a good present sit around
        unprocessedPresents.remove(presentBag[index.intValue()]);
        // update thankYouCardsWritten to account for the removal of the recently added present
        thankYouCardsWritten.set(thankYouCardsWritten.get() + 1);
        // System.out.println(thankYouCardsWritten.get());
      }
    }
    catch (Exception e)
    {
      System.out.println("Exception has occured" + e);
    }
    
    return;
  }
  
  // randomly shuffles int array using Fisher-Yates algorithm
  static void shuffleBag(int[] arr)
  {
    Random rnd = ThreadLocalRandom.current();
    for(int i = arr.length - 1; i > 0; i--)
    {
      int index = rnd.nextInt(i + 1);
      int a = arr[index];
      arr[index] = arr[i];
      arr[i] = a;
    }
  }
  
  public static void main(String args[])
  {
    unprocessedPresents = new LockFreeList();
    // create int array for threads to read through to get values for presents
    int[] presentBag = new int[500000];
    for(int i = 0; i < 500000; i++)
      presentBag[i] = i;
    // now shuffle the presentBag with the above method
    //shuffleBag(presentBag);
    // while arrays are not thread safe when being edited, 
    // this array will only be read by the threads to get their values,
    // they will each increment an AtomicInteger to keep all the threads moving their index forward
    try
    {
      AtomicInteger currentIndex = new AtomicInteger(0);
      AtomicInteger thankYouCardsWritten = new AtomicInteger(0);
      Thread servant1 = new Thread(new Servant(currentIndex, thankYouCardsWritten, presentBag));
      Thread servant2 = new Thread(new Servant(currentIndex, thankYouCardsWritten, presentBag));
      Thread servant3 = new Thread(new Servant(currentIndex, thankYouCardsWritten, presentBag));
      Thread servant4 = new Thread(new Servant(currentIndex, thankYouCardsWritten, presentBag));
      
      servant1.start();
      servant2.start();
      servant3.start();
      servant4.start();
      
      servant1.join();
      servant2.join();
      servant3.join();
      servant4.join();
      System.out.println(currentIndex);
      System.out.println(thankYouCardsWritten);
    }
    catch (Exception e)
    {
      System.out.println("Exception has occured" + e);
    }
  }
}

