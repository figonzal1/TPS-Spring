package com.ts.apigateway.mensajeria;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.ts.apigateway.modelo.Categoria;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

@Component("mensajero")
public class MsgImpl implements MsgAdapter {

	private static final String QUEUE_NAME = "categoria_request";
	private static final Log LOGGER = LogFactory.getLog(MsgImpl.class);
	private ConnectionFactory factory;

	@Override
	public void send(Categoria categoria) {

		try {
			factory = setFactory();
			Connection connection = factory.newConnection();

			Channel channel = connection.createChannel();

			channel.queueDeclare(QUEUE_NAME, false, false, false, null);
			LOGGER.info("Creando queue: " + QUEUE_NAME);

			//byte[] data = SerializationUtils.serialize(categoria);
			byte[] data = (new Gson().toJson(categoria)).getBytes(StandardCharsets.UTF_8);

			channel.basicPublish("", QUEUE_NAME, null, data);
			LOGGER.info("[x] Enviando por queue: " + new Gson().toJson(categoria));

			channel.close();
			connection.close();

		} catch (NoSuchAlgorithmException | KeyManagementException | URISyntaxException | IOException | TimeoutException e) {
			e.printStackTrace();
		}

	}

	private ConnectionFactory setFactory() throws NoSuchAlgorithmException, KeyManagementException
			, URISyntaxException {

		ConnectionFactory factory = new ConnectionFactory();
		factory.setRequestedHeartbeat(30);
		factory.setUri(
				"amqp://xuueptgg:hYmOJdYsGPSSW-rvY_WSRXB1OK2YW8II@fox.rmq.cloudamqp.com/xuueptgg");

		return factory;
	}

}