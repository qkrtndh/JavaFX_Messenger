package application;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Client {
	Socket socket;
	
	//생성자를 통해 클라이언트 클래스 호출시 넘어온 매개변수를 입력
	public Client(Socket socket) {
		this.socket = socket;
		//지속적으로 클라이언트로부터 메세지를 전달받음
		receive();
	}
	
	//클라이언트로부터 메세지를 받는 메소드
	public void receive() {
		//하나의 쓰레드를 만드는 경우 Runnable 로 주로 만든다.
		Runnable thread = new Runnable() {

			//내부적으로 반드시 run메소드가 존재해야함
			@Override
			public void run() {
				//클라이언트 사용시 에러 발생 가능하므로 try-catch로 감쌈
				try {
					//지속적으로 클라이언트로부터 전달받을 수 있다.
					while(true) {
						//내용을 전달받을 inputstream
						InputStream in = socket.getInputStream();
						//한번에 512byte를 전달받을 버퍼
						byte[] buffer = new byte[512];
						//buffer에 내용을 전달받는다.
						int length = in.read(buffer);
						//메세지에 오류가 있을때 오류발생
						while(length == -1) throw new IOException();
						//서버 콘솔에 수신성공표시
						//현재 접속중인 클라이언트의 주소정보 출력
						//thread의 이름 출력
						System.out.println("[메세지 수신 성공]"+socket.getRemoteSocketAddress()+Thread.currentThread().getName());
						
						//버퍼로부터 전달받은 메세지를 문자열로 옮김
						String message = new String(buffer,0,length,"UTF-8");
						
						//다른 모든 클라이언트들에게도 메세지를 보낼 수 있도록 만듬
						for(Client client : Main.clients) {
							client.send(message);
						}
					} 
				}
				catch(Exception e) {
					try {
						System.out.println("[메세지 수신 오류]"+socket.getRemoteSocketAddress()+Thread.currentThread().getName());
					}
					
					catch(Exception e2) {
											
						e2.printStackTrace();
					}
				}
			}
			
		};
		//threadpool에 만들어진 thread를 등록한다.
		//클라이언트가 접속할때 생성된 thread를 안정적으로 관리하기 위함.
		Main.threadPool.submit(thread);
	}
	
	//클라이언트에게 메세지를 전송하는 메소드
	public void send(String message) {
		Runnable thread = new Runnable() {

			@Override
			public void run() {
				//내용을 전달할 OutputStream
				try {
					OutputStream out = socket.getOutputStream();
					byte[] buffer = message.getBytes("UTF-8");
					
					//버퍼의 메세지를 가져온다.
					out.write(buffer);
					//전송
					out.flush();
				} catch (Exception e) {
					try {
						System.out.println("[메세지 송신 오류]"+socket.getRemoteSocketAddress()+Thread.currentThread().getName());
						
						//오류가 발생해서 클라이언트의 접속이 끊겼으니 서버에서 클라이언트 삭제
						Main.clients.remove(Client.this);
						//오류가 생긴 클라이언트의 소캣을 닫는다
						socket.close();
					} catch (Exception e2) {
						e2.printStackTrace();
					}
				}
				
			}
			
		};
		Main.threadPool.submit(thread);
	}
	
}
