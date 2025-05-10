package com.p2pnetwork;

import com.p2pnetwork.node.Node;

import java.net.UnknownHostException;

public class Main {
    public static void main(String[] args) throws UnknownHostException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9000;
        Node node = new Node(port);
        node.start();
    }
}
