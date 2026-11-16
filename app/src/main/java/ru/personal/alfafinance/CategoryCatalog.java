package ru.personal.alfafinance;
import java.util.*;
public final class CategoryCatalog {
 public static final String[][] EXPENSE={
 {"Повседневные","Продукты","Кафе и рестораны","Транспорт","Автомобиль","Покупки"},
 {"Дом и обязательства","Жильё и ЖКХ","Связь и интернет","Подписки","Налоги и сборы","Кредиты и проценты"},
 {"Личное и семья","Здоровье","Красота и уход","Одежда и обувь","Образование","Дети","Питомцы"},
 {"Досуг","Развлечения","Спорт","Путешествия","Подарки и помощь","Прочее"}};
 public static final String[][] INCOME={
 {"Работа","Зарплата","Аванс","Премия","Подработка","Бизнес"},
 {"Другие поступления","Проценты и дивиденды","Кешбэк","Подарки","Возврат покупки","Пособия и пенсия","Продажа имущества","Прочий доход"}};
 public static List<String> names(boolean income){List<String> r=new ArrayList<>();for(String[] g:income?INCOME:EXPENSE)for(int i=1;i<g.length;i++)r.add(g[i]);return r;}
 public static String icon(String s){switch(s){
 case "Продукты":return "cart";case "Кафе и рестораны":return "food";case "Транспорт":case "Автомобиль":return "car";
 case "Жильё и ЖКХ":return "home";case "Связь":case "Связь и интернет":return "phone";case "Подписки":return "calendar";
 case "Налоги и сборы":case "Кредиты и проценты":case "Проценты и дивиденды":return "bank";
 case "Здоровье":case "Спорт":return "heart";case "Красота и уход":return "star";case "Покупки":case "Одежда и обувь":case "Продажа имущества":return "bag";
 case "Образование":return "book";case "Дети":case "Пособия и пенсия":return "person";case "Питомцы":return "pet";
 case "Развлечения":return "play";case "Путешествия":return "plane";case "Подарки и помощь":case "Подарки":return "gift";
 case "Зарплата":case "Аванс":case "Премия":case "Подработка":case "Бизнес":return "briefcase";
 case "Кешбэк":case "Возврат покупки":return "refresh";default:return "more";
 }}
 private CategoryCatalog(){}
}
