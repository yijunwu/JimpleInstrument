class TestInvoke {
  private static int calls=0;
  public static void main(String[] args) {
 
    for (int i=1; i<2; i+=1) {
//      System.out.println(goo(null,3));
      System.out.println(goo2(i,3));
    } 
//    goo(12,3);
  }

  private static int foo(int a) {
	  return a+1;
  }
  @checker
  private static int goo2(int a, int b) {
	  int c =33;
	  while (a != b) {
		  a = foo(a);
		    if (a > b) {
		      a = a - b;
		    }
		    else {
		    	c=c-a;
		      b = b - a;		      
		    }
		  }
	  return c;
  }
//  @checker
  private static int goo(SLL sll, int x){
//	  int c =33;
//	  while (a != b) {
//		  a = foo(a);
//		    if (a > b) {
//		      a = a - b;
//		    }
//		    else {
//		    	c=c-a;
//		      b = b - a;		      
//		    }
//		  }
//	  return c;
	  SLL n1 = new SLL();
	  SLL n2 = new SLL();
	  SLL n3 = new SLL();
	  SLL n4 = new SLL();

	  n1.d=13;
	  n2.d=12;
	  n3.d=11;
	  n4.d=10;

	  n1.l=n2;
	  n2.l=n3;
	  n3.l=n4;
	  
	  SLL tmp=n1;
	  while(tmp!=null)
	  {
		  if(tmp.d>x)
			  tmp=tmp.l;
		  else if(tmp.d<x)
			  tmp=tmp.r;
		  else if(tmp.d==x)
			  return tmp.d;
	  }
	  return -1;
  }

	  //return 1;
//  }
}

class SLL{
int d=0;
SLL l=null;
SLL r=null;

}
/*
class TestInvoke {
private static int calls=0;
public static void main(String[] args) {

  SLL head = new SLL();
  head.d=50;
  SLL tmp = head;
for(int i=0;i<10;i++)
{
	SLL t = new SLL();
	t.d=i;
	tmp.n=t;
	tmp=tmp.n;
}
while(head.n!=null)
{
	System.out.println(head.d);
	head=head.n;
}
}

@checker
private static void fill(SLL head,int val){
  SLL t;
  t = head;
  while (t != null) {
    t.d = val;
    t = t.n;
  }
}

}

*/