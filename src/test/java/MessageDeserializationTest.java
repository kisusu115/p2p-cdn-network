import com.fasterxml.jackson.core.type.TypeReference;
import com.p2pnetwork.message.Message;
import com.p2pnetwork.message.MessageType;
import com.p2pnetwork.message.dto.IntroduceContent;
import com.p2pnetwork.network.MessageSender;
import com.p2pnetwork.node.Node;
import com.p2pnetwork.util.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class MessageDeserializationTest {
    private MessageSender messageSender;
    private Node node;
    private ServerSocket serverSocket;

    @BeforeEach
    public void setup() throws IOException {
        serverSocket = new ServerSocket(10001);
        node = new Node(9001);  // Node 객체 생성
        messageSender = new MessageSender(node);  // MessageSender 객체 생성
    }

    @DisplayName("Message<?> 메시지 수신 및 MessageDeserializer가 정상적으로 작동하는지 확인")
    @Test
    public void testMessageDeserialization() throws IOException, InterruptedException {
        AtomicReference<Throwable> threadException = new AtomicReference<>();

        // Given: 테스트용 IntroduceContent 메시지 생성
        IntroduceContent introduceContent = IntroduceContent.of("127.0.0.1", 9003);
        Message<IntroduceContent> exampleMessage = Message.of(MessageType.INTRODUCE, "Node1", "Node2", introduceContent);

        // Given: 서버가 클라이언트를 기다리고 메시지 수신 준비
        Thread serverThread = new Thread(() -> {
            try (Socket clientSocket = serverSocket.accept()) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String receivedMessage = reader.readLine();
                System.out.println("Received message: " + receivedMessage);

                // 메시지를 받으면 JsonUtils로 변환 후 Deserialize
                Message<?> message = JsonUtils.fromJson(receivedMessage, new TypeReference<Message<?>>() {});

                // Then: 받은 메시지가 예상한 값과 일치하는지 검증
                assertNotNull(message);
                assertEquals(MessageType.INTRODUCE, message.getType());
                assertTrue(message.getContent() instanceof IntroduceContent);

                IntroduceContent content = (IntroduceContent) message.getContent();
                assertEquals("127.0.0.1", content.getIp());
                assertEquals(9003, content.getPort());
            } catch (Throwable e) {
                threadException.set(e); // 예외 저장
            }
        });

        serverThread.start();
        Thread.sleep(500);

        // When: 클라이언트가 메시지를 서버에 전송
        try (Socket clientSocket = new Socket("localhost", 10001)) {
            messageSender.sendMessage(clientSocket, exampleMessage);
        }

        serverThread.join();  // 서버 스레드 종료 대기

        // Then: 자식 스레드에서 발생한 예외가 있다면 테스트 실패 처리
        if (threadException.get() != null) {
            threadException.get().printStackTrace();
            fail("Assertion failed in server thread: " + threadException.get().getMessage());
        }
    }
}
