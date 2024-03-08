package main;

import java.io.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Separator_NP_VP {
    //Входной файл
    FileInputStream inputFile;
    //Выходной файл с НП
    FileOutputStream fNP;
    //Выходной файл с ВП-обратное
    FileOutputStream fVP;
    //Выходной файл с ВП-прямое
    FileOutputStream fVP_straight;
    //Текущий режим
    int tekRez;

    //Инициализируем поля
    Separator_NP_VP(String input, String fNP, String fVP, String fVP_straight) throws FileNotFoundException {
        this.inputFile = new FileInputStream(input);
        this.fNP = new FileOutputStream(fNP);
        this.fVP = new FileOutputStream(fVP);
        this.fVP_straight = new FileOutputStream(fVP_straight);
        tekRez = 1;
    }
    //Функция записывающая данные из буффера в зависимости от текущего режима
    void writeToFile(byte [] buff, int len) throws IOException {
        if(tekRez == 1)
            fNP.write(buff, 0, len);
        else if(tekRez == 2)
            fVP.write(buff, 0, len);
            else
                fVP_straight.write(buff, 0, len);

    }
    //Функция читает файл и, в зависимости от режима, записывает полученный
    //массив байтов в НП или ВП файл соответственно с помощью ф-ии writeToFile
    void run() throws IOException {
        byte [] buff = new byte[90000];
        int number, type, typeTek, len, typeFF, count = 0;
        //Считываем первые 32 байта, которые являются служебным сообщением начала сеанса
        inputFile.read(buff, 0, 32);
        //Записываем начало сеанса в оба выходных бинарных файла
        fNP.write(buff, 0, 32);
        fVP.write(buff, 0, 32);
        fVP_straight.write(buff, 0, 32);


        //Читаем каждые 16 байт файла
        while(inputFile.read(buff,0,16) > 0) {
            //Находим номер записи
            number = ((buff[0] << 8) & 0xFF00) | (buff[1]&0xFF);
            //Если служебная            СЗ - служебная запись
            if(number == 0xFFFF) {
                typeFF = buff[6] & 0xFF;
                //Если СЗ смены режима
                if(typeFF == 4) {
                    //Вычисляем тип НП или ВП
                    typeTek = ((buff[12] << 24) & 0xFF000000) | ((buff[13] << 16) & 0xFF0000) | ((buff[14] << 8) & 0xFF00) | (buff[15] & 0xFF);
                    if(typeTek == 0 || typeTek == 1) {
                        tekRez = 1;
                    }
                    else if (typeTek == 2 || typeTek == 4 || typeTek == 7){
                        tekRez = 2;
                    }else {
                        tekRez = typeTek;
                    }
                }// Если СЗ конца сеанса - записываем ее в конец файла
                else if (typeFF == 3) {
                    fNP.write(buff, 0, 16);
                    fVP.write(buff, 0, 16);
                    fVP_straight.write(buff, 0, 16);
                }
                //Записываем СЗ в файл
                writeToFile(buff, 16);

            }//Если не служебная
            else {
                //Записываем считанные байты в выходной файл
                writeToFile(buff, 16);
                type = buff[7] & 0xF;
                //Если тип POINT дополнительно считываем байты размером с величину массива и записываем их в выходной файл
                if(type == 3) {
                    len = buff[10] << 8 & 0xFF00 | buff[11] & 0xFF;
                    if(len > 4) {
                        inputFile.read(buff,0,len-4);
                        writeToFile(buff, len-4);
                    }
                }
            }
        }
    }

    //Создает дополнительные текстовые файлы для просмотра 
    // полученных файлов в 16-м виде и отладки программы
    public void print() throws IOException {
        FileInputStream file_NP = new FileInputStream("fNP.CM1");
        FileInputStream file_VP = new FileInputStream("fVP.CM1");
        FileInputStream file_VP_straight = new FileInputStream("fVP_straight.CM1");

        byte [] buff = new byte[16];
        int size;
        int count = 0;
        FileWriter outNP = new FileWriter("outNP");
        FileWriter outVP = new FileWriter("outVP");
        FileWriter outVP_straight = new FileWriter("outVP_straight");
        
        //Читаем каждые 16 байт файла
        while((size = file_NP.read(buff,0,16)) > 0) {
            outNP.write(String.format("%08x ", count));
            for (int i = 0; i < size; i++) {
                outNP.write(String.format("%02x ", buff[i]));
                if(i == 7) {
                    outNP.write(String.format("  "));
                }
            }
            outNP.write("\n ");
            count += size;
        }

        count = 0;

        //Читаем каждые 16 байт файла
        while((size = file_VP.read(buff,0,16)) > 0) {
            outVP.write(String.format("%08x ", count));
            for (int i = 0; i < size; i++) {
                outVP.write(String.format("%02x ", buff[i]));
                if(i == 7) {
                    outVP.write(String.format("  "));
                }
            }
            outVP.write("\n ");
            count += size;
        }

        count = 0;

        //Читаем каждые 16 байт файла
        while((size = file_VP_straight.read(buff,0,16)) > 0) {
            outVP_straight.write(String.format("%08x ", count));
            for (int i = 0; i < size; i++) {
                outVP_straight.write(String.format("%02x ", buff[i]));
                if(i == 7) {
                    outVP_straight.write(String.format("  "));
                }
            }
            outVP_straight.write("\n ");
            count += size;
        }

        file_VP.close();
        file_NP.close();
        file_VP_straight.close();
    }
    
    public static void main(String[] args) throws IOException {
        Separator_NP_VP obj = new Separator_NP_VP("src/file.knp", "fNP.CM1", "fVP.CM1", "fVP_straight.CM1");
        obj.run();
        obj.fNP.close();
        obj.fVP.close();
        obj.fVP_straight.close();
        obj.print();
//        ReverseTMRecords reverse = new ReverseTMRecords("fVP.CM1", "fVP_reversed.CM1");
//        reverse.generate();
    }
}