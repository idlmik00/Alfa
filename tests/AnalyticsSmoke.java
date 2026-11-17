package ru.personal.alfafinance;
import java.time.*;
import java.util.*;
public final class AnalyticsSmoke {
 static void require(boolean ok){if(!ok)throw new AssertionError("Analytics invariant");}
 public static void main(String[] args){
  Ledger l=new Ledger();l.put(new Ledger.Account("a","Test","Карта",10000));l.put(new Ledger.Account("b","Cash","Наличные",0));
  LocalDate d=LocalDate.of(2025,3,12);
  l.put(new Ledger.Entry("e",Ledger.OUT,"a",null,"Продукты","Test",d,125));
  l.put(new Ledger.Entry("i",Ledger.IN,null,"a","Зарплата","Test",d,1000));
  l.put(new Ledger.Entry("t",Ledger.MOVE,"a","b","","",d,50));
  l.put(new Ledger.Entry("p",Ledger.OUT,"a",null,"Продукты","Test",LocalDate.of(2025,2,28),250));
  ReportPeriod month=ReportPeriod.of("month",d,null,null),week=ReportPeriod.of("week",d,null,null);
  require(month.sum(l,Ledger.OUT)==125&&month.sum(l,Ledger.IN)==1000&&month.categories(l,Ledger.OUT).get("Продукты")==125);
  require(month.previous("month").sum(l,Ledger.OUT)==250&&week.start.equals(LocalDate.of(2025,3,10)));
  require(ReportPeriod.of("year",d,null,null).sum(l,Ledger.OUT)==375);
  require(new ReportPeriod(d,d).entries(l,Ledger.OUT,"Продукты").size()==1);
  require(MccCatalog.count()==280&&MccCatalog.category("5411").equals("Продукты")&&MccCatalog.category("7995").equals("Азартные игры")&&MccCatalog.category("9999")==null);
  String[] check={"5411","5812","4111","4900","6300","5995","8351","7995","6538","3501"};
  for(String code:check)require(CategoryCatalog.names(false).contains(MccCatalog.category(code)));
  System.out.println("Analytics and MCC checks passed");
 }
}