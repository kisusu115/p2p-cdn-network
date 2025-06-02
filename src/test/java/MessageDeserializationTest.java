import com.fasterxml.jackson.core.type.TypeReference;
import com.p2pnetwork.message.Message;
import com.p2pnetwork.message.MessageType;
import com.p2pnetwork.routing.RoutingEntry;
import com.p2pnetwork.node.Node;
import com.p2pnetwork.node.NodeRole;
import com.p2pnetwork.network.MessageSender;
import com.p2pnetwork.util.JsonUtils;
import org.junit.jupiter.api.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MessageDeserializationTest {
    private ServerSocket serverSocket;
    private Node node;
    private MessageSender messageSender;

    @BeforeEach
    void setUp() throws Exception {
        serverSocket = new ServerSocket(10001);
        node = new Node(37.5665, 126.9780, 9001);
        messageSender = new MessageSender(node);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
    }

    @DisplayName("RoutingEntry 메시지 역직렬화 정상 동작 확인")
    @Test
    void testRoutingEntryDeserialization() throws Exception {
        AtomicReference<Throwable> threadException = new AtomicReference<>();

        // Given: RoutingEntry 메시지 생성
        RoutingEntry entry = new RoutingEntry("xn76f_82d234bb", "127.0.0.1", 9003, NodeRole.PEER);
        Message<RoutingEntry> exampleMessage = new Message<>(
                MessageType.INTRODUCE, "Node1", "Node2", entry, System.currentTimeMillis()
        );

        // 서버 스레드: 메시지 수신 및 검증
        Thread serverThread = new Thread(() -> {
            try (Socket clientSocket = serverSocket.accept();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"))) {

                String receivedMessage = reader.readLine();
                assertNotNull(receivedMessage, "메시지가 수신되지 않음");

                // 메시지 역직렬화
                Message<?> message = JsonUtils.fromJson(receivedMessage, new TypeReference<Message<?>>() {});
                assertNotNull(message, "역직렬화된 메시지가 null");

                // 타입 및 내용 검증
                assertEquals(MessageType.INTRODUCE, message.getType());
                assertTrue(message.getContent() instanceof RoutingEntry);

                RoutingEntry content = (RoutingEntry) message.getContent();
                assertEquals("127.0.0.1", content.getIp());
                assertEquals(9003, content.getPort());
                assertEquals(NodeRole.PEER, content.getRole());
                assertEquals("xn76f_82d234bb", content.getNodeId());

            } catch (Throwable e) {
                threadException.set(e);
            }
        });

        serverThread.start();
        Thread.sleep(300); // 서버 준비 대기

        // When: 클라이언트가 메시지 전송
        try (Socket clientSocket = new Socket("localhost", 10001)) {
            messageSender.sendMessage(clientSocket, exampleMessage);
        }

        serverThread.join();

        // Then: 예외 발생시 테스트 실패
        if (threadException.get() != null) {
            threadException.get().printStackTrace();
            fail("서버 스레드에서 Assertion 실패: " + threadException.get().getMessage());
        }
    }
}
