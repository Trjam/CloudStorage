package com.polozov.cloudstorage.lesson01;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }


    @Override
    public void run() {
        try (
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                DataInputStream in = new DataInputStream(socket.getInputStream())
        ) {
            System.out.printf("Client %s connected\n", socket.getInetAddress());
            while (true) {
                String command = in.readUTF();

                //Вывод команды перенес выше и добавил вывод имени файла, чтоб в консоли упорядочено хоть было, чисто удобства ради
                System.out.println(command);

                if ("upload".equals(command)) {
                    try {
                        String filename = in.readUTF();
                        System.out.println(filename);

                        File file = new File("server" + File.separator + filename);
                        if (!file.exists()) {
                            file.createNewFile();
                        }

                        FileOutputStream fos = new FileOutputStream(file);

                        long size = in.readLong();
                        byte[] buffer = new byte[8 * 1024];

                        for (int i = 0; i < (size + (buffer.length - 1)) / (buffer.length); i++) {
                            int read = in.read(buffer);
                            fos.write(buffer, 0, read);
                        }

                        fos.close();

                        out.writeUTF("OK");
                        System.out.println("receiving status: OK");

                    } catch (Exception e) {
                        out.writeUTF("FATAL ERROR");
                    }

                } else if ("download".equals(command)) {
                    // TODO: 14.06.2021
                    try {
                        String filename = in.readUTF();
                        System.out.println(filename);
                        File file = new File("server" + File.separator + filename);
                        if (!file.exists()) {
                            throw new FileNotFoundException();
                        }

                        long fileLength = file.length();
                        FileInputStream fis = new FileInputStream(file);

                        out.writeLong(fileLength);

                        int read = 0;
                        byte[] buffer = new byte[8 * 1024];
                        while ((read = fis.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }

                        out.flush();

                        String status = in.readUTF();
                        System.out.println("sending status: " + status);

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else if ("exit".equals(command)) {
                    System.out.printf("Client %s disconnected correctly\n", socket.getInetAddress());
                    break;

                } else {
                    out.writeUTF(command);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
