package application;
	
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;


public class Main extends Application {
	
	//ExecutorService 병렬작업시 여러개의 작업을 효율적으로 처리하기 위해 사용되는 라이브러리
	//한정된 자원으로 안정적으로 서버를 사용하기위해 사용한다.
	public static ExecutorService threadPool;
	public static Vector<Client> clients = new Vector<Client>();
	
	ServerSocket serverSocket;
	
	//서버를 구동시켜서 클라이언트의 연결을 기다리는 메소드
	public void startServer(String IP, int port) {
		try {
			//서버소캣 생성,활성화
			serverSocket = new ServerSocket();
			//bind를 통해 자신의 ip와 특정 port번호로 클라이언트의 접속을 기다리게한다.
			serverSocket.bind(new InetSocketAddress(IP,port));
			
		} catch (Exception e) {
			e.printStackTrace();
			//서버소캣이 닫혀있지 않다면
			if(!serverSocket.isClosed()) {
				stopServer();
			}
			return;
		}
		
		//서버가 소캣을 잘 열어서 접속을 기다릴 수 있다면 클라이언트의 접속을 기다림
		Runnable thread = new Runnable() {

			@Override
			public void run() {
				while(true) {
					try {
						//서버소캣을 통해 들어오는 클라이언트 소캣을 기다림.
						Socket socket = serverSocket.accept();
						
						//받은 클라이언트를 등록함.
						clients.add(new Client(socket));
						
						System.out.println("[클라이언트 접속]"+socket.getRemoteSocketAddress()+Thread.currentThread().getName());
					}
					catch(Exception e) {
						if(!serverSocket.isClosed()) {
							stopServer();
						}
						break;
					}
				}
				
			}
			
		};
		//threadpool 초기화
		threadPool = Executors.newCachedThreadPool();
		
		//threadpool에 클라이언트를 기다리는 thread를 첫 thread로 넣엊줌
		threadPool.submit(thread);
	}
	
	//서버 종료 후 자원 반환
	public void stopServer() {
		try {
			//현재 작동중인 모든 소켓 닫기
			
			//iterator를 통해 각 클라언트 개별 접근
			Iterator<Client> iterator = clients.iterator();
			while(iterator.hasNext()){
				Client client = iterator.next();
				client.socket.close();
				iterator.remove();
			}
			//서버 소켓 닫기
			if(serverSocket != null && !serverSocket.isClosed()) {
				serverSocket.close();
			}
			//threadpool 종료
			if(threadPool != null && !threadPool.isShutdown()) {
				threadPool.shutdown();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	//UI를 생성하고 프로그램을 실질적으로 구동하는 메소드
	//javafx는 fxml이라는 디자인 코드를 사용함
	//다운받은 javafx scene builder가 java fxml과 css를 관리하는 툴
	//이번 프로젝트에서는 scene builder없이 코드로 작성하여 구현함
	
	//대부분의 import는 javafx로부터 한다.
	@Override
	public void start(Stage primaryStage) {
		//전체 디자인을 담을 수 있는 pane 생성(layout을 생성한다고 생각)
		BorderPane root = new BorderPane();
		
		//내부에 5만큼 padding을 준다.
		root.setPadding(new Insets(5));
		
		TextArea textArea = new TextArea();
		textArea.setEditable(false);
		textArea.setFont(new Font("맑은 고딕",15));
		
		root.setCenter(textArea);
		
		Button toggleButton = new Button("시작하기");
		toggleButton.setMaxWidth(Double.MAX_VALUE);
		//borderpane에 마진을 줌
		BorderPane.setMargin(toggleButton,new Insets(1,0,0,0));
		root.setBottom(toggleButton);
		
		String IP = "127.0.0.1";
		int port = 9876;
		
		toggleButton.setOnAction(event->{
			if(toggleButton.getText().equals("시작하기")) {
				startServer(IP,port);
				//javafx는 버튼 클릭 후 바로 textarea에 반영되는 것이 아니라
				//runlater같은 메소드를 이용해서  gui요소를 출력하도록해야한다.
				Platform.runLater(()->{
					//문자열 생성
					String message = String.format("[서버시작]\n",IP,port);
					textArea.appendText(message);
					toggleButton.setText("종료하기");
				});
			}
			else {
				stopServer();
				Platform.runLater(()->{
					String message = String.format("[서버종료]\n",IP,port);
					textArea.appendText(message);
					toggleButton.setText("시작하기");
				});
			}
		});
		
		//화면 크기 구성
		Scene scene = new Scene(root,400,400);
		primaryStage.setTitle("[채팅서버]");
		primaryStage.setOnCloseRequest(event->stopServer());
		primaryStage.setScene(scene);
		primaryStage.show();
	}
	
	
	//프로그램의 진입점
	public static void main(String[] args) {
		launch(args);
	}
}