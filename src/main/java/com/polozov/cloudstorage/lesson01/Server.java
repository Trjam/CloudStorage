package com.polozov.cloudstorage.lesson01;

import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    // TODO: 14.06.2021
    // организовать корректный вывод статуса
	// c else норм приходит, точнее не уходит эхо, когда не надо

    // подумать почему так реализован цикл в ClientHandler
	// ибо делим файл на размер буфера, получаем количество циклов, сколько раз надо прочитать полный буфер + 1 проход на не полный буфер
    public Server() {
        ExecutorService service = Executors.newFixedThreadPool(4);
        try (ServerSocket server = new ServerSocket(5678)) {
            System.out.println("Server started");
            while (true) {
                service.execute(new ClientHandler(server.accept()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Server();
    }
}
