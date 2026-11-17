package ru.personal.alfafinance;
import java.util.*;
import java.util.regex.*;
public final class BankRules {
 static final Pattern MCC=Pattern.compile("\\bMCC\\s*:\\s*(\\d{4})",Pattern.CASE_INSENSITIVE);
 static String lower(String s){return s.toLowerCase(Locale.ROOT).replace('ё','е');}
 static boolean has(String s,String... terms){for(String t:terms)if(s.contains(t))return true;return false;}
 static String extract(String s,String regex){Matcher m=Pattern.compile(regex,Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE).matcher(s);return m.find()?m.group(1).trim():"";}
 public static String mcc(String d){Matcher m=MCC.matcher(d);return m.find()?m.group(1):"";}
 public static String merchant(String d){
  String place=extract(d,"место совершения операции\\s*:\\s*(.*?)(?:,\\s*MCC\\s*:|$)");
  if(!place.isEmpty()){int i=place.lastIndexOf('/');return i<0?place:place.substring(i+1).trim();}
  String pay=extract(d,"Получатель\\s+(.+?)(?:\\s*\\.\\s*Без НДС|$)");if(!pay.isEmpty())return pay;
  pay=extract(d,"(?:в пользу|\\bв)\\s+(.+?)(?:\\s+через Систему|,\\s*\\+?\\d|$)");
  if(!pay.isEmpty()&&has(lower(d),"платеж","платёж"))return pay;
  return "";
 }
 public static boolean transfer(String d){String s=lower(d);return has(s,"категория: перевод","перевод клиенту","перевод между","между своими","между собственными","внесение средств","внесение наличных","снятие наличных","выдача наличных")||mcc(d).equals("6011")||mcc(d).equals("6010");}
 public static String category(BankStatement.Row r){
  String d=lower(r.description+" "+r.category),merchant=lower(merchant(r.description)),m=mcc(r.description);
  if(transfer(r.description))return "Переводы — проверить";
  if(r.amount>0){
   if(has(d,"возврат","refund"))return "Возврат покупки";
   if(has(d,"зарплат"))return "Зарплата";
   if(has(d,"кешбэк","кэшбэк"))return "Кешбэк";
   if(has(d,"преми"))return "Премия";
   if(has(d,"процент","дивиденд"))return "Проценты и дивиденды";
   return "Прочий доход";
  }
  if(has(d,"мобильная связь","телефон, интернет","категория: интернет","мегафон","билайн"))return "Связь и интернет";
  if(has(d,"коммунальн","газпром межрегионгаз","жкх"))return "Жильё и ЖКХ";
  if(has(merchant,"pyaterochka","perekrestok","magnit","diksi","дикси","пятероч","магнит","lenta","vkusvill","kr asnoe","krasnoe&beloe","красное"))return "Продукты";
  if(has(merchant,"afisha","афиша","cinema"))return "Развлечения";
  switch(m){
   case "5411":case "5422":case "5441":case "5451":case "5462":case "5499":return "Продукты";
   case "5811":case "5812":case "5813":case "5814":return "Кафе и рестораны";
   case "4111":case "4112":case "4121":case "4131":case "4789":return "Транспорт";
   case "5541":case "5542":case "5533":case "7538":return "Автомобиль";
   case "5912":case "8011":case "8021":case "8062":return "Здоровье";
   case "4812":case "4814":case "4899":return "Связь и интернет";
   case "5300":case "5311":case "5331":case "5399":case "5993":case "5999":return "Покупки";
   case "5691":case "5651":case "5661":return "Одежда и обувь";
   case "5251":case "5200":case "5712":return "Дом и ремонт";
   case "5992":return "Подарки и помощь";
   case "4511":case "4722":case "7011":return "Путешествия";
   case "5818":case "7372":return "Цифровые услуги";
  }
  if(m.matches("3\\d{3}")&&Integer.parseInt(m)>=3000&&Integer.parseInt(m)<=3299)return "Путешествия";
  if(has(d,"исходящий платеж qr","оплата через alfa pay"))return "Покупки — уточнить";
  return "Прочее";
 }
 public static String clean(BankStatement.Row r){
  String d=r.description;String merchant=merchant(d);
  if(!merchant.isEmpty()){
   // Never copy account/card numbers or phone numbers into the visible note.
   merchant=merchant.replaceAll("\\+?\\d[\\d* ()-]{8,}\\d","").replaceAll("\\s+"," ").trim();
   if(!merchant.isEmpty())return merchant;
  }
  if(transfer(d))return r.amount>0?"Входящий перевод":"Исходящий перевод";
  String c=extract(d,"^Категория:\\s*([^\\.]+)");
  if(!c.isEmpty())return c.replaceAll("\\+?\\d[\\d* ()-]{8,}\\d","").trim();
  return r.amount>0?"Поступление по выписке":"Оплата по выписке";
 }
 public static String stable(BankStatement.Row r){
  String code=r.code.trim();
  if(code.matches("(?i)[A-Z][A-Z0-9]{8,}"))return r.date+"|"+r.amount+"|"+code;
  return r.date+"|"+r.amount+"|"+lower(merchant(r.description))+"|"+mcc(r.description)+"|"+lower(r.category);
 }
 private BankRules(){}
}
