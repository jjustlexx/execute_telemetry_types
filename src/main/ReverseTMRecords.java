package main;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class ReverseTMRecords {

    byte[] start; // Запись начала сеанса
    byte[] end; // Запись конца сеанса

    //Массив, который будет в себе хранить массивы с МАССИВАМИ БАЙТ!!!!Первой записью каждого внутреннего массива будет служебная запись смены режима
    ArrayList<ArrayList<byte[]>> notReversed = new ArrayList<>();
    //Внутренний массив, будет хранить в себе все параметры, принадлежащие одной служебной записи смены режима. СЗ будет находиться на первом месте
    ArrayList<byte[]> records;

    static FileInputStream fInput;
    static FileOutputStream fOutput;

    byte[] buff = new byte[4200];


    ReverseTMRecords(String f_Input, String f_Output) throws IOException {
        fInput = new FileInputStream(f_Input);
        fOutput = new FileOutputStream(f_Output);
        load();
//        print();
        reverse();
    }

    //Генерирует выходной файл, записывая в него каждый элемент массива notReversed от начала до конца
    public void generate() throws IOException {
        fOutput.write(start);
        for(ArrayList<byte[]> item : notReversed) {
            for(byte[] item2 : item)
                fOutput.write(item2);
        }
        fOutput.write(end);
    }

    //Здесь мы заполняем наш массив notReversed
    private void load() throws IOException {
        //Считываем первые 32 байта служебного сообщения
        fInput.read(buff, 0, 32);
        start = Arrays.copyOfRange(buff, 0, 32);

        //Читаем файл по 16 байт
        while (fInput.read(buff, 0, 16) > 0){
            int number = buff[0] << 8 & 0xFF00 | buff[1] & 0xFF;

            if(number == 0xFFFF) {
                int type = buff[6] & 0xF;
                //Если найденная запись является СЗ смены режима и массив records не пустой,
                // то добавляем records в notReversed и обнуляем массив records
                //Записываем первым элементом служебную запись смены режима
                if(type == 4) {
                    if (records != null)
                        notReversed.add(records);
                    records = new ArrayList<>();
                    byte[] record = Arrays.copyOfRange(buff, 0, 16);
                    records.add(record);
                }else{
                    //Если СЗ люого другого типа просто добавляем ее в массив
                    byte[] otherRecord = Arrays.copyOfRange(buff, 0, 16);
                    records.add(otherRecord);
                }
            }
            else {
                //Если параметр не СЗ
                int parameterType = buff[7] & 0xF;
                //Если параметр не типа POINT - простодобавляем запись в массив
                if(parameterType != 3){
                    byte[] otherRecord = Arrays.copyOfRange(buff, 0, 16);
                    records.add(otherRecord);
                }
                else{
                    //Если POINT
                    int size = buff[10] << 8 & 0xFF00 | buff[11] & 0xFF;
                    //Если длина массива POINT > 4 - мы берем первые 16 байт и следющие байты длинной в размер 
                    // массива, компануем в один массив байт и добавляем во внутренний массив records
                    if (size > 4) {
                        byte[] record1 = Arrays.copyOfRange(buff, 0, 16);
                        fInput.read(buff, 0, size - 4);
                        byte[] record2 = Arrays.copyOfRange(buff, 0, size - 4);
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        outputStream.write(record1);
                        outputStream.write(record2);

                        records.add(outputStream.toByteArray());
                    } else {
                        //Если длина массива <= 4 - просто добавляем запись во внутренний массив records
                        byte[] record = Arrays.copyOfRange(buff, 0, 16);
                        records.add(record);
                    }
                }
            }
        }
        //Записываем в end сообщение конца сеанса
        end = Arrays.copyOfRange(buff, 0, 16);
        //Добавляем во внешний массив notReversed последний не добавленный внутренний массив records
        //и убираем последнюю запись, которая является записью конца сеанса
        if (records != null) {
            records.removeLast();
            notReversed.add(records);
        }


    }
    
    //Выводим получившийся массив notReversed в консоль для отладки
    void print(){
        for(ArrayList<byte[]> item1 : notReversed) {
            for (byte[] item2 : item1) {
                for (byte item3 : item2) {
                    System.out.printf("%02x ", item3);

                }
                System.out.println();
            }
            System.out.println("\n");
        }
    }
    
    //Переворот получившегося массива
    void reverse(){

        for(ArrayList<byte[]> item : notReversed) {
            byte[] sz = item.getFirst();
            item.removeFirst();
            Collections.reverse(item);
            item.addFirst(sz);
        }
        Collections.reverse(notReversed);
    }
//    for(ArrayList<byte[]> item : notReversed) {
//        byte[] sz = item.getFirst();
//        int typeOf = ((buff[12] << 24) & 0xFF000000) | ((buff[13] << 16) & 0xFF0000)
//                | ((buff[14] << 8) & 0xFF00) | (buff[15] & 0xFF);
//        if(typeOf == 2 || typeOf == 4 || typeOf == 7 || typeOf == 33){
//            item.removeFirst();
//            Collections.reverse(item);
//            item.addFirst(sz);
//        }
//    }
//        Collections.reverse(notReversed);
//}
}
