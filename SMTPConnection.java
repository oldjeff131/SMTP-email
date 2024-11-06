import java.net.*;
import java.io.*;
import java.util.*;
import javax.swing.JOptionPane; 
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;

public class SMTPConnection {

    private Socket connection;
    private BufferedReader fromServer;
    private DataOutputStream toServer;


    private static final int SMTP_PORT = 25;
    private static final String CRLF = "\r\n";

    private boolean isConnected = false;


	//SMTP連線建立
    public SMTPConnection(Envelope envelope) throws IOException 
	{
		try{
	//建立TCP連線和初始化
		connection = new Socket(envelope.DestAddr, SMTP_PORT);
		fromServer = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		toServer =   new DataOutputStream(connection.getOutputStream());
		String password="your password";
		String reply = fromServer.readLine();
		if (parseReply(reply) != 220) 
	{
		throw new IOException("Error: " + reply);
	}

		String localhost = InetAddress.getLocalHost().getHostName();
		sendCommand("HELO " + localhost, 250); //ReplyCode=250
		isConnected = true;
		serverLST(envelope);
		authenticate(envelope,password);
}
catch(IOException e)
{
	JOptionPane.showMessageDialog(null,"ERR: " + e,"ERROR",JOptionPane.ERROR_MESSAGE);
}
    }

    //將SMTP升級為LST(參考GPT跟同學)
	public void serverLST(Envelope envelope)throws IOException
	{
		try
		{
			String localhost = InetAddress.getLocalHost().getHostName();
			sendCommand("HELO " + localhost, 250); //ReplyCode=250

			//啟動STARTTLS加密機制，ReplyCode=220
			sendCommand("STARTTLS ", 220);

			//使用 SSLSocketFactory 創建 TLS 加密的Socket
			SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        	SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(connection, envelope.DestHost, 465, true);
		
			//重設fromServer和toServer設定，將新的STARTTLS加密機制加入到Server系統裡
			fromServer = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
        	toServer = new DataOutputStream(sslSocket.getOutputStream());
		
			//重置EHLO 
        	sendCommand("EHLO " + localhost, 250);
		}
		catch(IOException e)
		{
			JOptionPane.showMessageDialog(null,"ERR: " + e,"ERROR",JOptionPane.ERROR_MESSAGE);
		}
		
	}

    public void send(Envelope envelope) throws IOException 
	{
		int number=0;
		String emailsendname=envelope.Recipient;
		List<String> namelist = new ArrayList<>();//建立收件者的list
		namelist=cutname(emailsendname);//將切割好的收件者的list放入填入變數
		int num=namelist.size();//取得list的數量
 
		if (num<2){//判斷最多
			do{
				sendCommand("MAIL FROM:<" + envelope.Sender + ">", 250);//寄件人 ReplyCode=250
				sendCommand("RCPT TO:<" + namelist.get(number) + ">", 250);//收件人 ReplyCode=250
				sendCommand("DATA", 354);//資料 ReplyCode=354
				sendCommand(envelope.Message.toString() + CRLF + ".", 250); // ReplyCode=250
				number++;
				}while(number<2);
				System.out.println("Mail sent succesfully!");
		}
		else
		{
			JOptionPane.showMessageDialog(null,"最多寄兩人","ERROR",JOptionPane.ERROR_MESSAGE);//跳出警告視窗
		}
		
	}
	
	public List<String> cutname(String name1) throws IOException
	{
		List<String> namelist = new ArrayList<>();//建立list
		String returnname="";
		for (String retval: name1.split("\\,")){//以，分割收件者

			returnname=retval;
			if(!returnname.contains("@"))//檢查收件者格式有無@
			{
				JOptionPane.showMessageDialog(null,"缺少@","ERROR",JOptionPane.ERROR_MESSAGE);//跳出警告視窗
				break;
			}
			else
			{
				namelist.add(returnname);//將切割完的收件者添加進list裡
			}
		}
		return namelist;//回傳整理好的list
	}



	//關閉SMTP連結
    public void close() 
	{
		isConnected = false;
		try 
		{
		//關閉通訊 ReplyCode=221
	    	sendCommand("QUIT", 221);
	    	connection.close();
		} 
		catch (IOException e) 
		{
	    	System.out.println("Unable to close connection: " + e);
	    	isConnected = true;
		}
    }

	   //發送SMTP指令
    private void sendCommand(String command, int rc) throws IOException 
	{
		String reply = null;

		// 發送 SMTP 指令
		System.out.println("Comm:"+command);
		toServer.writeBytes(command + CRLF);
		if(rc != -1)
		{
			do
			{
				// 接收伺服器的回應訊息
				reply = fromServer.readLine();
				System.out.println("Server:"+reply);
				if(parseReply(reply) != rc)
				{
					throw new IOException("Error: " + reply);
				}
			}while(reply.startsWith(rc + "-"));
		}
    }
	//轉換成int tpye類型並提取前三位數
	private int parseReply(String reply) 
	{
		return Integer.parseInt(reply.substring(0, 3));
	}


    protected void finalize() throws Throwable 
	{
		if(isConnected) 
		{
	    	close();
		}
		super.finalize();
    }

	//撿查server驗證帳密
	private void authenticate(Envelope envelop,String password) throws IOException {
		try
		{
        	if ( password== null || password.isEmpty()) 
			{
            	System.out.println("No password provided, skipping authentication.");
            	return;
        	}

        	// 發送AUTH LOGIN 指令
        	sendCommand("AUTH LOGIN", 334);

        	// 發送使用者郵件地址
        	sendCommand(Base64.getEncoder().encodeToString(envelop.Sender.getBytes()), 334);

        	// 發送應用密碼 
        	sendCommand(Base64.getEncoder().encodeToString(password.getBytes()), 235);
		}
		catch(IOException e)
		{
			JOptionPane.showMessageDialog(null,"ERR: " + e,"ERROR",JOptionPane.ERROR_MESSAGE);
		}
    }

}