package ru.personal.alfafinance;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
public final class Ledger {
 public static final String IN="income", OUT="expense", MOVE="transfer";
 public static final long MAX=1000000000000L;
 public static final class Account {
  public final String id,name,type; public final long opening;
  public Account(String id,String name,String type,long opening){
   if(id==null||id.isEmpty()||name==null||name.trim().isEmpty()||name.length()>100||!Arrays.asList("Карта","Наличные","Счёт").contains(type)||opening < -MAX||opening>MAX)throw new IllegalArgumentException("Проверьте название, тип и остаток");
   this.id=id;this.name=name.trim();this.type=type;this.opening=opening;
  }
 }
 public static final class Entry {
  public final String id,type,from,to,category,note; public final LocalDate date; public final long amount;
  public Entry(String id,String type,String from,String to,String category,String note,LocalDate date,long amount){
   if(id==null||id.isEmpty()||date==null||date.isAfter(LocalDate.now())||amount<=0||amount>MAX||!Arrays.asList(IN,OUT,MOVE).contains(type))throw new IllegalArgumentException("Проверьте дату и сумму");
   this.id=id;this.type=type;this.from=from;this.to=to;this.category=category==null?"":category.trim();this.note=note==null?"":note.trim();this.date=date;this.amount=amount;
  }
 }
 public final Map<String,Account> accounts=new LinkedHashMap<>();
 public final Map<String,Entry> entries=new LinkedHashMap<>();
 public final Map<String,Long> limits=new LinkedHashMap<>();
 public final Set<String> imports=new LinkedHashSet<>();
 public final Map<String,String> bankAccounts=new LinkedHashMap<>(), bankRows=new LinkedHashMap<>(), merchantRules=new LinkedHashMap<>(), entryMerchants=new LinkedHashMap<>(), entryMcc=new LinkedHashMap<>(), autoCategories=new LinkedHashMap<>();
 public final Set<String> manualCategories=new HashSet<>();
 public void put(Account a){if(accounts.size()>=200&&!accounts.containsKey(a.id))throw new IllegalArgumentException("Не больше 200 счетов");accounts.put(a.id,a);}
 public boolean used(String id){for(Entry e:entries.values())if(id.equals(e.from)||id.equals(e.to))return true;return false;}
 public void removeAccount(String id){if(used(id))throw new IllegalArgumentException("Сначала перенесите или удалите операции этого счёта");accounts.remove(id);bankAccounts.values().removeIf(id::equals);bankRows.keySet().removeIf(k->k.startsWith(id+"|"));}
 public void put(Entry e){
  if(entries.size()>=100000&&!entries.containsKey(e.id))throw new IllegalArgumentException("Достигнут лимит операций");
  boolean valid=IN.equals(e.type)?e.from==null&&accounts.containsKey(e.to):OUT.equals(e.type)?e.to==null&&accounts.containsKey(e.from):accounts.containsKey(e.from)&&accounts.containsKey(e.to)&&!Objects.equals(e.from,e.to);
  if(!valid)throw new IllegalArgumentException("Выберите разные действующие счета");
  if(!MOVE.equals(e.type)&&e.category.isEmpty())throw new IllegalArgumentException("Укажите категорию");
  entries.put(e.id,e);
 }
 public long balance(String id){Account a=accounts.get(id);if(a==null)throw new IllegalArgumentException("Счёт не найден");long n=a.opening;for(Entry e:entries.values()){if(id.equals(e.from))n=Math.subtractExact(n,e.amount);if(id.equals(e.to))n=Math.addExact(n,e.amount);}return n;}
 public long total(){long n=0;for(String id:accounts.keySet())n=Math.addExact(n,balance(id));return n;}
 public long sum(YearMonth m,String type){long n=0;for(Entry e:entries.values())if(type.equals(e.type)&&YearMonth.from(e.date).equals(m))n=Math.addExact(n,e.amount);return n;}
 public SortedMap<String,Long> categories(YearMonth m){SortedMap<String,Long> result=new TreeMap<>();for(Entry e:entries.values())if(OUT.equals(e.type)&&YearMonth.from(e.date).equals(m))result.merge(e.category,e.amount,Math::addExact);return result;}
 public long limit(YearMonth m,String category){return limits.getOrDefault(m+"|"+category,0L);}
 public void setLimit(YearMonth m,String category,long n){if(n<0||n>MAX)throw new IllegalArgumentException("Некорректный лимит");String key=m+"|"+category;if(n==0)limits.remove(key);else limits.put(key,n);}
 public long daily(YearMonth m,LocalDate today){if(!YearMonth.from(today).equals(m)||limit(m,"")<=0)return 0;return Math.max(0,limit(m,"")-sum(m,OUT))/(m.lengthOfMonth()-today.getDayOfMonth()+1);}
 public List<Entry> sorted(){List<Entry> r=new ArrayList<>(entries.values());Collections.reverse(r);r.sort(Comparator.comparing((Entry e)->e.date).reversed());return r;}
}
