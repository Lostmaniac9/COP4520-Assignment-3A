import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.ThreadLocalRandom;

// make a list of 500k numbers and randomly shuffle it and then start pulling from it.
// 

class Node
{
  T item;
  int key;
  AtomicMarkableReference<Node> next;
  boolean marked = false;
}

class Window
{
  public Node pred, curr;
  
  Window(Node myPred, Node myCurr)
  {
    pred = myPred;
    curr = myCurr;
  }
  
  static Window find(Node head, int key)
  {
    Node pred = null, curr = null, succ = null;
    boolean[] marked = {false};
    boolean snip;
    
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

class LockFreeList
{
  Node head;
  
  public boolean add(T item)
  {
    int key = item.hashCode();
    while(true)
    {
      Window window = Window.find(head, key);
      Node pred = window.pred, curr = window.curr;
      if(curr.key == key)
      {
        return false;
      } else
      {
        Node node = new Node(item);
        node.next = new AtomicMarkableReference(curr, false);
        if(pred.next.compareAndSet(curr, node, false, false))
          return true;
      }
    }
  }
  
  public boolean remove(T item)
  {
    int key = item.hashCode();
    boolean snip;
    while(true)
    {
      Window window = Window.find(head, key);
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
  
  public boolean contains(T item)
  {
    int key = item.hashCode();
    Node curr = head;
    while(curr.key < key)
    {
      curr = curr.next.getReference();
    }
    return (curr.key == key && !curr.next.isMarked());
  }
}

