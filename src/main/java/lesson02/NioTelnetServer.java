package lesson02;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class NioTelnetServer {
    private static String userName = "";
    private static Path userPath;
    private static Path currentPath;
    private static final String LS_COMMAND = "\tls      view all files from current directory\n\r";
    private static final String MKDIR_COMMAND = "\tmkdir [path]directory        create new directory\n\r";
    private static final String TOUCH_COMMAND = "\ttouch [path]filename     create new file\n\r";
    private static final String CHANGENICK_COMMAND = "\tchangenick nickname     change your nickname\n\r";
    private static final String CD_COMMAND = "\tcd [path]directory      change directory\n\r";
    private static final String RM_COMMAND = "\trm [path]filename | directory       remove file or directory\n\r";
    private static final String COPY_COMMAND = "\tcope source target        copy file or directory\n\r";
    private static final String CAT_COMMAND = "\tcat filename       view test file\n\r";

    private final ByteBuffer buffer = ByteBuffer.allocate(512);

    private Map<String, SocketAddress> clients = new HashMap<>();

    public NioTelnetServer() throws Exception {
        // имхо, тут лучше мапа с именем юзера в качестве ключа зашла, дабы ники не повторялись и не было бы проблем
        // с отображением чужого хранилища + пишем в мапу адрес, с которого зашел клиент последний раз
        clients.put("User1", null);
        clients.put("User2", null);
        clients.put("User3", null);

        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress(5679));
        server.configureBlocking(false);
        Selector selector = Selector.open();

        server.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started");
        while (server.isOpen()) {
            selector.select();
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    handleAccept(key, selector);
                } else if (key.isReadable()) {
                    handleRead(key, selector);
                }
                iterator.remove();
            }
        }
    }

    private void handleAccept(SelectionKey key, Selector selector) throws IOException {
        userName = "";
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        System.out.println("Client connected. IP:" + channel.getRemoteAddress());
        channel.register(selector, SelectionKey.OP_READ, "skjghksdhg");
        channel.write(ByteBuffer.wrap("Hello user!\n\r".getBytes(StandardCharsets.UTF_8)));
        channel.write(ByteBuffer.wrap("Enter --help for support info\n\r".getBytes(StandardCharsets.UTF_8)));
        channel.write(ByteBuffer.wrap("Before start, enter your nickname please\n\r".getBytes(StandardCharsets.UTF_8)));
    }


    private void handleRead(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        SocketAddress client = channel.getRemoteAddress();
        int readBytes = channel.read(buffer);
        if (readBytes < 0) {
            channel.close();
            return;
        } else if (readBytes == 0) {
            return;
        }

        buffer.flip();
        StringBuilder sb = new StringBuilder();
        while (buffer.hasRemaining()) {
            sb.append((char) buffer.get());
        }
        buffer.clear();

        // TODO: 21.06.2021
        // touch (filename) - создание файла
        // mkdir (dirname) - создание директории
        // cd (path | ~ | ..) - изменение текущего положения
        // rm (filename / dirname) - удаление файла / директории
        // copy (src) (target) - копирование файлов / директории
        // cat (filename) - вывод содержимого текстового файла
        // changenick (nickname) - изменение имени пользователя

        // добавить имя клиента

        if (key.isValid()) {
            String command = sb.toString()
                    .replace("\n", "")
                    .replace("\r", "");

            if (userName.isEmpty()) {
                setUser(command, client);
            } else if ("--help".equals(command)) {
                sendMessage(LS_COMMAND, selector, client);
                sendMessage(MKDIR_COMMAND, selector, client);
                sendMessage(TOUCH_COMMAND, selector, client);
                sendMessage(CD_COMMAND, selector, client);
                sendMessage(RM_COMMAND, selector, client);
                sendMessage(COPY_COMMAND, selector, client);
                sendMessage(CAT_COMMAND, selector, client);
                sendMessage(CHANGENICK_COMMAND, selector, client);
            } else if ("ls".equals(command)) {
                sendMessage(getFilesList().concat("\n\r"), selector, client);
            } else if (command.startsWith("touch ")) {
                String[] cmd = command.split(" ", 2);
                if (createFile(cmd[1])) {
                    channel.write(ByteBuffer.wrap("File already exist or wrong path.\n\r".getBytes(StandardCharsets.UTF_8)));
                } else {
                    channel.write(ByteBuffer.wrap("File created successful.\n\r".getBytes(StandardCharsets.UTF_8)));
                }
            } else if (command.startsWith("changenick ")) {
                String[] cmd = command.split(" ", 2);
                setUser(cmd[1], client);
            } else if (command.startsWith("cd ")) {
                String[] cmd = command.split(" ", 2);
                if (changeDirectory(cmd[1])) {
                    channel.write(ByteBuffer.wrap("No such directory found.\n\r".getBytes(StandardCharsets.UTF_8)));
                }
            } else if (command.startsWith("rm ")) {
                String[] cmd = command.split(" ", 2);
                if (removeFileOrDir(cmd[1])) {
                    channel.write(ByteBuffer.wrap("No such directory or file found.\n\r".getBytes(StandardCharsets.UTF_8)));
                } else {
                    channel.write(ByteBuffer.wrap("File or directory removed successful.\n\r".getBytes(StandardCharsets.UTF_8)));
                }
            } else if (command.startsWith("copy ")) {
                String[] cmd = command.split(" ", 3);
                if (copyFileOrDir(cmd[1], cmd[2])) {
                    channel.write(ByteBuffer.wrap("No such source or target found.\n\r".getBytes(StandardCharsets.UTF_8)));
                } else {
                    channel.write(ByteBuffer.wrap("File or directory copied successful.\n\r".getBytes(StandardCharsets.UTF_8)));
                }
            } else if (command.startsWith("cat ")) {
                String[] cmd = command.split(" ", 2);
                if (Files.exists(Path.of(currentPath.toString(), cmd[1]))) {
                    sendMessage(Files.readString(Path.of(currentPath.toString(), cmd[1])) + "\n\r", selector, client);
                } else {
                    channel.write(ByteBuffer.wrap("No such file found.\n\r".getBytes(StandardCharsets.UTF_8)));
                }
            } else if (command.startsWith("mkdir ")) {
                String[] cmd = command.split(" ", 2);
                if (createDir(cmd[1])) {
                    channel.write(ByteBuffer.wrap("Directory already exist.\n\r".getBytes(StandardCharsets.UTF_8)));
                } else {
                    channel.write(ByteBuffer.wrap("Directory created successful.\n\r".getBytes(StandardCharsets.UTF_8)));
                }
            }
        }
        if (userName.isEmpty()) {
            channel.write(ByteBuffer.wrap(("Enter nickname: ").getBytes(StandardCharsets.UTF_8)));
        } else {
            channel.write(ByteBuffer.wrap((userName + "@" + userRootPathReplacement(currentPath.toString()) + ": ").getBytes(StandardCharsets.UTF_8)));
        }
    }

    private boolean createDir(String name) throws IOException {
        if (!Files.exists(Path.of(currentPath.toString(), name))) {
            Files.createDirectories(Path.of(currentPath.toString(), name));
        } else {
            return true;
        }
        return false;
    }

    private boolean copyFileOrDir(String src, String target) {
        Path srcDir = Path.of(currentPath.toString(), src);
        Path targetDir = Path.of(currentPath.toString(), target, srcDir.toFile().getName());
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
                    Files.copy(Path.of(currentPath.toString(), src), Path.of(currentPath.toString(), target), StandardCopyOption.REPLACE_EXISTING);
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

    private void setUser(String name, SocketAddress socketAddress) {
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

    private void sendMessage(String message, Selector selector, SocketAddress client) throws IOException {
        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && key.channel() instanceof SocketChannel) {
                if (((SocketChannel) key.channel()).getRemoteAddress().equals(client)) {
                    ((SocketChannel) key.channel()).write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));
                }
            }
        }
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
