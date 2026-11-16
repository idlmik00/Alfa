package ru.personal.alfafinance;
import java.time.*;
public class LedgerSmoke {
 static void eq(long a,long b){if(a!=b)throw new AssertionError(a+" != "+b);}
 static void fail(Runnable r){try{r.run();}catch(IllegalArgumentException e){return;}throw new AssertionError("Expected validation failure");}
 public static void main(String[] args){
  Ledger l=new Ledger();LocalDate d=LocalDate.now();YearMonth m=YearMonth.from(d);
  l.put(new Ledger.Account("a","Карта","Карта",100000));l.put(new Ledger.Account("b","Наличные","Наличные",20000));eq(l.total(),120000);eq(l.sum(m,Ledger.IN),0);
  l.put(new Ledger.Entry("1",Ledger.IN,null,"a","Зарплата","",d,50000));l.put(new Ledger.Entry("2",Ledger.OUT,"a",null,"Еда","",d,12500));l.put(new Ledger.Entry("3",Ledger.MOVE,"a","b","","",d,10000));
  eq(l.balance("a"),127500);eq(l.balance("b"),30000);eq(l.total(),157500);eq(l.sum(m,Ledger.IN),50000);eq(l.sum(m,Ledger.OUT),12500);eq(l.categories(m).get("Еда"),12500);eq(l.sum(m.minusMonths(1),Ledger.OUT),0);
  l.setLimit(m,"",30000);eq(l.daily(m,d),17500/(m.lengthOfMonth()-d.getDayOfMonth()+1));fail(()->l.removeAccount("a"));fail(()->l.put(new Ledger.Entry("x",Ledger.MOVE,"a","a","","",d,1)));fail(()->l.put(new Ledger.Entry("x",Ledger.OUT,"missing",null,"Еда","",d,1)));
  l.put(new Ledger.Entry("2",Ledger.OUT,"a",null,"Еда","",d,25000));eq(l.total(),145000);l.entries.remove("3");eq(l.balance("b"),20000);eq(l.total(),145000);System.out.println("Ledger smoke tests passed");
 }
}
