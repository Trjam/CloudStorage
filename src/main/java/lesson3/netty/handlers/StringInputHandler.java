package lesson3.netty.handlers;

import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import lesson02.NioTelnetServer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

public class StringInputHandler extends ChannelInboundHandlerAdapter {

    public static final ConcurrentLinkedDeque<SocketChannel> channels = new ConcurrentLinkedDeque<>();

    private static String userName = "";
    private static Path userPath;
    private static Path currentPath; // = Path.of("server", "user1");
    private static final String LS_COMMAND = "\tls                       view all files from current directory\n\r";
    private static final String MKDIR_COMMAND = "\tmkdir [path]directory    create new directory\n\r";
    private static final String TOUCH_COMMAND = "\ttouch [path]filename     create new file\n\r";
    private static final String CHANGENICK_COMMAND = "\tchangenick nickname      change your nickname\n\r";
    private static final String CD_COMMAND = "\tcd [path]directory       change directory\n\r";
    private static final String RM_COMMAND = "\trm [path]file | dir      remove file or directory\n\r";
    private static final String COPY_COMMAND = "\tcope source target       copy file or directory\n\r";
    private static final String CAT_COMMAND = "\tcat filename             view test file\n\r";

    private Map<String, Channel> clients = new HashMap<String, Channel>();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client connected: " + ctx.channel());
        channels.add((SocketChannel) ctx.channel());
        ctx.writeAndFlush("Hello user!\n\r" +
                "Enter --help for support info\n\r" +
                "Before start, enter your nickname please\n\n\r")
                .addListener(ChannelFutureListener.CLOSE_ON_FAILURE);

        clients.put("User1", null);
        clients.put("User2", null);
        clients.put("User3", null);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client disconnected: " + ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String message = String.valueOf(msg).replace("\r\n", "");
        //System.out.println("message: " + message.replace("\n", ""));


        // TODO: 21.06.2021
        // touch (filename) - создание файла
        // mkdir (dirname) - создание директории
        // cd (path | ~ | ..) - изменение текущего положения
        // rm (filename / dirname) - удаление файла / директории
        // copy (src) (target) - копирование файлов / директории
        // cat (filename) - вывод содержимого текстового файла
        // changenick (nickname) - изменение имени пользователя

        // добавить имя клиента


        if (userName.isEmpty()) {
            setUser(message, ctx.channel());
        } else if ("--help".equals(message)) {

            sendMessage(LS_COMMAND +
                    MKDIR_COMMAND +
                    TOUCH_COMMAND +
                    CD_COMMAND +
                    RM_COMMAND +
                    COPY_COMMAND +
                    CAT_COMMAND +
                    CHANGENICK_COMMAND, ctx);
        } else if ("ls".equals(message)) {
            sendMessage(getFilesList().concat("\n\r"), ctx);
        } else if (message.startsWith("touch ")) {
            String[] cmd = message.split(" ", 2);
            if (createFile(cmd[1])) {
                sendMessage("File already exist or wrong path.\n\r", ctx);
            } else {
                sendMessage("File created successful.\n\r", ctx);
            }
        } else if (message.startsWith("changenick ")) {
            String[] cmd = message.split(" ", 2);
            setUser(cmd[1], ctx.channel());
        } else if (message.startsWith("cd ")) {
            String[] cmd = message.split(" ", 2);
            if (changeDirectory(cmd[1])) {
                sendMessage("No such directory found.\n\r", ctx);
            }
        } else if (message.startsWith("rm ")) {
            String[] cmd = message.split(" ", 2);
            if (removeFileOrDir(cmd[1])) {
                sendMessage("No such directory or file found.\n\r", ctx);
            } else {
                sendMessage("File or directory removed successful.\n\r", ctx);
            }
        } else if (message.startsWith("copy ")) {
            String[] cmd = message.split(" ", 3);
            if (copyFileOrDir(cmd[1], cmd[2])) {
                sendMessage("No such source found. Or target directory already exist.\n\r", ctx);
            } else {
                sendMessage("File or directory copied successful.\n\r", ctx);
            }
        } else if (message.startsWith("cat ")) {
            String[] cmd = message.split(" ", 2);
            if (Files.exists(Path.of(currentPath.toString(), cmd[1]))) {
                sendMessage(Files.readString(Path.of(currentPath.toString(), cmd[1])) + "\n\r", ctx);
            } else {
                sendMessage("No such file found.\n\r", ctx);
            }
        } else if (message.startsWith("mkdir ")) {
            String[] cmd = message.split(" ", 2);
            if (createDir(cmd[1])) {
                sendMessage("Directory already exist.\n\r", ctx);
            } else {
                sendMessage("Directory created successful.\n\r", ctx);
            }
        }

        if (userName.isEmpty()) {
            sendMessage("Enter nickname: ", ctx);
        } else {
            sendMessage(userName + "@" + userRootPathReplacement(currentPath.toString()) + ": ", ctx);
        }
    }


    private boolean createDir(String name) {
        if (!Files.exists(Path.of(currentPath.toString(), name))) {
            try {
                Files.createDirectories(Path.of(currentPath.toString(), name));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            return true;
        }
        return false;
    }

    private boolean copyFileOrDir(String src, String target) {
        Path srcDir = Path.of(currentPath.toString(), src);
        Path targetDir = Path.of(currentPath.toString(),target,srcDir.toFile().getName());
        if (Files.exists(srcDir)) {
            if (Files.isDirectory(srcDir)) {
                if (!Files.exists(targetDir)) {
                    try {
                        Files.createDirectory(targetDir);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                String[] files = srcDir.toFile().list();
                assert files != null;
                for (String file : files) {
                    if (Files.isDirectory(Path.of(srcDir.toString(), file))) {
                        copyFileOrDir(Path.of(src, file).toString(), Path.of(target, srcDir.toFile().getName()).toString());
                    } else {
                        copyFileOrDir(Path.of(src, file).toString(), Path.of(target, srcDir.toFile().getName(), file).toString());
                    }
                }
            } else {
                try {
                    Files.copy(Path.of(currentPath.toString(),src), Path.of(currentPath.toString(), target), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            return true;
        }
        return false;
    }

    private boolean removeFileOrDir(String name) {
        if (Files.exists(Path.of(currentPath.toString(), name))) {
            try {
                Files.delete(Path.of(currentPath.toString(), name));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            return true;
        }
        return false;
    }

    private boolean changeDirectory(String path) {
        if (path.equals("~")) {
            currentPath = userPath;
        } else if (path.equals("..")) {
            currentPath = currentPath.getParent();
        } else {
            if (Files.exists(Path.of(currentPath.toString(), path))) {
                currentPath = Path.of(currentPath.toString(), path);
            } else {
                return true;
            }
        }
        return false;
    }

    private void setUser(String name, Channel socketAddress) {
        for (String mapKey : clients.keySet()) {
            if (mapKey.equalsIgnoreCase(name)) {
                userName = mapKey;
                userPath = Path.of("server", name);
                currentPath = userPath;
                clients.replace(mapKey, socketAddress);

                // наверное лучше эту часть перенести в метод создания пользователя, допустим он когдато будет
                // а пока что так оставим, чтоб руками не создавать
                if (!Files.exists(Path.of("server", mapKey))) {
                    try {
                        Files.createDirectory(Path.of("server", mapKey));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
        }
    }

    private void sendMessage(String message, ChannelHandlerContext ctx) throws IOException {
        ctx.write(message).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
        ctx.flush();
        //channels.forEach(c -> c.writeAndFlush(message));
    }

    private String getFilesList() {
        String[] servers = new File(currentPath.toString()).list();
        assert servers != null;
        return String.join("\n\r", servers);
    }

    private boolean createFile(String name) {
        Path path = Path.of(currentPath.toString(), name);
        if (!Files.exists(path)) {
            try {
                Files.createFile(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            return true;
        }
        return false;
    }

    public String userRootPathReplacement(String path) {
        return path.replace(userPath.toString(), "home");
    }


    public static void main(String[] args) throws Exception {
        new NioTelnetServer();

    }
}


