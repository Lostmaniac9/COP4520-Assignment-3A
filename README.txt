
# COP4520-Assignment-3A
To run, use "javac Servant.java" followed by "java Servant".

My solution uses the lock-free list detailed at the end of chapter nine in the textbook. Most of the implementation has to do with that.
Being a lock-free list from the textbook, the efficiency and correctness of the structure can be pretty well determined already, especially since my implmenetation is nearly identical to that in the book, as is to be expected.
Also note, at the end of the program it prints the number of presents handled by the servants, followed by the number of thank you cards written.
This is done to easily showcase the accuracy of the count, barring a minor issue detailed futher down.

My solution to the concurrency problem with there being too few thank you cards was pretty simple, I imposed a numerical order on the presents being put in the queue.
I pull presents from an array (represented as int values) and I put those ints into nodes in the lock-free queue.
Originally, I had that array of presents scrambled to represent the random pile of presents that the servants had to work through in the original problem.
What I believe was causing the thank you cards to come up short every time was the alternating adding and removing random present ids.
Since the present ids were random, they were being added into random spots in the queue.
Also, since the servants alternated adding and removing presents, I believe this caused an issue where so much chaos in the queue caused the CAS operations to constantly fail, creating the awkward discrepancy between the presents counted and thank you cards written.

Therefore, I chose to unscramble the pile of presents and add them into the queue in a predictable numerical order.
That way, every present that gets added will be added to the end of the queue of presents, thereby avoiding a lot of the chaos causing the CAS operations to fail.
This single change caused the number of thank you cards written to equal the amount of presents nearly every time.

I say nearly every time, because unfortunately I must admit that there is a bug in my solution that causes the number of thank you cards to irregularly (in my testing) to be two cards short of the number of presents.
I am pained to write that I have no idea what could be causing this, perhaps a CAS operation can still fail disastrously even when the presents are placed in the queue in order? It is unknown to me.

EDIT: I am adding this extra bit here because, after a bit of further testing, I have found that my program has an absurd bug that I cannot explain.
At lines 201 and 208 there are a pair of print statements that I used for debugging the program.
When these print statements are removed from the program it runs nearly instantly, but for some reason still has the issue of far few thank you cards written then there should be.
However, if those print statements are left active in the code, it runs signficantly slower, but it also gets an accurate count of the thank you cards, barring the minor issue above.
I have no clue why this is and unfortunately no time left to explore this further.
I assume having to run the program in what is effectively "debug mode" will hurt my points in the end, though it DOES come to the correct answer in "debug mode", so maybe that will earn me some credit in the end.
I left the print statements disabled in the final version of the program, feel free to uncomment them and see for yourself, I would love some sort of explanation for this because I am utterly mystified.

Textbook was referenced in chapter 9 for an implementation of a lock-free FIFO queue.
Github link to functional example of textbook code: https://github.com/pramalhe/ConcurrencyFreaks/blob/master/Java/com/concurrencyfreaks/list/HarrisAMRLinkedList.java
Some minor portions were taken from this link, the biggest one being a check at line 137, noted in the comments.
