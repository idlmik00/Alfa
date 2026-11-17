package ru.personal.alfafinance;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
public final class ReportPeriod {
 public final LocalDate start,end;
 public ReportPeriod(LocalDate start,LocalDate end){if(start==null||end==null||end.isBefore(start))throw new IllegalArgumentException("Некорректный период");this.start=start;this.end=end;}
 public static ReportPeriod of(String mode,LocalDate anchor,LocalDate customStart,LocalDate customEnd){
 switch(mode){case "week":LocalDate monday=anchor.minusDays(anchor.getDayOfWeek().getValue()-1);return new ReportPeriod(monday,monday.plusDays(6));case "year":return new ReportPeriod(anchor.withDayOfYear(1),anchor.withMonth(12).withDayOfMonth(31));case "custom":return new ReportPeriod(customStart,customEnd);default:YearMonth m=YearMonth.from(anchor);return new ReportPeriod(m.atDay(1),m.atEndOfMonth());}}
 public boolean contains(LocalDate d){return !d.isBefore(start)&&!d.isAfter(end);}
 public long sum(Ledger l,String type){long n=0;for(Ledger.Entry e:l.entries.values())if(type.equals(e.type)&&contains(e.date))n+=e.amount;return n;}
 public Map<String,Long> categories(Ledger l,String type){Map<String,Long> by=new TreeMap<>();for(Ledger.Entry e:l.entries.values())if(type.equals(e.type)&&contains(e.date))by.merge(e.category,e.amount,Long::sum);return by;}
 public List<Ledger.Entry> entries(Ledger l,String type,String category){List<Ledger.Entry> rows=new ArrayList<>();for(Ledger.Entry e:l.sorted())if(contains(e.date)&&(type==null||type.equals(e.type))&&(category==null||category.equals(e.category)))rows.add(e);return rows;}
 public ReportPeriod previous(String mode){switch(mode){case "week":return new ReportPeriod(start.minusWeeks(1),end.minusWeeks(1));case "month":return of("month",start.minusMonths(1),null,null);case "year":return of("year",start.minusYears(1),null,null);default:long days=ChronoUnit.DAYS.between(start,end)+1;return new ReportPeriod(start.minusDays(days),start.minusDays(1));}}
}
