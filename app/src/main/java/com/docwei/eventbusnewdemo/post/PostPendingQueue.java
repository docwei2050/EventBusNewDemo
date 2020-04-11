package com.docwei.eventbusnewdemo.post;

public class PostPendingQueue {
    PostPending head;
    PostPending tail;
   public  synchronized  void enqueue(PostPending postPending){
       if(tail!=null){
           tail.next=postPending;
           tail=postPending;
       }else if (head==null){
           head=tail=postPending;
       }
       notifyAll();
   }

   public synchronized  PostPending poll(long time) throws InterruptedException {
       if(head==null){
           wait(time);
       }
        return poll();
   }

    public synchronized  PostPending poll() {
        PostPending postPending=head;
       if(postPending!=null){
           head=postPending.next;
           if(head==null){
               tail=null;
           }
       }
       return postPending;
    }
}
