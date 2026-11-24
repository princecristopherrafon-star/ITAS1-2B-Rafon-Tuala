package serverrafontuala;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ServerRafonTuala {
    private ConcurrentHashMap<String, PrintWriter> clients = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, String> orders = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, String> orderOwner = new ConcurrentHashMap<>();
    private int nextOrderId = 1;

    private String pollQuestion = null;
    private ArrayList<String> pollOptions = new ArrayList<>();
    private ArrayList<Integer> pollVotes = new ArrayList<>();
    private Set<String> alreadyVoted = ConcurrentHashMap.newKeySet();

    private String menuText = "MENU: Burger - 60.00 ;; Rice - 20.00 ;; Soda - 30 ;; Chiken - 80.00";
    private double totalBill = 0;
    private double totalPaid = 0;
    private boolean orderClosed = false;

    public static void main(String[] args) {
        new ServerRafonTuala().startServer(5000);
    }

    void startServer(int port) {
        System.out.println("======================================");
        System.out.println("       🍔 CANTEEN SERVER");
        System.out.println("======================================");
        try {
            ServerSocket ss = new ServerSocket(port);
            System.out.println("✅ Server running on port: " + port);
            System.out.println("📌 Your LAN IP: " + InetAddress.getLocalHost().getHostAddress());
            System.out.println("📌 Clients connect to THIS IP!\n");
            System.out.println("Commands: /menu, /order, /orders, /clear, /poll, /vote, /pay, /close, /name, /exit\n");
            
            ExecutorService pool = Executors.newCachedThreadPool();
            while (true) {
                Socket s = ss.accept();
                pool.execute(new Handler(s));
            }
        } catch (IOException e) {
            System.out.println("❌ Server Error: " + e.getMessage());
        }
    }

    void sendToAll(String msg) {
        for (PrintWriter w : clients.values()) {
            synchronized (w) { w.println(msg); }
        }
    }

    void sendUserList() {
        sendToAll("USERS:" + String.join(",", new TreeSet<>(clients.keySet())));
    }

    String makeOrderList() {
        if (orders.isEmpty()) return "No orders yet. Use /order <item>";
        StringBuilder sb = new StringBuilder();
        for (int id : new TreeSet<>(orders.keySet())) {
            sb.append(id).append(". ").append(orders.get(id))
              .append(" (by ").append(orderOwner.get(id)).append(") ;; ");
        }
        sb.append("BILL: Total: $").append(String.format("%.2f", totalBill))
          .append(" | Paid: $").append(String.format("%.2f", totalPaid))
          .append(" | Due: $").append(String.format("%.2f", totalBill - totalPaid));
        if (orderClosed) sb.append(" | STATUS: CLOSED");
        return sb.toString();
    }

    String makePollText() {
        if (pollQuestion == null) return "No active poll. Use /poll Question | Option1 | Option2";
        StringBuilder sb = new StringBuilder("📊 POLL: " + pollQuestion + "\n");
        int total = 0; 
        for (int v : pollVotes) total += v;
        for (int i = 0; i < pollOptions.size(); i++) {
            sb.append(i + 1).append(". ").append(pollOptions.get(i))
              .append(" — ").append(pollVotes.get(i)).append(" vote(s)\n");
        }
        sb.append("Total votes: ").append(total).append(" | Use: /vote <number>");
        return sb.toString();
    }

    class Handler implements Runnable {
        Socket socket; 
        PrintWriter out; 
        BufferedReader in; 
        String nick;

        Handler(Socket s) { socket = s; }

        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                // Get nickname from client
                String name = in.readLine();
                if (name == null || name.trim().isEmpty()) {
                    out.println("ERROR:Please enter a nickname.");
                    closeAll(); 
                    return;
                }
                name = name.trim();
                
                if (clients.containsKey(name)) {
                    out.println("ERROR:Nickname already taken. Try another.");
                    closeAll();
                    return;
                }
                
                nick = name;
                clients.put(nick, out);
                System.out.println("✅ [" + nick + "] connected | Online: " + clients.size());

                // Welcome new client
                out.println("WELCOME:" + nick);
                out.println("SYSTEM:Welcome to the Canteen! Use /menu to see items.");
                sendToAll("SYSTEM:" + nick + " joined the canteen.");
                sendUserList();
                out.println("MENU:" + menuText);
                out.println("ORDERS:" + makeOrderList());
                out.println("POLL:" + makePollText());

                // Listen for messages
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.trim().isEmpty()) { 
                        out.println("SYSTEM:Message cannot be blank."); 
                        continue; 
                    }
                    
                    // Remove "Nickname: " prefix if client sends it
                    String msg = line;
                    int idx = line.indexOf(": ");
                    if (idx != -1) msg = line.substring(idx + 2).trim();

                    // ===== COMMANDS =====
                    
                    if (msg.equalsIgnoreCase("/exit")) {
                        break;
                    }
                    else if (msg.equalsIgnoreCase("/menu")) {
                        out.println("MENU:" + menuText);
                    }
                    else if (msg.toLowerCase().startsWith("/name ")) {
                        String newName = msg.substring(6).trim();
                        if (newName.isEmpty()) {
                            out.println("ERROR:Usage: /name <newname>");
                        }
                        else if (clients.containsKey(newName)) {
                            out.println("ERROR:Name already taken.");
                        }
                        else {
                            clients.remove(nick);
                            String old = nick; 
                            nick = newName;
                            clients.put(nick, out);
                            out.println("WELCOME:" + nick);
                            sendToAll("SYSTEM:" + old + " changed name to " + nick);
                            sendUserList();
                        }
                    }
                    else if (msg.toLowerCase().startsWith("/order ")) {
                        if (orderClosed) {
                            out.println("ERROR:Order list is closed.");
                            continue;
                        }
                        String item = msg.substring(7).trim();
                        if (item.isEmpty()) {
                            out.println("ERROR:Usage: /order <item name>");
                            continue;
                        }
                        orders.put(nextOrderId, item);
                        orderOwner.put(nextOrderId, nick);
                        totalBill += getPrice(item);
                        sendToAll("ORDER:" + nick + " ordered: " + item);
                        sendToAll("ORDERS:" + makeOrderList());
                        nextOrderId++;
                    }
                    else if (msg.equalsIgnoreCase("/orders")) {
                        out.println("ORDERS:" + makeOrderList());
                    }
                    else if (msg.equalsIgnoreCase("/clear")) {
                        orders.clear();
                        orderOwner.clear();
                        totalBill = 0;
                        totalPaid = 0;
                        orderClosed = false;
                        sendToAll("SYSTEM:📋 All orders have been cleared.");
                        sendToAll("ORDERS:" + makeOrderList());
                    }
                    else if (msg.toLowerCase().startsWith("/poll ")) {
                        String[] parts = msg.substring(6).split("\\s*\\|\\s*");
                        if (parts.length < 3) {
                            out.println("ERROR:Usage: /poll Question | Option1 | Option2");
                            continue;
                        }
                        pollQuestion = parts[0];
                        pollOptions.clear();
                        pollVotes.clear();
                        alreadyVoted.clear();
                        for (int i = 1; i < parts.length; i++) {
                            if (!parts[i].trim().isEmpty()) {
                                pollOptions.add(parts[i].trim());
                                pollVotes.add(0);
                            }
                        }
                        sendToAll("POLL:" + makePollText());
                    }
                    else if (msg.toLowerCase().startsWith("/vote ")) {
                        if (pollQuestion == null) {
                            out.println("ERROR:No active poll.");
                            continue;
                        }
                        if (alreadyVoted.contains(nick)) {
                            out.println("ERROR:You have already voted.");
                            continue;
                        }
                        String numStr = msg.substring(6).trim();
                        if (!numStr.matches("\\d+")) {
                            out.println("ERROR:Use: /vote 1 or /vote 2");
                            continue;
                        }
                        int choice = Integer.parseInt(numStr) - 1;
                        if (choice < 0 || choice >= pollOptions.size()) {
                            out.println("ERROR:Invalid option number.");
                            continue;
                        }
                        alreadyVoted.add(nick);
                        pollVotes.set(choice, pollVotes.get(choice) + 1);
                        sendToAll("SYSTEM:" + nick + " voted! ✅");
                        sendToAll("POLL:" + makePollText());
                    }
                    else if (msg.toLowerCase().startsWith("/pay ")) {
                        String amountStr = msg.substring(5).trim();
                        if (!amountStr.matches("\\d+(\\.\\d+)?")) {
                            out.println("ERROR:Enter a valid amount. Example: /pay 5.00");
                            continue;
                        }
                        double amount = Double.parseDouble(amountStr);
                        totalPaid += amount;
                        sendToAll("SYSTEM:" + nick + " paid $" + String.format("%.2f", amount));
                        sendToAll("ORDERS:" + makeOrderList());
                    }
                    else if (msg.equalsIgnoreCase("/close")) {
                        orderClosed = true;
                        sendToAll("SYSTEM:🔒 Order list is now CLOSED.");
                        sendToAll("ORDERS:" + makeOrderList());
                    }
                    else {
                        // Regular chat message
                        sendToAll("CHAT:" + nick + ": " + line);
                    }
                }
            } 
            catch (SocketException e) {
                System.out.println("👤 [" + nick + "] disconnected");
            }
            catch (IOException e) {
                // Silent disconnect
            }
            finally {
                closeAll();
            }
        }

        private double getPrice(String item) {
            String lower = item.toLowerCase();
            if (lower.contains("burger")) return 60.00;
            if (lower.contains("Rice")) return 20.00;
            if (lower.contains("soda")) return 30.00;
            if (lower.contains("chiken")) return 30.00;
            return 0.00; // unknown item
        }

        void closeAll() {
            if (nick != null && clients.remove(nick) != null) {
                System.out.println("👋 [" + nick + "] left | Online: " + clients.size());
                sendToAll("SYSTEM:" + nick + " has left.");
                sendUserList();
            }
            try { if (out != null) out.close(); } catch (Exception e) {}
            try { if (in != null) in.close(); } catch (Exception e) {}
            try { if (socket != null && !socket.isClosed()) socket.close(); } catch (Exception e) {}
        }
    }
}